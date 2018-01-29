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

    @Test
    fun validateRegexDate() {
        val expected = """
            {
                "bar": "01-01-2017"
            }
            """
        val actual = "{\"bar\": \"01-02-2017\"}"
        val validator = JsonValidator(CompareMode.STRICT, ConstantExpression(expected), "bar" to """^\s*(3[01]|[12][0-9]|0?[1-9])\-(1[012]|0?[1-9])\-((?:19|20)\d{2})\s*$""")
        val result = validator.validate(context, request(), response(actual))
        assertTrue(result.message) { result.success }
    }

    @Test
    fun validateRegexDateArray() {
        val expected = """
            [
                {
                    "foo": [
                        2018,
                        29,
                        10
                    ]
                }
            ]
            """
        val actual = "[\n{\n\"foo\": [\n2018,\n29,\n11\n]\n}\n]"
        val validator = JsonValidator(CompareMode.STRICT, ConstantExpression(expected), "*.foo" to """\[\d{4},\d{2},\d{2}\]""")
        val result = validator.validate(context, request(), response(actual))
        assertTrue(result.message) { result.success }
    }

    @Test
    fun validateRegexDateNested() {
        val expected = """
            [
                {
                    "foo": {
                        "bar": [
                            2018,
                            29,
                            10
                        ]
                    },
                    "foo": {
                        "bar": [
                            2017,
                            29,
                            10
                        ]
                    }
                }
            ]
            """
        val actual = "[\n{\n\"foo\": {\n\"bar\": [\n2018,\n29,\n11\n]\n},\n\"foo\": {\n\"bar\": [\n2017,\n29,\n11\n]\n}\n}\n]"
        val validator = JsonValidator(CompareMode.STRICT, ConstantExpression(expected), "*.foo.bar" to """\[\d{4},\d{2},\d{2}\]""")
        val result = validator.validate(context, request(), response(actual))
        assertTrue(result.message) { result.success }
    }
}