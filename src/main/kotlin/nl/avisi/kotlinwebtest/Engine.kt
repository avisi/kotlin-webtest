/**
 * For licensing, see LICENSE.txt
 * @author Rein Krul
 */
package nl.avisi.kotlinwebtest

import nl.avisi.kotlinwebtest.properties.PropertyBag
import nl.avisi.kotlinwebtest.xml.XmlContext
import java.net.URL
import kotlin.reflect.KClass

// Requests
interface StepRequest

interface StepResponse : Result


// TestCase
@WebTestDsl
abstract class TestStep<RequestType : StepRequest, ResponseType : StepResponse>(override val testCase: TestCase, val request: RequestType) : TestCaseBuilder {
    val validators: MutableList<Validator<RequestType, ResponseType>> = mutableListOf()
        get() = field
    val afterwards: MutableList<(context: ExecutionContext) -> Unit> = mutableListOf()
    var name: String? = null

    fun register(validator: Validator<RequestType, ResponseType>) {
        validators.add(validator)
    }

    infix fun afterwards(init: AfterwardsConfigurer.() -> Unit) {
        AfterwardsConfigurer().init()
    }

    inner class AfterwardsConfigurer {

        val assign: AssignmentBuilder = AssignmentBuilder()
    }

    inner class AssignmentBuilder {
        infix fun property(name: String): PropertyBuilder =
                PropertyBuilder(name)

        inner class PropertyBuilder(private val propertyName: String) {
//            infix fun from(value: Expression) {
//                afterwards.add({context -> context.properties[propertyName] = })
//            }

            infix fun from(value: String) {
                afterwards.add({ context -> context.properties[propertyName] = value })
            }
        }
    }

}

@WebTestDsl
class TestCase(val name: String) : TestCaseBuilder {

    val steps: MutableList<TestStep<*, *>> = mutableListOf()

    val step: StepBuilder = StepBuilder(this)

    override val testCase: TestCase
        get() = this
}

interface TestCaseBuilder {

    val testCase: TestCase
}


// Execution
interface Executor<in StepType : TestStep<*, *>> {
    fun execute(step: StepType, executionContext: ExecutionContext): StepResponse
}

class TestConfiguration(val xml: XmlContext = XmlContext(), val properties: PropertyBag = PropertyBag()) : Extendable()

abstract class Extendable {
    val extensions: MutableMap<KClass<*>, Any> = mutableMapOf()

    inline operator fun <reified T : Any> get(type: KClass<T>): T =
            extensions.getOrPut(type, { T::class.java.newInstance() }) as T

    inline operator fun <reified T : Any> set(type: KClass<T>, value: T) =
            extensions.put(type, value)
}

class ExecutionContext(val configuration: TestConfiguration,
                       val properties: PropertyBag = PropertyBag(),
                       var previousRequest: StepRequest? = null,
                       var previousResponse: StepResponse? = null)


class Endpoint(val name: String?, url: String) : Extendable() {

    val url = URL(url)
    var credentials: Credentials? = null
}