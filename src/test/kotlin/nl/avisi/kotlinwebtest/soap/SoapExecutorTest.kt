package nl.avisi.kotlinwebtest.soap

import nl.avisi.kotlinwebtest.Endpoint
import nl.avisi.kotlinwebtest.ExecutionContext
import nl.avisi.kotlinwebtest.TestCase
import nl.avisi.kotlinwebtest.TestConfiguration
import nl.avisi.kotlinwebtest.xml.toXml
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
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.slf4j.LoggerFactory
import org.xmlunit.builder.DiffBuilder
import kotlin.test.assertFalse
import kotlin.test.assertTrue

const val MIMETYPE_SOAPXML = "application/soap+xml"

class SoapExecutorTest {

    private val log = LoggerFactory.getLogger(SoapExecutor::class.java)
    private val simpleResponseText: String = readResponse("soap/simple-soap-response.xml")
    private val port = 7543
    private lateinit var server: HttpServer

    @Before
    fun setUp() {
        server = ServerBootstrap.bootstrap()
                .setListenerPort(port)
                .setExceptionLogger { log.error("An error occurred", it) }
                .registerHandler("/simple/*", SimpleHandler(simpleResponseText))
                .registerHandler("/mtom/*", MtomHandler(simpleResponseText))
                .create()
                .apply {
                    start()
                }
    }

    @After
    fun tearDown() {
        server.stop()
    }

    @Test
    fun execute() {
        val step = SoapTestStep(TestCase("Multipart")).apply {
            endpoint = Endpoint("Endpoint", "http://localhost:$port/simple")
            request.text("foobar")
        }
        val response = SoapExecutor().execute(step, ExecutionContext(TestConfiguration())) as SoapStepResponse
        assertTrue { response.success }
        assertFalse {
            DiffBuilder.compare(simpleResponseText)
                    .withTest(response.document.toXml())
                    .ignoreComments()
                    .ignoreWhitespace()
                    .checkForSimilar()
                    .build()
                    .hasDifferences()
        }
    }

    @Test
    fun executeMtom() {
        val step = SoapTestStep(TestCase("Multipart")).apply {
            endpoint = Endpoint("Endpoint", "http://localhost:$port/mtom")
            request.text("foobar")
        }
        val response = SoapExecutor().execute(step, ExecutionContext(TestConfiguration())) as SoapStepResponse
        assertTrue { response.success }
        assertTrue { response.mtom }
        assertFalse {
            DiffBuilder.compare(simpleResponseText)
                    .withTest(response.document.toXml())
                    .ignoreComments()
                    .ignoreWhitespace()
                    .checkForSimilar()
                    .build()
                    .hasDifferences()
        }
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
            val entity = MultipartEntityBuilder.create()
                    .setMode(HttpMultipartMode.STRICT) // TODO: Is this right?
                    .setContentType(multipart)
                    .addPart(rootPart)
                    .build()
            response.entity = entity

        }
    }

    private fun readResponse(fileName: String) =
            Thread.currentThread().contextClassLoader.getResourceAsStream(fileName).reader().readText()
}