package nl.avisi.kotlinwebtest.jdbc

import nl.avisi.kotlinwebtest.ExecutionContext
import nl.avisi.kotlinwebtest.TestConfiguration
import org.junit.Test
import java.util.HashMap
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BooleanValidatorTest {

    companion object {
        private val context = ExecutionContext(TestConfiguration())
    }

    fun actual(): ArrayList<HashMap<String, String>> {
        val actual = arrayListOf<HashMap<String, String>>()
        val row_1 = hashMapOf<String, String>()
        row_1.put("name", "")
        row_1.put("price", "52642")
        actual.add(0, row_1)

        val row_2 = hashMapOf<String, String>()
        row_2.put("name", "Mercedes")
        row_2.put("price", "57127")
        actual.add(1, row_2)
        return actual
    }

    fun empty() = arrayListOf<HashMap<String, String>>()

    @Test
    fun isNull() {
        val validator = BooleanValidator("name", 0, true)
        val result = validator.validate(context, request(), response(actual()))
        assertTrue(result.message) { result.success }
    }

    @Test
    fun NoMatchExpectedNull() {
        val validator = BooleanValidator("name", 0, true)
        val result = validator.validate(context, request(), response(actual()))
        assertTrue(result.message) { result.success }
    }

    @Test
    fun NoMatchExpectedNotNull() {
        val validator = BooleanValidator("name", 1, false)
        val result = validator.validate(context, request(), response(actual()))
        assertTrue(result.message) { result.success }
    }

    @Test
    fun NoResults() {
        val validator = BooleanValidator("name", 0, false)
        val result = validator.validate(context, request(), response(empty()))
        assertFalse(result.message) { result.success }
    }

    @Test
    fun isNullWithoutPosition() {
        val validator = BooleanValidator("name", null, false)
        val result = validator.validate(context, request(), response(actual()))
        assertFalse(result.message) { result.success }
    }

    @Test
    fun OutOfBounds() {
        val validator = BooleanValidator("name", 999999, false)
        val result = validator.validate(context, request(), response(actual()))
        assertFalse(result.message) { result.success }
    }

    @Test
    fun ColumnNotFound() {
        val validator = BooleanValidator("test", 0, true)
        val result = validator.validate(context, request(), response(actual()))
        assertFalse(result.message) { result.success }
    }
}