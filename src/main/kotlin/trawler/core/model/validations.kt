package trawler.core.model


enum class ValidationType(val validationTypeName: String) {
    REGEX("regex"),
    STARTS_WITH("startsWith"),
    CONTAINS("contains"),
    ENUM("enum"),
    ENDS_WITH("endsWith"),
    MIN("min"),
    REQUIRED("required"),
    MAX("max"),
}


data class StringValidation(override val type: ValidationType, override val value: String) : Validation<String>
data class RegexValidation(override val type: ValidationType, override val value: Regex) : Validation<Regex>
data class IntValidation(override val type: ValidationType, override val value: Int) : Validation<Int>

interface Validation<T> {
    val type: ValidationType
    val value: T
}

data class ValidationRule (
    val name: String,
    val validations: List<Validation<*>>
)

