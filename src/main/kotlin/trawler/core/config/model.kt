package trawler.core.config

import trawler.core.model.FieldType

data class ModelConfig(val tags: List<TagDefinition>,
                       val validations: List<ValidationDefinition>,
                       val fieldTypes: List<FieldTypeDefinition>,
                       val models: List<ModelDefinition>
)

data class TagDefinition(val moduleName: String, val name: String, val description: String?)

data class ValidationDefinition(val moduleName: String, val name: String,  val description: String?,
                                val validationConfig: List<ValidationConfig>)
data class ValidationConfig( val name: String, val param : String)

data class FieldTypeDefinition(val moduleName: String, val name: String,
                           val fieldType: FieldType,
                           val description: String? = null,
                           val validations: List<String> = listOf())

data class FieldDefinition(val name: String, val fieldDefinition: String, val description: String?=null,  val required: Boolean=true,
                               val tags: List<String> = listOf())


data class AssociationDefinition(val name: String, val definition: String, val description: String?=null,  val required: Boolean=true,
                           val tags: List<String> = listOf(), val many: Boolean = false)

data class ModelDefinition(val moduleName: String,
                           val name: String,
                           val fields: List<FieldDefinition> = listOf(),
                           val associations: List<AssociationDefinition> = listOf(),
                           val extends:List<ModelDefinition> = listOf(),
                           val tags : List<String> = listOf(),
                           val description: String?=null)





