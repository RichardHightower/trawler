package trawler.core

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import trawler.core.reader.YamlConfigReader
import java.io.File

internal class YamlConfigReaderTest {

    @Test
    fun readYamlConfig() {
        val file = File("./src/test/resources/model.yaml")

        val configResult = YamlConfigReader().readYamlConfig(file)

        if (!configResult.success) {
            println(configResult.message())
        }
        assertTrue(configResult.success)

        println(configResult.result().validations)

        assertTrue(configResult.result().validations.any { it.name == "phone" })

        val phoneValidation = configResult.result().validations.find { it.name == "phone" }!!

        assertTrue(phoneValidation.validationConfig.any { it.name == "min" })
        assertTrue(phoneValidation.validationConfig.any { it.name == "max" })
        assertTrue(phoneValidation.validationConfig.any { it.name == "regex" })

        val regexRuleConfig = phoneValidation.validationConfig.find { it.name == "regex" }
        assertEquals("\\(?\\<[0-9]{3}[-) ]?[0-9]{3}[ -]?[0-9]{4}\\>", regexRuleConfig?.param)




        println(configResult.result().fieldTypes)

        println(configResult.result().models)



    }
}
