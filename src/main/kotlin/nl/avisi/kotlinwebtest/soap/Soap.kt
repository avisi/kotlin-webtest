/**
 * For licensing, see LICENSE.txt
 * @author Rein Krul
 */
package nl.avisi.kotlinwebtest.soap

import nl.avisi.kotlinwebtest.Endpoint
import nl.avisi.kotlinwebtest.ExecutionContext
import nl.avisi.kotlinwebtest.Executor
import nl.avisi.kotlinwebtest.StepRequest
import nl.avisi.kotlinwebtest.StepResponse
import nl.avisi.kotlinwebtest.http.HttpHeader
import nl.avisi.kotlinwebtest.http.HttpRequest
import nl.avisi.kotlinwebtest.http.HttpResponse
import nl.avisi.kotlinwebtest.http.HttpResponsePart
import nl.avisi.kotlinwebtest.http.HttpStepResponse
import nl.avisi.kotlinwebtest.http.MultipartHttpResponse
import nl.avisi.kotlinwebtest.http.getHttpClient
import nl.avisi.kotlinwebtest.http.parseMultipartResponse
import nl.avisi.kotlinwebtest.interpolateExpressions
import nl.avisi.kotlinwebtest.xml.NamespaceDeclaration
import nl.avisi.kotlinwebtest.xml.toDocument
import nl.avisi.kotlinwebtest.xml.toXml
import org.apache.commons.io.IOUtils
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ByteArrayEntity
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.message.BasicNameValuePair
import org.slf4j.LoggerFactory
import org.w3c.dom.Document
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern
import javax.activation.MimeType


val soapNamespace = NamespaceDeclaration("soap", "http://schemas.xmlsoap.org/soap/envelope/")
private val MAX_RESPONSE_LOG_LENGTH = 5000

class SoapStepRequest(var body: SoapRequestBody? = null) : StepRequest, HttpRequest() {
    lateinit var testStep: SoapTestStep
    var attachment: ByteArrayEntity? = null

    infix fun text(data: String) {
        body = RawSoapRequestBody(data)
    }

    override infix fun endpoint(endpoint: Endpoint) {
        testStep.endpoint = endpoint
    }

    infix fun file(fileName: String) {
        body = FileSoapRequestBody(fileName)
    }

    infix fun attachment(fileName: String) = ContentTypeBuilder(this, fileName)

    inner class ContentTypeBuilder(val testStep: SoapStepRequest, val fileName: String) {
        infix fun contentType(contentType: String) = ContentIdBuilder(testStep, fileName, contentType)
    }

    inner class ContentIdBuilder(val testStep: SoapStepRequest, val fileName: String, val contentType: String) {

        infix fun contentID(contentID: String) {
            multipartBuilder(testStep, fileName, contentType, contentID)
        }

        private fun multipartBuilder(testStep: SoapStepRequest, fileName: String, contentType: String, contentID: String) {
            try {
                val UUID = "--uuid:6a129943-007b-46dc-8cd5-7bcf2b8a47c7"
                val attachment = javaClass.classLoader.getResourceAsStream(fileName)
                var soapPart = IOUtils.toString(ContentIdBuilder::class.java.classLoader.getResourceAsStream("multipartTemplate.txt"))
                        .replace("@SOAP", testStep.body!!.data)
                        .replace("@FILENAME", contentID)
                        .replace("@CONTENT-TYPE", contentType)
                        .replace("@CONTENT-ID", contentID)
                        .replace("@UUID", UUID)
                val matcher = Pattern.compile(">cid:(.*?)<").matcher(soapPart)
                if (matcher.find() && matcher.group(1) == contentID) soapPart = soapPart.replace(Regex(">cid:.*.<"), "><inc:Include href=\"cid:${matcher.group(1)}\" xmlns:inc=\"http://www.w3.org/2004/08/xop/include\"/><")
                else error("Can not find given Content-ID")
                val content: ByteArray = soapPart.toByteArray() + readFully(attachment) + "$UUID--".toByteArray()
                testStep.attachment = ByteArrayEntity(content)
            } catch (e: Exception) {
                error("Multipart parsing failed.")
            }
        }

        @Throws(IOException::class)
        private fun readFully(stream: InputStream): ByteArray {
            val buffer = ByteArray(8192)
            val baos = ByteArrayOutputStream()
            var bytesRead = 0
            while (bytesRead != -1) {
                baos.write(buffer, 0, bytesRead)
                bytesRead = stream.read(buffer)
            }
            return baos.toByteArray()
        }
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

    companion object {
        private val log = LoggerFactory.getLogger(SoapExecutor::class.java)
    }

    override fun execute(step: SoapTestStep, executionContext: ExecutionContext): StepResponse {
        val stepName = if (step.name.isNullOrBlank()) step.javaClass.simpleName else "${step.name} (${step.javaClass.simpleName})"
        log.info("Step: $stepName")
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

        val url = step.resolveUrl(endpoint)
        log.info("Sending request:\n URL: {}\n Data: \n{}", url, requestData.shortText)
        var httpResponse: HttpResponse? = null
        try {
            getHttpClient(endpoint.credentials ?: request.credentials).use { client ->
                val httpRequest = HttpPost(url)
                configuration.defaults.headers.forEach { (name, value) -> httpRequest.setHeader(name, value) }
                request.headers.forEach { (name, value) -> httpRequest.setHeader(name, value) }
                if (request.attachment != null) {
                    httpRequest.setHeader("Content-Type", "multipart/related; type=\"application/xop+xml\"; start=\"<rootpart@soapui.org>\"; start-info=\"application/soap+xml\"; action=\"\"; boundary=\"uuid:6a129943-007b-46dc-8cd5-7bcf2b8a47c7\"")
                    httpRequest.setHeader("MIME-Version", "1.0")
                    httpRequest.entity = request.attachment
                } else httpRequest.entity = StringEntity(requestData, ContentType.TEXT_XML.withCharset(StandardCharsets.UTF_8))
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
            log.info("Response, headers: ${httpResponse?.headers}, \nbody:\n ${response.body?.data!!.shortText}")
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

    private val ByteArray.shortText: String
        get() = toXml(toDocument(String(this), false)).shortText

    private val String.shortText: String
        get() = this.substring(0, Math.min(MAX_RESPONSE_LOG_LENGTH, this.length)) + if (this.length > MAX_RESPONSE_LOG_LENGTH) "..." else ""
}

class SoapTestConfiguration(val defaults: SoapRequestDefaults = SoapRequestDefaults())

class SoapRequestDefaults(var endpoint: Endpoint? = null) : HttpRequest() {

    override fun endpoint(endpoint: Endpoint) {
        this.endpoint = endpoint
    }
}