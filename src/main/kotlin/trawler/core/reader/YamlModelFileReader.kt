package trawler.core.reader

import com.fasterxml.jackson.databind.JsonNode
import trawler.core.Config
import trawler.core.Constants
import trawler.core.internal.util.Result
import trawler.core.internal.util.Results
import java.io.File

class YamlModelFileReader(
    file: File,
    moduleName: String,
    apiVersion: String,
    kind: String,
    rootNode: JsonNode,
    private val modelFile: JsonNode?
) : YamlConfigReaderComponent<Result<Config>>(file, moduleName, apiVersion, kind, rootNode, modelFile) {
    override fun process(): Result<Config> = readModelFromYamlFile(modelFile!!)

    fun readModelFromYamlFile(jsonNode: JsonNode): Result<Config> {
        val results = Results<Config>()


        val validationReader = YamlModelValidationReader(
            file,
            apiVersion,
            kind,
            moduleName,
            jsonNode,
            jsonNode.get(Constants.MODEL_VALIDATION_RULES)
        )


        val validationRuleResults = validationReader.process()


        val validationRuleNames = validationRuleResults.filter { it.success }.map { it.result().name }.toSet()

        val fieldReader = YamlModelFieldTypeReader(
            file,
            apiVersion,
            kind,
            moduleName,
            validationRuleNames,
            jsonNode,
            jsonNode.get(Constants.MODEL_FIELD_DEFINITIONS)
        )
        val fieldTypes = fieldReader.process()

        val fieldTypeNames = fieldTypes.filter { it.success }.map { it.result().name }.toSet()
        val modelReader = YamlModelDefinitionReader(
            file,
            apiVersion,
            kind,
            moduleName,
            jsonNode,
            jsonNode.get(Constants.MODEL_DEFINITIONS),
            fieldTypeNames.toSet()
        )


        val models = modelReader.process()

        return if (validationRuleResults.any { it.isError() || it.isFatal() } || fieldTypes.any { it.isError() || it.isFatal() }
            || models.any { it.isError() || it.isFatal() }) {
            val messages =
                models.filter { it.isError() || it.isFatal() }.flatMap { it.message().causedBy.plus(it.message()) } +
                        validationRuleResults.filter { it.isError() || it.isFatal() }
                            .flatMap { it.message().causedBy.plus(it.message()) } +
                        fieldTypes.filter { it.isError() || it.isFatal() }
                            .flatMap { it.message().causedBy.plus(it.message()) }
            results.errors("Problems reading models", messages)
        } else {
            val validationDefinitions = validationRuleResults.filter { it.success }.map { it.result() }
            val fieldTypeDefinitions = fieldTypes.filter { it.success }.map { it.result() }
            val modelDefinitions = models.filter { it.success }.map { it.result() }
            results.result(Config(listOf(), validationDefinitions, fieldTypeDefinitions, models = modelDefinitions))
        }
    }

}
