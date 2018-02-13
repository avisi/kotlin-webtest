/**
 * For licensing, see LICENSE.txt
 * @author Rein Krul
 */
package nl.avisi.kotlinwebtest.http

import nl.avisi.kotlinwebtest.Credentials
import nl.avisi.kotlinwebtest.Endpoint
import nl.avisi.kotlinwebtest.StepRequest
import nl.avisi.kotlinwebtest.StepResponse


abstract class HttpRequest : StepRequest {
    var method: HttpMethod = HttpMethod.GET
    var path: String? = null
    var headers: MutableList<HttpHeader> = mutableListOf()
    var credentials: Credentials? = null

    abstract infix fun endpoint(endpoint: Endpoint)

    infix fun path(path: String) {
        this.path = path
    }

    infix fun method(method: HttpMethod) {
        this.method = method
    }

    infix fun credentials(credentials: Credentials) {
        this.credentials = credentials
    }

    infix fun header(header: String): RequestHeaderBuilder =
            RequestHeaderBuilder(header, this)

    inner class RequestHeaderBuilder(private val header: String, private val request: HttpRequest) {
        infix fun value(value: String) {
            request.headers.add(HttpHeader(header, value))
        }
    }
}

interface HttpStepResponse : StepResponse {
    val http: HttpResponse?
}

open class HttpResponse(val statusCode: Int, data: ByteArray?, headers: List<HttpHeader>) : HttpResponsePart(data, headers)

open class HttpResponsePart(val data: ByteArray?,
                            val headers: List<HttpHeader>) {
    val dataAsString: String?
        get() =
            data?.toString(Charsets.UTF_8)

    val contentType: String?
        get() = headers.firstOrNull { it.name.equals(HttpHeaders.CONTENT_TYPE.headerName, true) }?.value
}

class MultipartHttpResponse(statusCode: Int, mimeBodyPart: ByteArray, headers: List<HttpHeader>, val parts: List<HttpResponsePart>) : HttpResponse(statusCode, mimeBodyPart, headers)
