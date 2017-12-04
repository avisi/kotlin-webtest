/**
 * For licensing, see LICENSE.txt
 * @author Rein Krul
 */
package nl.avisi.kotlinwebtest.rest

import nl.avisi.kotlinwebtest.*
import nl.avisi.kotlinwebtest.expressions.ExpressionEvaluator
import nl.avisi.kotlinwebtest.expressions.findExpressions
import nl.avisi.kotlinwebtest.http.HttpHeader
import nl.avisi.kotlinwebtest.http.HttpRequest
import nl.avisi.kotlinwebtest.http.HttpResponse
import nl.avisi.kotlinwebtest.http.HttpStepResponse
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClients
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.charset.StandardCharsets

class RestRequest(var body: String? = null) : StepRequest, HttpRequest() {

    lateinit var testStep: RestTestStep

    infix fun text(data: String) {
        body = data
    }

    override infix fun endpoint(endpoint: Endpoint) {
        testStep.endpoint = endpoint
    }
}

class RestStepResponse(override val http: HttpResponse?,
                       override val success: Boolean,
                       override val message: String? = null) : HttpStepResponse {
}

class RestTestStep(testCase: TestCase) : TestStep<RestRequest, RestStepResponse>(testCase, RestRequest()) {
    var endpoint: Endpoint? = null

    init {
        request.testStep = this
    }

    fun resolveUrl(configuration: RestTestConfiguration): String? {
        return (endpoint ?: configuration.defaults.endpoint ?: return null).url.toString() + request.path
    }

}

class RestExecutor : Executor<RestTestStep> {

    override fun execute(step: RestTestStep, executionContext: ExecutionContext): StepResponse {
        val request = step.request
        val requestData = (request.body ?: error("No body configured for REST test step."))
                .let { interpolateExpressions(it, executionContext) }
        val configuration = executionContext.configuration[RestTestConfiguration::class]

        var httpResponse: HttpResponse? = null
        try {
            getHttpClient().use {
                val httpRequest = HttpPost(step.resolveUrl(configuration))
                configuration.defaults.headers.forEach { (name, value) -> httpRequest.setHeader(name, value) }
                request.headers.forEach { (name, value) -> httpRequest.setHeader(name, value) }
                log.info("Sending request to {}: {}", httpRequest.uri, requestData)
                httpRequest.entity = StringEntity(requestData, ContentType.APPLICATION_JSON.withCharset(StandardCharsets.UTF_8))
                val response = it.execute(httpRequest)
                response.use {
                    response.entity.content.use {
                        httpResponse = HttpResponse(
                                statusCode = response.statusLine.statusCode,
                                data = it.readBytes(),
                                headers = response.allHeaders.map { HttpHeader(it.name, it.value) })
                    }
                }
            }
        } catch (e: IOException) {
            log.error("REST request failed:", e)
            return RestStepResponse(httpResponse, false, "REST request failed: ${e.message}").also {
                with(executionContext) {
                    previousRequest = request
                }
            }
        }
        log.info("Response: ${httpResponse?.data}")
        return RestStepResponse(httpResponse, true).also {
            with(executionContext) {
                previousRequest = request
                previousResponse = it
            }
        }
    }

    private fun interpolateExpressions(text: String, executionContext: ExecutionContext): String {
        val evaluator = ExpressionEvaluator(executionContext)
        var interpolatedRequestData = text
        findExpressions(text).forEach {
            val (token, expression) = it
            val value = evaluator.evaluate(expression) ?: "".also { log.warn("Property evaluated to empty string: $token") }
            interpolatedRequestData = interpolatedRequestData.replace(token, value)
        }
        return interpolatedRequestData
    }

    private fun getHttpClient() =
            HttpClients.createDefault()

    companion object {
        private val log = LoggerFactory.getLogger(RestExecutor::class.java)
    }
}

class RestTestConfiguration(val defaults: RestRequestDefaults = RestRequestDefaults())

class RestRequestDefaults(var endpoint: Endpoint? = null) : HttpRequest() {

    override fun endpoint(endpoint: Endpoint) {
        this.endpoint = endpoint
    }
}
