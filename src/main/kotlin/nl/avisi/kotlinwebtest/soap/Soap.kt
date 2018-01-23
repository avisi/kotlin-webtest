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
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.methods.HttpPost
import org.apache.http.conn.ssl.SSLConnectionSocketFactory
import org.apache.http.conn.ssl.TrustSelfSignedStrategy
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.client.HttpClients
import org.apache.http.message.BasicNameValuePair
import org.apache.http.ssl.SSLContextBuilder
import org.slf4j.LoggerFactory
import org.w3c.dom.Document
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.nio.charset.StandardCharsets
import javax.activation.MimeType
import javax.net.ssl.HostnameVerifier


val soapNamespace = NamespaceDeclaration("soap", "http://schemas.xmlsoap.org/soap/envelope/")
private val MAX_RESPONSE_LOG_LENGTH = 2000

class SoapStepRequest(var body: SoapRequestBody? = null) : StepRequest, HttpRequest() {
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
            body?.let { toDocument(ByteArrayInputStream(it.data)) }
                    ?: error("Missing HTTP response.")

    val body: HttpResponsePart?
        get() =
            http?.let {
                if (mtom) {
                    // TODO: What if this part is missing?
                    (it as MultipartHttpResponse).parts[0]
                } else {
                    it
                }
            }

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

        var url = step.resolveUrl(endpoint)
        log.info("Sending request:\n URL: {}\n Data: {}", url, requestData)
        var httpResponse: HttpResponse? = null
        try {
            getHttpClient(endpoint.credentials ?: request.credentials).use { client ->
                val httpRequest = HttpPost(url)
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

        return SoapStepResponse(httpResponse, endpoint, true).also { response ->
            log.info("Response, headers: ${httpResponse?.headers}, body: ${response.body?.data?.shortText}")
            with(executionContext) {
                previousRequest = request
                previousResponse = response
            }
        }
    }

    private fun parseContentType(contentType: String): ContentType =
            // TODO: Charset should be parsed!
            MimeType(contentType)
                    .let { ct ->
                        ContentType.create(ct.baseType)
                                .withParameters(*ct.parameters.names
                                        .asSequence()
                                        .map { name -> BasicNameValuePair(name as String, ct.parameters[name]) }
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

    private fun getHttpClient(credentials: Credentials?) =
            HttpClients.custom()
                    .setSSLSocketFactory(SSLConnectionSocketFactory(sslContext(), sslHostnameVerifier()))
                    .setDefaultCredentialsProvider(credentialsProvider(credentials))
                    .build()

    private fun credentialsProvider(credentials: Credentials?) =
            credentials?.let {
                BasicCredentialsProvider().apply {
                    setCredentials(AuthScope.ANY, mapCredentials(credentials))
                }
            }

    private fun mapCredentials(credentials: Credentials): UsernamePasswordCredentials =
            when (credentials) {
                is UsernamePassword -> UsernamePasswordCredentials(credentials.user, credentials.password)
                else -> error("Unsupported credential type: " + credentials)
            }

    private fun sslHostnameVerifier(): HostnameVerifier =
            HostnameVerifier { _, _ -> true }

    private fun sslContext() =
            SSLContextBuilder().loadTrustMaterial(null, TrustSelfSignedStrategy()).build()

    private val ByteArray.shortText: String
        get() =
            String(this, 0, Math.min(MAX_RESPONSE_LOG_LENGTH, size)) + if (size > MAX_RESPONSE_LOG_LENGTH) "..." else ""

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