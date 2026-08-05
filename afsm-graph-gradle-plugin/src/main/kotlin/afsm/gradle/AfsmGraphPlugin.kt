package afsm.gradle

import java.io.File
import javax.inject.Inject
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.testing.Test

public abstract class AfsmGraphExtension @Inject constructor(
    objects: ObjectFactory,
) {
    /** Where `generateAfsmMmd` writes generated diagrams. */
    public val outputDir: DirectoryProperty = objects.directoryProperty()

    /**
     * Checked-in baseline compared by `verifyAfsmMmd` and written by
     * `updateAfsmMmd`.
     *
     * Commit this directory so graph drift is reviewed like any other source
     * change instead of being regenerated silently.
     */
    public val checkedInDir: DirectoryProperty = objects.directoryProperty()

    public val mmdOptions: Property<String> = objects.property(String::class.java)
        .convention("Flow")

    public val variant: Property<String> = objects.property(String::class.java)
        .convention("debug")

    /** Adds `verifyAfsmMmd` to the `check` task when a baseline exists. */
    public val verifyOnCheck: Property<Boolean> = objects.property(Boolean::class.java)
        .convention(true)

    public val addProcessorDependency: Property<Boolean> = objects.property(Boolean::class.java)
        .convention(true)

    public val processorDependency: Property<String> = objects.property(String::class.java)
        .convention(AfsmGraphPluginDefaults.processorDependency)
}

internal object AfsmGraphPluginDefaults {
    const val EXPORT_MAIN_CLASS: String = "afsm.core.AfsmMmdExport"

    val processorDependency: String by lazy {
        val properties = java.util.Properties()
        val resourceName = "afsm/gradle/afsm-graph-plugin.properties"
        val stream = requireNotNull(
            javaClass.classLoader.getResourceAsStream(resourceName),
        ) {
            "Afsm graph plugin resource is missing: $resourceName"
        }

        stream.use(properties::load)
        requireNotNull(properties.getProperty("processorDependency")) {
            "Afsm graph plugin resource is missing processorDependency."
        }
    }
}

/**
 * Generates and verifies Afsm Mermaid diagrams for an Android module.
 *
 * Graph export runs `afsm.core.AfsmMmdExport` as a plain JVM program. The
 * plugin does not add a test framework, generate sources into the consumer's
 * test source set, or reflect into the Android Gradle plugin DSL.
 */
public class AfsmGraphPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create(
            "afsmGraph",
            AfsmGraphExtension::class.java,
            project.objects,
        )
        extension.outputDir.convention(
            project.layout.buildDirectory.dir("generated/afsm/mmd"),
        )
        extension.checkedInDir.convention(
            project.layout.projectDirectory.dir("afsm-graph"),
        )

        val mmdOptions = project.providers
            .gradleProperty("afsmMmdOptions")
            .orElse(extension.mmdOptions)

        val generateMmd = project.tasks.register(
            GENERATE_TASK,
            JavaExec::class.java,
            Action<JavaExec> { task ->
                task.group = "documentation"
                task.description = "Generates Afsm state machine .mmd graph files."
                task.mainClass.set(AfsmGraphPluginDefaults.EXPORT_MAIN_CLASS)
                task.outputs.dir(extension.outputDir)
                task.argumentProviders.add {
                    listOf(
                        extension.outputDir.get().asFile.absolutePath,
                        mmdOptions.get(),
                    )
                }
            },
        )

        val verifyMmd = project.tasks.register(
            VERIFY_TASK,
            VerifyAfsmMmdTask::class.java,
            Action<VerifyAfsmMmdTask> { task ->
                task.group = "verification"
                task.description =
                    "Fails when generated Afsm graphs differ from the checked-in baseline."
                task.generatedDir.set(extension.outputDir)
                task.baselineDir.set(extension.checkedInDir)
                task.updateTaskPath.set("${project.path.trimEnd(':')}:$UPDATE_TASK")
                task.dependsOn(generateMmd)
            },
        )

        project.tasks.register(
            UPDATE_TASK,
            UpdateAfsmMmdTask::class.java,
            Action<UpdateAfsmMmdTask> { task ->
                task.group = "documentation"
                task.description = "Copies generated Afsm graphs into the checked-in baseline."
                task.generatedDir.set(extension.outputDir)
                task.baselineDir.set(extension.checkedInDir)
                task.dependsOn(generateMmd)
            },
        )

        project.afterEvaluate {
            if (extension.addProcessorDependency.get()) {
                val kspConfiguration = project.configurations.findByName("ksp")
                requireNotNull(kspConfiguration) {
                    "Afsm graph generation requires the com.google.devtools.ksp plugin. " +
                        "Apply it before io.github.afsm.graph or set " +
                        "afsmGraph.addProcessorDependency=false and configure KSP manually."
                }
                project.dependencies.add("ksp", extension.processorDependency.get())
            }

            val capitalizedVariant = extension.variant.get().replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase() else char.toString()
            }
            val unitTestTask = project.tasks.named(
                "test${capitalizedVariant}UnitTest",
                Test::class.java,
            )

            // The unit test runtime classpath is the one Android classpath that
            // already contains main classes, generated KSP output, dependencies,
            // and the mockable android.jar. The FileCollection is read here
            // rather than through `unitTestTask.map { ... }`, because a
            // task-derived provider would make graph generation *run* the unit
            // tests instead of only reusing their classpath.
            val unitTestClasspath = unitTestTask.get().classpath
            val compileTasks = project.tasks.matching { task ->
                task.name == "compile${capitalizedVariant}UnitTestKotlin" ||
                    task.name == "compile${capitalizedVariant}UnitTestJavaWithJavac"
            }

            generateMmd.configure(
                Action<JavaExec> { task ->
                    task.classpath(unitTestClasspath)
                    task.dependsOn(compileTasks)
                    task.shouldRunAfter(unitTestTask)
                },
            )

            if (extension.verifyOnCheck.get() && extension.checkedInDir.get().asFile.isDirectory) {
                project.tasks.named("check").configure(
                    Action<org.gradle.api.Task> { task -> task.dependsOn(verifyMmd) },
                )
            }
        }
    }

    private companion object {
        const val GENERATE_TASK = "generateAfsmMmd"
        const val VERIFY_TASK = "verifyAfsmMmd"
        const val UPDATE_TASK = "updateAfsmMmd"
    }
}

/**
 * Compares generated graphs against the checked-in baseline.
 *
 * Regenerating a diagram is not verification: without this task a stale
 * committed diagram and a changed machine can disagree forever. Any added,
 * removed, or changed file fails the build.
 */
public abstract class VerifyAfsmMmdTask : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public abstract val generatedDir: DirectoryProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public abstract val baselineDir: DirectoryProperty

    /** Task path suggested in the failure message. */
    @get:Input
    public abstract val updateTaskPath: Property<String>

    @TaskAction
    public fun verify() {
        val generated = generatedDir.get().asFile
        val baseline = baselineDir.get().asFile
        val generatedGraphs = generated.mmdFilesByRelativePath()
        val baselineGraphs = baseline.mmdFilesByRelativePath()

        val problems = buildList {
            (generatedGraphs.keys - baselineGraphs.keys).sorted().forEach { path ->
                add("missing from baseline: $path")
            }
            (baselineGraphs.keys - generatedGraphs.keys).sorted().forEach { path ->
                add("no longer generated: $path")
            }
            (generatedGraphs.keys intersect baselineGraphs.keys).sorted().forEach { path ->
                val generatedText = generatedGraphs.getValue(path).readText()
                val baselineText = baselineGraphs.getValue(path).readText()
                if (generatedText != baselineText) {
                    add("out of date: $path\n${describeDifference(baselineText, generatedText)}")
                }
            }
        }

        if (problems.isEmpty()) {
            return
        }

        throw GradleException(
            buildString {
                appendLine("Afsm graphs differ from the checked-in baseline in ${baseline.path}:")
                problems.forEach { problem -> appendLine("  - $problem") }
                appendLine()
                append("Run '${updateTaskPath.get()}' and commit the result.")
            },
        )
    }

    private fun describeDifference(
        baselineText: String,
        generatedText: String,
    ): String {
        val baselineLines = baselineText.lines()
        val generatedLines = generatedText.lines()

        return buildString {
            (baselineLines - generatedLines.toSet()).forEach { line ->
                appendLine("      - $line")
            }
            (generatedLines - baselineLines.toSet()).forEach { line ->
                append("      + $line\n")
            }
        }.trimEnd()
    }
}

/**
 * Copies generated graphs into the checked-in baseline directory.
 */
public abstract class UpdateAfsmMmdTask : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public abstract val generatedDir: DirectoryProperty

    @get:OutputDirectory
    public abstract val baselineDir: DirectoryProperty

    @TaskAction
    public fun update() {
        val generated = generatedDir.get().asFile
        val baseline = baselineDir.get().asFile
        baseline.mkdirs()

        baseline.mmdFilesByRelativePath().forEach { (_, file) -> file.delete() }
        generated.mmdFilesByRelativePath().forEach { (path, file) ->
            val target = baseline.resolve(path)
            target.parentFile.mkdirs()
            file.copyTo(target, overwrite = true)
            logger.lifecycle("Afsm graph baseline updated: ${target.path}")
        }
    }
}

private fun File.mmdFilesByRelativePath(): Map<String, File> {
    if (!isDirectory) {
        return emptyMap()
    }

    return walkTopDown()
        .filter { file -> file.isFile && file.extension == "mmd" }
        .associateBy { file -> file.relativeTo(this).invariantSeparatorsPath }
}
