/**
 * For licensing, see LICENSE.txt
 * @author Rein Krul
 */
package nl.avisi.kotlinwebtest.http

import org.apache.commons.fileupload.MultipartStream
import org.apache.http.HttpResponse
import org.apache.http.entity.ContentType
import org.apache.http.message.BasicLineParser
import org.apache.http.util.CharArrayBuffer
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream

fun parseMultipartResponse(stream: InputStream, contentType: ContentType, response: HttpResponse): MultipartHttpResponse {
    val multipartStream = MultipartStream(stream, contentType.getParameter("boundary").toByteArray(), 4096, null)
    val parts = mutableListOf<HttpResponsePart>()
    if (multipartStream.skipPreamble()) {
        do {
            val headers = multipartStream.readHeaders()
                    .split("\r\n")
                    .filterNot(String::isBlank)
                    .map { header -> BasicLineParser().parseHeader(CharArrayBuffer(header.length).also { it.append(header) }) }
                    .map { HttpHeader(it.name, it.value) }
            // TODO: Overflow to disk (OOM can happen here)
            val inputStream = ByteArrayOutputStream()
                    .also { multipartStream.readBodyData(it) }
                    .toByteArray()
                    .let(::ByteArrayInputStream)
            parts.add(HttpResponsePart(inputStream.readBytes(), headers))
        } while (multipartStream.readBoundary())
    }
    return MultipartHttpResponse(
            response.statusLine.statusCode,
            stream.readBytes(),
            response.allHeaders.map { HttpHeader(it.name, it.value) }, parts)
}
