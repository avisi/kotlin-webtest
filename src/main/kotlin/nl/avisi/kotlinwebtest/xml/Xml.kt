/**
 * For licensing, see LICENSE.txt
 * @author Rein Krul
 */
package nl.avisi.kotlinwebtest.xml

import org.w3c.dom.Document
import org.w3c.dom.Node
import java.io.InputStream
import java.io.StringWriter
import javax.xml.XMLConstants
import javax.xml.namespace.NamespaceContext
import javax.xml.namespace.QName
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

class XmlContext(val namespaces: MutableList<NamespaceDeclaration> = mutableListOf())

class NamespaceDeclaration(val prefix: String, val namespace: String)


/**
 * Class implemented as described at https://docs.oracle.com/javase/7/docs/api/javax/xml/namespace/NamespaceContext.html
 */
class SimpleNamespaceContext(namespaces: List<NamespaceDeclaration>) : NamespaceContext {

    private val namespaces: List<NamespaceDeclaration> = namespaces.toList()

    override fun getPrefix(namespaceUri: String?): String? =
            when (namespaceUri) {
                null -> throw IllegalArgumentException("No namespaceUri supplied.")
                XMLConstants.XML_NS_URI -> XMLConstants.XML_NS_PREFIX
                XMLConstants.XMLNS_ATTRIBUTE_NS_URI -> XMLConstants.XMLNS_ATTRIBUTE
                else -> namespaces.firstOrNull { it.namespace == namespaceUri }?.prefix
            }


    override fun getPrefixes(namespaceUri: String): MutableIterator<String?> =
            mutableListOf(getPrefix(namespaceUri)).iterator()

    override fun getNamespaceURI(prefix: String?): String =
            when (prefix) {
                null -> throw IllegalArgumentException("No prefix supplied.")
                XMLConstants.XML_NS_PREFIX -> XMLConstants.XML_NS_URI
                XMLConstants.XMLNS_ATTRIBUTE -> XMLConstants.XMLNS_ATTRIBUTE_NS_URI
                else -> namespaces.firstOrNull { it.prefix == prefix }?.namespace ?: XMLConstants.NULL_NS_URI
            }
}

fun Document.evaluate(xpath: String, namespaces: List<NamespaceDeclaration>, type: XPathType = XPathType.String): EvaluateResult? {
    try {
        val expression = XPathFactory.newInstance()
                .newXPath()
                .apply {
                    namespaceContext = SimpleNamespaceContext(namespaces)
                }
                .compile(xpath) ?: return null
        return when (type) {
            XPathType.Node -> NodeValue(expression.evaluate(this, type.getXpathtype()) as? Node ?: return null)
            XPathType.Number -> NumberValue(expression.evaluate(this, type.getXpathtype()) as? Double ?: return null)
            else -> StringValue(expression.evaluate(this, type.getXpathtype()) as? String ?: return null)
        }
    } catch (e: Exception) {
        return ErrorValue(e.message!!)
    }

}

sealed class EvaluateResult(open val value: String)
class NumberValue(val number: Double) : EvaluateResult("")
class NodeValue(val node: Node) : EvaluateResult("")
class StringValue(value: String) : EvaluateResult(value)
class ErrorValue(value: String) : EvaluateResult(value)


enum class XPathType {
    Node,
    Number,
    String;

    internal fun getXpathtype(): QName =
            when (this) {
                Node -> XPathConstants.NODE
                Number -> XPathConstants.NUMBER
                String -> XPathConstants.STRING
            }
}

fun toXml(node: Node): String {
    val transformerFactory = TransformerFactory.newInstance()
    val transformer = transformerFactory.newTransformer()
    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
    transformer.setOutputProperty(OutputKeys.INDENT, "yes")
    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
    val writer = StringWriter()
    transformer.transform(DOMSource(node), StreamResult(writer))
    return writer.toString()
}

fun Document.toXml(): String =
        toXml(this)

fun fragmentToDocument(xmlFragment: String, namespaces: List<NamespaceDeclaration>): Document =
        if (namespaces.isNotEmpty())
            toDocument(xmlFragment, false)
                    .apply {
                        namespaces.forEach({ namespace -> documentElement.setAttribute("xmlns:${namespace.prefix}", namespace.namespace) })
                    }
                    .toXml()
                    .let { toDocument(it) }
        else
            toDocument(xmlFragment)


fun toDocument(xml: InputStream, namespaceAware: Boolean = true): Document {
    return try {
        DocumentBuilderFactory
                .newInstance()
                .apply { isNamespaceAware = namespaceAware }
                .newDocumentBuilder()
                .parse(xml)
    } catch (e: Exception) {
        DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
    }
}


fun toDocument(xml: String, namespaceAware: Boolean = true): Document =
        toDocument(xml.byteInputStream(), namespaceAware)

fun String.asPrettyXml(): String =
        toXml(toDocument(this))

