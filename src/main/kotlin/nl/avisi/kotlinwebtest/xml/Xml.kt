package nl.avisi.kotlinwebtest.xml

import org.w3c.dom.Document
import org.w3c.dom.Node
import java.io.StringWriter
import javax.xml.XMLConstants
import javax.xml.namespace.NamespaceContext
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


fun Document.evaluate(xpath: String, namespaces: List<NamespaceDeclaration>): Node? {
    val expression = XPathFactory.newInstance()
            .newXPath()
            .apply {
                namespaceContext = SimpleNamespaceContext(namespaces)
            }
            .compile(xpath)
    return expression.evaluate(this, XPathConstants.NODE) as? Node
}


fun toXml(node: Node): String {
    val transformerFactory = TransformerFactory.newInstance()
    val transformer = transformerFactory.newTransformer()
    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
    val writer = StringWriter()
    transformer.transform(DOMSource(node), StreamResult(writer))
    return writer.toString()
}

fun toDocument(xml: String): Document =
        DocumentBuilderFactory
                .newInstance()
                .apply { isNamespaceAware = true }
                .newDocumentBuilder()
                .parse(xml.byteInputStream())

fun String.asPrettyXml(): String =
        toXml(toDocument(this))

