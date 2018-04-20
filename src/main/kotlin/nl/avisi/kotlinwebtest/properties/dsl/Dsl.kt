/**
 * For licensing, see LICENSE.txt
 * @author Rein Krul
 */
package nl.avisi.kotlinwebtest.properties.dsl

import nl.avisi.kotlinwebtest.StepBuilder
import nl.avisi.kotlinwebtest.TestConfiguration
import nl.avisi.kotlinwebtest.WebTest
import nl.avisi.kotlinwebtest.properties.PropertyTestStep

fun WebTest.properties(init: PropertiesBuilder.() -> Unit) =
        PropertiesBuilder(testConfiguration).apply(init)

infix fun StepBuilder.property(init: PropertyTestStep.() -> Unit): PropertyTestStep =
        PropertyTestStep(testCase).apply {
            init()
            testCase.steps.add(this)
        }

class PropertiesBuilder(val configuration: TestConfiguration) {

    operator fun String.minus(value: String) {
        configuration.properties[this] = value
    }

    operator fun String.minus(value: Int) {
        configuration.properties[this] = value.toString()
    }
}