package nl.avisi.kotlinwebtest.properties

import nl.avisi.kotlinwebtest.Request
import nl.avisi.kotlinwebtest.Response
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

class PropertyRequest(val name: String) : Request {
    lateinit var expression: Expression

    constructor(name: String, expression: Expression) : this(name) {
        this.expression = expression
    }
}

class PropertyResponse : Response {
    override val success: Boolean
        get() = true
    override val message: String?
        get() = null
}

class PropertyTestStep(testCase: TestCase) : TestStep<PropertyRequest, Response>(testCase, PropertyRequest(""))

class PropertyBuilder(val request: PropertyRequest) {

    infix fun from(expression: Expression) {
        request.expression = expression
    }
}