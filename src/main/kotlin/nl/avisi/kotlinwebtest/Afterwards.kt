/**
 * For licensing, see LICENSE.txt
 * @author Martijn Heuvelink
 */
package nl.avisi.kotlinwebtest

interface After<in RequestType : StepRequest, in ResponseType : StepResponse> {
    fun afterwards(executionContext: ExecutionContext, request: RequestType, response: ResponseType): AfterResult

    fun success(): AfterResult =
            SuccessAfterResult(this)

    fun failure(message: String): AfterResult =
            FailureAfterResult(this, message)
}

interface AfterResult : Result {
    val validator: After<*, *>
}

class SuccessAfterResult(override val validator: After<*, *>) : AfterResult {
    override val success: Boolean = true
    override val message: String? = null
}

class FailureAfterResult(override val validator: After<*, *>,
                         override val message: String) : AfterResult {
    override val success: Boolean = false
}

enum class Source {
    RESPONSE,
    REQUEST
}