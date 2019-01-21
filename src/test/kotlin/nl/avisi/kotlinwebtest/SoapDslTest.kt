package nl.avisi.kotlinwebtest

import nl.avisi.kotlinwebtest.expressions.property
import nl.avisi.kotlinwebtest.properties.dsl.properties
import nl.avisi.kotlinwebtest.soap.SoapServer
import nl.avisi.kotlinwebtest.soap.dsl.afterwards
import nl.avisi.kotlinwebtest.soap.dsl.attachment
import nl.avisi.kotlinwebtest.soap.dsl.soap
import nl.avisi.kotlinwebtest.soap.dsl.validate
import nl.avisi.kotlinwebtest.xml.XPathType
import nl.avisi.kotlinwebtest.xml.dsl.xml
import org.junit.After
import org.junit.Test

class SoapDslTest : WebTest() {

    private val server: SoapServer = SoapServer()

    override fun configure() {
        soap {
            default {
                request endpoint endpoint("Endpoint", "http://localhost:${server.port}") {
                    soap validation_schema "xsd/cars.xsd"
                }
            }
        }
        xml {
            declare namespace "http://schemas.xmlsoap.org/soap/envelope/" prefixed_as "soap"
            declare namespace "xsd/cars.xsd" prefixed_as "ns1"
        }
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun test() {
        val actual = """
            <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/" >
                <soap:Body>
                    <car xmlns="webtest::vehicles::cars">
                        <wheel>front-left</wheel>
                        <prices>
                            <price>5</price>
                            <price>654</price>
                            <price>4325</price>
                        </prices>
                    </car>
                </soap:Body>
            </soap:Envelope>
            """
        test("Test") {
            properties {
                configuration.properties["actual"] = actual
            }
            step soap {
                name = "Testcall"
                request path "/simple/"
                request text ""
                validate {
                    http_status() matches 200

                    http_header("Content-Type") matches "text/xml; charset=UTF-8"

                    xsd()
                    !soap_fault()
                    is_soap_response()

                    xpath("/*", XPathType.Node) matches actual
                    xpath("/*", XPathType.Node) matches property("actual")
                    xpath("/soap:Envelope/soap:Body/*[local-name()='car']/*[local-name()='wheel']", XPathType.String) matches "front-left"
                    xpath("/soap:Envelope/soap:Body/*[local-name()='car']/*[local-name()='wheel']", XPathType.String) matches_regex "front.*."
                    attachments_count(0)
                }
            }
            step soap {
                name = "Testcall mtom"
                request path "/mtom/"
                request text """
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/" >
                    <soap:Body>
                        <car xmlns="webtest::vehicles::cars">
                            <wheel>front-left</wheel>
                             <prices>
                                <price>5</price>
                                <price>654</price>
                                <price>4325</price>
                            </prices>
                            <bijlage>cid:test</bijlage>
                        </car>
                    </soap:Body>
                </soap:Envelope>
                """
                request attachment "soap/mtom-soap-response.xml" contentType "txt" contentID "test"
                validate {
                    http_status() matches 200
                    xsd()
                    !soap_fault()
                    is_soap_response()

                    xpath("/soap:Envelope/soap:Body/*[local-name()='car']/*[local-name()='time1']", "/soap:Envelope/soap:Body/*[local-name()='car']/*[local-name()='time2']") time_difference_less_than 1001
                    xpath("/soap:Envelope/soap:Body/*[local-name()='car']/*[local-name()='time1']", "/soap:Envelope/soap:Body/*[local-name()='car']/*[local-name()='time2']") time_difference_greater_than 999
                    xpath("/soap:Envelope/soap:Body/*[local-name()='car']/*[local-name()='time1']", "/soap:Envelope/soap:Body/*[local-name()='car']/*[local-name()='time2']") time_difference_equals 1000

                    attachments_count(1)
                    attachment {
                        attachment_index = 0
                        attachments_size() greater_than 365
                        attachments_size() less_than 367
                        attachments_size() equals 366
                        attachment_header("Content-Transfer-Encoding") matches "binary"
                    }
                }
                afterwards {
                    assign("/soap:Envelope/soap:Body/*[local-name()='car']/*[local-name()='wheel']", Source.REQUEST) to property("actual")
                }
            }
        }
        execute()
    }
}