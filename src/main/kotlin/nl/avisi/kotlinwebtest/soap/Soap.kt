/**
 * For licensing, see LICENSE.txt
 * @author Rein Krul
 */
package nl.avisi.kotlinwebtest.soap

import nl.avisi.kotlinwebtest.Endpoint
import nl.avisi.kotlinwebtest.ExecutionContext
import nl.avisi.kotlinwebtest.Executor
import nl.avisi.kotlinwebtest.Request
import nl.avisi.kotlinwebtest.Response
import nl.avisi.kotlinwebtest.expressions.ExpressionEvaluator
import nl.avisi.kotlinwebtest.expressions.findExpressions
import nl.avisi.kotlinwebtest.http.HttpHeader
import nl.avisi.kotlinwebtest.http.HttpRequest
import nl.avisi.kotlinwebtest.http.HttpResponse
import nl.avisi.kotlinwebtest.http.ReceivedHttpResponse
import nl.avisi.kotlinwebtest.xml.NamespaceDeclaration
import nl.avisi.kotlinwebtest.xml.asPrettyXml
import nl.avisi.kotlinwebtest.xml.toDocument
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClients
import org.slf4j.LoggerFactory
import org.w3c.dom.Document
import org.xml.sax.SAXException
import java.io.IOException
import java.net.URL
import java.nio.charset.StandardCharsets

val soapNamespace = NamespaceDeclaration("soap", "http://schemas.xmlsoap.org/soap/envelope/")

class SoapRequest(var body: SoapRequestBody? = null) : Request, HttpRequest() {
    lateinit var testStep: SoapTestStep

    infix fun text(data: String) {
        body = RawSoapRequestBody(data)
    }

    override infix fun endpoint(endpoint: Endpoint) {
        testStep.endpoint = endpoint
    }

    infix fun file(fileName: String) {
        body = FileSoapRequestBody(fileName)
    }
}

class SoapResponse(override val http: ReceivedHttpResponse?,
                   override val success: Boolean,
                   override val message: String? = null) : HttpResponse {

    val document: Document by lazy {
        if (http == null) error("Missing HTTP response.")
        toDocument(http.data)
    }
}

interface SoapRequestBody {
    val data: String
}

class FileSoapRequestBody(private val file: String) : SoapRequestBody {
    override val data: String
        get() = resolveFile(file).openStream().reader().readText()

    private fun resolveFile(file: String): URL =
            Thread.currentThread().contextClassLoader.getResource(file + ".xml") ?: error("File not found: $file")
}

class RawSoapRequestBody(override val data: String) : SoapRequestBody


class SoapExecutor : Executor<SoapTestStep> {

    override fun execute(step: SoapTestStep, executionContext: ExecutionContext): Response {
        val request = step.request
        val requestData = (request.body?.data ?: error("No body configured for SOAP test step."))
                .let { interpolateExpressions(it, executionContext) }
        val configuration = executionContext.configuration[SoapTestConfiguration::class]

        fun buildError(response: ReceivedHttpResponse?, exception: Throwable): SoapResponse =
                SoapResponse(response, false, "SOAP request failed: ${exception.message}").also {
                    with(executionContext) {
                        previousRequest = request
                    }
                }

        log.info("Sending request: {}", requestData)
        var httpResponse: ReceivedHttpResponse? = null
        try {
            getHttpClient().use {
                val httpRequest = HttpPost(step.resolveUrl(configuration))
                configuration.defaults.headers.forEach { (name, value) -> httpRequest.setHeader(name, value) }
                request.headers.forEach { (name, value) -> httpRequest.setHeader(name, value) }
                httpRequest.entity = StringEntity(requestData, ContentType.TEXT_XML.withCharset(StandardCharsets.UTF_8))
                val response = it.execute(httpRequest)
                response.use {
                    response.entity.content.use {
                        httpResponse = ReceivedHttpResponse(
                                statusCode = response.statusLine.statusCode,
                                data = it.reader().readText(),
                                headers = response.allHeaders.map { HttpHeader(it.name, it.value) })
                    }
                }
            }
        } catch (e: IOException) {
            log.error("SOAP request failed.", e)
            return buildError(httpResponse, e)
        }
        try {
            log.info("Response: ${httpResponse?.data?.asPrettyXml()}")
        } catch (e: SAXException) {
            log.error("Invalid SOAP response: ${httpResponse?.data}", e)
            return buildError(httpResponse, e)
        }

        return SoapResponse(httpResponse, true).also {
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
        private val log = LoggerFactory.getLogger(SoapExecutor::class.java)
    }
}

class SoapTestConfiguration(val defaults: SoapRequestDefaults = SoapRequestDefaults())

class SoapRequestDefaults(var endpoint: Endpoint? = null) : HttpRequest() {

    override fun endpoint(endpoint: Endpoint) {
        this.endpoint = endpoint
    }
}