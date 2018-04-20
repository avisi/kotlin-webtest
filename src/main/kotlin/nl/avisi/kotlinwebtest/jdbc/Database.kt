/**
 * For licensing, see LICENSE.txt
 * @author Martijn Heuvelink
 */
package nl.avisi.kotlinwebtest.jdbc

import nl.avisi.kotlinwebtest.StepRequest
import nl.avisi.kotlinwebtest.StepResponse

abstract class DatabaseRequest : StepRequest {
    var driver: String? = null
    var url: String? = null
    var query: String? = null

    infix fun driver(driver: String) {
        this.driver = driver
    }

    infix fun url(url: String) {
        this.url = url
    }
}

interface DatabaseStepResponse : StepResponse {
    val resultSet: List<HashMap<String, String>>
}