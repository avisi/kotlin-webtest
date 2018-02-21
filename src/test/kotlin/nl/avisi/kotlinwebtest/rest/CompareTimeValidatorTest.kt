package nl.avisi.kotlinwebtest.rest

import nl.avisi.kotlinwebtest.ExecutionContext
import nl.avisi.kotlinwebtest.TestConfiguration
import nl.avisi.kotlinwebtest.soap.InequalityType
import org.junit.Test

class CompareTimeValidatorTest {
    companion object {
        private val context = ExecutionContext(TestConfiguration())
    }

    val actual = """
        [
        {
            "time1": [2018,4,17,16,3,41,826000000],
            "time2": [2018,4,17,16,3,42,826000000]
        },
        {
            "name": test,
            "empty": null
        }
        ]
        """"

    @Test
    fun TimeNotParsable() {
        val validator = CompareTimeValidator("$[1].name", "$[0].time2", 1000, InequalityType.LessThan)
        val result = validator.validate(context, request(), response(actual))
        kotlin.test.assertFalse(result.message) { result.success }
    }

    @Test
    fun FirstValueNull() {
        val validator = CompareTimeValidator("$[1].empty", "$[1].time1", 1000, InequalityType.LessThan)
        val result = validator.validate(context, request(), response(actual))
        kotlin.test.assertFalse(result.message) { result.success }
    }

    @Test
    fun SecondValueNull() {
        val validator = CompareTimeValidator("$[1].time1", "$[1].empty", 1000, InequalityType.LessThan)
        val result = validator.validate(context, request(), response(actual))
        kotlin.test.assertFalse(result.message) { result.success }
    }

    @Test
    fun FirstValueDoesnotExists() {
        val validator = CompareTimeValidator("$[1].test", "$[0].time1", 1000, InequalityType.LessThan)
        val result = validator.validate(context, request(), response(actual))
        kotlin.test.assertFalse(result.message) { result.success }
    }

    @Test
    fun SecondValueDoesnotExists() {
        val validator = CompareTimeValidator("$[0].time1", "$[1].test", 1000, InequalityType.LessThan)
        val result = validator.validate(context, request(), response(actual))
        kotlin.test.assertFalse(result.message) { result.success }
    }

    @Test
    fun execute() {
        val validator = CompareTimeValidator("$[0].time1", "$[0].time2", 1001, InequalityType.LessThan)
        val result = validator.validate(context, request(), response(actual))
        kotlin.test.assertTrue(result.message) { result.success }
    }
}