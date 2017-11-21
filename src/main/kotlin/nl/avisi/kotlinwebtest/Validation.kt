/**
 * For licensing, see LICENSE.txt
 * @author Rein Krul
 */
package nl.avisi.kotlinwebtest

interface Validator<in RequestType : Request, in ResponseType : Response> {
    fun validate(executionContext: ExecutionContext, request: RequestType, response: ResponseType): ValidatorResult

    fun success(): SuccessValidatorResult =
            SuccessValidatorResult(this)

    fun failure(message: String): FailedValidatorResult =
            FailedValidatorResult(this, message)
}

interface ValidatorResult : Result {
    val validator: Validator<*, *>
}

class SuccessValidatorResult(override val validator: Validator<*, *>) : ValidatorResult {
    override val success: Boolean = true
    override val message: String? = null
}

class FailedValidatorResult(override val validator: Validator<*, *>,
                            override val message: String) : ValidatorResult {
    override val success: Boolean = false
}

class ValidatorConfigurer<out RequestType : Request, out ResponseType : Response>(private val builder: TestStep<RequestType, ResponseType>) {

    fun register(validator: Validator<RequestType, ResponseType>) {
        builder.register(validator)
    }
}