package nl.avisi.kotlinwebtest.rest.dsl

import nl.avisi.kotlinwebtest.KosoteTest
import nl.avisi.kotlinwebtest.StepBuilder
import nl.avisi.kotlinwebtest.TestConfiguration
import nl.avisi.kotlinwebtest.http.HttpStatusValidationBuilder
import nl.avisi.kotlinwebtest.rest.RestRequestDefaults
import nl.avisi.kotlinwebtest.rest.RestResponse
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
    fun http_status(): HttpStatusValidationBuilder<RestResponse> = HttpStatusValidationBuilder(step)
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