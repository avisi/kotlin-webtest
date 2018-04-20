/**
 * For licensing, see LICENSE.txt
 * @author Martijn Heuvelink
 */
package nl.avisi.kotlinwebtest.rest

import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.PathNotFoundException
import nl.avisi.kotlinwebtest.After
import nl.avisi.kotlinwebtest.AfterResult
import nl.avisi.kotlinwebtest.ExecutionContext
import nl.avisi.kotlinwebtest.expressions.PropertyExpression
import org.json.JSONObject
import org.slf4j.LoggerFactory


class JsonPathValue(val step: RestTestStep, private val jsonPath: String, private var prop: PropertyExpression) : After<RestStepRequest, RestStepResponse> {

    companion object {
        private val log = LoggerFactory.getLogger(JsonPathValue::class.java)
    }

    override fun afterwards(executionContext: ExecutionContext, request: RestStepRequest, response: RestStepResponse): AfterResult {
        val document = JsonPath.parse(response.body)

        val actualValue: Any = try {
            document.read(jsonPath)
        } catch (e: PathNotFoundException) {
            return failure("Incorrect Json Path: $jsonPath (${e.message})")
        }
        if (actualValue is Map<*, *>) executionContext.configuration.properties[prop.name] = JSONObject(actualValue).toString()
        else executionContext.configuration.properties[prop.name] = actualValue.toString()
        log.info("Assigned '$actualValue' to property ${prop.name}")
        return success()
    }
}