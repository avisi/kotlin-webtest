/**
 * For licensing, see LICENSE.txt
 * @author Rein Krul
 */
package nl.avisi.kotlinwebtest.http

import nl.avisi.kotlinwebtest.Endpoint
import nl.avisi.kotlinwebtest.Response


abstract class HttpRequest {
    var method: HttpMethod = HttpMethod.GET
    var path: String = "/"
    var headers: MutableList<HttpHeader> = mutableListOf()

    abstract infix fun endpoint(endpoint: Endpoint)

    infix fun path(path: String) {
        this.path = path
    }

    infix fun method(method: HttpMethod) {
        this.method = method
    }

    infix fun header(header: String): RequestHeaderBuilder {
        return RequestHeaderBuilder(header, this)
    }

    inner class RequestHeaderBuilder(private val header: String, private val request: HttpRequest) {
        infix fun value(value: String) {
            request.headers.add(HttpHeader(header, value))
        }
    }
}

interface HttpResponse : Response {
    val http: ReceivedHttpResponse?
}

class ReceivedHttpResponse(val statusCode: Int,
                           val data: String,
                           val headers: List<HttpHeader>) {
    val contentType: String?
        get() = headers.firstOrNull { it.name.equals(HttpHeaders.CONTENT_TYPE.headerName, true) }?.value
}