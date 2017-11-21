/**
 * For licensing, see LICENSE.txt
 * @author Rein Krul
 */
package nl.avisi.kotlinwebtest.soap

import java.net.URL

data class Schema(val url: URL)

data class Schemas(private val schemas: List<Schema> = listOf()) : List<Schema> by schemas