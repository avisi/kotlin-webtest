package nl.avisi.kotlinwebtest

import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.ok
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import com.github.tomakehurst.wiremock.junit.WireMockRule
import nl.avisi.kotlinwebtest.http.HttpMethod
import nl.avisi.kotlinwebtest.rest.dsl.rest
import nl.avisi.kotlinwebtest.rest.dsl.validate
import org.junit.Rule
import org.junit.Test

class RestDslTest : WebTest() {

    companion object {
        private val RANDOM_PORT = 0
    }

    val wiremock = WireMockRule(options().port(RANDOM_PORT))
        @Rule get() = field

    override fun configure() {
        rest {
            default {
                request endpoint Endpoint("stub", "http://localhost:${wiremock.port()}")
            }
        }
    }

    @Test
    fun test() {
        val actual = """
            [
                {
                    "foo": {
                        "bar": [
                            2018,
                            29,
                            10
                        ]
                    }
                }
            ]
            """
        wiremock.stubFor(get(urlEqualTo("/foo")).willReturn(ok(actual)))

        test("Test") {
            step rest {
                name = "Testcall"
                request method HttpMethod.GET
                request path "/foo"
                validate {
                    http_status() matches 200
                    json_path("$[0].foo.bar") matches listOf(2018, 29, 10)
                    json() matches actual
                    json_path("$[0].foo") matches mapOf("bar" to listOf(2018, 29, 10))

                }
            }
        }
        execute()
    }

}