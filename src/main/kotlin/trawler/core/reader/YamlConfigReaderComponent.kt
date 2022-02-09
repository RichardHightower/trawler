package trawler.core.reader

import com.fasterxml.jackson.databind.JsonNode
import trawler.core.Constants
import trawler.core.RegexRule
import java.io.File

abstract class YamlConfigReaderComponent<T>(
    val file: File,
    val apiVersion: String,
    val kind: String,
    val moduleName: String,
    val rootNode: JsonNode,
    val node: JsonNode?
) {



    private fun checkRegex(value: String, name: String, regexRule: RegexRule, node: JsonNode?, file: File) =
        regexRule.matchValue(value, name, node, file)

    fun checkAlphaNumeric(value: String, name: String, node: JsonNode?, file: File) =
        checkRegex(value, name, Constants.REGEX_ALPHANUMERIC, node, file)

    fun checkAlpha(value: String, name: String, node: JsonNode?, file: File) =
        checkRegex(value, name, Constants.REGEX_ALPHA, node, file)


    abstract fun process(): T
}


