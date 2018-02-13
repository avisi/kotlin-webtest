package nl.avisi.kotlinwebtest.rest

import org.apache.http.HttpRequest
import org.apache.http.HttpResponse
import org.apache.http.HttpStatus
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.impl.bootstrap.HttpServer
import org.apache.http.impl.bootstrap.ServerBootstrap
import org.apache.http.protocol.HttpContext
import org.apache.http.protocol.HttpRequestHandler
import org.slf4j.LoggerFactory

class RestServer : AutoCloseable {
    val body: String = readAsString("rest/rest-response.json")
    val port = 7543

    companion object {
        private lateinit var server: HttpServer
        private val log = LoggerFactory.getLogger(RestServer::class.java)
    }

    init {
        server = ServerBootstrap
                .bootstrap()
                .setListenerPort(port)
                .setExceptionLogger { log.error("An error occurred", it) }
                .registerHandler("/rest/*", Handler(body))
                .registerHandler("/no-content/*", NoContentHandler())
                .create()
                .apply {
                    start()
                }
    }

    override fun close() {
        server.stop()
    }

    private inner class Handler(private val body: String) : HttpRequestHandler {
        override fun handle(request: HttpRequest, response: HttpResponse, context: HttpContext) {
            response.entity = StringEntity(body, ContentType.APPLICATION_JSON.withCharset(Charsets.UTF_8))
        }
    }

    private inner class NoContentHandler : HttpRequestHandler {
        override fun handle(request: HttpRequest, response: HttpResponse, context: HttpContext) {
            response.setStatusCode(HttpStatus.SC_NO_CONTENT)
        }
    }

    private fun readAsString(fileName: String) =
            Thread.currentThread().contextClassLoader.getResourceAsStream(fileName).reader().readText()
}