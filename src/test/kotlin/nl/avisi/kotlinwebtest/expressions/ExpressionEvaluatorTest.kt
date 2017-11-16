/**
 * For licensing, see LICENSE.txt
 * @author Rein Krul
 */
package nl.avisi.kotlinwebtest.expressions

import nl.avisi.kotlinwebtest.ExecutionContext
import nl.avisi.kotlinwebtest.TestConfiguration
import nl.avisi.kotlinwebtest.properties.PropertyBag
import org.junit.Test

import org.junit.Assert.*

class ExpressionEvaluatorTest {

    @Test
    fun evaluateConstant() {
        val properties = PropertyBag()
        val expected = "bar"
        properties["foo"] = expected
        val evaluator = ExpressionEvaluator(ExecutionContext(TestConfiguration(), properties))
        val actual = evaluator.evaluate(PropertyExpression("foo"))
        assertEquals(expected, actual)
    }

    @Test
    fun evaluateProperty() {
        val evaluator = ExpressionEvaluator(ExecutionContext(TestConfiguration(), PropertyBag()))
        val actual = evaluator.evaluate(ConstantExpression("bar"))
        assertEquals("bar", actual)
    }
}