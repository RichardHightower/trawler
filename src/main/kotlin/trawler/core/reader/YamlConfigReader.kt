package trawler.core.reader


import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import trawler.core.config.ModelConfig
import trawler.core.internal.util.Result
import trawler.core.internal.util.Results
import java.io.File

class YamlConfigReader {

    fun readYamlConfig(file: File): Result<ModelConfig> {
        val results = Results<ModelConfig>()
        val mapper = ObjectMapper(YAMLFactory())
        val jsonNode = mapper.readTree(file)
        val apiVersion = jsonNode.get(Constants.API_VERSION).asText()
        val kind = jsonNode.get(Constants.KIND).asText()
        val moduleName = jsonNode.get(Constants.META_DATA).get(Constants.MODULE_NAME).asText()
        if (kind == Constants.KIND_MODEL) {
            val modelFileReader = YamlModelFileReader(file, apiVersion, kind,moduleName, jsonNode, jsonNode)
            return modelFileReader.process()
        }
        return results.result(ModelConfig(listOf(), listOf(), listOf(), listOf()))
    }


}
