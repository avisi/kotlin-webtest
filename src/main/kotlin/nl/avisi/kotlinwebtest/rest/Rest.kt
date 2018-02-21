/**
 * For licensing, see LICENSE.txt
 * @author Rein Krul
 */
package nl.avisi.kotlinwebtest.rest

import nl.avisi.kotlinwebtest.Endpoint
import nl.avisi.kotlinwebtest.ExecutionContext
import nl.avisi.kotlinwebtest.Executor
import nl.avisi.kotlinwebtest.StepResponse
import nl.avisi.kotlinwebtest.TestCase
import nl.avisi.kotlinwebtest.TestStep
import nl.avisi.kotlinwebtest.http.HttpHeader
import nl.avisi.kotlinwebtest.http.HttpMethod
import nl.avisi.kotlinwebtest.http.HttpRequest
import nl.avisi.kotlinwebtest.http.HttpResponse
import nl.avisi.kotlinwebtest.http.HttpStepResponse
import nl.avisi.kotlinwebtest.http.getHttpClient
import nl.avisi.kotlinwebtest.interpolateExpressions
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.entity.FileEntity
import org.apache.http.entity.StringEntity
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets

class RestStepRequest(var body: String = "", var file: File? = null) : HttpRequest() {

    lateinit var testStep: RestTestStep

    infix fun text(data: String) {
        body = data
    }

    infix fun attachment(data: String) {
        file = File((Thread.currentThread().contextClassLoader.getResource(data) ?: error("File can not be found")).file)
    }

    override infix fun endpoint(endpoint: Endpoint) {
        testStep.endpoint = endpoint
    }
}

class RestStepResponse(override val http: HttpResponse?,
                       override val success: Boolean,
                       override val message: String? = null) : HttpStepResponse {
    val body: String?
        get() =
            http?.dataAsString
}

class RestTestStep(testCase: TestCase) : TestStep<RestStepRequest, RestStepResponse>(testCase, RestStepRequest()) {
    var endpoint: Endpoint? = null

    init {
        request.testStep = this
    }

    fun resolveUrl(endpoint: Endpoint): String? {
        return endpoint.url.toString() + request.path
    }

    fun resolveEndpoint(configuration: RestTestConfiguration): Endpoint? =
            endpoint ?: configuration.defaults.endpoint
}

class RestExecutor : Executor<RestTestStep> {

    companion object {
        private val log = LoggerFactory.getLogger(RestExecutor::class.java)
    }

    override fun execute(step: RestTestStep, executionContext: ExecutionContext): StepResponse {
        val stepName = if (step.name.isNullOrBlank()) step.javaClass.simpleName else "${step.name} (${step.javaClass.simpleName})"
        log.info("Step: $stepName")
        val request = step.request
        val requestData: String? = request.body.let { interpolateExpressions(it, executionContext) }
        val configuration = executionContext.configuration[RestTestConfiguration::class]
        val endpoint = step.resolveEndpoint(configuration) ?: error("No endpoint configured for REST test step.")

        val url = step.resolveUrl(endpoint)?.let { interpolateExpressions(it, executionContext) }
        var httpResponse: HttpResponse? = null
        try {
            getHttpClient(endpoint.credentials ?: request.credentials).use { it ->
                val httpRequest = when (request.method) {
                    HttpMethod.GET -> HttpGet(url)
                    HttpMethod.POST -> HttpPost(url).also {
                        if (request.file == null) {
                            requestData ?: error("No body configured for REST test step.")
                            it.entity = StringEntity(requestData, ContentType.APPLICATION_JSON.withCharset(StandardCharsets.UTF_8))
                        } else {
                            it.entity = FileEntity(request.file)
                        }
                    }
                    else -> error("Request method not supported: ${request.method}")
                }
                configuration.defaults.headers.forEach { (name, value) -> httpRequest.setHeader(name, value) }
                request.headers.forEach { (name, value) -> httpRequest.setHeader(name, value) }
                log.info("Sending {} request to {}{}", httpRequest.method, httpRequest.uri, requestData?.let { " with data: $it" } ?: "")
                val response = it.execute(httpRequest)
                httpResponse = response.use {
                    val responseBody = response.entity?.let { it.content.use { it.readBytes() } }
                    HttpResponse(
                            statusCode = response.statusLine.statusCode,
                            data = responseBody,
                            headers = response.allHeaders.map { HttpHeader(it.name, it.value) })
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
        val responseAsString = httpResponse?.data?.let { String(it) }
        when (responseAsString) {
            null -> log.info("No response.")
            else -> log.info("Response: $responseAsString")
        }
        return RestStepResponse(httpResponse, true).also {
            with(executionContext) {
                previousRequest = request
                previousResponse = it
            }
        }
    }
}

class RestTestConfiguration(val defaults: RestRequestDefaults = RestRequestDefaults())

class RestRequestDefaults(var endpoint: Endpoint? = null) : HttpRequest() {

    override fun endpoint(endpoint: Endpoint) {
        this.endpoint = endpoint
    }
}
