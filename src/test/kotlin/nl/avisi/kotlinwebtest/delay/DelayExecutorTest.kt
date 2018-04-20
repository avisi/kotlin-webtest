package nl.avisi.kotlinwebtest.delay

import nl.avisi.kotlinwebtest.ExecutionContext
import nl.avisi.kotlinwebtest.TestCase
import nl.avisi.kotlinwebtest.TestConfiguration
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.test.assertFalse

class DelayExecutorTest {

    @Test
    fun execute() {
        val step = DelayTestStep(TestCase("Delay")).apply {
            delay = 1000
        }
        val response = DelayExecutor().execute(step, ExecutionContext(TestConfiguration())) as DelayStepResponse
        assertTrue(response.success)
    }

    @Test
    fun executeNegativeValue() {
        val step = DelayTestStep(TestCase("Delay")).apply {
            delay = -1000
        }
        val response = DelayExecutor().execute(step, ExecutionContext(TestConfiguration())) as DelayStepResponse
        assertFalse(response.success)
        assert(response.message == "DelayExecutor: The delay is negative")
    }

    @Test
    fun executeIntercupt() {
        val step = DelayTestStep(TestCase("Delay")).apply {
            delay = 1000
        }
        Thread.currentThread().interrupt()
        val response = DelayExecutor().execute(step, ExecutionContext(TestConfiguration())) as DelayStepResponse
        assertFalse(response.success)
        assert(response.message == "DelayExecutor: There was a unexpected error")
    }

    @Test
    fun executeNoValue() {
        val step = DelayTestStep(TestCase("Delay")).apply {
        }
        val response = DelayExecutor().execute(step, ExecutionContext(TestConfiguration())) as DelayStepResponse
        assertTrue(response.success)
    }
}