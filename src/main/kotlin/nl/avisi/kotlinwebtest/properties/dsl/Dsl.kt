/**
 * For licensing, see LICENSE.txt
 * @author Rein Krul
 */
package nl.avisi.kotlinwebtest.properties.dsl

import nl.avisi.kotlinwebtest.WebTest
import nl.avisi.kotlinwebtest.StepBuilder
import nl.avisi.kotlinwebtest.TestConfiguration
import nl.avisi.kotlinwebtest.properties.PropertyTestStep

fun WebTest.properties(init: PropertiesBuilder.() -> Unit) {
    val builder = PropertiesBuilder(testConfiguration)
    builder.init()
}

infix fun StepBuilder.property(init: PropertyTestStep.() -> Unit): PropertyTestStep {
    val step = PropertyTestStep(testCase)
    step.init()
    testCase.steps.add(step)
    return step
}


class PropertiesBuilder(val configuration: TestConfiguration) {

    operator fun String.minus(value: String) {
        configuration.properties[this] = value
    }

    operator fun String.minus(value: Int) {
        configuration.properties[this] = value.toString()
    }
}