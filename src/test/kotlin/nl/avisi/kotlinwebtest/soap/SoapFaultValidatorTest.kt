/**
 * For licensing, see LICENSE.txt
 * @author Rein Krul
 */
package nl.avisi.kotlinwebtest.soap

import nl.avisi.kotlinwebtest.ExecutionContext
import nl.avisi.kotlinwebtest.TestConfiguration
import org.junit.Test

import org.junit.Assert.*

class SoapFaultValidatorTest {
    @Test
    fun validateSoapFaultExpectedAndFound() {
        val validator = SoapFaultValidator(true)
        assertTrue(validator.validate(ExecutionContext(TestConfiguration()), response(soapClientFault)).success)
    }

    @Test
    fun validateSoapFaultExpectedButNotFound() {
        val validator = SoapFaultValidator(true)
        assertFalse(validator.validate(ExecutionContext(TestConfiguration()), response(emptySoapEnvelope)).success)
    }

    @Test
    fun validateNoSoapFaultAndNotFound() {
        val validator = SoapFaultValidator(false)
        assertTrue(validator.validate(ExecutionContext(TestConfiguration()), response(emptySoapEnvelope)).success)
    }

    @Test
    fun validateNoSoapFaultButFound() {
        val validator = SoapFaultValidator(false)
        assertFalse(validator.validate(ExecutionContext(TestConfiguration()), response(soapClientFault)).success)
    }
}