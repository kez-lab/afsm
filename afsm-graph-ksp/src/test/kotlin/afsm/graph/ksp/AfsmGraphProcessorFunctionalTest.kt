package afsm.graph.ksp

import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertTrue
import org.gradle.testkit.runner.GradleRunner

class AfsmGraphProcessorFunctionalTest {
    @Test
    fun `processor generates registry for top-level machine property`() {
        val projectDir = createKspFixture()
        projectDir.writeMachineSource(
            """
            package example

            import afsm.core.AfsmGraph

            @AfsmGraph(id = "Direct", fileName = "DirectMachine.mmd")
            internal val directMachine: TestMachine = testMachine("Direct")
            """.trimIndent(),
        )

        gradle(projectDir)
            .withArguments("compileKotlin", "--stacktrace")
            .build()

        val registry = projectDir.resolve(
            "build/generated/ksp/main/kotlin/afsm/generated/AfsmGeneratedGraphRegistry.kt",
        )
        assertTrue(registry.isFile)
        val registrySource = registry.readText()
        assertContains(registrySource, "id = \"Direct\"")
        assertContains(registrySource, "fileName = \"DirectMachine.mmd\"")
        assertContains(registrySource, "createTopology = { example.directMachine.topology }")
    }

    @Test
    fun `processor rejects unsafe graph property shapes`() {
        val projectDir = createKspFixture()
        projectDir.writeMachineSource(
            """
            package example

            import afsm.core.AfsmGraph

            @AfsmGraph
            private val PrivateMachine: TestMachine = testMachine("Private")

            object MachineHolder {
                @AfsmGraph
                val MemberMachine: TestMachine = testMachine("Member")
            }

            @AfsmGraph
            var MutableMachine: TestMachine = testMachine("Mutable")

            @AfsmGraph
            val ComputedMachine: TestMachine
                get() = testMachine("Computed")

            @AfsmGraph
            val NotMachine: String = "not a graph source"
            """.trimIndent(),
        )

        val result = gradle(projectDir)
            .withArguments("compileKotlin", "--stacktrace")
            .buildAndFail()

        assertContains(result.output, "@AfsmGraph property must not be private.")
        assertContains(result.output, "@AfsmGraph property must be top-level.")
        assertContains(result.output, "@AfsmGraph property must be an immutable val.")
        assertContains(result.output, "@AfsmGraph property must have a stable backing field.")
        assertContains(result.output, "@AfsmGraph property must implement AfsmReducer.")
    }

    @Test
    fun `processor generates registry for object and no required arg class machines`() {
        val projectDir = createKspFixture()
        projectDir.writeMachineSource(
            """
            package example

            import afsm.core.AfsmGraph

            @AfsmGraph(id = "Zeta", fileName = "Zeta.mmd")
            object ZetaMachine : TestMachine by testMachine("Zeta")

            @AfsmGraph(id = "Alpha", fileName = "Alpha.mmd")
            class AlphaMachine(
                private val machine: TestMachine = testMachine("Alpha"),
            ) : TestMachine by machine
            """.trimIndent(),
        )

        gradle(projectDir)
            .withArguments("compileKotlin", "--stacktrace")
            .build()

        val registry = projectDir.resolve(
            "build/generated/ksp/main/kotlin/afsm/generated/AfsmGeneratedGraphRegistry.kt",
        )
        assertTrue(registry.isFile)
        val registrySource = registry.readText()
        assertContains(registrySource, "id = \"Alpha\"")
        assertContains(registrySource, "fileName = \"Alpha.mmd\"")
        assertContains(registrySource, "createTopology = { example.AlphaMachine().topology }")
        assertContains(registrySource, "id = \"Zeta\"")
        assertContains(registrySource, "fileName = \"Zeta.mmd\"")
        assertContains(registrySource, "createTopology = { example.ZetaMachine.topology }")
        assertTrue(registrySource.indexOf("id = \"Alpha\"") < registrySource.indexOf("id = \"Zeta\""))
    }

    @Test
    fun `processor uses class name defaults for graph id and file name`() {
        val projectDir = createKspFixture()
        projectDir.writeMachineSource(
            """
            package example

            import afsm.core.AfsmGraph

            @AfsmGraph
            object DefaultMachine : TestMachine by testMachine("Default")
            """.trimIndent(),
        )

        gradle(projectDir)
            .withArguments("compileKotlin", "--stacktrace")
            .build()

        val registry = projectDir.resolve(
            "build/generated/ksp/main/kotlin/afsm/generated/AfsmGeneratedGraphRegistry.kt",
        )
        val registrySource = registry.readText()
        assertContains(registrySource, "id = \"DefaultMachine\"")
        assertContains(registrySource, "fileName = \"DefaultMachine.mmd\"")
    }

    @Test
    fun `processor rejects classes that are not afsm graph sources`() {
        val projectDir = createKspFixture()
        projectDir.writeMachineSource(
            """
            package example

            import afsm.core.AfsmGraph

            @AfsmGraph
            class NotMachine
            """.trimIndent(),
        )

        val result = gradle(projectDir)
            .withArguments("compileKotlin", "--stacktrace")
            .buildAndFail()

        assertContains(result.output, "@AfsmGraph class must implement AfsmReducer.")
    }

    @Test
    fun `processor rejects reducers that are not graph sources`() {
        val projectDir = createKspFixture()
        projectDir.writeMachineSource(
            """
            package example

            import afsm.core.AfsmGraph
            import afsm.core.AfsmReducer
            import afsm.core.AfsmTransition

            @AfsmGraph
            object ReducerOnlyMachine : AfsmReducer<String, TestEvent, TestCommand> {
                override fun transition(
                    state: String,
                    event: TestEvent,
                ): AfsmTransition<String, TestCommand> {
                    return AfsmTransition.handled(state)
                }
            }
            """.trimIndent(),
        )

        val result = gradle(projectDir)
            .withArguments("compileKotlin", "--stacktrace")
            .buildAndFail()

        assertContains(result.output, "@AfsmGraph class must implement AfsmGraphSource.")
    }

    @Test
    fun `processor rejects private graph classes`() {
        val projectDir = createKspFixture()
        projectDir.writeMachineSource(
            """
            package example

            import afsm.core.AfsmGraph

            @AfsmGraph
            private object PrivateMachine : TestMachine by testMachine("Private")
            """.trimIndent(),
        )

        val result = gradle(projectDir)
            .withArguments("compileKotlin", "--stacktrace")
            .buildAndFail()

        assertContains(result.output, "@AfsmGraph class must not be private.")
    }

    @Test
    fun `processor rejects graph annotations on interfaces`() {
        val projectDir = createKspFixture()
        projectDir.writeMachineSource(
            """
            package example

            import afsm.core.AfsmGraph

            @AfsmGraph
            interface InterfaceMachine
            """.trimIndent(),
        )

        val result = gradle(projectDir)
            .withArguments("compileKotlin", "--stacktrace")
            .buildAndFail()

        assertContains(result.output, "@AfsmGraph can only be used on classes or objects.")
    }

    @Test
    fun `processor rejects required constructor parameters`() {
        val projectDir = createKspFixture()
        projectDir.writeMachineSource(
            """
            package example

            import afsm.core.AfsmGraph

            class Dependency

            @AfsmGraph
            class RequiredDependencyMachine(
                private val dependency: Dependency,
            ) : TestMachine by testMachine("RequiredDependency")
            """.trimIndent(),
        )

        val result = gradle(projectDir)
            .withArguments("compileKotlin", "--stacktrace")
            .buildAndFail()

        assertContains(
            result.output,
            "@AfsmGraph class must be constructible with no required parameters or be an object.",
        )
    }

    @Test
    fun `processor rejects unsafe graph file names`() {
        val projectDir = createKspFixture()
        projectDir.writeMachineSource(
            """
            package example

            import afsm.core.AfsmGraph

            @AfsmGraph(fileName = "../Unsafe.mmd")
            object UnsafeMachine : TestMachine by testMachine("Unsafe")
            """.trimIndent(),
        )

        val result = gradle(projectDir)
            .withArguments("compileKotlin", "--stacktrace")
            .buildAndFail()

        assertContains(result.output, "Afsm graph fileName must be a safe relative .mmd path.")
    }

    @Test
    fun `processor rejects duplicate graph ids and file names`() {
        val projectDir = createKspFixture()
        projectDir.writeMachineSource(
            """
            package example

            import afsm.core.AfsmGraph

            @AfsmGraph(id = "Duplicate", fileName = "Duplicate.mmd")
            object FirstMachine : TestMachine by testMachine("First")

            @AfsmGraph(id = "Duplicate", fileName = "Duplicate.mmd")
            object SecondMachine : TestMachine by testMachine("Second")
            """.trimIndent(),
        )

        val result = gradle(projectDir)
            .withArguments("compileKotlin", "--stacktrace")
            .buildAndFail()

        assertContains(result.output, "Duplicate Afsm graph id: Duplicate.")
        assertContains(result.output, "Duplicate Afsm graph fileName: Duplicate.mmd.")
    }

    private fun gradle(projectDir: File): GradleRunner {
        return GradleRunner.create()
            .withProjectDir(projectDir)
            .forwardOutput()
    }

    private fun createKspFixture(): File {
        val projectDir = createTempDirectory(prefix = "afsm-graph-ksp-test-").toFile()
        projectDir.writeTextFile(
            "settings.gradle.kts",
            """
            pluginManagement {
                repositories {
                    gradlePluginPortal()
                    google()
                    mavenCentral()
                }
            }

            dependencyResolutionManagement {
                repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
                repositories {
                    google()
                    mavenCentral()
                }
            }

            rootProject.name = "afsm-graph-ksp-test"
            """.trimIndent(),
        )
        projectDir.writeTextFile(
            "build.gradle.kts",
            """
            plugins {
                kotlin("jvm") version "2.0.21"
                id("com.google.devtools.ksp") version "2.0.21-1.0.28"
            }

            kotlin {
                jvmToolchain(17)
            }

            dependencies {
                implementation(files("${moduleJar("afsm-core").escapedPath}"))
                ksp(files("${moduleJar("afsm-graph-ksp").escapedPath}"))
            }
            """.trimIndent(),
        )
        projectDir.writeTextFile(
            "src/main/kotlin/example/TestMachineSupport.kt",
            """
            package example

            import afsm.core.AfsmDefaultMachine
            import afsm.core.AfsmTopology
            import afsm.core.AfsmTopologyState
            import afsm.core.AfsmTransition

            sealed interface TestEvent
            sealed interface TestCommand

            typealias TestMachine = AfsmDefaultMachine<String, TestEvent, TestCommand>

            fun testMachine(id: String): TestMachine {
                return object : TestMachine {
                    override val initialState: String = id
                    override val topology: AfsmTopology = AfsmTopology(
                        states = listOf(AfsmTopologyState(id = id)),
                        transitions = emptyList(),
                        initialStateId = id,
                    )

                    override fun transition(
                        state: String,
                        event: TestEvent,
                    ): AfsmTransition<String, TestCommand> {
                        return AfsmTransition.handled(state)
                    }
                }
            }
            """.trimIndent(),
        )
        return projectDir
    }

    private fun File.writeMachineSource(source: String) {
        writeTextFile("src/main/kotlin/example/TestMachines.kt", source)
    }

    private fun File.writeTextFile(
        path: String,
        text: String,
    ) {
        resolve(path).also { file ->
            file.parentFile.mkdirs()
            file.writeText(text)
        }
    }

    private fun rootProjectFile(path: String): File {
        val cwd = File(System.getProperty("user.dir"))
        val root = if (cwd.resolve("settings.gradle.kts").isFile) {
            cwd
        } else {
            cwd.parentFile
        }
        return root.resolve(path)
    }

    private fun moduleJar(moduleName: String): File {
        val libsDir = rootProjectFile("$moduleName/build/libs")
        val jars = libsDir.listFiles { file ->
            file.isFile &&
                file.extension == "jar" &&
                !file.name.endsWith("-sources.jar") &&
                !file.name.endsWith("-javadoc.jar")
        }.orEmpty()

        return requireNotNull(jars.maxByOrNull { it.lastModified() }) {
            "Missing class jar for $moduleName in ${libsDir.absolutePath}."
        }
    }

    private val File.escapedPath: String
        get() = absolutePath
            .replace(File.separatorChar, '/')
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
}
