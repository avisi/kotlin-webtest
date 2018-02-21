/**
 * For licensing, see LICENSE.txt
 * @author Rein Krul
 */
package nl.avisi.kotlinwebtest.soap

import nl.avisi.kotlinwebtest.ExecutionContext
import nl.avisi.kotlinwebtest.TestConfiguration
import nl.avisi.kotlinwebtest.xml.NamespaceDeclaration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class
XSDValidatorTest {
    @Test
    fun validateMissingSoapBody() {
        val input = """<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/" />"""
        testFailure(input, "cvc-complex-type.2.4.b: The content of element 'soap:Envelope' is not complete. One of '{\"http://schemas.xmlsoap.org/soap/envelope/\":Header, \"http://schemas.xmlsoap.org/soap/envelope/\":Body}' is expected.")
    }

    @Test
    fun validateSoapBodyEmpty() {
        val input = """
        <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
          <soap:Body />
        </soap:Envelope>
        """
        testSuccess(input)
    }

    @Test
    fun validateUnmappedDefaultNamespace() {
        val input = """
        <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
          <soap:Body>
            <car type="audi" />
          </soap:Body>
        </soap:Envelope>
        """
        testFailure(input, "cvc-elt.1: Cannot find the declaration of element 'car'.")
    }

    @Test
    fun validate() {
        val input = """
        <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
          <soap:Body>
            <car xmlns="webtest::vehicles::cars">
                <wheel>front-left</wheel>
            </car>
          </soap:Body>
        </soap:Envelope>
        """
        testSuccess(input, listOf("xsd/cars.xsd"))
    }

    private fun testFailure(input: String, message: String) {
        val context = ExecutionContext(TestConfiguration())
        context.configuration.xml.namespaces.add(NamespaceDeclaration("soap", "http://schemas.xmlsoap.org/soap/envelope/"))
        val validator = XSDValidator()
        val actual = validator.validate(context, request(), response(input))
        assertFalse(actual.success)
        assertEquals("XSD validation failed: $message", actual.message)
    }

    private fun testSuccess(input: String, schemas: List<String> = listOf()) {
        val context = ExecutionContext(TestConfiguration())
        context.configuration.xml.namespaces.add(NamespaceDeclaration("soap", "http://schemas.xmlsoap.org/soap/envelope/"))
        val validator = XSDValidator()
        val response = response(input)
        response.endpoint[Schemas::class] = Schemas(schemas.map { Thread.currentThread().contextClassLoader.getResource(it) }.map(::Schema))
        val actual = validator.validate(context, request(), response)
        if (actual.message != null) {
            println(actual.message)
        }
        assertTrue(actual.success)
        assertNull(actual.message)
    }
}