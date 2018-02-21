/**
 * For licensing, see LICENSE.txt
 * @author Rein Krul
 */
package nl.avisi.kotlinwebtest.soap.dsl

import nl.avisi.kotlinwebtest.Endpoint
import nl.avisi.kotlinwebtest.EndpointConfigurer
import nl.avisi.kotlinwebtest.Source
import nl.avisi.kotlinwebtest.StepBuilder
import nl.avisi.kotlinwebtest.TestConfiguration
import nl.avisi.kotlinwebtest.WebTest
import nl.avisi.kotlinwebtest.expressions.ConstantExpression
import nl.avisi.kotlinwebtest.expressions.Expression
import nl.avisi.kotlinwebtest.expressions.PropertyExpression
import nl.avisi.kotlinwebtest.http.HttpHeaderValidationBuilder
import nl.avisi.kotlinwebtest.http.HttpStatusValidationBuilder
import nl.avisi.kotlinwebtest.soap.AttachmentHeaderValidator
import nl.avisi.kotlinwebtest.soap.AttachmentSizeValidator
import nl.avisi.kotlinwebtest.soap.AttachmentsCountValidator
import nl.avisi.kotlinwebtest.soap.CompareTimeValidator
import nl.avisi.kotlinwebtest.soap.ContainsValidator
import nl.avisi.kotlinwebtest.soap.InequalityType
import nl.avisi.kotlinwebtest.soap.Schema
import nl.avisi.kotlinwebtest.soap.Schemas
import nl.avisi.kotlinwebtest.soap.SoapFaultValidator
import nl.avisi.kotlinwebtest.soap.SoapRequestDefaults
import nl.avisi.kotlinwebtest.soap.SoapResponseValidator
import nl.avisi.kotlinwebtest.soap.SoapTestConfiguration
import nl.avisi.kotlinwebtest.soap.SoapTestStep
import nl.avisi.kotlinwebtest.soap.XPathValidator
import nl.avisi.kotlinwebtest.soap.XPathValue
import nl.avisi.kotlinwebtest.soap.XQueryValidator
import nl.avisi.kotlinwebtest.soap.XSDValidator
import nl.avisi.kotlinwebtest.xml.XPathType

fun WebTest.soap(init: SoapSettingsBuilder.() -> Unit) =
        SoapSettingsBuilder(testConfiguration).apply(init)

infix fun SoapTestStep.validate(init: Validation.() -> Unit) {
    Validation(this).init()
}

infix fun SoapTestStep.afterwards(init: After.() -> Unit) {
    After(this).init()
}

infix fun StepBuilder.soap(init: SoapTestStep.() -> Unit): SoapTestStep =
        SoapTestStep(testCase).apply {
            init()
            testCase.steps.add(this)
        }

class Validation(val step: SoapTestStep) {
    fun xpath(xpath: String, type: XPathType = XPathType.String) = XPathValidationBuilder(step, xpath, type)
    fun xpath(xpathFirst: String, xpathSecond: String) = XPathCompareValidationBuilder(step, xpathFirst, xpathSecond)
    fun http_status() = HttpStatusValidationBuilder(step)
    fun http_header(header: String) = HttpHeaderValidationBuilder(header, step)
    fun xsd() = step.validators.add(XSDValidator())
    fun soap_fault() = SoapFaultValidationBuilder(step)
    fun is_soap_response() = step.validators.add(SoapResponseValidator())
    fun contains(string: String) = step.validators.add(ContainsValidator(string))
    fun xquery(xquery: String) = XQueryValidationBuilder(step, xquery)
    fun attachments_count(value: Int) = step.validators.add(AttachmentsCountValidator(value))
}

infix fun Validation.attachment(init: Attachment.() -> Unit) =
        Attachment(this).init()

class Attachment(val validate: Validation) {
    var attachment_index: Int = 0
    fun attachments_size() = AttachmentSizeValidatorBuilder(validate.step, attachment_index)
    fun attachment_header(header: String) = AttachmentHeaderValidatorBuilder(validate.step, header, attachment_index)
}

class After(private val step: SoapTestStep) {
    fun assign(xpath: String, value: Source): XPathValueBuilder = XPathValueBuilder(step, xpath, value)
}

class XPathValidationBuilder(private val step: SoapTestStep, val xpath: String, val type: XPathType) {
    infix fun matches(value: String) {
        matches(ConstantExpression(value))
    }

    infix fun matches(value: Expression) {
        step.validators.add(XPathValidator(xpath, value, type, false))
    }

    infix fun matches_regex(value: String) {
        matches_regex(ConstantExpression(value))
    }

    infix fun matches_regex(value: Expression) {
        step.validators.add(XPathValidator(xpath, value, type, true))
    }
}

class XPathCompareValidationBuilder(private val step: SoapTestStep, private val xpathFirst: String, private val xpathSecond: String) {

    infix fun time_difference_greater_than(difference: Int) {
        step.validators.add(CompareTimeValidator(xpathFirst, xpathSecond, difference, InequalityType.GreaterThan))
    }

    infix fun time_difference_less_than(difference: Int) {
        step.validators.add(CompareTimeValidator(xpathFirst, xpathSecond, difference, InequalityType.LessThan))
    }

    infix fun time_difference_equals(difference: Int) {
        step.validators.add(CompareTimeValidator(xpathFirst, xpathSecond, difference, InequalityType.Equals))
    }
}

class SoapFaultValidationBuilder(step: SoapTestStep) {
    private val validator = SoapFaultValidator(true)

    init {
        step.validators.add(validator)
    }

    operator fun not() {
        validator.mustContainSoapFault = false
    }
}

class SoapSettingsBuilder(val configuration: TestConfiguration) {
    fun default(init: DefaultSettingsBuilder.() -> Unit) {
        val builder = DefaultSettingsBuilder(configuration[SoapTestConfiguration::class].defaults)
        builder.init()
    }

    inner class DefaultSettingsBuilder(val request: SoapRequestDefaults)
}

class SoapEndpointSettingsConfigurer(private val endpoint: Endpoint) {
    infix fun validation_schema(file: String) {
        // TODO: This enables immutability of the 'Schemas' class, but it sure looks a bit funny...
        val url = Thread.currentThread().contextClassLoader.getResource(file) ?: error("Schema not found: $file")
        endpoint[Schemas::class] = Schemas(endpoint[Schemas::class].plus(Schema(url)))
    }
}

class XPathValueBuilder(private val step: SoapTestStep, val xpath: String, val value: Source) {
    infix fun to(prop: PropertyExpression) {
        step.afterwards.add(XPathValue(step, xpath, prop, value))
    }
}

class AttachmentSizeValidatorBuilder(private val step: SoapTestStep, private val attachment: Int) {
    infix fun greater_than(size: Int) {
        step.validators.add(AttachmentSizeValidator(size, attachment, InequalityType.GreaterThan))
    }

    infix fun less_than(size: Int) {
        step.validators.add(AttachmentSizeValidator(size, attachment, InequalityType.LessThan))
    }

    infix fun equals(size: Int) {
        step.validators.add(AttachmentSizeValidator(size, attachment, InequalityType.Equals))
    }
}

class AttachmentHeaderValidatorBuilder(private val step: SoapTestStep, private val header: String, private val attachment: Int) {

    infix fun matches(value: String) {
        step.validators.add(AttachmentHeaderValidator(header, value, attachment))
    }
}

class XQueryValidationBuilder(private val step: SoapTestStep, val xpath: String) {
    infix fun matches(value: String) {
        matches(ConstantExpression(value))
    }

    infix fun matches(value: Expression) {
        step.validators.add(XQueryValidator(xpath, value))
    }

    infix fun matches_file(path: String) {
        matches(Thread.currentThread().contextClassLoader.getResource(path).readText())
    }
}

val EndpointConfigurer.soap: SoapEndpointSettingsConfigurer
    get() = SoapEndpointSettingsConfigurer(this.endpoint)

