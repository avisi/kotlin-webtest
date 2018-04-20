/**
 * For licensing, see LICENSE.txt
 * @author Martijn Heuvelink
 */
package nl.avisi.kotlinwebtest.jdbc

import nl.avisi.kotlinwebtest.ExecutionContext
import nl.avisi.kotlinwebtest.Validator
import nl.avisi.kotlinwebtest.ValidatorResult
import nl.avisi.kotlinwebtest.expressions.Expression
import nl.avisi.kotlinwebtest.expressions.ExpressionEvaluator
import nl.avisi.kotlinwebtest.interpolateExpressions
import nl.avisi.kotlinwebtest.soap.InequalityType
import java.text.SimpleDateFormat
import java.time.Duration
import java.util.Date


const val NORESULTSERROR: String = "The given query returned no results"
const val OUTOFBOUNDSERROR: String = "There are not enough result with given index"


class QueryValidator(private val columnName: String, private val position: Int?, private var expectedValue: Expression? = null, private val regex: Boolean) : Validator<JdbcStepRequest, JdbcStepResponse> {

    override fun validate(executionContext: ExecutionContext, request: JdbcStepRequest, response: JdbcStepResponse): ValidatorResult {
        val expectedValue = expectedValue.let {
            interpolateExpressions(expectedValue
                    ?.let { ExpressionEvaluator(executionContext).evaluate(it) }
                    ?: return failure("${javaClass.simpleName}: missing expected value"), executionContext)
        }
        return when {
            response.resultSet.isEmpty() -> failure(NORESULTSERROR)
            position == null -> {
                response.resultSet.map { it.filter { it.key == columnName } }
                        .firstOrNull { match(it[columnName], expectedValue) }
                        ?.let { success() }
                        ?: failure("${javaClass.simpleName}: No match found for: $columnName")
            }
            else -> {
                when {
                    resultDoesnotContainsKey(response, position, columnName) -> failure("${javaClass.simpleName}: No match found for: $columnName")
                    response.resultSet.size <= position -> failure(OUTOFBOUNDSERROR)
                    match(response.resultSet[position].getValue(columnName), expectedValue) -> success()
                    else -> failure("${javaClass.simpleName}: Column: $columnName \nExpected: \n\t$expectedValue \nActual: \n\t${response.resultSet[position].getValue(columnName)}")
                }
            }
        }
    }

    private fun match(value: String?, expectedValue: String): Boolean {
        return when {
            value == null -> return false
            regex -> value.matches(Regex(expectedValue))
            value == expectedValue -> true
            else -> false
        }
    }
}

class RowCountValidator(private val expectedValue: Int, private val columnName: String?) : Validator<JdbcStepRequest, JdbcStepResponse> {

    override fun validate(executionContext: ExecutionContext, request: JdbcStepRequest, response: JdbcStepResponse): ValidatorResult {
        return when (columnName) {
            null -> {
                if (response.resultSet.size != expectedValue) failure("there were ${response.resultSet.size} row(s) instead of $expectedValue row(s)")
                else success()
            }
            else -> {
                val resultsFiltered = response.resultSet.map { it.filter { it.key == columnName } }.filter { !it[columnName].isNullOrEmpty() }
                when {
                    response.resultSet.isEmpty() -> failure(NORESULTSERROR)
                    resultDoesnotContainsKey(response, 0, columnName) -> failure("Column '$columnName' does not exist.")
                    resultsFiltered.size == expectedValue -> success()
                    else -> failure("Expected: $expectedValue but was ${resultsFiltered.size}")
                }
            }
        }
    }
}

class BooleanValidator(private val columnName: String, private val position: Int?, private var expectedValue: Boolean) : Validator<JdbcStepRequest, JdbcStepResponse> {

    override fun validate(executionContext: ExecutionContext, request: JdbcStepRequest, response: JdbcStepResponse): ValidatorResult {

        return when (position) {
            null -> {
                failure("Index is unknown")
            }
            else -> {
                when {
                    response.resultSet.isEmpty() -> failure(NORESULTSERROR)
                    response.resultSet.size <= position -> failure(OUTOFBOUNDSERROR)
                    resultDoesnotContainsKey(response, position, columnName) -> failure("Column '$columnName' does not exist.")
                    else -> if (expectedValue)
                        if (!isNullOrEmpty(response)) failure("The column $columnName is not null")
                        else success()
                    else
                        if (isNullOrEmpty(response)) failure("The column $columnName is null")
                        else success()
                }
            }
        }
    }

    private fun isNullOrEmpty(response: JdbcStepResponse): Boolean {
        return response.resultSet[position!!].getValue(columnName).isNullOrEmpty()
    }
}

class CompareTimeValidator(private val first: String, private val pos1: Int = 0, val second: String, private val pos2: Int = 0, private val interval: Int, val type: InequalityType) : Validator<JdbcStepRequest, JdbcStepResponse> {

    override fun validate(executionContext: ExecutionContext, request: JdbcStepRequest, response: JdbcStepResponse): ValidatorResult {
        if (pos1 > response.resultSet.size || pos2 > response.resultSet.size) return failure("Out of bounds. There are only ${response.resultSet.size} results")
        val firstValue = convertResultSetToTimestamp(first, pos1, executionContext, response) ?: return failure("There was a error with parsing the time")
        val secondValue = convertResultSetToTimestamp(second, pos2, executionContext, response) ?: return failure("There was a error with parsing the time")
        return when {
            resultDoesnotContainsKey(response, pos1, first) -> failure("Column '$first' does not exist.")
            resultDoesnotContainsKey(response, pos2, second) -> failure("Column '$second' does not exist.")
            else -> {
                val dif = differenceBetweenDates(firstValue, secondValue)
                when (type) {
                    InequalityType.GreaterThan -> {
                        if (dif > interval) success()
                        else failure("The interval is $dif")
                    }
                    InequalityType.LessThan -> {
                        if (dif < interval) success()
                        else failure("The interval is $dif")
                    }
                    InequalityType.Equals -> {
                        if (dif == interval.toLong()) success()
                        else failure("The interval is $dif")
                    }
                }

            }
        }
    }

    fun differenceBetweenDates(date1: Date, date2: Date): Long {
        val dif = Duration.between(date1.toInstant(), date2.toInstant()).toMillis()
        return if (dif < 0) dif / -1
        else dif
    }

    private fun convertResultSetToTimestamp(columnName: String, position: Int, executionContext: ExecutionContext, response: JdbcStepResponse): Date? =
            try {
                val actualValue = response.resultSet[position].getValue(columnName)
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(actualValue.let { interpolateExpressions(it, executionContext) })
            } catch (e: Exception) {
                null
            }
}

fun resultDoesnotContainsKey(response: JdbcStepResponse, position: Int, columnName: String) = !response.resultSet[position].containsKey(columnName)