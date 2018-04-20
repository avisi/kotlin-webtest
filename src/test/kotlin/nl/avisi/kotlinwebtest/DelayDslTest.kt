package nl.avisi.kotlinwebtest

import nl.avisi.kotlinwebtest.delay.dsl.delay
import org.junit.Test

class DelayDslTest : WebTest() {

    @Test
    fun test() {
        test("Delay Test") {
            step delay {
                delay = 1000
            }
        }
        execute()
    }

}