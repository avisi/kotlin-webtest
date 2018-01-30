/**
 * For licensing, see LICENSE.txt
 * @author Rein Krul
 */
package nl.avisi.kotlinwebtest

@DslMarker
annotation class WebTestDsl

fun endpoint(url: String, init: (EndpointConfigurer.() -> Unit)? = null): Endpoint =
        endpoint(null, url, init)

fun endpoint(name: String?, url: String, init: (EndpointConfigurer.() -> Unit)? = null): Endpoint =
        Endpoint(name, url).apply {
            with(EndpointConfigurer(this)) {
                if (init != null) {
                    init()
                }
            }
        }

class EndpointConfigurer(val endpoint: Endpoint) {

    val request = EndpointRequestConfigurer()

    inner class EndpointRequestConfigurer {
        infix fun credentials(credentials: Credentials) {
            endpoint.credentials = credentials
        }
    }
}
