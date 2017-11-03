package nl.reikrul.kosote.soap.dsl

import nl.reikrul.kosote.ValidatorConfigurer
import nl.reikrul.kosote.soap.HttpStatusValidator
import nl.reikrul.kosote.soap.SoapRequest
import nl.reikrul.kosote.soap.SoapResponse
import nl.reikrul.kosote.soap.XPathValidator
import nl.reikrul.kosote.soap.XSDValidator


fun SoapTestStep.validate(configurer: ValidatorConfigurer<SoapRequest, SoapResponse>.() -> Unit): SoapTestStep {
    configurer(ValidatorConfigurer(this))
    return this
}

fun ValidatorConfigurer<SoapRequest, SoapResponse>.xsd() {
    register(XSDValidator())
}

fun ValidatorConfigurer<SoapRequest, SoapResponse>.http_status(range: ClosedRange<Int>) {
    register(HttpStatusValidator(range))
}

fun ValidatorConfigurer<SoapRequest, SoapResponse>.xpath(xpath: String): XPathValidator.Validation =
        XPathValidator.Validation(xpath).also {
            register(XPathValidator(it))
        }


fun ValidatorConfigurer<SoapRequest, SoapResponse>.no_soap_fault() {
    register(XSDValidator())
}




