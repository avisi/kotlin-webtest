/**
 * For licensing, see LICENSE.txt
 * @author Rein Krul
 */
package nl.avisi.kotlinwebtest.soap.dsl

import nl.avisi.kotlinwebtest.Endpoint
import nl.avisi.kotlinwebtest.EndpointConfigurer
import nl.avisi.kotlinwebtest.KosoteTest
import nl.avisi.kotlinwebtest.StepBuilder
import nl.avisi.kotlinwebtest.TestConfiguration
import nl.avisi.kotlinwebtest.expressions.ConstantExpression
import nl.avisi.kotlinwebtest.expressions.Expression
import nl.avisi.kotlinwebtest.http.HttpStatusValidationBuilder
import nl.avisi.kotlinwebtest.soap.Schema
import nl.avisi.kotlinwebtest.soap.Schemas
import nl.avisi.kotlinwebtest.soap.SoapFaultValidator
import nl.avisi.kotlinwebtest.soap.SoapStepRequest
import nl.avisi.kotlinwebtest.soap.SoapRequestDefaults
import nl.avisi.kotlinwebtest.soap.SoapStepResponse
import nl.avisi.kotlinwebtest.soap.SoapResponseValidator
import nl.avisi.kotlinwebtest.soap.SoapTestConfiguration
import nl.avisi.kotlinwebtest.soap.SoapTestStep
import nl.avisi.kotlinwebtest.soap.XPathValidator
import nl.avisi.kotlinwebtest.soap.XSDValidator

fun KosoteTest.soap(init: SoapSettingsBuilder.() -> Unit) {
    val builder = SoapSettingsBuilder(testConfiguration)
    builder.init()
}

infix fun SoapTestStep.validate(init: Validation.() -> Unit) {
    Validation(this).init()
}

infix fun StepBuilder.soap(init: SoapTestStep.() -> Unit): SoapTestStep {
    val step = SoapTestStep(testCase)
    step.init()
    testCase.steps.add(step)
    return step
}

class Validation(private val step: SoapTestStep) {
    fun xpath(xpath: String): XPathValidationBuilder = XPathValidationBuilder(step, xpath)
    fun http_status(): HttpStatusValidationBuilder<SoapStepRequest, SoapStepResponse> = HttpStatusValidationBuilder(step)
    fun xsd() = step.validators.add(XSDValidator())
    fun soap_fault() = SoapFaultValidationBuilder(step)
    fun is_soap_response() = step.validators.add(SoapResponseValidator())
}

class XPathValidationBuilder(private val step: SoapTestStep, val xpath: String) {
    infix fun matches(value: String) {
        matches(ConstantExpression(value))
    }

    infix fun matches(value: Expression) {
        step.validators.add(XPathValidator(xpath, value))
    }
}

class SoapFaultValidationBuilder(step: SoapTestStep) {
    private val validator = SoapFaultValidator(true)

    init {
        step.validators.add(validator)
    }

    operator fun not() {
        validator.mustContainSoapFault = false
    }
}

class SoapSettingsBuilder(val configuration: TestConfiguration) {
    fun default(init: DefaultSettingsBuilder.() -> Unit) {
        val builder = DefaultSettingsBuilder(configuration[SoapTestConfiguration::class].defaults)
        builder.init()
    }

    inner class DefaultSettingsBuilder(val request: SoapRequestDefaults)
}

class SoapEndpointSettingsConfigurer(private val endpoint: Endpoint) {
    infix fun validation_schema(file: String) {
        // TODO: This enables immutability of the 'Schemas' class, but it sure looks a bit funny...
        val url = Thread.currentThread().contextClassLoader.getResource(file) ?: error("Schema not found: $file")
        endpoint[Schemas::class] = Schemas(endpoint[Schemas::class].plus(Schema(url)))
    }
}

val EndpointConfigurer.soap: SoapEndpointSettingsConfigurer
    get() = SoapEndpointSettingsConfigurer(this.endpoint)

