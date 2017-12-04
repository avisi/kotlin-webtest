/**
 * For licensing, see LICENSE.txt
 * @author Rein Krul
 */
package nl.avisi.kotlinwebtest

import nl.avisi.kotlinwebtest.properties.PropertyExecutor
import nl.avisi.kotlinwebtest.properties.PropertyTestStep
import nl.avisi.kotlinwebtest.rest.RestExecutor
import nl.avisi.kotlinwebtest.rest.RestTestStep
import nl.avisi.kotlinwebtest.soap.SoapExecutor
import nl.avisi.kotlinwebtest.soap.SoapTestStep
import org.junit.Assert.assertTrue
import org.junit.Before
import org.slf4j.LoggerFactory
import java.io.StringWriter

abstract class KosoteTest {

    val testConfiguration = TestConfiguration()
    private val project = Project(formatName())
    private val executors: MutableMap<Class<*>, Executor<*>> = mutableMapOf()

    @Before
    fun initializeTestEngine() {
        with(executors) {
            put(SoapTestStep::class.java, SoapExecutor())
            put(PropertyTestStep::class.java, PropertyExecutor())
            put(RestTestStep::class.java, RestExecutor())
        }
        configure()
    }

    /**
     * Override this function if you want to configure your test.
     */
    open fun configure() {
    }

    // Executing
    fun execute() {
        log.info("Executing ${project.name}")
        val executionContext = ExecutionContext(testConfiguration)
        val testResults = project.testCases.map({ test ->
            log.info("Running ${test.name}")
            // TODO: Fix this unchecked cast
            test.steps.map { executeStep(it as (TestStep<StepRequest, StepResponse>), executionContext) }
                    .let { TestResult(test, it) }
        }).let(::Results)
        StringWriter().also {
            TextResultWriter(it).write(testResults)
        }.toString().also { log.info(it) }

        assertTrue("One or more tests failed.", testResults.tests.all { it.success })

    }

    fun test(name: String, init: TestCase.() -> Unit): TestCase {
        val testCase = TestCase(name)
        testCase.init()
        project.testCases.add(testCase)
        return testCase
    }

    private fun <StepType : TestStep<StepRequest, in StepResponse>> executeStep(step: StepType, context: ExecutionContext): StepResult {
        val executor = executors[step.javaClass] ?: error("No executor found for step type: ${step.javaClass.simpleName}")
        val response = (executor as Executor<StepType>).execute(step, context)
        return if (response.success) {
            val validationResults = step.validators.map { it.validate(context, step.request, response) }
            StepResult(step, validationResults, true)
        } else {
            StepResult(step, listOf(), false)
        }
    }

    private fun formatName(): String {
        val className = javaClass.simpleName
        return if (className.endsWith(testPostfix))
            className.substring(0, className.length - testPostfix.length)
        else
            className
    }

    companion object {
        const val testPostfix = "Test"
        private val log = LoggerFactory.getLogger(KosoteTest::class.java)
    }
}

/**
 * Empty on purpose: extension methods define functionality for building steps.
 */
class StepBuilder(val testCase: TestCase)