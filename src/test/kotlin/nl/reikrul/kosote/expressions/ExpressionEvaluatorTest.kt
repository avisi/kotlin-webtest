package nl.reikrul.kosote.expressions

import nl.reikrul.kosote.ExecutionContext
import nl.reikrul.kosote.properties.PropertyBag
import org.junit.Test

import org.junit.Assert.*

class ExpressionEvaluatorTest {

    @Test
    fun evaluateConstant() {
        val properties = PropertyBag()
        val expected = "bar"
        properties["foo"] = expected
        val evaluator = ExpressionEvaluator(ExecutionContext(properties))
        val actual = evaluator.evaluate(PropertyExpression("foo"))
        assertEquals(expected, actual)
    }

    @Test
    fun evaluateProperty() {
        val evaluator = ExpressionEvaluator(ExecutionContext(PropertyBag()))
        val actual = evaluator.evaluate(ConstantExpression("bar"))
        assertEquals("bar", actual)
    }
}