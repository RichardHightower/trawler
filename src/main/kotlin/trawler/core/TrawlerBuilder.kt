package trawler.core

import trawler.core.internal.util.Result
import trawler.core.internal.util.Results
import trawler.core.reader.YamlConfigReader
import java.io.File
import trawler.core.config.ModelConfig
import trawler.core.config.ModelConfigReader
import trawler.core.config.ProcessConfigResponse
import trawler.core.model.ModelObjectMeta

class Trawler(val models: List<ModelObjectMeta>) {

}

class TrawlerBuilder {

    var configDirectories : List<File> = listOf()

    fun withConfigDirectories(vararg directories: String) :TrawlerBuilder {
        configDirectories = directories.map { File(it) }
        return this
    }

    fun build() : Result<Trawler> {
        val results = Results<Trawler>()

        if (configDirectories.isEmpty()) {
            return results.error("No config directories")
        }

        //For now just use the first directory and first file
        val dir: File = configDirectories.first()

        val yamlReader = YamlConfigReader()

        val files = dir.listFiles()

        return if (files != null) {
            val configResults = files.map{ yamlReader.readYamlConfig(it) }
            if (configResults.any { it.isAnyErrorType() }) {
                results.errors("Errors reading config files in dir $dir", configResults.map { it.message() })
            } else {
                val modelConfig: ModelConfig = configResults.first { it.success }.result()
                val configProcessor =  ModelConfigReader()
                val config: ProcessConfigResponse = configProcessor.processConfig(modelConfig)

                if (config.isError()) {
                    results.errors("Errors processing config", config.messages)
                } else {
                    results.result(Trawler(config.backendDataModule.modelObjects))
                }


            }
        } else {
            results.error("No files in config directory $dir")
        }

    }

}
