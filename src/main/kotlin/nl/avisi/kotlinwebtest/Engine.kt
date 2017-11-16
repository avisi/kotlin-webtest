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
interface Request

interface Response : Result


// TestCase
@KosoteDsl
abstract class TestStep<RequestType : Request, ResponseType : Response>(override val testCase: TestCase, val request: RequestType) : TestCaseBuilder {
    val validators: MutableList<Validator<ResponseType>> = mutableListOf()
        get() = field
    val afterwards: MutableList<(context: ExecutionContext) -> Unit> = mutableListOf()
    var name: String? = null

    fun register(validator: Validator<ResponseType>) {
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

@KosoteDsl
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
    fun execute(step: StepType, executionContext: ExecutionContext): Response
}

class TestConfiguration(val xml: XmlContext = XmlContext()) {

    val extensions: MutableList<Any> = mutableListOf()

    inline operator fun <reified T : Any> get(type: KClass<T>): T {
        return extensions.firstOrNull { type.isInstance(it) } as T? ?: T::class.java.newInstance().also { extensions.add(it) }
    }
}

class ExecutionContext(val configuration: TestConfiguration,
                       val properties: PropertyBag = PropertyBag(),
                       var previousRequest: Request? = null,
                       var previousResponse: Response? = null)


class Endpoint(val name: String, url_: String, private val configurer: (Endpoint.() -> Unit)? = null) {

    val url = URL(url_)
}