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
import org.junit.After
import org.junit.Before
import org.junit.Test
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
            request.path("/")
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
            request.path("/")
            request.text("foobar")
        }
        val response = SoapExecutor().execute(step, ExecutionContext(TestConfiguration())) as SoapStepResponse
        assertTrue { response.success }
        assertTrue { response.mtom }
        assertFalse {
            DiffBuilder.compare(server.mtomResponseText)
                    .withTest(response.document.toXml())
                    .ignoreComments()
                    .ignoreWhitespace()
                    .checkForSimilar()
                    .build()
                    .hasDifferences()
        }
    }

}