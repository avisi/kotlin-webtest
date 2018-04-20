package nl.avisi.kotlinwebtest.jdbc

import nl.avisi.kotlinwebtest.ExecutionContext
import nl.avisi.kotlinwebtest.TestCase
import nl.avisi.kotlinwebtest.TestConfiguration
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JDBCExecutorTest {

    private lateinit var server: JdbcServer

    @Before
    fun setUp() {
        server = JdbcServer()
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun execute() {
        val step = JdbcTestStep(TestCase("Jdbc")).apply {
            request.driver("org.h2.Driver")
            request.query("select * from cars")
            request.url("jdbc:h2:tcp://localhost/~/database")
        }
        val response = JdbcExecutor().execute(step, ExecutionContext(TestConfiguration())) as JdbcStepResponse
        assertTrue(response.success)
    }

    @Test(expected = IllegalStateException::class)
    fun executeNoDriver() {
        val step = JdbcTestStep(TestCase("Jdbc")).apply {
            request.query("select * from cars")
            request.url("jdbc:h2:tcp://localhost/~/database")
        }
        val response = JdbcExecutor().execute(step, ExecutionContext(TestConfiguration())) as JdbcStepResponse
        assertFalse(response.success)
    }

    @Test(expected = IllegalStateException::class)
    fun executeNoQuery() {
        val step = JdbcTestStep(TestCase("Jdbc")).apply {
            request.driver("org.h2.Driver")
            request.url("jdbc:h2:tcp://localhost/~/database")
        }
        val response = JdbcExecutor().execute(step, ExecutionContext(TestConfiguration())) as JdbcStepResponse
        assertFalse(response.success)
    }

    @Test(expected = IllegalStateException::class)
    fun executeNoUrl() {
        val step = JdbcTestStep(TestCase("Jdbc")).apply {
            request.driver("org.h2.Driver")
            request.query("select * from cars")
        }
        val response = JdbcExecutor().execute(step, ExecutionContext(TestConfiguration())) as JdbcStepResponse
        assertFalse(response.success)
    }

    @Test
    fun executeEmptyQuery() {
        val step = JdbcTestStep(TestCase("Jdbc")).apply {
            request.driver("org.h2.Driver")
            request.query("")
            request.url("jdbc:h2:tcp://localhost/~/database")

        }
        val response = JdbcExecutor().execute(step, ExecutionContext(TestConfiguration())) as JdbcStepResponse
        assertFalse(response.success)
    }

    @Test
    fun executeSQLGrammerError() {
        val step = JdbcTestStep(TestCase("Jdbc")).apply {
            request.driver("org.h2.Driver")
            request.query("Foute query")
            request.url("jdbc:h2:tcp://localhost/~/database")

        }
        val response = JdbcExecutor().execute(step, ExecutionContext(TestConfiguration())) as JdbcStepResponse
        assertFalse(response.success)
    }

    @Test
    fun executeDriverNotFound() {
        val step = JdbcTestStep(TestCase("Jdbc")).apply {
            request.driver("Foute driver")
            request.query("select * from cars")
            request.url("jdbc:h2:tcp://localhost/~/database")
        }
        val response = JdbcExecutor().execute(step, ExecutionContext(TestConfiguration())) as JdbcStepResponse
        assertFalse(response.success)
    }
}
