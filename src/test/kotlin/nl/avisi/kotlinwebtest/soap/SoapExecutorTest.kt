/**
 * For licensing, see LICENSE.txt
 * @author Rein Krul
 */
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

    private lateinit var server: SoapServer

    @Before
    fun setUp() {
        server = SoapServer()
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun execute() {
        val step = SoapTestStep(TestCase("Multipart")).apply {
            endpoint = Endpoint("Endpoint", "http://localhost:${server.port}/simple")
            request.text("foobar")
        }
        val response = SoapExecutor().execute(step, ExecutionContext(TestConfiguration())) as SoapStepResponse
        assertTrue { response.success }
        assertFalse {
            DiffBuilder.compare(server.simpleResponseText)
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
            endpoint = Endpoint("Endpoint", "http://localhost:${server.port}/mtom")
            request.text("foobar")
        }
        val response = SoapExecutor().execute(step, ExecutionContext(TestConfiguration())) as SoapStepResponse
        assertTrue { response.success }
        assertTrue { response.mtom }
        assertFalse {
            DiffBuilder.compare(server.simpleResponseText)
                    .withTest(response.document.toXml())
                    .ignoreComments()
                    .ignoreWhitespace()
                    .checkForSimilar()
                    .build()
                    .hasDifferences()
        }
    }

}