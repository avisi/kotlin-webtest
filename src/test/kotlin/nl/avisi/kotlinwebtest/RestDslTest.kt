package nl.avisi.kotlinwebtest

import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.ok
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import com.github.tomakehurst.wiremock.junit.WireMockRule
import nl.avisi.kotlinwebtest.expressions.property
import nl.avisi.kotlinwebtest.http.HttpMethod
import nl.avisi.kotlinwebtest.properties.dsl.properties
import nl.avisi.kotlinwebtest.rest.dsl.afterwards
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
                    },
                    "test": null,
                    "date": "2015-01-28",
                    "time1": [2018,4,17,16,3,41,826000000],
                    "time2": [2018,4,17,16,3,42,826000000]
                }
            ]
            """
        wiremock.stubFor(get(urlEqualTo("/foo")).willReturn(ok(actual).withHeader("Content-Type", "application/json")))

        test("Test") {
            properties {
                configuration.properties["bar"] = "18"
                configuration.properties["foo"] = actual
            }
            step rest {
                name = "Testcall"
                request method HttpMethod.GET
                request path "/foo"
                validate {
                    http_status() matches 200

                    http_header("Content-Type") matches "application/json"

                    json() matches actual
                    json() matches_file "rest/example_rest_response.json"

                    contains(arrayOf("2018", "#{bar}"))

                    json_path("$[0].test") is_null true

                    json_path("$[0].date") date_matches "2015-01-28"

                    json_path("$[0].foo") matches mapOf("bar" to listOf(2018, 29, 10))
                    json_path("$[0].foo.bar.length()") matches 3

                    json_path("$[0].time1", "$[0].time2") time_difference_less_than 1001
                    json_path("$[0].time1", "$[0].time2") time_difference_greater_than 999

                    json_path(property("foo"), "$[0].foo") matches "$[0].foo"
                    json_path(property("foo"), "$[0].foo") is_null false
                }
                afterwards {
                    assign("$[0].foo.bar") to property("bar")
                }
            }
        }
        execute()
    }

}