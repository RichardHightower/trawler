package trawler.core

import trawler.core.model.FieldType
import java.io.File

data class RegexRuleResult(val success: Boolean, val message: String? = null) {
    fun matched() = success
    fun failedMatch() = !success
}
data class RegexRule(val regex: Regex, val message:String) {
    fun isMatch( value:String) : Boolean = regex.matchEntire(value) !=null
    fun matchValue(value: String, name: String? = null, context: Any? = null,  file: File?=null) : RegexRuleResult =
        if (isMatch(value)) {
            RegexRuleResult(true)
        } else {
            if (name != null && context != null) {
                RegexRuleResult(false, "$name: $message \n $context \n $file")
            } else if (name !=null) {
                RegexRuleResult(false, "$name: $message $file")
            } else {
                RegexRuleResult(false, message)
            }
        }

}

object Constants {

    private fun regex( regex: Regex,  message:String) = RegexRule(regex, message)
    const val MODEL_ASSOCIATION_DEFINITION = "definition"
    const val MODEL_FIELD_DEFINITION = "definition"
    const val MODEL_ASSOCIATIONS = "associations"
    const val MODEL_FIELDS = "fields"
    const val DESCRIPTION = "description"
    const val MODULE_NAME = "module-name"
    const val META_DATA = "metadata"

    const val KIND_MODEL = "Model"
    const val KIND = "kind"
    const val API_VERSION = "apiVersion"
    const val MODEL_VALIDATION_RULES = "validation-rules"
    const val MODEL_FIELD_DEFINITIONS = "field-definitions"
    const val MODEL_DEFINITIONS = "defininitions"
    const val MODEL_DEFINITION_TYPE  = "type"
    const val MODEL_DEFINITION_VALIDATION_RULES  = "validation-rules"

    val VALID_FIELD_TYPES = FieldType.values().map { it.fieldTypeName }.toSet()

    val VALIDATION_CONFIG_RULES = setOf("min", "max", "regex", "startsWith", "endsWith", "contains")
    val VALIDATION_CONFIG_RULES_INT = setOf("min", "max")
    val VALIDATION_CONFIG_RULES_STRING = setOf("startsWith", "endsWith", "contains")
    val VALIDATION_CONFIG_RULES_REGEX = setOf("regex")

    const val MESSAGE_REGEX_ALPHANUMERIC = "must be alphanumeric with a length between 2 and 15 characters"
    const val MESSAGE_REGEX_ALPHANUMERIC_UNDERSCORE = "must be alphanumeric or underscore with a length between 2 and 15 characters"
    const val MESSAGE_REGEX_ALPHA = "must be letters with a length between 2 and 15 characters"

    val REGEX_ALPHANUMERIC_UNDERSCORE = regex(Regex("^[a-zA-Z0-9_]{2,15}$"), MESSAGE_REGEX_ALPHANUMERIC_UNDERSCORE)
    val REGEX_ALPHANUMERIC = regex(Regex("^[a-zA-Z0-9]{2,15}+$"), MESSAGE_REGEX_ALPHANUMERIC)
    val REGEX_ALPHA = regex(Regex("^[a-zA-Z]{2,15}$"), MESSAGE_REGEX_ALPHA)

}
