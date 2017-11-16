/**
 * For licensing, see LICENSE.txt
 * @author Rein Krul
 */
package nl.avisi.kotlinwebtest.soap

import nl.avisi.kotlinwebtest.Endpoint
import nl.avisi.kotlinwebtest.ExecutionContext
import nl.avisi.kotlinwebtest.TestCase
import nl.avisi.kotlinwebtest.TestStep
import nl.avisi.kotlinwebtest.Validator
import nl.avisi.kotlinwebtest.ValidatorResult
import nl.avisi.kotlinwebtest.expressions.Expression
import nl.avisi.kotlinwebtest.expressions.ExpressionEvaluator
import nl.avisi.kotlinwebtest.xml.NamespaceDeclaration
import nl.avisi.kotlinwebtest.xml.evaluate
import nl.avisi.kotlinwebtest.xml.toXml
import org.slf4j.LoggerFactory
import org.w3c.dom.Node
import org.xmlunit.builder.DiffBuilder
import org.xmlunit.diff.Comparison
import org.xmlunit.diff.ComparisonResult
import org.xmlunit.diff.ComparisonType
import javax.activation.MimeType
import javax.xml.xpath.XPathException

class SoapFaultValidator(var mustContainSoapFault: Boolean) : Validator<SoapResponse> {
    override fun validate(executionContext: ExecutionContext, response: SoapResponse): ValidatorResult {
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

class XSDValidator : Validator<SoapResponse> {
    override fun validate(executionContext: ExecutionContext, response: SoapResponse): ValidatorResult {
        return success()
    }
}

class SoapResponseValidator : Validator<SoapResponse> {
    override fun validate(executionContext: ExecutionContext, response: SoapResponse): ValidatorResult {
        val contentType = response.http?.contentType ?: return failure("SOAP response is missing content type.")
        if (MimeType(contentType).baseType != "text/xml") return failure("Unexpected Content-Type in response: $contentType")
        return success()
    }
}

class SoapTestStep(testCase: TestCase) : TestStep<SoapRequest, SoapResponse>(testCase, SoapRequest()) {
    var endpoint: Endpoint? = null

    init {
        request.testStep = this
    }

    fun resolveUrl(configuration: SoapTestConfiguration): String? {
        return (endpoint ?: configuration.defaults.endpoint ?: return null).url.toString() + request.path
    }
}

class XPathValidator(val xpath: String, var value: Expression? = null) : Validator<SoapResponse> {

    companion object {
        private val log = LoggerFactory.getLogger(XPathValidator::class.java)
    }

    override fun validate(executionContext: ExecutionContext, response: SoapResponse): ValidatorResult {
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