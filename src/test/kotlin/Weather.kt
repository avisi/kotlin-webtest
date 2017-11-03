import nl.reikrul.kosote.Endpoint
import nl.reikrul.kosote.KosoteTest
import nl.reikrul.kosote.expressions.property
import nl.reikrul.kosote.expressions.xpath
import nl.reikrul.kosote.properties.property
import nl.reikrul.kosote.soap.dsl.body
import nl.reikrul.kosote.soap.dsl.http_status
import nl.reikrul.kosote.soap.dsl.soap
import nl.reikrul.kosote.soap.dsl.validate
import org.junit.Test

class WeatherTest : KosoteTest() {

    @Test
    fun test() {
        // Setup
        val weather = Endpoint("weather", "http://www.webservicex.net/globalweather.asmx")

        // Define
        project.test("GetCitiesByCountry")
                .property("country", "Netherlands")
                // Inline SOAP request body
                .soap(weather)
                .body("""
<?xml version="1.0" encoding="utf-8"?>
<soap:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema"
               xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
    <soap:Body>
        <GetCitiesByCountry xmlns="http://www.webserviceX.NET">
            <CountryName>#{country}</CountryName>
        </GetCitiesByCountry>
    </soap:Body>
</soap:Envelope>
                """)
                // Load SOAP request from file
                .soap(weather, "GetCitiesByCountry")
                //.property("httpStatus") from xpath("")
                .validate {
                    // Apply some validation
                    http_status(200..300)
                    xpath("//*country") matches "Netherlands"
                    xpath("//*country") matches property("country")
                }

        // Run
        execute()
    }
}

