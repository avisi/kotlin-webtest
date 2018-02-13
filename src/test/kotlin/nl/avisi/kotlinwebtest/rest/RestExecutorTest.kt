package nl.avisi.kotlinwebtest.rest

import nl.avisi.kotlinwebtest.Endpoint
import nl.avisi.kotlinwebtest.ExecutionContext
import nl.avisi.kotlinwebtest.TestCase
import nl.avisi.kotlinwebtest.TestConfiguration
import nl.avisi.kotlinwebtest.http.HttpMethod
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RestExecutorTest {
    companion object {
        private lateinit var server: RestServer
    }

    @Before
    fun setUp() {
        server = RestServer()
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun executeGet() {
        val step = RestTestStep(TestCase("Json")).apply {
            endpoint = Endpoint("Rest", "http://localhost:${server.port}/rest")
            request.method(HttpMethod.GET)
            request.path("/")
        }
        val response = RestExecutor().execute(step, ExecutionContext(TestConfiguration())) as RestStepResponse
        assertTrue { response.success }
        JSONAssert.assertEquals(server.body, response.body,  JSONCompareMode.STRICT)
    }

    @Test
    fun executeNoContent() {
        val step = RestTestStep(TestCase("Json")).apply {
            endpoint = Endpoint("Rest", "http://localhost:${server.port}/no-content")
            request.method(HttpMethod.GET)
            request.path("/")
        }
        val response = RestExecutor().execute(step, ExecutionContext(TestConfiguration())) as RestStepResponse
        assertTrue { response.success }
        assertNull(response.body)
    }

    @Test
    fun executePost() {
        val step = RestTestStep(TestCase("Json")).apply {
            endpoint = Endpoint("Rest", "http://localhost:${server.port}/rest")
            request.method(HttpMethod.POST)
            request.text("foobar")
            request.path("/")
        }
        val response = RestExecutor().execute(step, ExecutionContext(TestConfiguration())) as RestStepResponse
        assertTrue { response.success }
        JSONAssert.assertEquals(server.body, response.body,  JSONCompareMode.STRICT)
    }
}