/**
 * For licensing, see LICENSE.txt
 * @author Rein Krul
 */
package nl.avisi.kotlinwebtest.expressions

import nl.avisi.kotlinwebtest.ExecutionContext
import java.util.regex.Pattern

interface Expression

class CompositeExpression(val first: Expression, val second: Expression) : Expression

operator fun Expression.plus(other: Expression): CompositeExpression =
        CompositeExpression(this, other)


data class ConstantExpression(val value: String) : Expression
data class PropertyExpression(val name: String) : Expression

class ExpressionEvaluator(private val executionContext: ExecutionContext) {

    fun evaluate(expression: Expression): String? =
            when (expression) {
                is ConstantExpression -> expression.value
                is PropertyExpression -> executionContext.properties[expression.name] ?: executionContext.configuration.properties[expression.name]
                else -> error("Unable to evaluate expression: $expression")
            }
}

fun findExpressions(text: String): List<Pair<String, PropertyExpression>> {
    val matcher = expressionRegex.matcher(text)
    val matches = mutableListOf<String>()
    while (matcher.find()) {
        matches.add(matcher.group(1))
    }
    return matches.map { Pair("#{$it}", PropertyExpression(it)) }.toList()
}

private val expressionRegex = Pattern.compile("#\\{([0-9a-zA-Z_-]+)}")