package nl.reikrul.kosote.properties

import nl.reikrul.kosote.Request
import nl.reikrul.kosote.RequestConfigurer
import nl.reikrul.kosote.Response
import nl.reikrul.kosote.TestCase
import nl.reikrul.kosote.TestCaseBuilder
import nl.reikrul.kosote.TestStep
import nl.reikrul.kosote.expressions.ConstantExpression
import nl.reikrul.kosote.expressions.Expression

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

class PropertyResponse : Response

class PropertyTestStep(testCase: TestCase) : TestStep<PropertyRequest, Response>(testCase)

class PropertyBuilder(val request: PropertyRequest) {

    infix fun from(expression: Expression) {
        request.expression = expression
    }
}
fun TestCaseBuilder.property(name: String): PropertyBuilder {

}

fun TestCaseBuilder.property(name: String, value: Expression): TestCaseBuilder =
        PropertyTestStep(testCase).also {
            val configurer = RequestConfigurer(it)
            val request = PropertyRequest(name, value)
            configurer.register(request)
            register(it)
        }

fun TestCaseBuilder.property(name: String, value: String): TestCaseBuilder {
    PropertyTestStep(testCase).also {
        val configurer = RequestConfigurer(it)
        val request = PropertyRequest(name).apply { expression = ConstantExpression(value) }
        configurer.register(request)
        register(it)
    }
    return this
}