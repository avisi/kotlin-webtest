/**
 * For licensing, see LICENSE.txt
 * @author Rein Krul
 */
package nl.avisi.kotlinwebtest.json

import org.skyscreamer.jsonassert.JSONCompareMode
import org.skyscreamer.jsonassert.JSONCompareResult
import org.skyscreamer.jsonassert.comparator.DefaultComparator

class WildcardTokenComparator(mode: JSONCompareMode) : DefaultComparator(mode) {

    override fun compareValues(prefix: String, expectedValue: Any, actualValue: Any, result: JSONCompareResult) {
        if (expectedValue is String && expectedValue == "**") {
            return
        } else {
            super.compareValues(prefix, expectedValue, actualValue, result)
        }
    }
}