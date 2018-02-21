/**
 * For licensing, see LICENSE.txt
 * @author Martijn Heuvelink
 */
package nl.avisi.kotlinwebtest.delay

import nl.avisi.kotlinwebtest.ExecutionContext
import nl.avisi.kotlinwebtest.Executor
import nl.avisi.kotlinwebtest.StepRequest
import nl.avisi.kotlinwebtest.StepResponse
import nl.avisi.kotlinwebtest.TestCase
import nl.avisi.kotlinwebtest.TestStep
import org.slf4j.LoggerFactory
import java.lang.IllegalArgumentException

sealed class DelayResult
class DelaySuccess : DelayResult()
class DelayFailure(val message: String) : DelayResult()

class DelayStepRequest : StepRequest

data class DelayStepResponse(override val success: Boolean,
                             override val message: String? = null) : StepResponse

class DelayTestStep(testCase: TestCase) : TestStep<DelayStepRequest, DelayStepResponse>(testCase, DelayStepRequest()) {
    var delay: Long = 0
}

class DelayExecutor : Executor<DelayTestStep> {

    companion object {
        private val log = LoggerFactory.getLogger(DelayExecutor::class.java)
    }

    override fun execute(step: DelayTestStep, executionContext: ExecutionContext): StepResponse =
            execute(step.delay).let {
                when (it) {
                    is DelaySuccess -> DelayStepResponse(true)
                    is DelayFailure -> DelayStepResponse(false, it.message)
                }
            }

    private fun execute(delay: Long): DelayResult =
            try {
                log.info("${this.javaClass.simpleName}: {}", delay)
                Thread.sleep(delay)
                DelaySuccess()
            } catch (e: IllegalArgumentException) {
                log.error("${this.javaClass.simpleName}: ${e.message}")
                DelayFailure("${this.javaClass.simpleName}: The delay is negative")
            } catch (e: InterruptedException) {
                log.error("${this.javaClass.simpleName}: ${e.message}")
                DelayFailure("${this.javaClass.simpleName}: There was a unexpected error")
            }
}