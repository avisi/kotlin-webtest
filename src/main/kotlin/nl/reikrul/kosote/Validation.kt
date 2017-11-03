package nl.reikrul.kosote

interface Validator<in ResponseType : Response> {
    fun validate(response: ResponseType): Boolean
}

class ValidatorConfigurer<RequestType : Request, out ResponseType : Response>(private val builder: TestStep<RequestType, ResponseType>) {

    fun register(validator: Validator<ResponseType>) {
        builder.register(validator)
    }
}