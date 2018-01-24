/**
 * For licensing, see LICENSE.txt
 * @author Rein Krul
 */
package nl.avisi.kotlinwebtest

import java.io.Writer

class TestResult(val test: TestCase, val steps: List<StepResult>) : Result {
    override val success: Boolean
        get() = steps.all { it.success }
    override val message: String? = null
}

class StepResult(val step: TestStep<*, *>, val validationResults: List<ValidatorResult>, val requestSuccess: Boolean) : Result {
    override val success: Boolean
        get() = requestSuccess && validationResults.all { it is SuccessValidatorResult }
    override val message: String? = null
}

class Results(val tests: List<TestResult>) : Result {
    override val success: Boolean
        get() = tests.all { it.success }
    override val message: String? = null
}

interface Result {
    val success: Boolean
    val message: String?
}

interface ResultWriter {
    fun write(results: Results)
}

class TextResultWriter(private val writer: Writer, private val newline: String = System.getProperty("line.separator")) : ResultWriter {

    override fun write(results: Results) {
        write("\n======== RESULTS ========")
        results.tests.forEach({
            write("[ ${it.successText} ] Test: ${it.test.name}")
            it.steps.forEach({
                val stepName = if (it.step.name.isNullOrBlank()) it.step.javaClass.simpleName else "${it.step.name} (${it.step.javaClass.simpleName})"
                write("[ ${it.successText} ] - Step: $stepName")
                it.validationResults.forEach {
                    write("[ ${it.successText} ]  * Validator: ${it.validator.javaClass.simpleName}")
                    if (it.message != null) {
                        write("            ${it.message}")
                    }
                }
            })
        })
        write("=========================")
        writer.flush()
    }

    private fun write(line: String) {
        writer.write("$line$newline")
    }
}

private val Result.successText: String
    get() = if (success) " OK " else "FAIL"