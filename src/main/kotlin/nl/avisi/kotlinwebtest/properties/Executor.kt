/**
 * For licensing, see LICENSE.txt
 * @author Rein Krul
 */
package nl.avisi.kotlinwebtest.properties

import nl.avisi.kotlinwebtest.ExecutionContext
import nl.avisi.kotlinwebtest.Executor
import nl.avisi.kotlinwebtest.Response
import nl.avisi.kotlinwebtest.expressions.ExpressionEvaluator
import org.slf4j.LoggerFactory

class PropertyExecutor : Executor<PropertyTestStep> {

    override fun execute(step: PropertyTestStep, executionContext: ExecutionContext): Response {
        val request = step.request ?: error("Request not configured.")
        val result = ExpressionEvaluator(executionContext).evaluate(request.expression)
        log.info("Evaluated property '${request.name}' to: $result")
        executionContext.properties[request.name] = result
        return PropertyResponse()
    }

    companion object {
        private val log = LoggerFactory.getLogger(PropertyExecutor::class.java)
    }
}