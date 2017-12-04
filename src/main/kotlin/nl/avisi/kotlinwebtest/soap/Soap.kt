/**
 * For licensing, see LICENSE.txt
 * @author Rein Krul
 */
package nl.avisi.kotlinwebtest.soap

import nl.avisi.kotlinwebtest.*
import nl.avisi.kotlinwebtest.expressions.ExpressionEvaluator
import nl.avisi.kotlinwebtest.expressions.findExpressions
import nl.avisi.kotlinwebtest.http.*
import nl.avisi.kotlinwebtest.xml.NamespaceDeclaration
import nl.avisi.kotlinwebtest.xml.toDocument
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClients
import org.apache.http.message.BasicNameValuePair
import org.slf4j.LoggerFactory
import org.w3c.dom.Document
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.nio.charset.StandardCharsets

val soapNamespace = NamespaceDeclaration("soap", "http://schemas.xmlsoap.org/soap/envelope/")

class SoapRequest(var body: SoapRequestBody? = null) : StepRequest, HttpRequest() {
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

class SoapStepResponse(override val http: HttpResponse?,
                       val endpoint: Endpoint,
                       override val success: Boolean,
                       override val message: String? = null) : HttpStepResponse {

    val document: Document
        get() =
            http?.let {
                if (mtom) {
                    // TODO: What if this part is missing?
                    (it as MultipartHttpResponse).parts[0].data
                } else {
                    it.data
                }.let { toDocument(ByteArrayInputStream(it)) }
            } ?: error("Missing HTTP response.")

    val mtom: Boolean
        get() =
            http is MultipartHttpResponse
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

    override fun execute(step: SoapTestStep, executionContext: ExecutionContext): StepResponse {
        // TODO: using error() not very nice (throws a technical error), should be handled in a functional way instead.
        val request = step.request
        val requestData = (request.body?.data ?: error("No body configured for SOAP test step."))
                .let { interpolateExpressions(it, executionContext) }
        val configuration = executionContext.configuration[SoapTestConfiguration::class]
        val endpoint = step.resolveEndpoint(configuration) ?: error("No endpoint configured for SOAP test step.")

        fun buildError(response: HttpResponse?, exception: Throwable): SoapStepResponse =
                SoapStepResponse(response, endpoint, false, "SOAP request failed: ${exception.message}").also {
                    with(executionContext) {
                        previousRequest = request
                    }
                }

        log.info("Sending request: {}", requestData)
        var httpResponse: HttpResponse? = null
        try {
            getHttpClient().use { client ->
                val httpRequest = HttpPost(step.resolveUrl(endpoint))
                configuration.defaults.headers.forEach { (name, value) -> httpRequest.setHeader(name, value) }
                request.headers.forEach { (name, value) -> httpRequest.setHeader(name, value) }
                httpRequest.entity = StringEntity(requestData, ContentType.TEXT_XML.withCharset(StandardCharsets.UTF_8))
                val response = client.execute(httpRequest)
                val contentType = response.getFirstHeader("Content-Type")
                        ?.let { parseContentType(it.value) }
                        ?: error("Missing Content-Type header")
                response.use {
                    it.entity.content.use {
                        httpResponse =
                                if (contentType.mimeType == "multipart/related") {
                                    parseMultipartResponse(it, contentType, response)
                                } else {
                                    parseResponse(response, it)
                                }
                    }
                }
            }
        } catch (e: IOException) {
            log.error("SOAP request failed.", e)
            return buildError(httpResponse, e)
        }
        log.info("Response, headers: ${httpResponse?.headers}, body: ${httpResponse?.data}")

        return SoapStepResponse(httpResponse, endpoint, true).also {
            with(executionContext) {
                previousRequest = request
                previousResponse = it
            }
        }
    }

    private fun parseContentType(contentType: String): ContentType =
            // TODO: Charset should be parsed!
            javax.mail.internet.ContentType(contentType)
                    .let { ct ->
                        ContentType.create(ct.baseType)
                                .withParameters(*ct.parameterList.names
                                        .asSequence()
                                        .map { name -> BasicNameValuePair(name as String, ct.parameterList[name]) }
                                        .toList()
                                        .toTypedArray())
                    }


    private fun parseResponse(response: org.apache.http.HttpResponse, stream: InputStream) =
            HttpResponse(response.statusLine.statusCode, stream.readBytes(), response.allHeaders.map { HttpHeader(it.name, it.value) })

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