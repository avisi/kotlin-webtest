package nl.avisi.kotlinwebtest.jdbc

import nl.avisi.kotlinwebtest.ExecutionContext
import nl.avisi.kotlinwebtest.TestConfiguration
import org.junit.Test
import java.util.HashMap
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RowCountValidatorTest {

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
        row_2.put("name", "")
        row_2.put("price", "57127")
        actual.add(1, row_2)

        val row_3 = hashMapOf<String, String>()
        row_3.put("name", "Skoda")
        row_3.put("price", "9000")
        actual.add(2, row_3)
        return actual
    }

    fun empty() = arrayListOf<HashMap<String, String>>()

    @Test
    fun count() {
        val validator = RowCountValidator(3, null)
        val result = validator.validate(context, request(), response(actual()))
        assertTrue(result.message) { result.success }
    }

    @Test
    fun countWrong() {
        val validator = RowCountValidator(2, null)
        val result = validator.validate(context, request(), response(actual()))
        assertFalse(result.message) { result.success }
    }

    @Test
    fun countWithColumn() {
        val validator = RowCountValidator(2, "name")
        val result = validator.validate(context, request(), response(actual()))
        assertTrue(result.message) { result.success }
    }

    @Test
    fun countWithColumnNoResults() {
        val validator = RowCountValidator(1, "name")
        val result = validator.validate(context, request(), response(empty()))
        assertFalse(result.message) { result.success }
    }

    @Test
    fun countWithColumnButDoesNotExists() {
        val validator = RowCountValidator(1, "Test")
        val result = validator.validate(context, request(), response(actual()))
        assertFalse(result.message) { result.success }
    }

    @Test
    fun countWithColumnWithHighIndex() {
        val validator = RowCountValidator(9999999, "name")
        val result = validator.validate(context, request(), response(actual()))
        assertFalse(result.message) { result.success }
    }
}