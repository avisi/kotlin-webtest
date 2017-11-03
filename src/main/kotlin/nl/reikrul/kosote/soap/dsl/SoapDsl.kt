package nl.reikrul.kosote.soap.dsl

import nl.reikrul.kosote.*
import nl.reikrul.kosote.soap.*

class SoapTestStep(val endpoint: Endpoint, testCase: TestCase) : TestStep<SoapRequest, SoapResponse>(testCase)

fun TestCaseBuilder.soap(endpoint: Endpoint, init: RequestConfigurer<SoapRequest, SoapResponse>.() -> SoapRequest): SoapTestStep =
        SoapTestStep(endpoint, testCase).also {
            val configurer = RequestConfigurer(it)
            val request = configurer.init()
            configurer.register(request)
            register(it)
        }

fun TestCaseBuilder.soap(endpoint: Endpoint, requestFile: String): SoapTestStep =
        SoapTestStep(endpoint, testCase).also {
            val configurer = RequestConfigurer(it)
            val request = FileSoapRequest(requestFile)
            configurer.register(request)
            register(it)
        }

fun TestCaseBuilder.soap(endpoint: Endpoint): SoapTestStep =
        SoapTestStep(endpoint, testCase).also { register(it) }

fun SoapTestStep.body(body: String): SoapTestStep {
    val configurer = RequestConfigurer(this)
    val request = RawSoapRequest(body.trim())
    configurer.register(request)
    return this
}
