package afsm.core

import java.io.File

/**
 * Command line entry point that writes `.mmd` graph files for every machine in
 * the KSP-generated [AfsmGraphRegistry].
 *
 * The Afsm Gradle plugin runs this class instead of injecting a generated unit
 * test into the consuming project, so graph generation does not force a test
 * framework, a test source set, or a test task on consumers.
 *
 * Usage:
 *
 * ```text
 * afsm.core.AfsmMmdExport <outputDir> [Flow|Full] [registryClassName]
 * ```
 */
public object AfsmMmdExport {
    public const val DEFAULT_REGISTRY_CLASS_NAME: String = "afsm.generated.AfsmGeneratedGraphRegistry"

    @JvmStatic
    public fun main(args: Array<String>) {
        val outputDir = args.getOrNull(0)
            ?: error("Afsm graph export requires an output directory argument.")
        val options = parseOptions(args.getOrNull(1))
        val registryClassName = args.getOrNull(2) ?: DEFAULT_REGISTRY_CLASS_NAME

        val written = export(
            outputDir = File(outputDir),
            options = options,
            registryClassName = registryClassName,
        )

        written.forEach { file ->
            println("Afsm graph written: ${file.absolutePath}")
        }
    }

    /**
     * Writes every registered graph into [outputDir] and returns the files.
     *
     * Fails when no registry was generated or when a registered graph produced
     * no readable Mermaid output, so a broken graph pipeline cannot pass
     * silently.
     */
    public fun export(
        outputDir: File,
        options: AfsmMmdOptions = AfsmMmdOptions.Flow,
        registryClassName: String = DEFAULT_REGISTRY_CLASS_NAME,
    ): List<File> {
        val registry = loadRegistry(registryClassName)

        check(registry.entries.isNotEmpty()) {
            "No Afsm graph entries were registered. Add @AfsmGraph to at least one Afsm graph source."
        }

        AfsmMmdWriter.writeAll(
            registry = registry,
            outputDir = outputDir,
            options = options,
        )

        return registry.entries.map { entry ->
            val file = outputDir.resolve(entry.fileName)
            check(file.isFile) {
                "Missing Afsm graph file for ${entry.id}: ${file.absolutePath}"
            }
            check(file.readText().startsWith(MMD_HEADER)) {
                "Invalid Afsm graph file for ${entry.id}: ${file.absolutePath} does not start with $MMD_HEADER."
            }
            file
        }
    }

    public fun parseOptions(value: String?): AfsmMmdOptions {
        return when (value) {
            null, "Flow" -> AfsmMmdOptions.Flow
            "Full" -> AfsmMmdOptions.Full
            else -> error("Unsupported Afsm MMD options: $value. Use Flow or Full.")
        }
    }

    private fun loadRegistry(registryClassName: String): AfsmGraphRegistry {
        val registryClass = try {
            Class.forName(registryClassName)
        } catch (error: ClassNotFoundException) {
            throw IllegalStateException(
                "No Afsm graph registry was generated ($registryClassName). " +
                    "Apply the KSP plugin with the afsm-graph-ksp processor and " +
                    "annotate at least one machine with @AfsmGraph.",
                error,
            )
        }

        val instance = registryClass.getField("INSTANCE").get(null)

        return instance as? AfsmGraphRegistry
            ?: error("$registryClassName does not implement AfsmGraphRegistry.")
    }

    private const val MMD_HEADER = "stateDiagram-v2"
}
