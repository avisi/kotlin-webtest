package nl.avisi.kotlinwebtest.rest

import nl.avisi.kotlinwebtest.ExecutionContext
import nl.avisi.kotlinwebtest.TestConfiguration
import nl.avisi.kotlinwebtest.expressions.ConstantExpression
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JsonValidatorTest {
    companion object {
        private val context = ExecutionContext(TestConfiguration())
    }

    @Test
    fun validate() {
        val expected = """
            {
                "foo": "bar"
            }
            """
        val actual = "{\"foo\": \"bar\"}"
        val validator = JsonValidator(CompareMode.STRICT, ConstantExpression(expected))
        val result = validator.validate(context, request(), response(actual))
        assertTrue(result.message) { result.success }
    }

    @Test
    fun validateWildcard() {
        val expected = """
            {
                "foo": "**"
            }
            """
        val actual = "{\"foo\": \"wildcard-matches\"}"
        val validator = JsonValidator(CompareMode.STRICT, ConstantExpression(expected))
        val result = validator.validate(context, request(), response(actual))
        assertTrue(result.message) { result.success }
    }

    @Test
    fun validateInvalidJson() {
        val expected = """
            {
                foobar
            }
            """
        val actual = "{\"foo\": \"bar\"}"
        val validator = JsonValidator(CompareMode.STRICT, ConstantExpression(expected))
        val result = validator.validate(context, request(), response(actual))
        assertFalse(result.message) { result.success }
    }

    @Test
    fun validateMismatch() {
        val expected = """
            {
                "bar": "foo"
            }
            """
        val actual = "{\"foo\": \"bar\"}"
        val validator = JsonValidator(CompareMode.STRICT, ConstantExpression(expected))
        val result = validator.validate(context, request(), response(actual))
        assertFalse(result.message) { result.success }
    }

    @Test
    fun validateLenientMode() {
        val expected = """
            {
                "bar": "foo"
            }
            """
        val actual = "{\"foo\": \"bar\",\"bar\":\"foo\"}"
        val validator = JsonValidator(CompareMode.LENIENT, ConstantExpression(expected))
        val result = validator.validate(context, request(), response(actual))
        assertTrue(result.message) { result.success }
    }


}