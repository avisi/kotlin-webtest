package nl.avisi.kotlinwebtest.rest

import nl.avisi.kotlinwebtest.ExecutionContext
import nl.avisi.kotlinwebtest.TestConfiguration
import nl.avisi.kotlinwebtest.http.HttpStatusValidator
import org.junit.Test
import kotlin.test.assertFalse

class HttpStatusValidatorTest {
    companion object {
        private val context = ExecutionContext(TestConfiguration())
    }

    @Test
    fun testHttpStatus() {
        val actual = """
            [
                {
                    "foo": {
                        "bar": [
                            2018,
                            29,
                            10
                        ]
                    }
                }
            ]
            """
        val validator = HttpStatusValidator(200..200)
        val result = validator.validate(context, request(), response(actual))
        kotlin.test.assertTrue(result.message) { result.success }
    }

    @Test
    fun testHttpStatusDoesntMatch() {
        val actual = """
            [
                {
                    "foo": {
                        "bar": [
                            2018,
                            29,
                            10
                        ]
                    }
                }
            ]
            """
        val validator = HttpStatusValidator(200..200)
        val result = validator.validate(context, request(), responseNotFound(actual))
        kotlin.test.assertFalse(result.message) { result.success }
    }

    @Test
    fun testHttpStatusNoResponse() {
        val validator = HttpStatusValidator(200..200)
        val result = validator.validate(context, request(), responseNoResponse())
        assertFalse(result.success)
        assert(result.message == "Missing response")
    }
}