/**
 * For licensing, see LICENSE.txt
 * @author Martijn Heuvelink
 */
package nl.avisi.kotlinwebtest.delay.dsl

import nl.avisi.kotlinwebtest.StepBuilder
import nl.avisi.kotlinwebtest.delay.DelayTestStep

infix fun StepBuilder.delay(init: DelayTestStep.() -> Unit): DelayTestStep =
        DelayTestStep(testCase).apply {
            init()
            testCase.steps.add(this)
        }