/**
 * For licensing, see LICENSE.txt
 * @author Rein Krul
 */
package nl.avisi.kotlinwebtest.soap

import nl.avisi.kotlinwebtest.Endpoint
import nl.avisi.kotlinwebtest.ExecutionContext
import nl.avisi.kotlinwebtest.TestConfiguration
import nl.avisi.kotlinwebtest.http.HttpHeader
import nl.avisi.kotlinwebtest.http.HttpResponse
import nl.avisi.kotlinwebtest.http.HttpResponsePart
import nl.avisi.kotlinwebtest.http.MultipartHttpResponse
import org.apache.http.entity.ContentType
import org.apache.http.message.BasicNameValuePair
import org.junit.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue


class SoapResponseValidatorTest {

    private val emptyResponse = "".toByteArray()

    @Test
    fun validate() {
        val stepRequest = SoapStepRequest(RawSoapRequestBody("test"))
        val response = HttpResponse(200, emptyResponse, listOf(HttpHeader("Content-Type", "text/xml; charset=UTF-8")))
        val stepResponse = SoapStepResponse(response, Endpoint(null, "http://test"), true)
        val actual = SoapResponseValidator().validate(ExecutionContext(TestConfiguration()), stepRequest, stepResponse)
        assertTrue(actual.success)
        assertNull(actual.message)
    }

    @Test
    fun validateMtom() {
        val rootContentType = ContentType.create("multipart/related").withParameters(BasicNameValuePair("type", MIMETYPE_SOAPXML),
                BasicNameValuePair("start", "<root>"),
                BasicNameValuePair("start-info", MIMETYPE_SOAPXML)).toString()
        val soapContentType = ContentType.create("application/xop+xml").withCharset(Charsets.UTF_8).withParameters(BasicNameValuePair("type", MIMETYPE_SOAPXML)).toString()

        val stepRequest = SoapStepRequest(RawSoapRequestBody("test"))
        val soapPartResponse = HttpResponsePart(emptyResponse, listOf(HttpHeader("Content-Type", soapContentType)))
        val rootPartResponse = MultipartHttpResponse(200, emptyResponse, listOf(HttpHeader("Content-Type", rootContentType)), listOf(soapPartResponse))
        val stepResponse = SoapStepResponse(rootPartResponse, Endpoint(null, "http://test"), true)
        val actual = SoapResponseValidator().validate(ExecutionContext(TestConfiguration()), stepRequest, stepResponse)
        assertTrue(actual.success)
        assertNull(actual.message)
    }
}