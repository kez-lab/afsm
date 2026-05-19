package afsm.gradle

import javax.inject.Inject
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.testing.Test

public abstract class AfsmGraphExtension @Inject constructor(
    objects: ObjectFactory,
) {
    public val outputDir: DirectoryProperty = objects.directoryProperty()

    public val variant: Property<String> = objects.property(String::class.java)
        .convention("debug")

    public val addProcessorDependency: Property<Boolean> = objects.property(Boolean::class.java)
        .convention(true)

    public val processorDependency: Property<String> = objects.property(String::class.java)
        .convention(AfsmGraphPluginDefaults.processorDependency)

    public val addJunitDependency: Property<Boolean> = objects.property(Boolean::class.java)
        .convention(true)

    public val junitDependency: Property<String> = objects.property(String::class.java)
        .convention("junit:junit:4.13.2")
}

internal object AfsmGraphPluginDefaults {
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

        val generatedTest = project.tasks.register(
            "generateAfsmMmdExportTest",
            GenerateAfsmMmdExportTestTask::class.java,
            Action<GenerateAfsmMmdExportTestTask> { task ->
                task.group = "documentation"
                task.description = "Generates the Afsm unit test that writes .mmd graph files."
                task.outputDir.convention(
                    project.layout.buildDirectory.dir("generated/afsm/graph-test/kotlin"),
                )
            },
        )

        project.pluginManager.withPlugin("com.android.application") {
            project.addGeneratedTestSource(generatedTest)
        }

        project.pluginManager.withPlugin("com.android.library") {
            project.addGeneratedTestSource(generatedTest)
        }

        val generateMmd = project.tasks.register(
            "generateAfsmMmd",
            Test::class.java,
            Action<Test> { task ->
                task.group = "documentation"
                task.description = "Generates Afsm state machine .mmd graph files."
                task.outputs.dir(extension.outputDir)
                task.include("afsm/generated/AfsmGeneratedMmdExportTest.class")
                task.testLogging.showStandardStreams = true
                task.doFirst {
                    task.systemProperty(
                        "afsm.mmd.outputDir",
                        extension.outputDir.get().asFile.absolutePath,
                    )
                }
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

            if (extension.addJunitDependency.get()) {
                project.dependencies.add("testImplementation", extension.junitDependency.get())
            }

            val variantName = extension.variant.get()
            val capitalizedVariant = variantName.replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase() else char.toString()
            }
            val testTaskName = "test${capitalizedVariant}UnitTest"
            val compileKotlinTaskName = "compile${capitalizedVariant}UnitTestKotlin"
            val compileJavaTaskName = "compile${capitalizedVariant}UnitTestJavaWithJavac"
            val kspTaskName = "ksp${capitalizedVariant}UnitTestKotlin"

            project.tasks.matching { task ->
                task.name == compileKotlinTaskName ||
                    task.name == compileJavaTaskName ||
                    task.name == kspTaskName
            }.configureEach(
                Action<Task> { task ->
                    task.dependsOn(generatedTest)
                },
            )

            val unitTestTask = project.tasks.named(testTaskName, Test::class.java)
            unitTestTask.configure(
                Action<Test> { task ->
                    task.exclude("afsm/generated/AfsmGeneratedMmdExportTest.class")
                },
            )
            generateMmd.configure(
                Action<Test> { task ->
                    task.dependsOn(compileKotlinTaskName)
                    task.dependsOn(compileJavaTaskName)
                    task.testClassesDirs = unitTestTask.get().testClassesDirs
                    task.classpath = unitTestTask.get().classpath
                    task.shouldRunAfter(unitTestTask)
                },
            )
        }
    }

    private fun Project.addGeneratedTestSource(
        generatedTest: TaskProvider<GenerateAfsmMmdExportTestTask>,
    ) {
        extensions.configure(
            "android",
            Action<Any> { android ->
                val sourceSets = android.callNoArg("getSourceSets")
                val testSourceSet = sourceSets.call("getByName", "test")
                val javaSources = testSourceSet.callNoArg("getJava")
                javaSources.call("srcDir", generatedTest.flatMap { it.outputDir })
            },
        )
    }

    private fun Any.callNoArg(methodName: String): Any {
        return call(methodName)
    }

    private fun Any.call(
        methodName: String,
        vararg args: Any,
    ): Any {
        val method = javaClass.methods.firstOrNull { method ->
            method.name == methodName && method.parameterTypes.size == args.size
        } ?: error("Afsm graph plugin could not call $methodName on ${javaClass.name}.")

        return method.invoke(this, *args)
            ?: error("Afsm graph plugin expected $methodName on ${javaClass.name} to return a value.")
    }
}

public abstract class GenerateAfsmMmdExportTestTask : DefaultTask() {
    @get:OutputDirectory
    public abstract val outputDir: DirectoryProperty

    @TaskAction
    public fun generate() {
        val outputFile = outputDir
            .file("afsm/generated/AfsmGeneratedMmdExportTest.kt")
            .get()
            .asFile
        outputFile.parentFile.mkdirs()
        outputFile.writeText(
            """
            package afsm.generated

            import afsm.core.AfsmMmdOptions
            import afsm.core.AfsmMmdWriter
            import afsm.core.AfsmGraphRegistry
            import java.io.File
            import org.junit.Assert.assertTrue
            import org.junit.Test

            class AfsmGeneratedMmdExportTest {
                @Test
                fun `writes all registered afsm graph files`() {
                    val outputDir = File(
                        System.getProperty("afsm.mmd.outputDir")
                            ?: "build/generated/afsm/mmd",
                    )
                    val registry = generatedRegistry()

                    AfsmMmdWriter.writeAll(
                        registry = registry,
                        outputDir = outputDir,
                        options = AfsmMmdOptions.Flow,
                    )

                    assertTrue(
                        "No Afsm graph entries were registered.",
                        registry.entries.isNotEmpty(),
                    )

                    registry.entries.forEach { entry ->
                        val graphFile = outputDir.resolve(entry.fileName)
                        assertTrue(
                            "Missing Afsm graph file: ${'$'}{entry.fileName}",
                            graphFile.isFile,
                        )
                        assertTrue(
                            "Invalid Afsm graph file: ${'$'}{entry.fileName}",
                            graphFile.readText().startsWith("stateDiagram-v2"),
                        )
                    }
                }

                private fun generatedRegistry(): AfsmGraphRegistry {
                    val registryClass = try {
                        Class.forName("afsm.generated.AfsmGeneratedGraphRegistry")
                    } catch (error: ClassNotFoundException) {
                        val message =
                            "No Afsm graph registry was generated. Add @AfsmGraph to at least one Afsm graph source, or run normal unit tests instead of generateAfsmMmd."
                        System.err.println(message)
                        throw AssertionError(
                            message,
                            error,
                        )
                    }

                    val instance = registryClass.getField("INSTANCE").get(null)
                    return instance as? AfsmGraphRegistry
                        ?: throw AssertionError(
                            "AfsmGeneratedGraphRegistry does not implement AfsmGraphRegistry.",
                        )
                }
            }
            """.trimIndent(),
        )
    }
}
