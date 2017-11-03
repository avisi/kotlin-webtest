package nl.reikrul.kosote.properties

import nl.reikrul.kosote.ExecutionContext
import nl.reikrul.kosote.Executor
import nl.reikrul.kosote.Response
import nl.reikrul.kosote.expressions.ExpressionEvaluator
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