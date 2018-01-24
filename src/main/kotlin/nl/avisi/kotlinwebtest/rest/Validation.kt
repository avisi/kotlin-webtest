package nl.avisi.kotlinwebtest.rest

import nl.avisi.kotlinwebtest.ExecutionContext
import nl.avisi.kotlinwebtest.Validator
import nl.avisi.kotlinwebtest.ValidatorResult
import nl.avisi.kotlinwebtest.expressions.Expression
import nl.avisi.kotlinwebtest.expressions.ExpressionEvaluator
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

class JsonValidator(val mode: CompareMode, var value: Expression? = null) : Validator<RestRequest, RestStepResponse> {
    companion object {
        private val log = LoggerFactory.getLogger(JsonValidator::class.java)
    }

    override fun validate(executionContext: ExecutionContext, request: RestRequest, response: RestStepResponse): ValidatorResult {
        val expectedValue = value?.let { ExpressionEvaluator(executionContext).evaluate(it) } ?: error("JsonValidator is missing expected value")

        if (response.body == null) return failure("JSON failure, no match found for $expectedValue")
        else {
            try {
                JSONCompare.compareJSON(expectedValue, response.body, mode.getJsonCompareMode())
                        .takeUnless { it.passed() }
                        ?.let { return failure("JSON failure:\r\n${it.message}") }
                        ?: return success()
            } catch (e: JSONException) {
                log.warn("Error parsing JSON: $expectedValue", e)
                return failure("JSON failure, no valid JSON supplied")
            }
        }
    }
}

