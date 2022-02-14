package trawler.core.model

enum class FieldType(val fieldTypeName: String) {
    STRING("string"),
    INT("int"),
    BOOLEAN("bool"),
    BIG_INT("big_int"),
    BIG_DECIMAL("decimal"),
    UUID("uuid"),
    DATE("date"),
    DATE_TIME("date_time"),
    ENUM("enum"),
}



data class Tag(val name: String)

data class Field(val name: String,
                 val required: Boolean,
                 val fieldType: FieldType,
                 val description: String?,
                 val validations: List<ValidationRule>,
                 val tags : Set<Tag>)

interface  Association {
     val name: String
     val required: Boolean
     val description: String?
     val many: Boolean
     val tags: Set<Tag>
    fun definition(): ModelObjectMeta
    fun definitionRef(): String
    fun isRef(): Boolean
}

data class AssociationModel(
    override val name: String,
    override val required: Boolean,
    override val description: String?,
    override val many: Boolean,
    override val tags : Set<Tag>,
    val definition: ModelObjectMeta
    ) : Association {
    override fun definition(): ModelObjectMeta = definition
    override fun isRef(): Boolean = false
    override fun definitionRef(): String = definition.name
}

data class AssociationRef(
    override val name: String,
    override val required: Boolean,
    override val many: Boolean,
    override val tags : Set<Tag>,
    val defName: String,
    override val description: String?
) : Association {
    override fun definition(): ModelObjectMeta = throw IllegalStateException()
    override fun definitionRef(): String = defName
    override fun isRef(): Boolean = true
}

data class ModelObjectMeta(val name: String, val moduleName : String, val description: String?, val fields: List<Field>,
                           val associations: List<Association>, val tags : Set<Tag>)
