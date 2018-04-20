/**
 * For licensing, see LICENSE.txt
 * @author Martijn Heuvelink
 */
package nl.avisi.kotlinwebtest.jdbc.dsl

import nl.avisi.kotlinwebtest.StepBuilder
import nl.avisi.kotlinwebtest.TestConfiguration
import nl.avisi.kotlinwebtest.WebTest
import nl.avisi.kotlinwebtest.expressions.ConstantExpression
import nl.avisi.kotlinwebtest.expressions.Expression
import nl.avisi.kotlinwebtest.jdbc.BooleanValidator
import nl.avisi.kotlinwebtest.jdbc.CompareTimeValidator
import nl.avisi.kotlinwebtest.jdbc.JdbcRequestDefaults
import nl.avisi.kotlinwebtest.jdbc.JdbcTestConfiguration
import nl.avisi.kotlinwebtest.jdbc.JdbcTestStep
import nl.avisi.kotlinwebtest.jdbc.QueryValidator
import nl.avisi.kotlinwebtest.jdbc.RowCountValidator
import nl.avisi.kotlinwebtest.soap.InequalityType

class JdbcSettingsBuilder(val configuration: TestConfiguration) {

    fun default(init: DefaultSettingsBuilder.() -> Unit) =
            DefaultSettingsBuilder(configuration[JdbcTestConfiguration::class].defaults).apply(init)

    inner class DefaultSettingsBuilder(val request: JdbcRequestDefaults)
}

infix fun JdbcTestStep.validate(init: Validation.() -> Unit) =
        Validation(this).init()

class Validation(val step: JdbcTestStep) {
    fun row_count(amount: Int, columnName: String? = null) = step.validators.add(RowCountValidator(amount, columnName))
    fun column(columnName: String): ColumnValidationBuilder = ColumnValidationBuilder(step, columnName, null)
}

infix fun Validation.row(init: Row.() -> Unit) =
        Row(this).init()

class Row(val validate: Validation) {
    var row_index: Int = 0
    fun column(columnName: String): ColumnValidationBuilder = ColumnValidationBuilder(validate.step, columnName, row_index)
    fun column(columnNameFirst: String, columnNameSecond: String): ColumnCompareValidationBuilder = ColumnCompareValidationBuilder(validate.step, columnNameFirst, columnNameSecond, row_index)
}

class ColumnValidationBuilder(private val step: JdbcTestStep, private val columnName: String, private val position: Int?) {

    infix fun matches(value: String) {
        matches(ConstantExpression(value))
    }

    infix fun matches(value: Expression) {
        step.validators.add(QueryValidator(columnName, position, value, false))
    }

    infix fun matches_regex(value: String) {
        matches_regex(ConstantExpression(value))
    }

    infix fun matches_regex(value: Expression) {
        step.validators.add(QueryValidator(columnName, position, value, true))
    }

    infix fun is_null(value: Boolean) {
        step.validators.add(BooleanValidator(columnName, position, value))
    }
}

class ColumnCompareValidationBuilder(private val step: JdbcTestStep, private val columnNameFirst: String, private val columnNameSecond: String, private val position: Int) {

    infix fun time_difference_greater_than(difference: Int) {
        step.validators.add(CompareTimeValidator(columnNameFirst, position, columnNameSecond, position, difference, InequalityType.GreaterThan))
    }

    infix fun time_difference_less_than(difference: Int) {
        step.validators.add(CompareTimeValidator(columnNameFirst, position, columnNameSecond, position, difference, InequalityType.LessThan))
    }
}

fun WebTest.jdbc(init: JdbcSettingsBuilder.() -> Unit) =
        JdbcSettingsBuilder(testConfiguration).apply(init)

infix fun StepBuilder.jdbc(init: JdbcTestStep.() -> Unit): JdbcTestStep =
        JdbcTestStep(testCase).apply {
            init()
            testCase.steps.add(this)
        }