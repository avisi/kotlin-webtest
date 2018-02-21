package nl.avisi.kotlinwebtest.jdbc

import nl.avisi.kotlinwebtest.ExecutionContext
import nl.avisi.kotlinwebtest.TestConfiguration
import nl.avisi.kotlinwebtest.soap.InequalityType
import org.junit.Test
import java.util.HashMap
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CompareTimeValidatorTest {

    companion object {
        private val context = ExecutionContext(TestConfiguration())
    }

    fun actual(): ArrayList<HashMap<String, String>> {
        val actual = arrayListOf<HashMap<String, String>>()
        val row_1 = hashMapOf<String, String>()
        row_1.put("time", "2013-10-21 00:00:11")
        row_1.put("time1", "2013-10-21 00:00:12")
        actual.add(0, row_1)

        val row_2 = hashMapOf<String, String>()
        row_2.put("name", "")
        row_2.put("time", "2013-10-21 00:00:00")
        actual.add(1, row_2)
        return actual
    }

    fun empty() = arrayListOf<HashMap<String, String>>()

    @Test
    fun TimeNotParsable() {
        val validator = CompareTimeValidator("name", 0, "time", 0, 1000, InequalityType.LessThan)
        val result = validator.validate(context, request(), response(actual()))
        assertFalse(result.message) { result.success }
    }

    @Test
    fun FirstValueNull() {
        val validator = CompareTimeValidator("name", 1, "time", 1, 1000, InequalityType.LessThan)
        val result = validator.validate(context, request(), response(actual()))
        assertFalse(result.message) { result.success }
    }

    @Test
    fun SecondValueNull() {
        val validator = CompareTimeValidator("time", 1, "name", 1, 1000, InequalityType.LessThan)
        val result = validator.validate(context, request(), response(actual()))
        assertFalse(result.message) { result.success }
    }

    @Test
    fun FirstValueDoesnotExists() {
        val validator = CompareTimeValidator("test", 1, "time", 1, 1000, InequalityType.LessThan)
        val result = validator.validate(context, request(), response(actual()))
        assertFalse(result.message) { result.success }
    }

    @Test
    fun SecondValueDoesnotExists() {
        val validator = CompareTimeValidator("time", 1, "test", 1, 1000, InequalityType.LessThan)
        val result = validator.validate(context, request(), response(actual()))
        assertFalse(result.message) { result.success }
    }

    @Test
    fun FirstValueOutOfBounds() {
        val validator = CompareTimeValidator("time", 1000, "test", 1, 1000, InequalityType.LessThan)
        val result = validator.validate(context, request(), response(actual()))
        assertFalse(result.message) { result.success }
    }

    @Test
    fun SecondValueOutOfBounds() {
        val validator = CompareTimeValidator("time", 1, "test", 1000, 1000, InequalityType.LessThan)
        val result = validator.validate(context, request(), response(actual()))
        assertFalse(result.message) { result.success }
    }


    @Test
    fun execute() {
        val validator = CompareTimeValidator("time", 0, "time1", 0, 1001, InequalityType.LessThan)
        val result = validator.validate(context, request(), response(actual()))
        assertTrue(result.message) { result.success }
    }
}