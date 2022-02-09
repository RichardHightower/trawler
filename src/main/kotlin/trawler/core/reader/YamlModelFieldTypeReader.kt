package trawler.core.reader

import com.fasterxml.jackson.databind.JsonNode
import trawler.core.Constants
import trawler.core.config.FieldTypeDefinition
import trawler.core.internal.util.ResponseMessage
import trawler.core.internal.util.Result
import trawler.core.internal.util.Results
import trawler.core.model.FieldType
import java.io.File

class YamlModelFieldTypeReader(
    file: File,
    moduleName: String,
    apiVersion: String,
    kind: String,
    private val moduleValidationRuleNames: Set<String>,
    rootNode: JsonNode,
    private val fieldDefinitions: JsonNode?,
    private val results: Results<FieldTypeDefinition> = Results()
) : YamlConfigReaderComponent<List<Result<FieldTypeDefinition>>>
    (file, moduleName, apiVersion, kind, rootNode, fieldDefinitions) {

    private fun resultError(message:String) : Result<FieldTypeDefinition> = results.error("$kind/$moduleName::$message \n from file $file" )
    private fun resultErrors(message:String, causedBy : List<ResponseMessage>) : Result<FieldTypeDefinition> =
        results.errors("$kind/$moduleName/::$message  \n from file $file", causedBy )
    private fun  returnResult(result: FieldTypeDefinition) : Result<FieldTypeDefinition> = results.result(result)

    override fun process(): List<Result<FieldTypeDefinition>> {

        if (fieldDefinitions != null) {
            return fieldDefinitions.fields().asSequence().toList().map { (name, fieldNode) ->

                val type = fieldNode.get(Constants.MODEL_DEFINITION_TYPE)?.asText()
                val description = fieldNode.get(Constants.DESCRIPTION)?.asText()

                val validationRuleNode = fieldNode.get(Constants.MODEL_DEFINITION_VALIDATION_RULES)


                val validationRules: List<String> = if (validationRuleNode != null) {
                    validationRuleNode.asSequence().map { it.asText() }.toList()
                } else {
                    listOf()
                }
               validateFields(name, type, validationRules, description)
            }
        } else {
            return listOf()
        }

    }

    private fun validateFields(
        name: String?,
        type: String?,
        validationRules: List<String>,
        description: String?
    ): Result<FieldTypeDefinition> {
        val resultList :MutableList<Result<FieldTypeDefinition>> = mutableListOf()

        if (name != null && type != null)  {
            validateName(name, resultList)
            validateType(type, resultList )
            validateValidationRules(validationRules, resultList)
        } else {
            if (name==null) {
                resultList.add(resultError("FieldDefinition name is required"))
            } else if (type==null) {
                resultList.add(resultError("FieldDefinition type is required"))
            }
        }

        return if (resultList.isNotEmpty()) {
            val messages: List<ResponseMessage> = resultList.filter { it.isAnyErrorType() }.map { it.message() }
            this.resultErrors("FieldType property validation error", messages)
        } else {
            val fieldType = FieldType.values().find { it.fieldTypeName == type }!!
            this.returnResult(FieldTypeDefinition(moduleName, name!!, fieldType, description, validationRules))
        }
    }

    private fun validateValidationRules(
        validationRules: List<String>,
        resultList: MutableList<Result<FieldTypeDefinition>>
    ) {
        validationRules.filter { !it.contains("/") }.forEach { vr ->
            if (!this.moduleValidationRuleNames.contains(vr)) {
                resultList.add(resultError("FieldDefinition validation rule $vr is not defined in module $moduleName, rule names defined: ($moduleValidationRuleNames)"))
            }
        }
    }

    private fun validateType(
        type: String,
        resultList: MutableList<Result<FieldTypeDefinition>>,
    ) {
        val typeAlphaCheck = Constants.REGEX_ALPHA.matchValue(type, "field definition name")
        if (typeAlphaCheck.failedMatch()) {
            resultList.add(resultError("FieldDefinition type $type did not validate"))
        }

        if (!Constants.VALID_FIELD_TYPES.contains(type)) {
            resultList.add(resultError("FieldDefinition type $type is not an allowed type (${Constants.VALID_FIELD_TYPES})"))
        }
    }

    private fun validateName(
        name: String,
        resultList: MutableList<Result<FieldTypeDefinition>>
    ) {
        val nameAlphaCheck = Constants.REGEX_ALPHANUMERIC.matchValue(name, "field definition name")
        if (nameAlphaCheck.failedMatch()) {
            resultList.add(resultError("FieldDefinition name $name did not validate"))
        }
    }
}
