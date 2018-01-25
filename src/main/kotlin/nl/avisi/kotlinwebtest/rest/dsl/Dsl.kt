/**
 * For licensing, see LICENSE.txt
 * @author Rein Krul
 */
package nl.avisi.kotlinwebtest.rest.dsl

import nl.avisi.kotlinwebtest.KosoteTest
import nl.avisi.kotlinwebtest.StepBuilder
import nl.avisi.kotlinwebtest.TestConfiguration
import nl.avisi.kotlinwebtest.TestStep
import nl.avisi.kotlinwebtest.expressions.ConstantExpression
import nl.avisi.kotlinwebtest.http.HttpStatusValidationBuilder
import nl.avisi.kotlinwebtest.rest.CompareMode
import nl.avisi.kotlinwebtest.rest.JsonValidator
import nl.avisi.kotlinwebtest.rest.RestRequest
import nl.avisi.kotlinwebtest.rest.RestRequestDefaults
import nl.avisi.kotlinwebtest.rest.RestStepResponse
import nl.avisi.kotlinwebtest.rest.RestTestConfiguration
import nl.avisi.kotlinwebtest.rest.RestTestStep
import nl.avisi.kotlinwebtest.soap.XPathValidator
import org.apache.commons.io.FileUtils
import org.json.JSONException
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.slf4j.LoggerFactory
import java.io.File

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
    fun http_status(): HttpStatusValidationBuilder<RestRequest, RestStepResponse> = HttpStatusValidationBuilder(step)
    fun json(mode: CompareMode = CompareMode.STRICT): JsonValidationBuilder<RestRequest, RestStepResponse> = JsonValidationBuilder(step, mode)
}

fun KosoteTest.rest(init: RestSettingsBuilder.() -> Unit) {
    val builder = RestSettingsBuilder(testConfiguration)
    builder.init()
}

infix fun StepBuilder.rest(init: RestTestStep.() -> Unit): RestTestStep {
    val step = RestTestStep(testCase)
    step.init()
    testCase.steps.add(step)
    return step
}

class JsonValidationBuilder<RequestType : RestRequest, ResponseType : RestStepResponse>(private val step: TestStep<RequestType, ResponseType>, val mode: CompareMode) {
    infix fun matches(value: String) {
        step.validators.add(JsonValidator(mode, ConstantExpression(value)))
    }

    infix fun matches_file(path: String) {
        matches(Thread.currentThread().contextClassLoader.getResource(path).readText())
    }
}