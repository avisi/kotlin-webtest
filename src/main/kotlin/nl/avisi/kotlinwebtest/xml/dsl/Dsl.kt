/**
 * For licensing, see LICENSE.txt
 * @author Rein Krul
 */
package nl.avisi.kotlinwebtest.xml.dsl

import nl.avisi.kotlinwebtest.TestConfiguration
import nl.avisi.kotlinwebtest.WebTest
import nl.avisi.kotlinwebtest.xml.NamespaceDeclaration

fun WebTest.xml(init: XmlSettingsBuilder.() -> Unit) =
        XmlSettingsBuilder(testConfiguration).apply(init)


class XmlSettingsBuilder(val configuration: TestConfiguration) {
    val declare: DeclarationBuilder = DeclarationBuilder()

    inner class DeclarationBuilder {
        infix fun namespace(namespace: String): NamespaceDeclarationBuilder =
                NamespaceDeclarationBuilder(namespace)

        inner class NamespaceDeclarationBuilder(private val namespace: String) {

            infix fun prefixed_as(prefix: String) {
                configuration.xml.namespaces.add(NamespaceDeclaration(prefix, namespace))
            }
        }
    }
}