package nl.reikrul.kosote.soap


import nl.reikrul.kosote.Engine
import nl.reikrul.kosote.ExecutionContext
import nl.reikrul.kosote.Executor
import nl.reikrul.kosote.HttpResponse
import nl.reikrul.kosote.Request
import nl.reikrul.kosote.Response
import nl.reikrul.kosote.Validator
import nl.reikrul.kosote.expressions.ConstantExpression
import nl.reikrul.kosote.expressions.Expression
import nl.reikrul.kosote.expressions.ExpressionEvaluator
import nl.reikrul.kosote.expressions.findExpressions
import nl.reikrul.kosote.soap.dsl.SoapTestStep
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClients
import org.slf4j.LoggerFactory
import java.net.URL
import java.nio.charset.StandardCharsets

class SoapEngine : Engine

// Requests
interface SoapRequest : Request

class FileSoapRequest(val file: String) : SoapRequest
class RawSoapRequest(val data: String) : SoapRequest

class SoapResponse(val http: HttpResponse?) : Response

// Validation
class XSDValidator : Validator<SoapResponse> {
    override fun validate(response: SoapResponse): Boolean {
        return true
    }
}

class HttpStatusValidator(val range: ClosedRange<Int>) : Validator<SoapResponse> {
    override fun validate(response: SoapResponse): Boolean {
        val http = response.http ?: error("Missing response")
        return range.contains(http.statusCode)
    }
}

class XPathValidator(private val validation: Validation) : Validator<SoapResponse> {

    override fun validate(response: SoapResponse): Boolean {
        return true
    }

    class Validation(val xpath: String, var value: Expression? = null) {

        infix fun matches(constant: String) {
            value = ConstantExpression(constant)
        }

        infix fun matches(expression: Expression) {
            value = expression
        }
    }
}

// Execution
class SoapExecutor : Executor<SoapTestStep> {

    override fun execute(step: SoapTestStep, executionContext: ExecutionContext): Response {
        val request = step.request ?: error("Request not configured.")

        val requestData =
                when (request) {
                    is RawSoapRequest -> request.data
                    is FileSoapRequest -> resolveFile(request.file).openStream().reader().readText()
                    else -> error("Unknown SOAP request type.")
                }.let { interpolateExpressions(it, executionContext) }

        log.info("Sending request: {}", requestData)
        var httpResponse: HttpResponse? = null
        getHttpClient().use {
            val httpRequest = HttpPost(step.endpoint.url.toURI())
            httpRequest.entity = StringEntity(requestData, ContentType.TEXT_XML.withCharset(StandardCharsets.UTF_8))
            val response = it.execute(httpRequest)
            response.use {
                response.entity.content.use {
                    httpResponse = HttpResponse(response.statusLine.statusCode, it.reader().readText())
                }
            }
        }
        log.debug("Response: ${httpResponse?.data}")
        return SoapResponse(httpResponse).also {
            with(executionContext) {
                previousRequest = request
                previousResponse = it
            }
        }
    }

    private fun interpolateExpressions(text: String, executionContext: ExecutionContext): String {
        val evaluator = ExpressionEvaluator(executionContext)
        var interpolatedRequestData = text
        findExpressions(text).forEach {
            val (token, expression) = it
            val value = evaluator.evaluate(expression) ?: "".also { log.warn("Property evaluated to empty string: $token") }
            interpolatedRequestData = interpolatedRequestData.replace(token, value)
        }
        return interpolatedRequestData
    }

    private fun getHttpClient() =
            HttpClients.createDefault()

    private fun resolveFile(file: String): URL =
            Thread.currentThread().contextClassLoader.getResource(file + ".xml") ?: error("File not found: $file")

    companion object {
        private val log = LoggerFactory.getLogger(SoapExecutor::class.java)
    }
}