package trawler.core

import trawler.core.config.*
import trawler.core.internal.util.ResponseMessage
import trawler.core.internal.util.ResponseMessageType
import trawler.core.internal.util.Result
import trawler.core.internal.util.Results
import trawler.core.model.*


data class BackendDataModule(val roles: List<Role>, val modelObjects: List<ModelObjectMeta>)


data class ProcessConfigResponse(val backendDataModule: BackendDataModule, val messages: List<ResponseMessage>) {
    fun isError(): Boolean {
        return messages.any {
            it.type == ResponseMessageType.ERROR || it.type == ResponseMessageType.FATAL
        }
    }
}

data class Config(val tags: List<TagDefinition>,
                  val validations: List<ValidationDefinition>,
                  val fieldTypes: List<FieldTypeDefinition>,
                  val models: List<ModelDefinition>
)

class ConfigReader(private val validationErrorsAreWarnings: Boolean=false, private val associationErrorsAreWarning : Boolean=false) {

    fun processConfig(
        config:Config
    ): ProcessConfigResponse = processConfig(config.tags, config.validations, config.fieldTypes, config.models)

    fun processConfig(
        tags: List<TagDefinition>,
        validations: List<ValidationDefinition>,
        fieldTypes: List<FieldTypeDefinition>,
        models: List<ModelDefinition>
    ): ProcessConfigResponse {
        val validationMap = validations.associateBy { it.moduleName.lowercase() + "/" + it.name }
        val fieldTypeMap = fieldTypes.associateBy { it.moduleName.lowercase() + "/" + it.name }
        val modelDefMap = models.associateBy { it.moduleName.lowercase() + "/" + it.name }


        val objectModelsResults = extractObjectModels(models, fieldTypeMap, validationMap, modelDefMap, tags)

        val messages = objectModelsResults.filter { it.isError() || it.isWarning() || it.isFatal() }.flatMap {
            it.message().causedBy.plus(it.message())
        }

        val objectModels = objectModelsResults.filter { it.success }.map { it.result() }
        val objectModelMap = objectModels.associateBy {  it.moduleName.lowercase() + "/" + it.name }

        val wasErrors = objectModelsResults.any { it.isError() || it.isWarning() || it.isFatal() }

        return if (wasErrors) {
            ProcessConfigResponse(backendDataModule = BackendDataModule(listOf(), objectModels), messages = messages)
        } else {
            val resolvedObjectModels = createFinalAssociations(objectModels, objectModelMap)
            ProcessConfigResponse(backendDataModule = BackendDataModule(listOf(),
                resolvedObjectModels),
                messages = messages)
        }
    }

    private fun normalizeTags(moduleName: String, tags: List<String>): List<Tag> =
        tags.map {
            if (it.contains("/")) {
                val split = it.split("/")
                val pathModuleName = split.first().lowercase()
                val itemName = split.last()
                Tag(pathModuleName.lowercase() + "/" + itemName)
            } else {
                Tag(moduleName.lowercase() + "/" + it)
            }
        }

    private fun refToNormalizeRefString(moduleName: String, ref: String): String =
        if (ref.contains("/")) {
            val split = ref.split("/")
            val refModuleName = split.first().lowercase()
            val itemName = split.last()
            "$refModuleName/$itemName"
        } else {
            "${moduleName.lowercase()}/$ref"
        }




    private fun createFinalAssociations(
        objectModels: List<ModelObjectMeta>,
        objectModelMap: Map<String, ModelObjectMeta>
    ): List<ModelObjectMeta> = objectModels.map { original ->
         val modelAssociations =  original.associations.map { aRef ->
                AssociationModel(aRef.name, aRef.required, aRef.description, aRef.many,
                    aRef.tags, objectModelMap[aRef.definitionRef()]!!) }
          original.copy(associations = modelAssociations)
    }


    private fun extractObjectModels(
        models: List<ModelDefinition>,
        fieldTypeMap: Map<String, FieldTypeDefinition>,
        validationMap: Map<String, ValidationDefinition>,
        modelMap: Map<String, ModelDefinition>,
        tags: List<TagDefinition>
    ): List<Result<ModelObjectMeta>> = models.map { model ->
        extractModelMeta(model, fieldTypeMap, validationMap, modelMap, tags)
    }

    private fun extractModelMeta(
        model: ModelDefinition,
        fieldTypeMap: Map<String, FieldTypeDefinition>,
        validationMap: Map<String, ValidationDefinition>,
        modelMap: Map<String, ModelDefinition>,
        allTags: List<TagDefinition>
    ): Result<ModelObjectMeta> {
        val tags = normalizeTags(model.moduleName, model.tags)

        val results = Results<ModelObjectMeta>()

        val fieldResults = extractFields(model, fieldTypeMap, validationMap)

        val associationResults: List<Result<Association>> = extractAssociations(model, modelMap)

        return if (fieldResults.any { it.isError() || it.isFatal() }) {
            val errors = fieldResults.filter { it.isError() || it.isFatal() }.map { it.responseMessage!! }
            results.errors(
                "Errors Processing Field Definitions for model ${model.name} in module ${model.moduleName}",
                errors
            )
        } else if (associationResults.any { it.isError() || it.isFatal() }) {
            val errors = associationResults.filter { it.isError() || it.isFatal() }.map { it.responseMessage!! }
            results.errors(
                "Errors Processing Association Definitions for model ${model.name} in module ${model.moduleName} $tags $allTags",
                errors
            )
        }
        else {
            val fields = fieldResults.filter { it.success }.map { it.result() }
            val associations = associationResults.filter { it.success && it.value !=null}.map { it.result() }
            val modelObject = ModelObjectMeta(
                name = model.name,
                description = model.description,
                fields = fields,
                associations = associations,
                tags = tags.toSet(),
                moduleName = model.moduleName
            )
            if (fieldResults.any { it.isWarning() }) {
                val warnings = fieldResults.filter { it.isWarning() }.map { it.responseMessage!! }
                val message =
                    "Warning processing Field Definitions for model ${model.name} in module ${model.moduleName}"
                results.resultWithWarning(modelObject, message, warnings)
            } else if (associationResults.any { it.isWarning() }) {
                val warnings = associationResults.filter { it.isWarning() }.map { it.responseMessage!! }
                val message =
                    "Warning processing Association Definitions for model ${model.name} in module ${model.moduleName}"
                results.resultWithWarning(modelObject, message, warnings)
            } else {
                results.result(modelObject)
            }

        }
    }

    private fun extractAssociations(
        model: ModelDefinition,
        modelMap: Map<String, ModelDefinition>
    ): List<Result<Association>> {
        val results = Results<Association>()

        return model.associations.map { aD ->
            val many = aD.definition.startsWith("[") && aD.definition.endsWith("]")
            val defRef = if(many) {
                aD.definition.substring(1, aD.definition.length -1)
            } else {
                aD.definition
            }
            val ref = refToNormalizeRefString(model.moduleName, defRef)
            val associatedModelDef = modelMap[ref]

            if (associatedModelDef != null) {
                results.result(AssociationRef(aD.name, aD.required, many,
                        normalizeTags(model.moduleName, aD.tags).toSet(),ref, aD.description))
            } else {
                if (associationErrorsAreWarning) {
                    results.warning( "Warning: Unable to find associated type name ${aD.name} ${aD.definition} for model ${model.name} of module ${model.moduleName}")
                } else {
                    results.error( "Error: Unable to find associated type name ${aD.name} ${aD.definition} for model ${model.name} of module ${model.moduleName}")
                }
            }
        }
    }


    private fun extractValidation(
        modelName: String,
        moduleName: String,
        fieldDef: FieldDefinition,
        validationDefinition: ValidationDefinition,
      validationConfigs: List<ValidationConfig>
    ): List<Result<Validation<*>>> {
        val results = Results<Validation<*>>()
        return validationConfigs.map { vc ->
            val vType = ValidationType.values().findLast { it.validationTypeName == vc.name }
            if (vType == null) {

                if (validationErrorsAreWarnings) {
                    results.warning(
                        "Warning: Unable to find validation type name ${vc.name} for validation ${validationDefinition.name} field ${fieldDef.name}" +
                                " for model $modelName " +
                                "in module $moduleName  "
                    )
                } else {
                    results.error(
                        "Error: Unable to find validation type name ${vc.name} for validation ${validationDefinition.name} field ${fieldDef.name}" +
                                " for model $modelName " +
                                "in module $moduleName  "
                    )
                }

            } else {
                try {
                    val validation = when (vType) {
                        ValidationType.CONTAINS -> StringValidation(vType, vc.param)
                        ValidationType.ENDS_WITH -> StringValidation(vType, vc.param)
                        ValidationType.REGEX -> RegexValidation(vType, Regex(vc.param))
                        ValidationType.STARTS_WITH -> StringValidation(vType, vc.param)
                        else -> IntValidation(vType, vc.param.toInt())
                    }
                    results.result(validation)
                } catch (ex:Exception) {
                    if (validationErrorsAreWarnings) {
                        results.warning(
                            "Warning: Unable to find validation type name ${vc.name} param ${vc.param} ($vType) for validation ${validationDefinition.name} field ${fieldDef.name}" +
                                    " for model $modelName " +
                                    "in module $moduleName \n${ex.message} \n${ex.stackTraceToString()}"
                        )
                    } else {
                        results.error(
                            "Error: Unable to find validation type name ${vc.name} param ${vc.param} ($vType) for validation ${validationDefinition.name} field ${fieldDef.name}" +
                                    " for model $modelName " +
                                    "in module $moduleName \n${ex.message} \n${ex.stackTraceToString()}"
                        )
                    }

                }
            }

        }
    }

    private fun extractValidationRules(
        modelName: String,
        moduleName: String,
        fieldDef: FieldDefinition,
        fieldTypedDef: FieldTypeDefinition,
        validationMap: Map<String, ValidationDefinition>
    ): List<Result<ValidationRule>> {
        val results = Results<ValidationRule>()
        val validationsRules = fieldTypedDef.validations.map { validationDefName ->
            val ref = refToNormalizeRefString(moduleName, validationDefName)
            val validationDefinition: ValidationDefinition? = validationMap[ref]
            if (validationDefinition != null) {

                val validationResults = extractValidation(
                    modelName,
                    moduleName,
                    fieldDef,
                    validationDefinition,
                    validationDefinition.validationConfig
                )

                if (validationResults.any { it.isError() || it.isFatal()} ) {
                    val errors = validationResults.filter { it.isError() || it.isFatal()} .map{it.responseMessage!!}
                    val message = "Unable to process validations for validation ${validationDefinition.name} " +
                            "for field ${fieldDef.name} (${fieldDef.fieldDefinition}) for model $modelName " +
                            "in module $moduleName"
                    results.errors( message, errors)
                } else  {

                    val validations: List<Validation<*>> = validationResults.filter { it.success }.filter { it.value!=null }.map { it.result() }
                    val validationRule = ValidationRule(validationDefinition.name, validations)

                    if (validationResults.any { it.isWarning()}) {
                        val warnings = validationResults.filter { it.isWarning()} .map{it.responseMessage!!}
                        val message = "Warning while processing validations for validation ${validationDefinition.name} for field" +
                                " ${fieldDef.name} (${fieldDef.fieldDefinition}) for model $modelName " +
                                "in module $moduleName"
                        results.resultWithWarning(validationRule, message, warnings)
                    } else {
                        results.result(validationRule)
                    }
                }


            } else {

                if (validationErrorsAreWarnings) {
                    val rule = ValidationRule(ref, listOf())
                    results.resultWithWarning(rule,
                        "Warning: Unable to find validation definition  name $validationDefName and ref" +
                                " $ref for field ${fieldDef.name} (${fieldDef.fieldDefinition}) for model $modelName " +
                                "in module $moduleName  "
                    )
                } else {
                    results.error(
                        "Error: Unable to find validation definition  name $validationDefName and ref" +
                                " $ref for field ${fieldDef.name} (${fieldDef.fieldDefinition}) for model $modelName " +
                                "in module $moduleName  "
                    )
                }


            }
        }
        return validationsRules
    }

    private fun extractFields(
        model: ModelDefinition,
        fieldTypeMap: Map<String, FieldTypeDefinition>,
        validationMap: Map<String, ValidationDefinition>
    ): List<Result<Field>> = model.fields.map { fieldDef ->
        val results = Results<Field>()

        val fieldTypeDefRef = refToNormalizeRefString(model.moduleName, fieldDef.fieldDefinition)
        val fieldTypedDef: FieldTypeDefinition? = fieldTypeMap[fieldTypeDefRef]
        val fieldTags = normalizeTags(model.moduleName, fieldDef.tags)

        if (fieldTypedDef != null) {

            val validationRulesResponse =
                extractValidationRules(model.name, fieldTypedDef.moduleName, fieldDef, fieldTypedDef, validationMap)

            if (validationRulesResponse.any { it.isError() || it.isFatal()} ) {
                val errors = validationRulesResponse.filter { it.isError() || it.isFatal()} .map{it.responseMessage!!}
                val message = "Unable to process validations for field ${fieldDef.name} (${fieldDef.fieldDefinition}) for model ${model.name} " +
                        "in module ${model.moduleName}"
                results.errors( message, errors)
            } else {
                val field = Field(
                    name = fieldDef.name, required = fieldDef.required, fieldType = fieldTypedDef.fieldType,
                    validations = validationRulesResponse.filter { it.success }.map { it.result() },
                    tags = fieldTags.toSet(), description = fieldDef.description
                )
                if (validationRulesResponse.any { it.isWarning()}) {
                    val warnings = validationRulesResponse.filter { it.isWarning()} .map{it.responseMessage!!}
                    val message = "Warning while processing validations for field" +
                            " ${fieldDef.name} (${fieldDef.fieldDefinition}) for model ${model.name} " +
                            "in module ${model.moduleName}"
                    results.resultWithWarning(field, message, warnings)
                } else {
                    results.result(field)
                }
            }
        } else {
            results.error(
                "Unable to find field definition for field ${fieldDef.name} (${fieldDef.fieldDefinition}) Ref $fieldTypeDefRef for model ${model.name} " +
                        "in module ${model.moduleName} \n $fieldDef"
            )
        }
    }


}
