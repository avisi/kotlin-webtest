package nl.reikrul.kosote

import java.io.Writer

class TestResult(val test: TestCase, val steps: List<StepResult>) : SuccessIndicator {
    override val success: Boolean
        get() = steps.all { it.success }
}

class StepResult(val step: TestStep<*, *>, val validationResults: List<ValidationResult>) : SuccessIndicator {
    override val success: Boolean
        get() = validationResults.all { it.success }
}

class ValidationResult(val validator: Validator<*>, override val success: Boolean) : SuccessIndicator

class Results(val tests: List<TestResult>) : SuccessIndicator {
    override val success: Boolean
        get() = tests.all { it.success }
}

interface SuccessIndicator {
    val success: Boolean
        get() = false
}

interface ResultWriter {
    fun write(results: Results)
}

class TextResultWriter(private val writer: Writer, private val newline: String = System.getProperty("line.separator")) : ResultWriter {

    override fun write(results: Results) {
        write("======== RESULTS ========")
        results.tests.forEach({
            write("[ ${it.successText} ] Test: ${it.test.name}")
            it.steps.forEach({
                write("[ ${it.successText} ] - Step: ${it.step.javaClass.simpleName}")
                it.validationResults.forEach({
                    write("[ ${it.successText} ]  * Validator: ${it.validator.javaClass.simpleName}")
                })
            })
        })
        write("=========================")
        writer.flush()
    }

    private fun write(line: String) {
        writer.write("$line$newline")
    }
}

private val SuccessIndicator.successText: String
    get() = if (success) " OK " else "FAIL"