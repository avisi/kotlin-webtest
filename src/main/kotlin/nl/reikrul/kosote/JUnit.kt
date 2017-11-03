package nl.reikrul.kosote

import nl.reikrul.kosote.properties.PropertyExecutor
import nl.reikrul.kosote.properties.PropertyTestStep
import nl.reikrul.kosote.soap.SoapExecutor
import nl.reikrul.kosote.soap.dsl.SoapTestStep
import org.junit.Before
import org.slf4j.LoggerFactory
import java.io.StringWriter

abstract class KosoteTest {

    val project = Project(formatName())
    private val executors: MutableMap<Class<*>, Executor<*>> = mutableMapOf()

    @Before
    fun setUp() {
        executors.put(SoapTestStep::class.java, SoapExecutor())
        executors.put(PropertyTestStep::class.java, PropertyExecutor())
    }

    // Executing
    fun execute() {
        log.info("Executing ${project.name}")
        val context = ExecutionContext()
        val testResults = project.testCases.map({
            log.info("Running ${it.name}")
            val test = it
            it.steps.map { executeStep(it, context) }
                    .let { TestResult(test, it) }
        }).let(::Results)
        StringWriter().also {
            TextResultWriter(it).write(testResults)
        }.toString().also { log.info(it) }

    }

    private fun <StepType : TestStep<Request, Response>> executeStep(step: StepType, context: ExecutionContext): StepResult {
        val executor = executors[step.javaClass] ?: error("No executor found for step type: ${step.javaClass.simpleName}")
        val response = (executor as Executor<StepType>).execute(step, context)
        val validationResults = step.validators.map {
            val success = it.validate(response)
            if (!success) {
                log.warn("Validation failed: ${it.javaClass.simpleName}")
            }
            ValidationResult(it, success)
        }
        return StepResult(step, validationResults)
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