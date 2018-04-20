/**
 * For licensing, see LICENSE.txt
 * @author Rein Krul
 */
package nl.avisi.kotlinwebtest.rest

import com.jayway.jsonpath.JsonPath
import net.minidev.json.JSONArray
import nl.avisi.kotlinwebtest.ExecutionContext
import nl.avisi.kotlinwebtest.FailedValidatorResult
import nl.avisi.kotlinwebtest.Validator
import nl.avisi.kotlinwebtest.ValidatorResult
import nl.avisi.kotlinwebtest.expressions.Expression
import nl.avisi.kotlinwebtest.expressions.ExpressionEvaluator
import nl.avisi.kotlinwebtest.interpolateExpressions
import nl.avisi.kotlinwebtest.json.WildcardTokenComparator
import nl.avisi.kotlinwebtest.soap.InequalityType
import org.json.JSONException
import org.json.JSONObject
import org.skyscreamer.jsonassert.JSONCompare
import org.skyscreamer.jsonassert.JSONCompareMode
import org.slf4j.LoggerFactory
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.format.DateTimeParseException
import java.util.Date

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

sealed class Result
class JsonResult(val value: Any?) : Result()
class ErrorResult(val value: FailedValidatorResult) : Result()


abstract class RestValidator : Validator<RestStepRequest, RestStepResponse> {

    protected fun getValueJsonPath(response: String, path: String): Result {
        if (response.isEmpty()) return ErrorResult(failure("JSON failure, response is empty"))
        val document = JsonPath.parse(response)
        return try {
            JsonResult(document.read(path))
        } catch (e: Exception) {
            ErrorResult(failure("Incorrect Json Path: $path (${e.message})"))
        }
    }

    protected fun jsonArrayToLocalDate(actualValue: JSONArray): Date? {
        val year = actualValue[0].toString()
        var month = actualValue[1].toString()
        var day = actualValue[2].toString()
        if (month.length == 1) month = "0" + month
        if (day.length == 1) day = "0" + day
        var date = "$year-$month-$day"
        return try {
            if (actualValue.size > 3) {
                var hours = actualValue[3].toString()
                var minutes = actualValue[4].toString()
                var seconds = actualValue[5].toString()
                val millis = actualValue[6].toString().substring(0, 3)
                if (hours.length == 1) hours = "0" + hours
                if (minutes.length == 1) minutes = "0" + minutes
                if (seconds.length == 1) seconds = "0" + seconds
                date += " $hours:$minutes:$seconds.$millis"
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").parse(date)
            } else {
                SimpleDateFormat("yyyy-MM-dd").parse(date)

            }
        } catch (e: DateTimeParseException) {
            LoggerFactory.getLogger(javaClass).warn("Error parsing Date: ${e.parsedString}", e)
            null
        }
    }

    protected fun isNull(actualValue: Any?, expectedValue: Boolean, jsonPath: String): ValidatorResult {
        return if (expectedValue)
            if (actualValue == null) success()
            else failure("Expected: The jsonPath $jsonPath isn't null")
        else
            if (actualValue != null) success()
            else failure("Expected: The jsonPath $jsonPath is null")
    }
}

class JsonValidator(val mode: CompareMode, var value: Expression? = null) : RestValidator() {

    override fun validate(executionContext: ExecutionContext, request: RestStepRequest, response: RestStepResponse): ValidatorResult {
        val expectedValue = value?.let { ExpressionEvaluator(executionContext).evaluate(it) } ?: return failure("JsonValidator is missing expected value")
        return when (response.body) {
            null -> failure("JSON failure, no match found for $expectedValue")
            else -> assertJson(expectedValue, response.body!!, mode)
        }
    }
}

class JsonPathIsNullValidator(private val jsonPath: String, private val expectedValue: Boolean) : RestValidator() {

    override fun validate(executionContext: ExecutionContext, request: RestStepRequest, response: RestStepResponse): ValidatorResult {
        val actualValue = getValueJsonPath(response.body!!, jsonPath)
        return when (actualValue) {
            is ErrorResult -> actualValue.value
            is JsonResult -> isNull(actualValue.value, expectedValue, jsonPath)
        }
    }
}

class JsonPathFromPropIsNullValidator(private val prop: Expression, private val jsonPath: String, private val expectedValue: Boolean) : RestValidator() {

    override fun validate(executionContext: ExecutionContext, request: RestStepRequest, response: RestStepResponse): ValidatorResult {
        val prop = prop.let { ExpressionEvaluator(executionContext).evaluate(it) } ?: return failure("JsonPathFromPropIsNullValidator is missing expected value")
        val actualValue = getValueJsonPath(prop, jsonPath)
        return when (actualValue) {
            is ErrorResult -> actualValue.value
            is JsonResult -> isNull(actualValue.value, expectedValue, jsonPath)
        }
    }
}


class JsonPathValidator(private val jsonPath: String, private val expectedValue: Any?) : RestValidator() {

    override fun validate(executionContext: ExecutionContext, request: RestStepRequest, response: RestStepResponse): ValidatorResult {
        val expectedValue = expectedValue.let {
            when (it) {
                is Expression -> ExpressionEvaluator(executionContext).evaluate(it)
                else -> it
            }
        } ?: return failure("JsonPathValidator is missing expected value")
        val actualValue = getValueJsonPath(response.body!!, jsonPath)
        return when (actualValue) {
            is ErrorResult -> actualValue.value
            is JsonResult -> {
                if (expectedValue == actualValue.value || expectedValue.toString() == actualValue.value.toString()) {
                    success()
                } else {
                    failure("Expected: $expectedValue but was: ${actualValue.value}")
                }
            }
        }
    }
}

class JsonAssertWithPathValidator(private val jsonPath: String, private val expectedValue: Any, val json_path_property: String, private val compareMode: CompareMode) : RestValidator() {

    override fun validate(executionContext: ExecutionContext, request: RestStepRequest, response: RestStepResponse): ValidatorResult {
        val expected = expectedValue.let {
            when (it) {
                is Expression -> ExpressionEvaluator(executionContext).evaluate(it)
                else -> it
            }
        } ?: return failure("JsonAssertWithPathValidator is missing expected value")
        val actualValue = getValueJsonPath(response.body!!, jsonPath)
        val expectedValue = getValueJsonPath(expected.toString(), json_path_property)
        if (actualValue is ErrorResult) return actualValue.value
        if (expectedValue is ErrorResult) return expectedValue.value
        return when {
            (actualValue as JsonResult).value is JSONObject -> assertJson((expectedValue as JsonResult).value.toString(), actualValue.value.toString(), compareMode)
            (expectedValue as JsonResult).value.toString() == actualValue.value.toString() -> success()
            else -> failure("Expected: ${expectedValue.value} but was: ${actualValue.value}")
        }
    }
}


class JsonPathDateValidator(private val jsonPath: String, private val expectedValue: Any) : RestValidator() {

    override fun validate(executionContext: ExecutionContext, request: RestStepRequest, response: RestStepResponse): ValidatorResult {
        val expectedValue = expectedValue.let {
            when (it) {
                is Expression -> ExpressionEvaluator(executionContext).evaluate(it)
                else -> it
            }
        } ?: return failure("JsonPathDateValidator is missing expected value")
        val actualValue = getValueJsonPath(response.body!!, jsonPath)
        return when (actualValue) {
            is ErrorResult -> actualValue.value
            is JsonResult -> {
                when (actualValue.value) {
                    is JSONArray -> {
                        val date = SimpleDateFormat("yyyy-MM-dd").format(jsonArrayToLocalDate(actualValue.value) ?: return failure("cant parse date"))
                                ?: return failure("Date is null")
                        if (date == expectedValue) success()
                        else failure("Expected: $expectedValue but was: $date")
                    }
                    is String -> {
                        if (expectedValue.toString() == actualValue.value) {
                            success()
                        } else failure("Expected: $expectedValue but was: ${actualValue.value.toString()}")
                    }
                    else -> failure("Expected: $expectedValue but was: ${actualValue.value.toString()}")
                }
            }
        }
    }
}

class ContainsValidator(private var expectedValue: Array<String>) : RestValidator() {

    override fun validate(executionContext: ExecutionContext, request: RestStepRequest, response: RestStepResponse): ValidatorResult {
        val expectedValue = expectedValue.map { interpolateExpressions(it, executionContext) }
        val notFound = expectedValue.filter { !response.body!!.contains(it) }
        return if (notFound.isEmpty()) success() else failure("Response does not contains $notFound")
    }
}

class CompareTimeValidator(private val first: String, private val second: String, private val interval: Int, val type: InequalityType) : RestValidator() {

    override fun validate(executionContext: ExecutionContext, request: RestStepRequest, response: RestStepResponse): ValidatorResult {
        val firstValue = convertJsonPathToTimestamp(first, executionContext, response.body!!)
        val secondValue = convertJsonPathToTimestamp(second, executionContext, response.body!!)
        return when {
            secondValue == null || firstValue == null -> failure("The result cant be compared because it isn't from a know datatype or the value is null")
            type == InequalityType.GreaterThan -> {
                val dif = differenceBetweenDates(firstValue, secondValue)
                if (dif > interval) success()
                else failure("The interval is less than expected '$interval' but got '$dif'")
            }
            type == InequalityType.LessThan -> {
                val dif = differenceBetweenDates(firstValue, secondValue)
                if (dif < interval) success()
                else failure("The interval is greater than expected '$interval' but got '$dif'")
            }
            else -> {
                val dif = differenceBetweenDates(firstValue, secondValue)
                failure("The interval is $dif")
            }
        }
    }

    private fun convertJsonPathToTimestamp(jsonPath: String, executionContext: ExecutionContext, body: String): Date? {
        val actualValue = getValueJsonPath(body, jsonPath)
        return when (actualValue) {
            is ErrorResult -> null
            is JsonResult -> {
                when (actualValue.value) {
                    is String -> {
                        if (actualValue.value.isEmpty()) return null
                        try {
                            SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(actualValue.value.let { interpolateExpressions(it, executionContext) })
                        } catch (e: ParseException) {
                            null
                        }
                    }
                    is JSONArray -> jsonArrayToLocalDate(actualValue.value)
                    else -> null
                }
            }
        }
    }

    fun differenceBetweenDates(date1: Date, date2: Date): Long {
        val dif = Duration.between(date1.toInstant(), date2.toInstant()).toMillis()
        return if (dif < 0) dif / -1
        else dif
    }
}

private fun Validator<*, *>.assertJson(expectedValue: String, response: String, mode: CompareMode): ValidatorResult {
    try {
        JSONCompare.compareJSON(expectedValue, response, WildcardTokenComparator(mode.getJsonCompareMode()))
                .takeUnless { it.passed() }
                ?.let { return failure("JSON failure:\r\n${it.message}") }
                ?: return success()
    } catch (e: JSONException) {
        return failure("JSON failure, no valid JSON supplied")
    }
}