package trawler.core

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import trawler.core.internal.util.Results
import trawler.core.internal.util.Result
import java.io.File
import trawler.core.Constants
import trawler.core.config.FieldTypeDefinition
import trawler.core.config.ModelDefinition
import trawler.core.config.ValidationDefinition

class YamlConfigReader {


    fun readYamlConfig(file: File, jsonNode: JsonNode): Result<Config> {

        val results = Results<Config>()
        val validationRuleResults = processValidationRules(file, jsonNode, jsonNode.get(Constants.MODEL_VALIDATION_RULES))
        val fieldTypes : List<Result<FieldTypeDefinition>> = processFieldDefinitions(file, jsonNode, jsonNode.get(Constants.MODEL_FIELD_DEFINITIONS))
        val models = processDefinitions(file, jsonNode,jsonNode.get(Constants.MODEL_DEFINITIONS))

        return results.result(Config(listOf(), listOf(), listOf(), listOf()))
    }

    private fun processDefinitions(file: File, jsonNode: JsonNode, get: JsonNode?): List<Result<ModelDefinition>>  {
        return listOf()
    }

    private fun processFieldDefinitions(
        file: File,
        jsonNode: JsonNode,
        get: JsonNode?
    ): List<Result<FieldTypeDefinition>> {
        return listOf()
    }

    private fun processValidationRules(file: File, parentNode: JsonNode, validationRuleNode: JsonNode?): List<Result<ValidationDefinition>> {
        return listOf()
    }

    fun readYamlConfig(file: File): Result<Config> {

        val results = Results<Config>()

        val mapper = ObjectMapper(YAMLFactory())
        val jsonNode = mapper.readTree(file)



        val apiVersion = jsonNode.get(Constants.API_VERSION).asText()
        val kind = jsonNode.get(Constants.KIND).asText()

        if (kind == Constants.KIND_MODEL) {
            return readYamlConfig(file, jsonNode)
        }

        return results.result(Config(listOf(), listOf(), listOf(), listOf()))
    }


}
