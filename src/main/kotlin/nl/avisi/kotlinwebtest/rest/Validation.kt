package nl.avisi.kotlinwebtest.rest

import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.PathNotFoundException
import nl.avisi.kotlinwebtest.ExecutionContext
import nl.avisi.kotlinwebtest.Validator
import nl.avisi.kotlinwebtest.ValidatorResult
import nl.avisi.kotlinwebtest.expressions.Expression
import nl.avisi.kotlinwebtest.expressions.ExpressionEvaluator
import nl.avisi.kotlinwebtest.json.WildcardTokenComparator
import org.json.JSONException
import org.skyscreamer.jsonassert.JSONCompare
import org.skyscreamer.jsonassert.JSONCompareMode
import org.slf4j.LoggerFactory

enum class CompareMode {
    STRICT,
    LENIENT,
    NON_EXTENSIBLE,
    STRICT_ORDER;

    internal fun getJsonCompareMode(): JSONCompareMode =
            when (this) {
                CompareMode.STRICT -> JSONCompareMode.STRICT
                CompareMode.LENIENT -> JSONCompareMode.LENIENT
                CompareMode.NON_EXTENSIBLE -> JSONCompareMode.NON_EXTENSIBLE
                CompareMode.STRICT_ORDER -> JSONCompareMode.STRICT_ORDER
            }
}

class JsonValidator(val mode: CompareMode, var value: Expression? = null) : Validator<RestStepRequest, RestStepResponse> {
    companion object {
        private val log = LoggerFactory.getLogger(JsonValidator::class.java)
    }

    override fun validate(executionContext: ExecutionContext, request: RestStepRequest, response: RestStepResponse): ValidatorResult {
        val expectedValue = value?.let { ExpressionEvaluator(executionContext).evaluate(it) } ?: error("JsonValidator is missing expected value")
        return when (response.body) {
            null -> failure("JSON failure, no match found for $expectedValue")
            else -> assertJson(expectedValue, response, mode)
        }
    }
}

class JsonPathValidator(private val jsonPath: String, private val expectedValue: Any) : Validator<RestStepRequest, RestStepResponse> {

    companion object {
        private val log = LoggerFactory.getLogger(JsonPathValidator::class.java)
    }

    override fun validate(executionContext: ExecutionContext, request: RestStepRequest, response: RestStepResponse): ValidatorResult {
        if (response.body == null) return failure("JSON failure, no match found for $expectedValue")
        val document = JsonPath.parse(response.body)

        val actualValue: Any = try {
            document.read(jsonPath)
        } catch (e: PathNotFoundException) {
            return failure("Incorrect Json Path: $jsonPath (${e.message})")
        }

        return if (expectedValue == actualValue) {
            success()
        } else {
            failure("Expected: $expectedValue but was: $actualValue")
        }
    }
}

private fun Validator<*, *>.assertJson(expectedValue: String, response: RestStepResponse, mode: CompareMode): ValidatorResult {
    try {
        JSONCompare.compareJSON(expectedValue, response.body, WildcardTokenComparator(mode.getJsonCompareMode()))
                .takeUnless { it.passed() }
                ?.let { return failure("JSON failure:\r\n${it.message}") }
                ?: return success()
    } catch (e: JSONException) {
        LoggerFactory.getLogger(javaClass).warn("Error parsing JSON: $expectedValue", e)
        return failure("JSON failure, no valid JSON supplied")
    }
}