package trawler.core

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import trawler.core.config.*
import trawler.core.internal.util.ResponseMessageType
import trawler.core.model.FieldType
import trawler.core.model.ValidationType
import java.io.File


internal class ConfigReaderTest {
    val module1 = "Module1";
    val module2 = "Module2";

    val tags = listOf(
        TagDefinition(module1, "view1", "tag for view 1 description"),
        TagDefinition(module2, "adminView", "tag for adminView 1 description")

    )

    val validations = listOf(
        ValidationDefinition(module1, "phone", "val1 description", listOf(
            ValidationConfig( "min", "5"),
            ValidationConfig( "max", "15"),
            ValidationConfig( "regex", "\\(?\\<[0-9]{3}[-) ]?[0-9]{3}[ -]?[0-9]{4}\\>"),
        )),
        ValidationDefinition(module2, "phoneNumber", "val1 description", listOf(
            ValidationConfig( "min", "5"),
            ValidationConfig( "max", "15"),
            ValidationConfig( "regex", "\\(?\\<[0-9]{3}[-) ]?[0-9]{3}[ -]?[0-9]{4}\\>"),
        ))
    )

    val fieldTypes = listOf(
        FieldTypeDefinition(module1, "UUID", FieldType.UUID),
        FieldTypeDefinition(module1, "Name", FieldType.STRING),
        FieldTypeDefinition(module1, "PhoneNumber", FieldType.STRING, validations= listOf("phone")),
        FieldTypeDefinition(module2, "UUID2", FieldType.UUID),
        FieldTypeDefinition(module2, "Name2", FieldType.STRING),
        FieldTypeDefinition(module2, "PhoneNumber2", FieldType.STRING, validations= listOf("phoneNumber"))
    )

    val models:List<ModelDefinition> = listOf(
        ModelDefinition(module1, "Employee",
            fields = listOf(
                FieldDefinition("id", "UUID"),
                FieldDefinition("firstName", "Name"),
                FieldDefinition("lastName", "Name"),
                FieldDefinition("phoneNumber", "PhoneNumber", required = false)
            ),
            associations = listOf(
                AssociationDefinition("department", "Department")
            )
        ),
        ModelDefinition(module1, "Department",
            fields = listOf(
                FieldDefinition("id", "UUID")
            ),
            associations = listOf(
                AssociationDefinition("manager", "Employee"),
                AssociationDefinition("employees", "[Employee]"),
            )
        )
    )

    val configReader = ConfigReader()


    @Test
    fun processConfigHasNoErrors() {

        val response: ProcessConfigResponse = configReader.processConfig(tags, validations, fieldTypes, models)
        response.messages.forEach {
            println(it.message)
        }
        assertEquals(false, response.isError())
    }

    @Test
    fun processConfigHasEmployeeModel() {
        val response: ProcessConfigResponse = configReader.processConfig(tags, validations, fieldTypes, models)
        assertTrue(response.backendDataModule.modelObjects.any { it.name == "Employee" })
    }

    @Test
    fun processConfigHasEmployeeHasFields() {

        val response: ProcessConfigResponse = configReader.processConfig(tags, validations, fieldTypes, models)
        val fields = response.backendDataModule.modelObjects.find { it.name == "Employee" }?.fields!!
        assertTrue(fields.any { it.name == "id" })
        assertTrue(fields.any { it.name == "firstName" })
        assertTrue(fields.any { it.name == "lastName" })
        assertTrue(fields.any { it.name == "phoneNumber" })
    }

    @Test
    fun processConfigHasEmployeeIdFieldIsValid() {
        val response: ProcessConfigResponse = configReader.processConfig(tags, validations, fieldTypes, models)
        val fields = response.backendDataModule.modelObjects.find { it.name == "Employee" }?.fields!!
        val idField = fields.find { it.name == "id" }!!
        assertTrue(idField.required)
        assertTrue(idField.fieldType == FieldType.UUID)
    }

    @Test
    fun processConfigHasEmployeeFirstNameFieldIsValid() {
        val response: ProcessConfigResponse = configReader.processConfig(tags, validations, fieldTypes, models)
        val fields = response.backendDataModule.modelObjects.find { it.name == "Employee" }?.fields!!
        val firstNameField = fields.find { it.name == "firstName" }!!
        assertTrue(firstNameField.required)
        assertTrue(firstNameField.fieldType == FieldType.STRING)
    }

    @Test
    fun processConfigHasEmployeePhoneNumberFieldIsValid() {
        val response: ProcessConfigResponse = configReader.processConfig(tags, validations, fieldTypes, models)
        val fields = response.backendDataModule.modelObjects.find { it.name == "Employee" }?.fields!!
        val phoneNumberField = fields.find { it.name == "phoneNumber" }!!
        assertTrue(!phoneNumberField.required)
        assertTrue(phoneNumberField.fieldType == FieldType.STRING)
    }


    @Test
    fun processConfigHasEmployeePhoneNumberFieldHasValidConfigIsValid() {
        val response: ProcessConfigResponse = configReader.processConfig(tags, validations, fieldTypes, models)
        validationPhoneNumberField(response)

    }

    private fun validationPhoneNumberField(response: ProcessConfigResponse, validationRuleName:String = "phone" ) {
        val phoneNumberField =
            response.backendDataModule.modelObjects.find { it.name == "Employee" }?.fields!!.find { it.name == "phoneNumber" }!!

        assertTrue(phoneNumberField.validations.any { it.name == validationRuleName })
        val phoneValidation = phoneNumberField.validations.find { it.name == validationRuleName }!!

        assertEquals(validationRuleName, phoneValidation.name)

        assertTrue(phoneValidation.validations.any { it.type == ValidationType.MIN })
        assertTrue(phoneValidation.validations.any { it.type == ValidationType.MAX })
        assertTrue(phoneValidation.validations.any { it.type == ValidationType.REGEX })

        val maxValidation = phoneValidation.validations.find { it.type == ValidationType.MAX }!!

        assertEquals(15, maxValidation.value)
    }

    @Test
    fun employeeHasDepartmentAssociation() {
        val response: ProcessConfigResponse = configReader.processConfig(tags, validations, fieldTypes, models)
        val associations = response.backendDataModule.modelObjects.find { it.name == "Employee" }?.associations!!
        assertTrue(associations.any { it.name == "department" })
        val association = associations.find { it.name == "department" }!!
        assertFalse(association.isRef())
        assertEquals(association.definition().name, "Department")
    }

    @Test
    fun usingFieldFromAnotherModule() {

        val models:List<ModelDefinition> = listOf(
            ModelDefinition(
                module1, "Employee",
                fields = listOf(
                    FieldDefinition("id", "UUID"),
                    FieldDefinition("phoneNumber", "${module2.lowercase()}/PhoneNumber2", required = false)
                ),
            ),
        )

        val response: ProcessConfigResponse =
            configReader.processConfig(tags, validations, fieldTypes, models )

        response.messages.forEach {
            println(it.message)
        }
        assertFalse(response.isError())

        response.backendDataModule.modelObjects.find { it.name == "Employee" }!!.fields.any {it.name == "phoneNumber"}

    }


    @Test
    fun usingAssociationFromAnotherModule() {

        val models:List<ModelDefinition> = listOf(
            ModelDefinition(module1, "Employee",
                fields = listOf(
                    FieldDefinition("id", "UUID", tags= listOf("foo/FooBar")),
                    FieldDefinition("firstName", "Name"),
                    FieldDefinition("lastName", "Name"),
                    FieldDefinition("phoneNumber", "PhoneNumber", required = false)
                ),
                associations = listOf(
                    AssociationDefinition("department", "${module2.lowercase()}/Department")
                )
            ),
            ModelDefinition(module2, "Department",
                fields = listOf(
                    FieldDefinition("id", "${module1.lowercase()}/UUID")
                ),
                associations = listOf(
                    AssociationDefinition("manager", "${module1.lowercase()}/Employee")
                )
            )
        )

        val response: ProcessConfigResponse =
            configReader.processConfig(tags, validations, fieldTypes, models )

        response.messages.forEach {
            println(it.message)
        }
        assertFalse(response.isError())
        response.backendDataModule.modelObjects.find { it.name == "Employee" }!!.associations.any {it.name == "department"}
    }

    @Test
    fun usingValidationsFromAnotherModule() {

        val validations = listOf(
            ValidationDefinition(module1, "phone", "val1 description", listOf(
                ValidationConfig( "min", "5"),
                ValidationConfig( "max", "15"),
                ValidationConfig( "regex", "\\(?\\<[0-9]{3}[-) ]?[0-9]{3}[ -]?[0-9]{4}\\>"),
            )),
            ValidationDefinition(module2, "phoneNumber", "val1 description", listOf(
                ValidationConfig( "min", "5"),
                ValidationConfig( "max", "15"),
                ValidationConfig( "regex", "\\(?\\<[0-9]{3}[-) ]?[0-9]{3}[ -]?[0-9]{4}\\>"),
            ))
        )

        val fieldTypes = listOf(
            FieldTypeDefinition(module1, "UUID", FieldType.UUID),
            FieldTypeDefinition(module1, "Name", FieldType.STRING),
            FieldTypeDefinition(module1, "PhoneNumber", FieldType.STRING, validations= listOf("${module2.lowercase()}/phoneNumber")),
         )

        val response: ProcessConfigResponse =
            configReader.processConfig(tags, validations, fieldTypes, models )

        response.messages.forEach {
            println(it.message)
        }
        assertFalse(response.isError())
        validationPhoneNumberField(response, "phoneNumber")
    }


    @Test
    fun everyFieldTypeEveryValidation() {

        val validations = listOf(
            ValidationDefinition(
                module1, "phone", "val1 description",
                listOf(
                    ValidationConfig("startsWith", "510"),
                    ValidationConfig("endsWith", "1212"),
                    ValidationConfig("contains", "1212"),
                    ValidationConfig("min", "5"),
                    ValidationConfig("max", "15"),
                    ValidationConfig("regex", "\\(?\\<[0-9]{3}[-) ]?[0-9]{3}[ -]?[0-9]{4}\\>"),
                ),
            ),
            ValidationDefinition(
                module1, "age", "age description",
                listOf(
                    ValidationConfig("min", "5"),
                    ValidationConfig("max", "15"),
                ),
            )
        )

        val fieldTypes = listOf(
            FieldTypeDefinition(module1, "UUID", FieldType.UUID, validations=listOf("age")),
            FieldTypeDefinition(module1, "Age", FieldType.INT),
            FieldTypeDefinition(module1, "Salary", FieldType.BIG_DECIMAL),
            FieldTypeDefinition(module1, "IQ", FieldType.BIG_INT),

            FieldTypeDefinition(module1, "Name", FieldType.STRING),
            FieldTypeDefinition(module1, "PhoneNumber", FieldType.STRING, validations= listOf("phone")),
        )

        val models:List<ModelDefinition> = listOf(
            ModelDefinition(module1, "Employee",
                fields = listOf(
                    FieldDefinition("id", "UUID"),
                    FieldDefinition("age", "Age"),
                    FieldDefinition("salary", "Salary"),
                    FieldDefinition("iq", "IQ"),
                    FieldDefinition("firstName", "Name"),
                    FieldDefinition("lastName", "Name"),
                    FieldDefinition("phoneNumber", "PhoneNumber", required = false)
                )
            )
        )
        val response: ProcessConfigResponse =
            configReader.processConfig(tags, validations, fieldTypes, models )

        response.messages.forEach {
            println(it.message)
        }
        assertFalse(response.isError())
    }

    @Test
    fun errorCaseFieldDefinitionNotFound() {

        val badModel = ModelDefinition(module1, "EmployeeBad",
            fields = listOf(
                FieldDefinition("id", "FAKEFIELDDEFINITION")
            )
        )
        val response: ProcessConfigResponse =
            configReader.processConfig(tags, validations, fieldTypes, models + badModel)

        response.messages.forEach {
            println(it.message)
        }
        assertTrue(response.isError())
        response.messages.any{it.message.contains("Unable") &&
                it.message.contains("find") &&
                it.message.contains("FAKEFIELDDEFINITION")}
    }

    @Test
    fun errorCaseFieldDefinitionNotFoundWrongModuleError() {
        val badModel = ModelDefinition("FAKEMODULE", "EmployeeBad",
            fields = listOf(
                FieldDefinition("id", "UUID")
            )
        )
        val response: ProcessConfigResponse =
            configReader.processConfig(tags, validations, fieldTypes, models + badModel)

        response.messages.forEach {
            println(it.message)
        }
        assertTrue(response.isError())
        response.messages.any{it.message.contains("Unable") &&
                it.message.contains("find") &&
                it.message.contains("FAKEFIELDDEFINITION")}
    }

    @Test
    fun errorCaseValidationRuleNotFound() {
        val fieldTypes = listOf(
            FieldTypeDefinition(module1, "UUID", FieldType.UUID),
            FieldTypeDefinition(module1, "Name", FieldType.STRING),
            FieldTypeDefinition(module1, "PhoneNumber", FieldType.STRING, validations= listOf("CRAP"))
        )
        val response: ProcessConfigResponse =
            configReader.processConfig(tags, validations, fieldTypes, models )

        response.messages.forEach {
            println(it.message)
        }
        assertTrue(response.isError())
    }

    @Test
    fun errorCaseValidationRuleNotFoundWarningOnly() {
        val configReader = ConfigReader(true)
        val fieldTypes = listOf(
            FieldTypeDefinition(module1, "UUID", FieldType.UUID),
            FieldTypeDefinition(module1, "Name", FieldType.STRING),
            FieldTypeDefinition(module1, "PhoneNumber", FieldType.STRING, validations= listOf("CRAP"))
        )
        val response: ProcessConfigResponse =
            configReader.processConfig(tags, validations, fieldTypes, models )

        response.messages.forEach {
            println(it.message)
        }
        assertFalse(response.isError())
        assertTrue(response.messages.any { it.type == ResponseMessageType.WARNING })
    }

    @Test
    fun errorCaseBadValidationRule() {

        val validations = listOf(
            ValidationDefinition(module1, "phone", "val1 description", listOf(
                ValidationConfig( "minCRAPMESSUP", "5"),
                ValidationConfig( "max", "15"),
                ValidationConfig( "regex", "\\(?\\<[0-9]{3}[-) ]?[0-9]{3}[ -]?[0-9]{4}\\>"),
            )),
            ValidationDefinition(module2, "phoneNumber", "val1 description", listOf(
                ValidationConfig( "min", "5"),
                ValidationConfig( "max", "15"),
                ValidationConfig( "regex", "\\(?\\<[0-9]{3}[-) ]?[0-9]{3}[ -]?[0-9]{4}\\>"),
            ))
        )

        val response: ProcessConfigResponse =
            configReader.processConfig(tags, validations, fieldTypes, models )

        response.messages.forEach {
            println(it.message)
        }
        assertTrue(response.isError())
    }

    @Test
    fun errorCaseBadValidationRuleWarningOnly() {

        val configReader = ConfigReader(true)

        val validations = listOf(
            ValidationDefinition(module1, "phone", "val1 description", listOf(
                ValidationConfig( "minCRAPMESSUP", "5"),
                ValidationConfig( "max", "15"),
                ValidationConfig( "regex", "\\(?\\<[0-9]{3}[-) ]?[0-9]{3}[ -]?[0-9]{4}\\>"),
            )),
            ValidationDefinition(module2, "phoneNumber", "val1 description", listOf(
                ValidationConfig( "min", "5"),
                ValidationConfig( "max", "15"),
                ValidationConfig( "regex", "\\(?\\<[0-9]{3}[-) ]?[0-9]{3}[ -]?[0-9]{4}\\>"),
            ))
        )

        val response: ProcessConfigResponse =
            configReader.processConfig(tags, validations, fieldTypes, models )

        response.messages.forEach {
            println(it.message)
        }
        assertFalse(response.isError())
        assertTrue(response.messages.any { it.type == ResponseMessageType.WARNING })
    }


    @Test
    fun errorCaseBadValidationRuleParamProblem() {

        val configReader = ConfigReader()

        val validations = listOf(
            ValidationDefinition(module1, "phone", "val1 description", listOf(
                ValidationConfig( "min", "abc"),
                ValidationConfig( "max", "15"),
                ValidationConfig( "regex", "\\(?\\<[0-9]{3}[-) ]?[0-9]{3}[ -]?[0-9]{4}\\>"),
            )),
            ValidationDefinition(module2, "phoneNumber", "val1 description", listOf(
                ValidationConfig( "min", "5"),
                ValidationConfig( "max", "15"),
                ValidationConfig( "regex", "\\(?\\<[0-9]{3}[-) ]?[0-9]{3}[ -]?[0-9]{4}\\>"),
            ))
        )

        val response: ProcessConfigResponse =
            configReader.processConfig(tags, validations, fieldTypes, models )

        response.messages.forEach {
            println(it.message)
        }
        assertTrue(response.isError())
    }

    @Test
    fun errorCaseBadValidationRuleParamProblemWarningOnly() {

        val configReader = ConfigReader(true)

        val validations = listOf(
            ValidationDefinition(module1, "phone", "val1 description", listOf(
                ValidationConfig( "min", "abc"),
                ValidationConfig( "max", "15"),
                ValidationConfig( "regex", "\\(?\\<[0-9]{3}[-) ]?[0-9]{3}[ -]?[0-9]{4}\\>"),
            )),
            ValidationDefinition(module2, "phoneNumber", "val1 description", listOf(
                ValidationConfig( "min", "5"),
                ValidationConfig( "max", "15"),
                ValidationConfig( "regex", "\\(?\\<[0-9]{3}[-) ]?[0-9]{3}[ -]?[0-9]{4}\\>"),
            ))
        )

        val response: ProcessConfigResponse =
            configReader.processConfig(tags, validations, fieldTypes, models )

        response.messages.forEach {
            println(it.message)
        }
        assertFalse(response.isError())
        assertTrue(response.messages.any { it.type == ResponseMessageType.WARNING })
    }

    @Test
    fun errorCaseBadAssociation() {

        val models:List<ModelDefinition> = listOf(
            ModelDefinition(module1, "Employee",
                fields = listOf(
                    FieldDefinition("id", "UUID", tags= listOf("foo/FooBar", "foo")),
                    FieldDefinition("firstName", "Name"),
                    FieldDefinition("lastName", "Name"),
                    FieldDefinition("phoneNumber", "PhoneNumber", required = false)
                ),
                associations = listOf(
                    AssociationDefinition("department", "CRAP/Department")
                )
            ),
            ModelDefinition(module2, "Department",
                fields = listOf(
                    FieldDefinition("id", "${module1.lowercase()}/UUID")
                ),
                associations = listOf(
                    AssociationDefinition("manager", "CRAP/Employee")
                )
            )
        )

        val response: ProcessConfigResponse =
            configReader.processConfig(tags, validations, fieldTypes, models )

        response.messages.forEach {
            println(it.message)
        }
        assertTrue(response.isError())
    }


    @Test
    fun errorCaseBadAssociationWarningOnly() {

        val configReader = ConfigReader(associationErrorsAreWarning = true)
        val models:List<ModelDefinition> = listOf(
            ModelDefinition(module1, "Employee",
                fields = listOf(
                    FieldDefinition("id", "UUID", tags= listOf("foo/FooBar", "foo")),
                    FieldDefinition("firstName", "Name"),
                    FieldDefinition("lastName", "Name"),
                    FieldDefinition("phoneNumber", "PhoneNumber", required = false)
                ),
                associations = listOf(
                    AssociationDefinition("department", "CRAP/Department")
                )
            ),
            ModelDefinition(module2, "Department",
                fields = listOf(
                    FieldDefinition("id", "${module1.lowercase()}/UUID")
                ),
                associations = listOf(
                    AssociationDefinition("manager", "CRAP/Employee")
                )
            )
        )

        val response: ProcessConfigResponse =
            configReader.processConfig(tags, validations, fieldTypes, models )

        response.messages.forEach {
            println(it.message)
        }

        assertFalse(response.isError())
        assertTrue(response.messages.any { it.type == ResponseMessageType.WARNING })
    }

    @Test
    fun readYaml () {
        val mapper = ObjectMapper(YAMLFactory())
        val jsonNode = mapper.readTree(File("./src/test/resources/model.yaml"))


        val apiVersion = jsonNode.get("apiVersion")

        println(apiVersion.isTextual)
        //println(jsonNode.toPrettyString())

        val yaml = java.lang.StringBuilder()


        processNode(jsonNode, yaml, 0)

        println(yaml)

    }

    private fun processNode(jsonNode: JsonNode, yaml: StringBuilder, depth: Int) {
        if (jsonNode.isValueNode) {
            //println("VALUE ${jsonNode.asText()}")
            yaml.append(jsonNode.asText())
        } else if (jsonNode.isArray) {
            for (arrayItem in jsonNode) {
                appendNodeToYaml(arrayItem, yaml, depth, true)
            }
        } else if (jsonNode.isObject) {
            appendNodeToYaml(jsonNode, yaml, depth, false)
        }
    }

    private fun appendNodeToYaml(
        node: JsonNode, yaml: StringBuilder, depth: Int, isArrayItem: Boolean
    ) {
        val fields = node.fields()
        var isFirst = true
        while (fields.hasNext()) {
            val (key, value) = fields.next()
            addFieldNameToYaml(yaml, key, depth, isArrayItem && isFirst)
            processNode(value, yaml, depth + 1)
            isFirst = false
        }
    }

    private fun addFieldNameToYaml(
        yaml: StringBuilder, fieldName: String, depth: Int, isFirstInArray: Boolean
    ) {
        if (yaml.length > 0) {
            yaml.append("\n")
            val requiredDepth = if (isFirstInArray) depth - 1 else depth
            for (i in 0 until requiredDepth) {
                yaml.append("  ")
            }
            if (isFirstInArray) {
                yaml.append("- ")
            }
        }
        yaml.append(fieldName)
        yaml.append(": ")
    }
}
