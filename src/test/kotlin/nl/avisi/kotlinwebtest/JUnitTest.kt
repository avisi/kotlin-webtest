/**
 * For licensing, see LICENSE.txt
 * @author Rein Krul
 */
package nl.avisi.kotlinwebtest

import nl.avisi.kotlinwebtest.properties.dsl.properties
import nl.avisi.kotlinwebtest.properties.dsl.property
import nl.avisi.kotlinwebtest.soap.dsl.soap

/**
 * Class intended to demonstrate/test JUnit DSL features.
 */
class JUnitTest : KosoteTest() {

    override fun configure() {
        properties {
            "a" - "b"
        }
        soap {
            default { }
        }
    }

    fun test() {
        test("Example Test") {
            step property {
            }
            step soap {
                name = "SOAP Step 1"
                request file "some-request"
            }
        }
        execute()
    }
}