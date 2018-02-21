/**
 * For licensing, see LICENSE.txt
 * @author Martijn Heuvelink
 */
package nl.avisi.kotlinwebtest.soap

import nl.avisi.kotlinwebtest.After
import nl.avisi.kotlinwebtest.AfterResult
import nl.avisi.kotlinwebtest.ExecutionContext
import nl.avisi.kotlinwebtest.Source
import nl.avisi.kotlinwebtest.expressions.PropertyExpression
import nl.avisi.kotlinwebtest.interpolateExpressions
import nl.avisi.kotlinwebtest.xml.EvaluateResult
import nl.avisi.kotlinwebtest.xml.evaluate
import nl.avisi.kotlinwebtest.xml.toDocument
import org.slf4j.LoggerFactory


class XPathValue(val step: SoapTestStep, val xpath: String, private var prop: PropertyExpression, private val source: Source) : After<SoapStepRequest, SoapStepResponse> {

    companion object {
        private val log = LoggerFactory.getLogger(XPathValue::class.java)
    }

    override fun afterwards(executionContext: ExecutionContext, request: SoapStepRequest, response: SoapStepResponse): AfterResult {
        val result: EvaluateResult = when (source) {
            Source.RESPONSE -> response.document.evaluate(xpath, executionContext.configuration.xml.namespaces)
            Source.REQUEST -> {
                val requestString = request.body?.data.let { interpolateExpressions(it!!, executionContext) }
                val requestDoc = toDocument(requestString)
                requestDoc.evaluate(xpath, executionContext.configuration.xml.namespaces)
            }
        } ?: return failure("value is null with xpath:\n $xpath")
        executionContext.configuration.properties[prop.name] = result.value
        log.info("Assigned '${result.value}' to property ${prop.name}")
        return success()
    }
}