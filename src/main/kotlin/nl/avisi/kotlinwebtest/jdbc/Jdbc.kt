/**
 * For licensing, see LICENSE.txt
 * @author Martijn Heuvelink
 */
package nl.avisi.kotlinwebtest.jdbc

import nl.avisi.kotlinwebtest.ExecutionContext
import nl.avisi.kotlinwebtest.Executor
import nl.avisi.kotlinwebtest.StepResponse
import nl.avisi.kotlinwebtest.TestCase
import nl.avisi.kotlinwebtest.TestStep
import nl.avisi.kotlinwebtest.interpolateExpressions
import org.slf4j.LoggerFactory
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.SQLException
import java.util.ArrayList
import java.util.HashMap


class JdbcStepRequest : DatabaseRequest() {
    lateinit var testStep: JdbcTestStep

    infix fun query(query: String) {
        this.query = query
    }
}

class JdbcStepResponse(override val resultSet: List<HashMap<String, String>>,
                       override val success: Boolean,
                       override val message: String? = null) : DatabaseStepResponse

class JdbcTestStep(testCase: TestCase) : TestStep<JdbcStepRequest, JdbcStepResponse>(testCase, JdbcStepRequest()) {

    init {
        request.testStep = this
    }

    fun resolveUrl(configuration: JdbcTestConfiguration): String? =
            request.url ?: configuration.defaults.url

    fun resolveDriver(configuration: JdbcTestConfiguration): String? =
            request.driver ?: configuration.defaults.driver

}

class JdbcExecutor : Executor<JdbcTestStep> {

    companion object {
        private val log = LoggerFactory.getLogger(JdbcExecutor::class.java)
    }

    override fun execute(step: JdbcTestStep, executionContext: ExecutionContext): StepResponse {
        val stepName = if (step.name.isNullOrBlank()) step.javaClass.simpleName else "${step.name} (${step.javaClass.simpleName})"
        log.info("Step: $stepName")
        val query = step.request.query
                ?.let { interpolateExpressions(it, executionContext) }
                ?: error("No query configured for JDBC test step.")
        val configuration = executionContext.configuration[JdbcTestConfiguration::class]
        val url = step.resolveUrl(configuration) ?: error("No url configured for JDBC test step.")
        val driver = step.resolveDriver(configuration) ?: error("No driver configured for JDBC test step.")

        val result: List<HashMap<String, String>>
        try {
            Class.forName(driver)
            log.info("Query: {}", query)
            result = DriverManager.getConnection(url).use {
                it.createStatement().use {
                    resultSetToList(it.executeQuery(query))
                }
            }
            log.info("Response: {}", result)
        } catch (e: SQLException) {
            return jdbcFailure(e, executionContext, step)
        } catch (e: ClassNotFoundException) {
            return jdbcFailure(e, executionContext, step)
        } catch (e: Exception) {
            return jdbcFailure(e, executionContext, step)
        }
        return JdbcStepResponse(result, true).also {
            with(executionContext) {
                previousRequest = step.request
                previousResponse = it
            }
        }
    }

    private fun jdbcFailure(exception: Exception, executionContext: ExecutionContext, step: JdbcTestStep): JdbcStepResponse {
        log.error("JDBC request failed", exception)
        return JdbcStepResponse(emptyList(), false, "${exception.javaClass.simpleName}: ${exception.message}").also {
            with(executionContext) {
                previousRequest = step.request
            }
        }
    }

    private fun resultSetToList(rs: ResultSet): List<HashMap<String, String>> {
        val columnCount = rs.metaData.columnCount
        val rows = ArrayList<HashMap<String, String>>()
        while (rs.next()) {
            val row = HashMap<String, String>(columnCount)
            for (i in 1..columnCount) {
                row.put(rs.metaData.getColumnName(i), rs.getString(i))
            }
            rows.add(row)
        }
        return rows
    }
}

class JdbcTestConfiguration(val defaults: JdbcRequestDefaults = JdbcRequestDefaults())

class JdbcRequestDefaults : DatabaseRequest()