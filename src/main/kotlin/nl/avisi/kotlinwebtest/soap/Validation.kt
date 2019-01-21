/**
 * For licensing, see LICENSE.txt
 * @author Rein Krul
 */
package nl.avisi.kotlinwebtest.soap

import net.sf.saxon.Configuration
import net.sf.saxon.s9api.Processor
import nl.avisi.kotlinwebtest.Endpoint
import nl.avisi.kotlinwebtest.ExecutionContext
import nl.avisi.kotlinwebtest.TestCase
import nl.avisi.kotlinwebtest.TestStep
import nl.avisi.kotlinwebtest.Validator
import nl.avisi.kotlinwebtest.ValidatorResult
import nl.avisi.kotlinwebtest.expressions.Expression
import nl.avisi.kotlinwebtest.expressions.ExpressionEvaluator
import nl.avisi.kotlinwebtest.http.HttpHeaderValidator
import nl.avisi.kotlinwebtest.http.MultipartHttpResponse
import nl.avisi.kotlinwebtest.interpolateExpressions
import nl.avisi.kotlinwebtest.xml.ErrorValue
import nl.avisi.kotlinwebtest.xml.EvaluateResult
import nl.avisi.kotlinwebtest.xml.NamespaceDeclaration
import nl.avisi.kotlinwebtest.xml.NodeValue
import nl.avisi.kotlinwebtest.xml.NumberValue
import nl.avisi.kotlinwebtest.xml.XPathType
import nl.avisi.kotlinwebtest.xml.evaluate
import nl.avisi.kotlinwebtest.xml.fragmentToDocument
import nl.avisi.kotlinwebtest.xml.toDocument
import org.slf4j.LoggerFactory
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.SAXException
import org.xmlunit.builder.DiffBuilder
import org.xmlunit.diff.Comparison
import org.xmlunit.diff.ComparisonResult
import org.xmlunit.diff.ComparisonType
import java.io.StringReader
import java.net.URL
import java.text.SimpleDateFormat
import java.time.Duration
import java.util.Date
import java.util.regex.Pattern
import javax.activation.MimeType
import javax.xml.XMLConstants
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory

class SoapFaultValidator(var mustContainSoapFault: Boolean) : Validator<SoapStepRequest, SoapStepResponse> {
    override fun validate(executionContext: ExecutionContext, request: SoapStepRequest, response: SoapStepResponse): ValidatorResult {
        val nameSpaces = executionContext.configuration.xml.namespaces
        nameSpaces.add(soapNamespace)
        val fault = response.document.evaluate("//soap:Envelope/soap:Body/soap:Fault", nameSpaces)
        if (fault is ErrorValue) return failure("SoapFaultValidator failure: ${fault.value}")
        val hasSoapFault = (!fault!!.value.isNullOrBlank())
        return when {
            mustContainSoapFault and !hasSoapFault -> failure("A SOAP fault was expected, but not found.")
            !mustContainSoapFault and hasSoapFault -> failure("A SOAP fault was not expected, but one was found:\r\n " + fault.value)
            else -> success()
        }
    }
}

/**
 * TODO: This class should really validate using the service's WSDL
 */
class XSDValidator : Validator<SoapStepRequest, SoapStepResponse> {

    lateinit var SOAP: String

    fun getSoapUrl(executionContext: ExecutionContext): NamespaceDeclaration {
        return executionContext.configuration.xml.namespaces.find { it.prefix == "soap" } ?: error("Soap namespace is not defined")
    }

    private fun schemaFactory(): SchemaFactory =
        SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)

    private fun createSoapEnvelopeValidator(): javax.xml.validation.Validator =
        schemaFactory()
            .newSchema(URL(SOAP))
            .newValidator()

    private fun createSoapBodyValidator(sources: List<URL>): javax.xml.validation.Validator =
        schemaFactory()
            .newSchema(sources.map { StreamSource(it.toExternalForm()) }.toTypedArray())
            .newValidator()

    override fun validate(executionContext: ExecutionContext, request: SoapStepRequest, response: SoapStepResponse): ValidatorResult {
        SOAP = getSoapUrl(executionContext).namespace
        createSoapEnvelopeValidator().let { validator ->
            try {
                validator.validate(DOMSource(response.document))
            } catch (e: SAXException) {
                return failure("XSD validation failed: ${e.message}")
            }
        }

        createSoapBodyValidator(response.endpoint[Schemas::class].map { it.url }).let { validator ->
            val bodyNode = response.document.documentElement?.let {
                it.getElementsByTagNameNS(SOAP, "Body") ?: return failure("No 'Body' part found")
            } ?: return failure("Response can not be parsed as a Document")
            try {
                bodyNode.item(0).childNodes.forEach { validator.validate(DOMSource(it)) }
            } catch (e: SAXException) {
                return failure("XSD validation failed: ${e.message}")
            }
        }
        return success()
    }

    private fun NodeList.forEach(action: (Node) -> Unit) =
        (0 until this.length)
            .asSequence()
            .map { this.item(it) }
            .forEach { action(it) }
}

class SoapResponseValidator : Validator<SoapStepRequest, SoapStepResponse> {
    override fun validate(executionContext: ExecutionContext, request: SoapStepRequest, response: SoapStepResponse): ValidatorResult {
        val actualType = response.body?.contentType ?: return failure("SOAP response is missing content type.")
        val expectedType: String = if (response.mtom) "application/xop+xml" else "text/xml"
        if (MimeType(actualType).baseType != expectedType) return failure("Unexpected Content-Type in response, expected: $expectedType , actual: $actualType")
        return success()
    }
}

class SoapTestStep(testCase: TestCase) : TestStep<SoapStepRequest, SoapStepResponse>(testCase, SoapStepRequest()) {
    var endpoint: Endpoint? = null

    init {
        request.testStep = this
    }

    fun resolveUrl(endpoint: Endpoint): String =
        endpoint.url.toString() + request.path.orEmpty()

    fun resolveEndpoint(configuration: SoapTestConfiguration): Endpoint? =
        endpoint ?: configuration.defaults.endpoint
}

class XPathValidator(val xpath: String, var value: Expression? = null, val type: XPathType = XPathType.String, private val regex: Boolean = false) : Validator<SoapStepRequest, SoapStepResponse> {

    override fun validate(executionContext: ExecutionContext, request: SoapStepRequest, response: SoapStepResponse): ValidatorResult {
        val expectedValue = value.let {
            interpolateExpressions(value?.let { ExpressionEvaluator(executionContext).evaluate(it) }
                ?: return failure("XPathValidator is missing expected value or is empty"), executionContext)
        }
        val xpath = xpath.let { interpolateExpressions(it, executionContext) }
        return assertXpath(response, xpath, executionContext, type, expectedValue, regex)
    }
}

class ContainsValidator(val string: String) : Validator<SoapStepRequest, SoapStepResponse> {

    override fun validate(executionContext: ExecutionContext, request: SoapStepRequest, response: SoapStepResponse): ValidatorResult {
        val string = string.let { interpolateExpressions(it, executionContext) }
        if (String(response.body?.data!!).contains(string)) return success()
        return failure("Contains failure, expected '$string' but not found")
    }
}

class CompareTimeValidator(val first: String, val second: String, val interval: Int, val type: InequalityType) : Validator<SoapStepRequest, SoapStepResponse> {

    override fun validate(executionContext: ExecutionContext, request: SoapStepRequest, response: SoapStepResponse): ValidatorResult {
        val firstValue = convertXpathToTimestamp(first.let { interpolateExpressions(it, executionContext) }, executionContext, response)
        val secondValue = convertXpathToTimestamp(second.let { interpolateExpressions(it, executionContext) }, executionContext, response)
        if (firstValue == null || secondValue == null) return failure("One of the values can not be parsed")
        val dif = differenceBetweenDates(firstValue, secondValue)
        return when (type) {
            InequalityType.GreaterThan -> {
                if (dif > interval) success()
                else failure("The interval is greater than $dif")
            }
            InequalityType.LessThan -> {
                if (dif < interval) success()
                else failure("The interval is less than $dif")
            }
            InequalityType.Equals -> {
                if (dif == interval.toLong()) success()
                else failure("The interval is less than $dif")
            }
        }
    }

    private fun convertXpathToTimestamp(xpath: String, executionContext: ExecutionContext, response: SoapStepResponse): Date? {
        val result: EvaluateResult = response.document.evaluate(xpath.let { interpolateExpressions(it, executionContext) }, executionContext.configuration.xml.namespaces)
            ?: return null
        return if (result.value.isEmpty() || result is ErrorValue) {
            null
        } else {
            convertStringToTimestamp(result.value)
        }
    }

    private fun convertStringToTimestamp(date: String): Date {
        return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").parse(date)
    }

    fun differenceBetweenDates(date1: Date, date2: Date): Long {
        val dif = Duration.between(date1.toInstant(), date2.toInstant()).toMillis()
        return if (dif < 0) dif / -1
        else dif
    }
}

class AttachmentsCountValidator(private val expectedValue: Int) : Validator<SoapStepRequest, SoapStepResponse> {

    override fun validate(executionContext: ExecutionContext, request: SoapStepRequest, response: SoapStepResponse): ValidatorResult {
        return when {
            response.http is MultipartHttpResponse -> {
<<<<<<< Updated upstream
                response.http.parts.forEach {
                    it.headers.forEach { httpHeader ->
                        if (httpHeader.value.contains("attachment") || httpHeader.name.contains("Content-ID")) value++
                    }
=======
                return if ((response.http.parts.size - 1) == expectedValue) {
                    success()
                } else {
                    failure("Expected $expectedValue but found ${response.http.parts.size} attachment(s)")
>>>>>>> Stashed changes
                }
            }
            expectedValue == 0 -> success()
            else -> failure("Doesn't have a attachment")
        }
    }
}

class AttachmentSizeValidator(val size: Int, val attachment: Int, val type: InequalityType) : Validator<SoapStepRequest, SoapStepResponse> {

    override fun validate(executionContext: ExecutionContext, request: SoapStepRequest, response: SoapStepResponse): ValidatorResult {
        if (response.http is MultipartHttpResponse) {
            if (response.http.parts.size + 1 < attachment) return failure("out of index with attachments")
            return when (type) {
                InequalityType.GreaterThan -> {
                    if (response.http.parts[attachment + 1].data!!.size < size) return failure("size is ${response.http.parts[attachment + 1].data!!.size} instead of $size")
                    success()
                }
                InequalityType.LessThan -> {
                    if (response.http.parts[attachment + 1].data!!.size > size) return failure("size is ${response.http.parts[attachment + 1].data!!.size} instead of $size")
                    success()
                }
                InequalityType.Equals -> {
                    if (response.http.parts[attachment + 1].data!!.size != size) return failure("size is ${response.http.parts[attachment + 1].data!!.size} instead of $size")
                    success()
                }
            }
        } else
            return failure("Doesn't have a attachment")
    }
}

class AttachmentHeaderValidator(val header: String, val value: String, val attachment: Int) : Validator<SoapStepRequest, SoapStepResponse> {

    override fun validate(executionContext: ExecutionContext, request: SoapStepRequest, response: SoapStepResponse): ValidatorResult {
        return if (response.http is MultipartHttpResponse) {
            return HttpHeaderValidator(header, value).validateHeader(response.http.parts[attachment].headers)
        } else
            failure("Doesn't have a attachment")
    }
}

class XQueryValidator(private val xQuery: String, private val expectedValue: Expression) : Validator<SoapStepRequest, SoapStepResponse> {

    override fun validate(executionContext: ExecutionContext, request: SoapStepRequest, response: SoapStepResponse): ValidatorResult {
        val responseData = response.body?.dataAsString ?: return failure("There is no response data")
        val expectedValue = expectedValue.let {
            interpolateExpressions(expectedValue
                .let { ExpressionEvaluator(executionContext).evaluate(it) }
                ?: return failure("XQueryValidator is missing expected value or is empty"), executionContext)
        }
        val xQuery = xQuery.let { interpolateExpressions(it, executionContext) }
        val saxon = Processor(Configuration())
        val compiler = saxon.newXQueryCompiler()
        val exec = compiler.compile(xQuery)
        val builder = saxon.newDocumentBuilder()
        val src = StreamSource(StringReader(responseData))
        val query = exec.load()
        query.contextItem = builder.build(src)
        val comparisonResult = compare(expectedValue, toDocument((query.evaluate()
            ?: return failure(" There was a error")).toString()), executionContext.configuration.xml.namespaces)
        if (comparisonResult != null) {
            return failure("XQuery failure: $comparisonResult")
        }
        return success()
    }
}

enum class InequalityType {
    GreaterThan,
    LessThan,
    Equals
}

private fun compare(expected: String, actual: Node, namespaces: List<NamespaceDeclaration>): String? {
    return DiffBuilder.compare(fragmentToDocument(expected, namespaces))
        .withTest(actual)
        .ignoreComments()
        .ignoreWhitespace()
        .checkForSimilar()
        .withNamespaceContext(namespaces.associateBy({ it.prefix }, { it.namespace }))
        .withDifferenceEvaluator { comparison, outcome -> evaluateWildcards(comparison, outcome) }
        .build()
        .let { if (it.hasDifferences()) it.toString() else null }
}

private fun evaluateWildcards(comparison: Comparison, outcome: ComparisonResult): ComparisonResult {
    val expected = comparison.controlDetails.target?.nodeValue
    val actual = comparison.testDetails.target?.nodeValue
    if (!expected.isNullOrEmpty() && !actual.isNullOrEmpty() && expected!!.contains("*")) {
        return if (Pattern.matches(expected, actual)) ComparisonResult.EQUAL
        else outcome
    }
    return when {
        comparison.controlDetails.target?.nodeValue == "*" -> ComparisonResult.EQUAL
        comparison.type == ComparisonType.NAMESPACE_PREFIX -> ComparisonResult.EQUAL
        else -> outcome
    }
}

private fun Validator<*, *>.assertXpath(response: SoapStepResponse, xpath: String, executionContext: ExecutionContext, type: XPathType, expectedValue: String, regex: Boolean): ValidatorResult {
    val actualValue: EvaluateResult = response.document.evaluate(xpath, executionContext.configuration.xml.namespaces, type)
        ?: return failure("XPath failure, no match found for XPath: $xpath")
    try {
        when (actualValue) {
            is ErrorValue -> return failure("XPath failure: ${actualValue.value}")
            is NodeValue -> {
                val comparisonResult = compare(expectedValue, actualValue.node, executionContext.configuration.xml.namespaces)
                if (comparisonResult != null) {
                    return failure("XPath(Node) failure: $comparisonResult")
                }
            }
            is NumberValue -> {
                expectedValue.toDouble()
                    .takeIf { actualValue.number == it }
                    ?: return failure("XPath(Number) failure: \n XPath: $xpath\r\n Expected:\r\n ${expectedValue.toDoubleOrNull()}\r\n Actual:\r\n ${actualValue.number}")
            }
            else -> {
                if (regex) {
                    if (!actualValue.value.matches(Regex(expectedValue))) {
                        return failure("XPath failure: \n XPath: $xpath\r\n Expected:\r\n $expectedValue\r\n Actual:\r\n ${actualValue.value}")
                    }
                } else if (actualValue.value != expectedValue)
                    return failure("XPath failure: \n XPath: $xpath\r\n Expected:\r\n $expectedValue\r\n Actual:\r\n ${actualValue.value}")
            }
        }
    } catch (e: NumberFormatException) {
        LoggerFactory.getLogger(javaClass).warn("Cant cast: $xpath", e)
        return failure("Incorrect XPath: $xpath (${e.message})")
    } catch (e: Exception) {
        LoggerFactory.getLogger(javaClass).warn("Error compiling XPath: $xpath", e)
        return failure("Incorrect XPath: $xpath (${e.message})")
    }
    return success()
}