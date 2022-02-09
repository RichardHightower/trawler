package trawler.core.reader

import com.fasterxml.jackson.databind.JsonNode
import trawler.core.config.ValidationConfig
import trawler.core.config.ValidationDefinition
import trawler.core.internal.util.Result
import trawler.core.internal.util.Results
import java.io.File

class YamlModelValidationReader(
    file: File,
    moduleName: String,
    apiVersion: String,
    kind: String,
    rootNode: JsonNode,
    private val validationDefinitions: JsonNode?
) : YamlConfigReaderComponent<List<Result<ValidationDefinition>>>
    (file, moduleName, apiVersion, kind, rootNode, validationDefinitions) {


    override fun process(): List<Result<ValidationDefinition>> {
        val results = Results<ValidationDefinition>()
        if (validationDefinitions != null) {

            return validationDefinitions.fields().asSequence().map { (validationName, validationDefinition) ->

                val description = if (!validationDefinition.has(Constants.DESCRIPTION))
                    null
                else
                    validationDefinition.get(Constants.DESCRIPTION).asText()

                val alphaCheck = checkAlphaNumeric(
                    validationName,
                    "${Constants.MODEL_VALIDATION_RULES}:$validationName", validationDefinitions, file
                )

                if (alphaCheck.success) {

                    val processValidationConfigurationResults =
                        processValidationConfiguration(validationName, validationDefinition)

                    if (processValidationConfigurationResults.any { it.isError() || it.isFatal() }) {

                        val errors = processValidationConfigurationResults.filter { it.isError() || it.isFatal() }
                            .map { it.message() }

                        results.errors("Unable to process validation config for $validationName", errors)
                    } else {
                        val validationConfigs =
                            processValidationConfigurationResults.filter { it.success }.map { it.result() }
                        results.result(
                            ValidationDefinition(
                                name = validationName,
                                validationConfig = validationConfigs, moduleName = moduleName, description = description
                            )
                        )
                    }

                } else {
                    results.error(alphaCheck.message!!)
                }

            }.toList()
        } else {
            return listOf()
        }
    }


    private fun processValidationConfiguration(
        validationDefinitionName: String,
        validationDefinition: JsonNode

    ): List<Result<ValidationConfig>> =
        validationDefinition.fields().asSequence().map { (validationRuleName, param) ->
            processValidationConfig(validationRuleName, validationDefinitionName, param)
        }.toList()


    private fun processValidationConfig(
        validationRuleName: String?,
        validationDefinitionName: String,
        param: JsonNode?
    ): Result<ValidationConfig> {
        val results = Results<ValidationConfig>()

        if (validationRuleName != null) {
            val name = "${Constants.MODEL_VALIDATION_RULES}/$validationDefinitionName/$validationRuleName"
            val ruleResult = checkAlpha(
                validationRuleName,
                name, null, file
            )
            if (!ruleResult.success) {
                return results.error("Process Validation Config: ${ruleResult.message}")
            }

            if (!Constants.VALIDATION_CONFIG_RULES.contains(validationRuleName)) {
                return results.error("Process Validation Config: $name Rule config must be ${Constants.VALIDATION_CONFIG_RULES}}")
            }

            if (param != null) {
                if (param.isTextual) {
                    if (Constants.VALIDATION_CONFIG_RULES_REGEX.contains(validationRuleName)) {

                        try {
                            Regex(param.asText())
                        } catch (ex: Exception) {
                            return results.error("Process Validation Config: Regex: $name ${param.asText()} Rule config must be valid regex")
                        }
                    } else if (!Constants.VALIDATION_CONFIG_RULES_STRING.contains(validationRuleName)) {
                        return results.error(
                            "Process Validation Config: Text: $name ${param.asText()} " +
                                    "Rule config must be in ${Constants.VALIDATION_CONFIG_RULES_STRING}"
                        )
                    }

                } else if (param.isNumber) {
                    if (!Constants.VALIDATION_CONFIG_RULES_INT.contains(validationRuleName)) {
                        return results.error(
                            "Process Validation Config: Number: $name ${param.asText()} " +
                                    "Rule config must be in ${Constants.VALIDATION_CONFIG_RULES_INT}"
                        )
                    }
                }
                return Results<ValidationConfig>().result(ValidationConfig(validationRuleName, param.asText()))
            } else {
                return results.error("$name: Rule Value Missing")
            }

        } else {
            return results.error("${Constants.MODEL_VALIDATION_RULES}/$validationDefinitionName: Rule Name Missing")
        }

    }
}
