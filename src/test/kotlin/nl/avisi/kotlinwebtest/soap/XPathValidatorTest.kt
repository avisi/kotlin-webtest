/**
 * For licensing, see LICENSE.txt
 * @author Rein Krul
 */
package nl.avisi.kotlinwebtest.soap

import nl.avisi.kotlinwebtest.ExecutionContext
import nl.avisi.kotlinwebtest.TestConfiguration
import nl.avisi.kotlinwebtest.expressions.ConstantExpression
import nl.avisi.kotlinwebtest.xml.NamespaceDeclaration
import nl.avisi.kotlinwebtest.xml.XmlContext
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class XPathValidatorTest {
    @Test
    fun validateNoNamespaces() {
        val context = ExecutionContext(TestConfiguration())
        val input = """
            <root>
                <foo>
                    <bar>Hello</bar>
                </foo>
            </root>
        """.trimIndent()
        val expected = "<foo><bar>Hello</bar></foo>"

        val validator = XPathValidator("//foo", ConstantExpression(expected))
        val result = validator.validate(context, request(), response(input))
        assertTrue(result.message) { result.success }
    }

    @Test
    fun validateWithNamespace() {
        val xml = XmlContext().apply { namespaces.add(NamespaceDeclaration("input_b", "b")) }
        val context = ExecutionContext(TestConfiguration(xml))
        val input = """
            <root xmlns="a">
                <b:foo xmlns:b="b">
                    <c:bar xmlns:c="c">Hello</c:bar>
                </b:foo>
            </root>
        """.trimIndent()
        val expected = """<foo xmlns="b"><bar xmlns="c">Hello</bar></foo>"""

        val validator = XPathValidator("//input_b:foo", ConstantExpression(expected))
        val result = validator.validate(context, request(), response(input))
        assertTrue(result.message) { result.success }
    }

    @Test
    fun validateDepthMismatch() {
        val context = ExecutionContext(TestConfiguration(XmlContext()))
        val input = """
            <root>
                <foo>
                    <c>Hello</c>
                </foo>
            </root>
        """.trimIndent()
        val expected = """<c>Hello</c>"""

        val validator = XPathValidator("//foo", ConstantExpression(expected))
        val result = validator.validate(context, request(), response(input))
        assertFalse(result.message) { result.success }
    }

    @Test
    fun validateXPathErrorNamespaceNotBound() {
        val context = ExecutionContext(TestConfiguration(XmlContext()))
        val input = """
            <root>
                <foo>
                    <c>Hello</c>
                </foo>
            </root>
        """.trimIndent()
        val expected = """<c>Hello</c>"""

        val validator = XPathValidator("//foo:foo", ConstantExpression(expected))
        val result = validator.validate(context, request(), response(input))
        assertFalse(result.message) { result.success }
    }

    @Test
    fun validateXPathWithBoundNamespace() {
        val context = ExecutionContext(TestConfiguration(XmlContext(mutableListOf(NamespaceDeclaration("foo", "bar")))))
        val input = """
            <root xmlns="bar">
                <foo>
                    <c>Hello</c>
                </foo>
            </root>
        """.trimIndent()
        val expected = """<foo:c>Hello</foo:c>"""

        val validator = XPathValidator("//foo:c", ConstantExpression(expected))
        val result = validator.validate(context, request(), response(input))
        assertTrue(result.message) { result.success }
    }
}