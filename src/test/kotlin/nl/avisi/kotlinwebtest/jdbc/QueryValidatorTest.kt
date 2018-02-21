package nl.avisi.kotlinwebtest.jdbc

import nl.avisi.kotlinwebtest.ExecutionContext
import nl.avisi.kotlinwebtest.TestConfiguration
import nl.avisi.kotlinwebtest.expressions.ConstantExpression
import org.junit.Test
import java.util.HashMap
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QueryValidatorTest {

    companion object {
        private val context = ExecutionContext(TestConfiguration())
    }

    fun actual(): ArrayList<HashMap<String, String>> {
        val actual = arrayListOf<HashMap<String, String>>()
        val row_1 = hashMapOf<String, String>()
        row_1.put("name", "Audi")
        row_1.put("price", "52642")
        actual.add(0, row_1)

        val row_2 = hashMapOf<String, String>()
        row_2.put("name", "Mercedes")
        row_2.put("price", "57127")
        actual.add(1, row_2)
        return actual
    }

    @Test
    fun queryWithoutRegex() {
        val validator = QueryValidator("name", 0, ConstantExpression("Audi"), false)
        val result = validator.validate(context, request(), response(actual()))
        assertTrue(result.message) { result.success }
    }

    @Test
    fun queryWithoutExpected() {
        val validator = QueryValidator("name", 0, null, false)
        val result = validator.validate(context, request(), response(actual()))
        assertFalse(result.message) { result.success }
    }

    @Test
    fun queryNoMatchWithoutRegex() {
        val validator = QueryValidator("name", 1, ConstantExpression("Audi"), false)
        val result = validator.validate(context, request(), response(actual()))
        assertFalse(result.message) { result.success }
    }

    @Test
    fun queryWithRegex() {
        val validator = QueryValidator("name", 0, ConstantExpression(".*.ud.*."), true)
        val result = validator.validate(context, request(), response(actual()))
        assertTrue(result.message) { result.success }
    }

    @Test
    fun queryNoMatchWithRegex() {
        val validator = QueryValidator("name", 1, ConstantExpression(".*.udi.*."), true)
        val result = validator.validate(context, request(), response(actual()))
        assertFalse(result.message) { result.success }
    }

    @Test
    fun columnEmptyWithRegex() {
        val validator = QueryValidator("", 1, ConstantExpression(".*.udi.*."), true)
        val result = validator.validate(context, request(), response(actual()))
        assertFalse(result.message) { result.success }
    }

    @Test
    fun columnNotFoundWithRegex() {
        val validator = QueryValidator("notFound", 1, ConstantExpression(".*.udi.*."), true)
        val result = validator.validate(context, request(), response(actual()))
        assertFalse(result.message) { result.success }
    }

    @Test
    fun queryWithoutRegexWithoutPosition() {
        val validator = QueryValidator("name", null, ConstantExpression("Audi"), false)
        val result = validator.validate(context, request(), response(actual()))
        assertTrue(result.message) { result.success }
    }

    @Test
    fun queryWithRegexWithoutPosition() {
        val validator = QueryValidator("name", null, ConstantExpression(".*.ud.*."), true)
        val result = validator.validate(context, request(), response(actual()))
        assertTrue(result.message) { result.success }
    }

    @Test
    fun columnEmptyWithRegexWithoutPosition() {
        val validator = QueryValidator("", null, ConstantExpression(".*.ud.*."), true)
        val result = validator.validate(context, request(), response(actual()))
        assertFalse(result.message) { result.success }
    }

    @Test
    fun columnNotFoundWithRegexWithoutPosition() {
        val validator = QueryValidator("notFound", null, ConstantExpression(".*.ud.*."), true)
        val result = validator.validate(context, request(), response(actual()))
        assertFalse(result.message) { result.success }
    }
}