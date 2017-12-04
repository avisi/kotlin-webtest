/**
 * For licensing, see LICENSE.txt
 * @author Rein Krul
 */
package nl.avisi.kotlinwebtest.soap

import nl.avisi.kotlinwebtest.*
import nl.avisi.kotlinwebtest.expressions.Expression
import nl.avisi.kotlinwebtest.expressions.ExpressionEvaluator
import nl.avisi.kotlinwebtest.xml.NamespaceDeclaration
import nl.avisi.kotlinwebtest.xml.evaluate
import nl.avisi.kotlinwebtest.xml.toXml
import org.slf4j.LoggerFactory
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.SAXException
import org.xmlunit.builder.DiffBuilder
import org.xmlunit.diff.Comparison
import org.xmlunit.diff.ComparisonResult
import org.xmlunit.diff.ComparisonType
import java.net.URL
import javax.activation.MimeType
import javax.xml.XMLConstants
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory
import javax.xml.xpath.XPathException


class SoapFaultValidator(var mustContainSoapFault: Boolean) : Validator<SoapStepRequest, SoapStepResponse> {
    override fun validate(executionContext: ExecutionContext, request: SoapStepRequest, response: SoapStepResponse): ValidatorResult {
        val fault = response.document.evaluate("//soap:Envelope/soap:Body/soap:Fault", listOf(soapNamespace))
        val hasSoapFault = fault != null
        if (mustContainSoapFault and !hasSoapFault) {
            return failure("A SOAP fault was expected, but not found.")
        } else if (!mustContainSoapFault and hasSoapFault) {
            return failure("A SOAP fault was not expected, but one was found:\r\n " + toXml(fault!!))
        } else {
            return success()
        }
    }
}

/**
 * TODO: This class should really validate using the service's WSDL
 */
class XSDValidator : Validator<SoapStepRequest, SoapStepResponse> {
    private fun schemaFactory(): SchemaFactory =
            SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)

    private fun createSoapEnvelopeValidator(): javax.xml.validation.Validator =
            schemaFactory()
                    .newSchema(Thread.currentThread().contextClassLoader.getResource("soap/xsd/soap-1.1.xsd"))
                    .newValidator()

    private fun createSoapBodyValidator(sources: List<URL>): javax.xml.validation.Validator =
            schemaFactory()
                    .newSchema(sources.map { StreamSource(it.toExternalForm()) }.toTypedArray())
                    .newValidator()

    override fun validate(executionContext: ExecutionContext, request: SoapStepRequest, response: SoapStepResponse): ValidatorResult {
        createSoapEnvelopeValidator().let { validator ->
            try {
                validator.validate(DOMSource(response.document))
            } catch (e: SAXException) {
                return failure("XSD validation failed: ${e.message}")
            }
        }

        createSoapBodyValidator(response.endpoint[Schemas::class].map { it.url }).let { validator ->
            // TODO: Make this nicer
            val bodyNode = response.document.documentElement.getElementsByTagNameNS("http://schemas.xmlsoap.org/soap/envelope/", "Body")
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
        if (MimeType(actualType).baseType != expectedType) return failure("Unexpected Content-Type in response, expected: $expectedType: $actualType")
        return success()
    }
}

class SoapTestStep(testCase: TestCase) : TestStep<SoapStepRequest, SoapStepResponse>(testCase, SoapStepRequest()) {
    var endpoint: Endpoint? = null

    init {
        request.testStep = this
    }

    fun resolveUrl(endpoint: Endpoint): String =
            endpoint.url.toString() + request.path

    fun resolveEndpoint(configuration: SoapTestConfiguration): Endpoint? =
            endpoint ?: configuration.defaults.endpoint
}

class XPathValidator(val xpath: String, var value: Expression? = null) : Validator<SoapStepRequest, SoapStepResponse> {

    companion object {
        private val log = LoggerFactory.getLogger(XPathValidator::class.java)
    }

    override fun validate(executionContext: ExecutionContext, request: SoapStepRequest, response: SoapStepResponse): ValidatorResult {
        val expectedValue = value?.let { ExpressionEvaluator(executionContext).evaluate(it) } ?: error("XPathValidator is missing expected value")

        val actualValue: Node?
        try {
            actualValue = response.document.evaluate(xpath, executionContext.configuration.xml.namespaces)
        } catch (e: XPathException) {
            log.warn("Error compiling XPath: $xpath", e)
            return failure("Incorrect XPath: $xpath (${e.message})")
        }
        if (actualValue == null) {
            return failure("XPath failure, no match found for XPath: $xpath")
        } else {
            val comparisonResult = compare(expectedValue, actualValue, executionContext.configuration.xml.namespaces)
            if (comparisonResult != null) {
                val actualXml = toXml(actualValue)
                return failure("XPath failure: $comparisonResult,\r\n XPath: $xpath\r\n Expected:\r\n $expectedValue\r\n Actual:\r\n $actualXml")
            }
        }

        return success()
    }

    private fun compare(expected: String, actual: Node, namespaces: List<NamespaceDeclaration>): String? =
            DiffBuilder.compare(expected)
                    .withTest(actual)
                    .ignoreComments()
                    .ignoreWhitespace()
                    .checkForSimilar()
                    .withNamespaceContext(namespaces.associateBy({ it.prefix }, { it.namespace }))
                    .withDifferenceEvaluator { comparison, outcome -> evaluateWildcards(comparison, outcome) }
                    .build()
                    .let { if (it.hasDifferences()) it.toString() else null }

    private fun evaluateWildcards(comparison: Comparison, outcome: ComparisonResult): ComparisonResult =
            when {
                comparison.controlDetails.target?.nodeValue == "*" -> ComparisonResult.EQUAL
                comparison.type == ComparisonType.NAMESPACE_PREFIX -> ComparisonResult.EQUAL
                else -> outcome
            }
}