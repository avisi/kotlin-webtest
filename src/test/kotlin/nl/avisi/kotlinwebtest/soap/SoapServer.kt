/**
 * For licensing, see LICENSE.txt
 * @author Rein Krul
 */
package nl.avisi.kotlinwebtest.soap

import org.apache.http.HttpEntityEnclosingRequest
import org.apache.http.HttpRequest
import org.apache.http.HttpResponse
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.entity.mime.FormBodyPartBuilder
import org.apache.http.entity.mime.HttpMultipartMode
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.StringBody
import org.apache.http.impl.bootstrap.HttpServer
import org.apache.http.impl.bootstrap.ServerBootstrap
import org.apache.http.message.BasicNameValuePair
import org.apache.http.protocol.HttpContext
import org.apache.http.protocol.HttpRequestHandler
import org.apache.http.util.EntityUtils
import org.slf4j.LoggerFactory

class SoapServer : AutoCloseable {
    val simpleResponseText: String = readAsString("soap/simple-soap-response.xml")
    val mtomResponseText: String = readAsString("soap/mtom-soap-response.xml")
    val port = 7543
    private val log = LoggerFactory.getLogger(SoapExecutor::class.java)
    private var server: HttpServer

    init {
        server = ServerBootstrap.bootstrap()
                .setListenerPort(port)
                .setExceptionLogger { log.error("An error occurred", it) }
                .registerHandler("/simple/*", SimpleHandler(simpleResponseText))
                .registerHandler("/mtom/*", MtomHandler(mtomResponseText))
                .create()
                .apply {
                    start()
                }
    }

    override fun close() {
        server.stop()
    }

    private inner class SimpleHandler(private val responseText: String) : HttpRequestHandler {

        override fun handle(request: HttpRequest, response: HttpResponse, context: HttpContext) {
            response.entity = StringEntity(responseText, ContentType.TEXT_XML.withCharset(Charsets.UTF_8))
        }
    }

    private inner class MtomHandler(private val responseText: String) : HttpRequestHandler {

        override fun handle(request: HttpRequest, response: HttpResponse, context: HttpContext) {
            if (request is HttpEntityEnclosingRequest) {
                EntityUtils.consumeQuietly(request.entity)
            }
            val rootContentId = "<root>"
            val attachmentId = "<test>"
            val multipart = ContentType.create("multipart/related").withParameters(BasicNameValuePair("type", MIMETYPE_SOAPXML),
                    BasicNameValuePair("start", rootContentId),
                    BasicNameValuePair("start-info", MIMETYPE_SOAPXML))
            val rootPart = FormBodyPartBuilder.create()
                    .setName("root")
                    .setBody(StringBody(responseText, ContentType.APPLICATION_XML))
                    .setField("Content-Transfer-Encoding", "binary")
                    .setField("Content-Type", ContentType.create("application/xop+xml").withCharset(Charsets.UTF_8).withParameters(BasicNameValuePair("type", MIMETYPE_SOAPXML)).toString())
                    .setField("Content-ID", rootContentId)
                    .build()
            val attachmentPart = FormBodyPartBuilder.create()
                    .setName("attachment")
                    .setBody(StringBody(simpleResponseText, ContentType.TEXT_HTML))
                    .setField("Content-Transfer-Encoding", "binary")
                    .setField("Content-Type", "application/xml")
                    .setField("Content-ID", attachmentId)
                    .build()
            val entity = MultipartEntityBuilder.create()
                    .setMode(HttpMultipartMode.STRICT) // TODO: Is this right?
                    .setContentType(multipart)
                    .addPart(rootPart)
                    .addPart(attachmentPart)
                    .build()
            response.entity = entity

        }
    }

    private fun readAsString(fileName: String) =
            Thread.currentThread().contextClassLoader.getResourceAsStream(fileName).reader().readText()
}