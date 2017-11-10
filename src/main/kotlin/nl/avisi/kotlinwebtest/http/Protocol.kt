package nl.avisi.kotlinwebtest.http

enum class HttpMethod {
    GET,
    POST,
    PUT,
    DELETE,
    OPTION,
    HEAD,
    TRACE,
    PATCH
}

class HttpHeader(val name: String, val value: String) {
    operator fun component1(): String? = name
    operator fun component2(): String? = value
}

enum class HttpHeaders(val headerName: String) {

    CONTENT_TYPE("Content-Type")
}
