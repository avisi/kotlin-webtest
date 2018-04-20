/**
 * For licensing, see LICENSE.txt
 * @author Rein Krul
 */
package nl.avisi.kotlinwebtest

import nl.avisi.kotlinwebtest.expressions.ExpressionEvaluator
import nl.avisi.kotlinwebtest.expressions.findExpressions
import nl.avisi.kotlinwebtest.properties.PropertyBag
import nl.avisi.kotlinwebtest.xml.XmlContext
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URL
import java.util.regex.Pattern
import javax.xml.bind.DatatypeConverter.printBase64Binary
import kotlin.reflect.KClass

// Requests
interface StepRequest

interface StepResponse : Result


// TestCase
@WebTestDsl
abstract class TestStep<RequestType : StepRequest, ResponseType : StepResponse>(override val testCase: TestCase, val request: RequestType) : TestCaseBuilder {
    val validators: MutableList<Validator<RequestType, ResponseType>> = mutableListOf()
    val afterwards: MutableList<After<RequestType, ResponseType>> = mutableListOf()
    var name: String? = null

    fun register(validator: Validator<RequestType, ResponseType>) {
        validators.add(validator)
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

fun interpolateExpressions(text: String, executionContext: ExecutionContext): String {
    val log = LoggerFactory.getLogger(Executor::class.java)
    val evaluator = ExpressionEvaluator(executionContext)
    var interpolatedRequestData = text
    text.findExpressions().forEach { (token, expression) ->
        val value = evaluator.evaluate(expression) ?: "".also { log.warn("Property evaluated to empty string: $token") }
        interpolatedRequestData = interpolatedRequestData.replace(token, value)
    }
    text.findFiles().forEach { (token, file) ->
        val encoded = file.readBytes()
        interpolatedRequestData = interpolatedRequestData.replace(token, printBase64Binary(encoded))
    }
    return interpolatedRequestData
}

fun String.findFiles(): List<Pair<String, File>> {
    val matcher = fileRegex.matcher(this)
    val matches = mutableListOf<String>()
    while (matcher.find()) {
        matches.add(matcher.group(1))
    }
    return matches.map { Pair("%{$it}", loadFile(it)) }.toList()
}

private fun loadFile(resourcePath: String): File {
    return try {
        File(Thread.currentThread().contextClassLoader.getResource(resourcePath).file)
    } catch (e: Exception) {
        error("Failed to load file")
    }
}

private val fileRegex = Pattern.compile("%\\{([0-9a-zA-Z./_-]+)}")