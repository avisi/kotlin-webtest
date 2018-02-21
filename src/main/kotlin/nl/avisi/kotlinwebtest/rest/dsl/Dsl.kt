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
import nl.avisi.kotlinwebtest.expressions.Expression
import nl.avisi.kotlinwebtest.expressions.PropertyExpression
import nl.avisi.kotlinwebtest.http.HttpHeaderValidationBuilder
import nl.avisi.kotlinwebtest.http.HttpStatusValidationBuilder
import nl.avisi.kotlinwebtest.rest.CompareMode
import nl.avisi.kotlinwebtest.rest.CompareTimeValidator
import nl.avisi.kotlinwebtest.rest.ContainsValidator
import nl.avisi.kotlinwebtest.rest.JsonAssertWithPathValidator
import nl.avisi.kotlinwebtest.rest.JsonPathDateValidator
import nl.avisi.kotlinwebtest.rest.JsonPathFromPropIsNullValidator
import nl.avisi.kotlinwebtest.rest.JsonPathIsNullValidator
import nl.avisi.kotlinwebtest.rest.JsonPathValidator
import nl.avisi.kotlinwebtest.rest.JsonPathValue
import nl.avisi.kotlinwebtest.rest.JsonValidator
import nl.avisi.kotlinwebtest.rest.RestRequestDefaults
import nl.avisi.kotlinwebtest.rest.RestStepRequest
import nl.avisi.kotlinwebtest.rest.RestStepResponse
import nl.avisi.kotlinwebtest.rest.RestTestConfiguration
import nl.avisi.kotlinwebtest.rest.RestTestStep
import nl.avisi.kotlinwebtest.soap.InequalityType

class RestSettingsBuilder(val configuration: TestConfiguration) {
    fun default(init: DefaultSettingsBuilder.() -> Unit) =
            DefaultSettingsBuilder(configuration[RestTestConfiguration::class].defaults).apply(init)

    inner class DefaultSettingsBuilder(val request: RestRequestDefaults)
}

infix fun RestTestStep.validate(init: Validation.() -> Unit) {
    Validation(this).init()
}

infix fun RestTestStep.afterwards(init: After.() -> Unit) {
    After(this).init()
}

class Validation(private val step: RestTestStep) {

    fun http_status() = HttpStatusValidationBuilder(step)
    fun http_header(header: String) = HttpHeaderValidationBuilder(header, step)
    fun json(mode: CompareMode = CompareMode.STRICT) = JsonValidationBuilder(step, mode)
    fun json_path(jsonPath: String) = JsonPathValidationBuilder(step, jsonPath)
    fun json_path(property: Expression, json_path_property: String, compareMode: CompareMode = CompareMode.LENIENT) = JsonPathFromPropValidationBuilder(step, property, json_path_property, compareMode)
    fun json_path(jsonPathFirst: String, jsonPathSecond: String) = JsonPathCompareValidationBuilder(step, jsonPathFirst, jsonPathSecond)
    fun contains(string: Array<String>) = step.validators.add(ContainsValidator(string))
}

class After(private val step: RestTestStep) {

    fun assign(json_path: String): JsonPathValueBuilder = JsonPathValueBuilder(step, json_path)
}

fun WebTest.rest(init: RestSettingsBuilder.() -> Unit) =
        RestSettingsBuilder(testConfiguration).apply(init)

infix fun StepBuilder.rest(init: RestTestStep.() -> Unit): RestTestStep =
        RestTestStep(testCase).apply {
            init()
            testCase.steps.add(this)
        }

class JsonValidationBuilder(private val step: TestStep<RestStepRequest, RestStepResponse>, val compareMode: CompareMode) {
    infix fun matches(value: String) {
        matches(ConstantExpression(value))
    }

    infix fun matches(value: Expression) {
        step.validators.add(JsonValidator(compareMode, value))
    }

    infix fun matches_file(path: String) {
        matches(Thread.currentThread().contextClassLoader.getResource(path).readText())
    }
}

class JsonPathCompareValidationBuilder(private val step: TestStep<RestStepRequest, RestStepResponse>, private val xpathFirst: String, private val xpathSecond: String) {

    infix fun time_difference_greater_than(difference: Int) {
        step.validators.add(CompareTimeValidator(xpathFirst, xpathSecond, difference, InequalityType.GreaterThan))
    }

    infix fun time_difference_less_than(difference: Int) {
        step.validators.add(CompareTimeValidator(xpathFirst, xpathSecond, difference, InequalityType.LessThan))
    }
}

class JsonPathValidationBuilder(private val step: TestStep<RestStepRequest, RestStepResponse>, val jsonPath: String) {
    infix fun is_null(bool: Boolean) {
        step.validators.add(JsonPathIsNullValidator(jsonPath, bool))
    }

    infix fun matches(value: Any?) {
        step.validators.add(JsonPathValidator(jsonPath, value))
    }

    infix fun date_matches(value: String) {
        date_matches(ConstantExpression(value))
    }

    infix fun date_matches(value: Any) {
        step.validators.add(JsonPathDateValidator(jsonPath, value))
    }
}

class JsonPathFromPropValidationBuilder(private val step: TestStep<RestStepRequest, RestStepResponse>, val property: Expression, private val json_path_property: String, val compareMode: CompareMode) {

    infix fun is_null(bool: Boolean) {
        step.validators.add(JsonPathFromPropIsNullValidator(property, json_path_property, bool))
    }

    infix fun matches(json_path: String) {
        step.validators.add(JsonAssertWithPathValidator(json_path, property, json_path_property, compareMode))
    }
}

class JsonPathValueBuilder(private val step: RestTestStep, val xpath: String) {

    infix fun to(prop: PropertyExpression) {
        step.afterwards.add(JsonPathValue(step, xpath, prop))
    }
}