/**
 * For licensing, see LICENSE.txt
 * @author Rein Krul
 */
package nl.avisi.kotlinwebtest.properties

import nl.avisi.kotlinwebtest.StepRequest
import nl.avisi.kotlinwebtest.StepResponse
import nl.avisi.kotlinwebtest.TestCase
import nl.avisi.kotlinwebtest.TestStep
import nl.avisi.kotlinwebtest.expressions.Expression

class PropertyBag {

    private val properties: MutableMap<String, String?> = mutableMapOf()

    operator fun get(name: String): String? =
            properties[name]

    operator fun set(name: String, value: String?) {
        properties[name] = value
    }
}

class PropertyRequest(val name: String) : StepRequest {
    lateinit var expression: Expression

    constructor(name: String, expression: Expression) : this(name) {
        this.expression = expression
    }
}

class PropertyResponse : StepResponse {
    override val success: Boolean
        get() = true
    override val message: String?
        get() = null
}

class PropertyTestStep(testCase: TestCase) : TestStep<PropertyRequest, StepResponse>(testCase, PropertyRequest(""))

class PropertyBuilder(val request: PropertyRequest) {

    infix fun from(expression: Expression) {
        request.expression = expression
    }
}