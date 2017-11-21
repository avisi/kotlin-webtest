/**
 * For licensing, see LICENSE.txt
 * @author Rein Krul
 */
package nl.avisi.kotlinwebtest.soap

import nl.avisi.kotlinwebtest.Endpoint
import nl.avisi.kotlinwebtest.http.ReceivedHttpResponse

fun request() =
        SoapRequest(RawSoapRequestBody(emptySoapEnvelope))

fun response(xml: String) =
        SoapResponse(ReceivedHttpResponse(200, xml, listOf()), Endpoint(null, "http://localhost"), true)

val emptySoapEnvelope = """
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
  </soap:Body>
</soap:Envelope>
""".trim()

val soapClientFault = """
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <soap:Fault>
      <faultcode>soap:Client</faultcode>
      <faultstring>An error occured</faultstring>
    </soap:Fault>
  </soap:Body>
</soap:Envelope>
""".trim()