/**
 * For licensing, see LICENSE.txt
 * @author Rein Krul
 */
package nl.avisi.kotlinwebtest.http

import nl.avisi.kotlinwebtest.ExecutionContext
import nl.avisi.kotlinwebtest.TestStep
import nl.avisi.kotlinwebtest.Validator
import nl.avisi.kotlinwebtest.ValidatorResult


class HttpStatusValidator(val range: ClosedRange<Int>) : Validator<HttpRequest, HttpStepResponse> {
    override fun validate(executionContext: ExecutionContext, request: HttpRequest, response: HttpStepResponse): ValidatorResult {
        val http = response.http ?: error("Missing response")
        if (!range.contains(http.statusCode)) return failure("HTTP response code ${http.statusCode} not in range $range")
        return success()
    }
}

class HttpStatusValidationBuilder<RequestType : HttpRequest, ResponseType : HttpStepResponse>(private val step: TestStep<RequestType, ResponseType>) {
    infix fun matches(allowedRange: ClosedRange<Int>) {
        step.validators.add(HttpStatusValidator(allowedRange))
    }

    infix fun matches(allowedCode: Int) {
        step.validators.add(HttpStatusValidator(allowedCode..allowedCode))
    }
}

class HttpHeaderValidator(val header: String, val value: String) : Validator<HttpRequest, HttpStepResponse> {
    override fun validate(executionContext: ExecutionContext, request: HttpRequest, response: HttpStepResponse): ValidatorResult {
        val http = response.http ?: error("Missing response")

        val headers = http.headers.filter { it.name == header }
        if (headers.size > 1) failure("Header $header is more then once present.")
        val header = headers.firstOrNull { it.name == header } ?: return failure("Header $header is not present, got: ${http.headers.joinToString(", ") { it.name }}")
        return if (header.value == value)
            success()
        else
            failure("Header: $header expected: $value, but was: ${header.value}")
    }
}

class HttpHeaderValidationBuilder<RequestType : HttpRequest, ResponseType : HttpStepResponse>(private val header: String, private val step: TestStep<RequestType, ResponseType>) {

    infix fun matches(value: String) {
        step.validators.add(HttpHeaderValidator(header, value))
    }
}