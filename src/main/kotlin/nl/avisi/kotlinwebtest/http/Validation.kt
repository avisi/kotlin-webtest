package nl.avisi.kotlinwebtest.http

import nl.avisi.kotlinwebtest.ExecutionContext
import nl.avisi.kotlinwebtest.TestStep
import nl.avisi.kotlinwebtest.Validator
import nl.avisi.kotlinwebtest.ValidatorResult


class HttpStatusValidator(val range: ClosedRange<Int>) : Validator<HttpResponse> {
    override fun validate(executionContext: ExecutionContext, response: HttpResponse): ValidatorResult {
        val http = response.http ?: error("Missing response")
        if (!range.contains(http.statusCode)) return failure("HTTP response code ${http.statusCode} not in range $range")
        return success()
    }
}

class HttpStatusValidationBuilder<ResponseType : HttpResponse>(private val step: TestStep<*, ResponseType>) {
    infix fun matches(allowedRange: ClosedRange<Int>) {
        step.validators.add(HttpStatusValidator(allowedRange))
    }

    infix fun matches(allowedCode: Int) {
        step.validators.add(HttpStatusValidator(allowedCode..allowedCode))
    }
}