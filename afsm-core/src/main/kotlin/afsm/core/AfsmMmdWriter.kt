package afsm.core

import java.io.File
import java.nio.file.Path

public object AfsmMmdWriter {
    public fun writeAll(
        registry: AfsmGraphRegistry,
        outputDir: File,
        options: AfsmMmdOptions = AfsmMmdOptions.Flow,
    ) {
        registry.entries.forEach { entry ->
            val outputFile = resolveOutputFile(
                outputDir = outputDir,
                fileName = entry.fileName,
            )
            outputFile.parentFile.mkdirs()
            outputFile.writeText(entry.createTopology().toMmd(options) + "\n")
        }
    }

    private fun resolveOutputFile(
        outputDir: File,
        fileName: String,
    ): File {
        require(isSafeMmdFileName(fileName)) {
            "Afsm graph fileName must be a safe relative .mmd path: $fileName"
        }

        val root = outputDir.toPath().toAbsolutePath().normalize()
        val target = root.resolve(Path.of(fileName)).normalize()

        require(target.startsWith(root)) {
            "Afsm graph fileName must stay inside the output directory: $fileName"
        }

        return target.toFile()
    }

    private fun isSafeMmdFileName(fileName: String): Boolean {
        if (fileName.isBlank()) return false
        if (!fileName.endsWith(".mmd")) return false
        if (Path.of(fileName).isAbsolute) return false

        return fileName.split('/', '\\').all { segment ->
            segment.isNotBlank() && segment != "." && segment != ".."
        }
    }
}
