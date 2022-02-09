package trawler.core.reader

import com.fasterxml.jackson.databind.JsonNode
import trawler.core.Constants
import trawler.core.config.AssociationDefinition
import trawler.core.config.FieldDefinition
import trawler.core.config.ModelDefinition
import trawler.core.internal.util.ResponseMessage
import trawler.core.internal.util.Result
import trawler.core.internal.util.Results
import java.io.File

class YamlModelDefinitionReader(
    file: File,
    moduleName: String,
    apiVersion: String,
    kind: String,
    rootNode: JsonNode,
    private val modelDefinitionNodes: JsonNode?,
    private val fieldTypeDefinitionsNames: Set<String>
) : YamlConfigReaderComponent<List<Result<ModelDefinition>>>
    (file, moduleName, apiVersion, kind, rootNode, modelDefinitionNodes) {

    private var modelName :String = ""
    private val results = Results<ModelDefinition>()
    private fun resultError(message:String) : Result<ModelDefinition> = results.error("$kind/$moduleName/$modelName::$message \n from file $file" )
    private fun resultErrors(message:String, causedBy : List<ResponseMessage>) : Result<ModelDefinition> =
        results.errors("$kind/$moduleName/$modelName::$message  \n from file $file", causedBy )
    private fun  returnResult(result: ModelDefinition) : Result<ModelDefinition> = results.result(result)


    override fun process(): List<Result<ModelDefinition>>   {
        return modelDefinitionNodes?.fields()?.asSequence()?.toList()?.map { (name, modelNode) ->
            modelName = name
            val description = modelNode.get(Constants.DESCRIPTION)?.asText()
            val fields = modelNode.get(Constants.MODEL_FIELDS)?.fields()?.asSequence()
            val associations = modelNode.get(Constants.MODEL_ASSOCIATIONS)?.fields()?.asSequence()
            validateFields(name, description, fields, associations)
        }
            ?: listOf()
    }




    private fun validateFields(
        name: String?,
        description: String?,
        fields: Sequence<MutableMap.MutableEntry<String, JsonNode>>?,
        associations: Sequence<MutableMap.MutableEntry<String, JsonNode>>?
    ): Result<ModelDefinition> {
        val resultList :MutableList<Result<ModelDefinition>> = mutableListOf()

        if (name != null)  {
            validateName(name, resultList)
        } else {
            resultList.add(resultError("Model definition name is required"))
        }

        if (fields==null && associations == null) {
            resultList.add(resultError("Model definition fields or associations or both must be defined"))
        }

        val fieldDefinitionResults: List<Result<FieldDefinition>> = fields?.map { (fieldName, fieldNode) ->
            val fieldDef : Result<FieldDefinition> = validateFieldNode(fieldName, fieldNode)
            fieldDef
        }?.toList()
            ?: listOf()
        fieldDefinitionResults.filter { it.isAnyErrorType() }.forEach{ resultList.add(resultError(it.message().message))}

        val associationsDefinitionResults: List<Result<AssociationDefinition>> = associations?.map { (name, associationNode) ->
            val aDef : Result<AssociationDefinition> = validateAssociationNode(name, associationNode)
            aDef
        }?.toList()
            ?: listOf()
        associationsDefinitionResults.filter { it.isAnyErrorType() }.forEach{ resultList.add(resultError(it.message().message))}

        return if (resultList.isNotEmpty()) {
            val messages: List<ResponseMessage> = resultList.filter { it.isAnyErrorType() }.map { it.message() }
            this.resultErrors("Model Definition property validation errors", messages)
        } else {

            val fieldDefinitions = fieldDefinitionResults.filter { it.success }.map { it.result() }
            val associationDefinitions = associationsDefinitionResults.filter { it.success }.map { it.result() }
            this.returnResult(ModelDefinition(moduleName = moduleName, name = name!!, description = description,
                fields = fieldDefinitions, associations = associationDefinitions))
        }
    }

    private fun validateAssociationNode(name: String, associationNode: JsonNode): Result<AssociationDefinition> {
        val results = Results<AssociationDefinition>()


        if (!associationNode.has(Constants.MODEL_ASSOCIATION_DEFINITION)) {
            return results.error("$moduleName/$modelName/$name must have ${Constants.MODEL_ASSOCIATION_DEFINITION} defined")
        }
        val associationDef = associationNode.get("definition").asText()!!
        val description = associationNode.get(Constants.DESCRIPTION)?.asText()

        return results.result(AssociationDefinition(name = name, definition = associationDef, description = description ))
    }

    private fun validateFieldNode(fieldName: String, fieldNode: JsonNode): Result<FieldDefinition> {

       val results = Results<FieldDefinition>()
       if (!fieldNode.has(Constants.MODEL_FIELD_DEFINITION)) {
           return results.error("$moduleName/$modelName/$fieldName must have ${Constants.MODEL_FIELD_DEFINITION} defined")
       }
       val fieldDefinition = fieldNode.get(Constants.MODEL_FIELD_DEFINITION).asText()!!
       val description = fieldNode.get(Constants.DESCRIPTION)?.asText()
       if (!fieldDefinition.contains("/")) {
            if(!fieldTypeDefinitionsNames.contains(fieldDefinition)) {
                return results.error("$moduleName/$modelName/$fieldName ${Constants.MODEL_FIELD_DEFINITION} is not a defined type ($fieldTypeDefinitionsNames)")
            }
       }



        return results.result(FieldDefinition(name = fieldName, fieldDefinition = fieldDefinition, description = description ))
    }

    private fun validateName(
        value: String,
        resultList: MutableList<Result<ModelDefinition>>,
        area: String = "model definition name",
    ) {
        val nameAlphaCheck = Constants.REGEX_ALPHANUMERIC.matchValue(value, area)
        if (nameAlphaCheck.failedMatch()) {
            resultList.add(resultError("ModelDefinition name $value did not validate"))
        }
    }
}
