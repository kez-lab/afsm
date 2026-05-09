package afsm.core

import java.io.File

public object AfsmMmdWriter {
    public fun writeAll(
        registry: AfsmGraphRegistry,
        outputDir: File,
    ) {
        registry.entries.forEach { entry ->
            val outputFile = outputDir.resolve(entry.fileName)
            outputFile.parentFile.mkdirs()
            outputFile.writeText(entry.createTopology().toMmd() + "\n")
        }
    }
}
