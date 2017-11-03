package nl.reikrul.kosote

import nl.reikrul.kosote.properties.PropertyBag
import java.net.URL

// Requests
interface Request

interface ExecutedRequest<out Body> : Request {
    val body: Body
}

interface Response

class HttpResponse(val statusCode: Int, val data: String)

class RequestConfigurer<in RequestType : Request, ResponseType : Response>(private val step: TestStep<RequestType, ResponseType>) {

    fun register(request: RequestType) {
        step.register(request)
    }
}

// TestCase
abstract class TestStep<RequestType : Request, ResponseType : Response>(override val testCase: TestCase) : TestCaseBuilder {
    val validators: MutableList<Validator<ResponseType>> = mutableListOf()
    private var _request: RequestType? = null

    val request: RequestType?
        get() = _request

    fun register(validator: Validator<ResponseType>) {
        validators.add(validator)
    }

    fun register(request: RequestType) {
        if (this._request != null) {
            error("Request already configured")
        }
        this._request = request
    }

    override fun <RequestType : Request, ResponseType : Response> register(step: TestStep<RequestType, ResponseType>) {
        testCase.register(step)
    }

}

class TestCase(val name: String) : TestCaseBuilder {

    private val _steps: MutableList<TestStep<Request, Response>> = mutableListOf()
    val steps: List<TestStep<Request, Response>>
        get() = _steps.toList()

    override val testCase: TestCase
        get() = this

    override fun <RequestType : Request, ResponseType : Response> register(step: TestStep<RequestType, ResponseType>) {
        _steps.add(step as TestStep<Request, Response>)
    }
}

interface TestCaseBuilder {

    fun <RequestType : Request, ResponseType : Response> register(step: TestStep<RequestType, ResponseType>)

    val testCase: TestCase
}


// Execution
interface Executor<in StepType : TestStep<*, *>> {
    fun execute(step: StepType, executionContext: ExecutionContext): Response
}


class ExecutionContext(val properties: PropertyBag = PropertyBag(),
                       var previousRequest: Request? = null,
                       var previousResponse: Response? = null)


class Endpoint(val name: String, url_: String) {

    val url = URL(url_)
}

interface Engine