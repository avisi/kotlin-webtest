/**
 * For licensing, see LICENSE.txt
 * @author Rein Krul
 */
package nl.avisi.kotlinwebtest.rest.dsl

import nl.avisi.kotlinwebtest.StepBuilder
import nl.avisi.kotlinwebtest.TestConfiguration
import nl.avisi.kotlinwebtest.TestStep
import nl.avisi.kotlinwebtest.WebTest
import nl.avisi.kotlinwebtest.expressions.ConstantExpression
import nl.avisi.kotlinwebtest.http.HttpHeaderValidationBuilder
import nl.avisi.kotlinwebtest.http.HttpStatusValidationBuilder
import nl.avisi.kotlinwebtest.rest.CompareMode
import nl.avisi.kotlinwebtest.rest.JsonPathValidator
import nl.avisi.kotlinwebtest.rest.JsonValidator
import nl.avisi.kotlinwebtest.rest.RestRequestDefaults
import nl.avisi.kotlinwebtest.rest.RestStepRequest
import nl.avisi.kotlinwebtest.rest.RestStepResponse
import nl.avisi.kotlinwebtest.rest.RestTestConfiguration
import nl.avisi.kotlinwebtest.rest.RestTestStep

class RestSettingsBuilder(val configuration: TestConfiguration) {
    fun default(init: DefaultSettingsBuilder.() -> Unit) {
        val builder = DefaultSettingsBuilder(configuration[RestTestConfiguration::class].defaults)
        builder.init()
    }

    inner class DefaultSettingsBuilder(val request: RestRequestDefaults)
}

infix fun RestTestStep.validate(init: Validation.() -> Unit) {
    Validation(this).init()
}

class Validation(private val step: RestTestStep) {
    fun http_status() = HttpStatusValidationBuilder(step)
    fun http_header(header: String) = HttpHeaderValidationBuilder(header, step)
    fun json(mode: CompareMode = CompareMode.STRICT) = JsonValidationBuilder(step, mode)
    fun json_path(jsonPath: String) = JsonPathValidationBuilder(step, jsonPath)
}

fun WebTest.rest(init: RestSettingsBuilder.() -> Unit) {
    val builder = RestSettingsBuilder(testConfiguration)
    builder.init()
}

infix fun StepBuilder.rest(init: RestTestStep.() -> Unit): RestTestStep {
    val step = RestTestStep(testCase)
    step.init()
    testCase.steps.add(step)
    return step
}

class JsonValidationBuilder<RequestType : RestStepRequest, ResponseType : RestStepResponse>(private val step: TestStep<RequestType, ResponseType>, val mode: CompareMode, vararg val pathAndRegex: Pair<String, String>) {
    infix fun matches(value: String) {
        step.validators.add(JsonValidator(mode, ConstantExpression(value)))
    }

    infix fun matches_file(path: String) {
        matches(Thread.currentThread().contextClassLoader.getResource(path).readText())
    }
}

class JsonPathValidationBuilder<RequestType : RestStepRequest, ResponseType : RestStepResponse>(private val step: TestStep<RequestType, ResponseType>, val jsonPath: String) {
    infix fun matches(value: Any) {
        step.validators.add(JsonPathValidator(jsonPath, value))
    }
}