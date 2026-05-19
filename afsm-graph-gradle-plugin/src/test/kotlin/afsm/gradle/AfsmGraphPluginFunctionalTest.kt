package afsm.gradle

import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.gradle.testkit.runner.GradleRunner

class AfsmGraphPluginFunctionalTest {
    @Test
    fun `generateAfsmMmd runs only the generated graph export test`() {
        val projectDir = createAndroidProject(
            applyKsp = true,
            configureAfsmGraph = "addProcessorDependency.set(false)",
        )
        projectDir.writeTextFile(
            "app/src/test/kotlin/example/FailingAppTest.kt",
            """
            package example

            import org.junit.Assert.fail
            import org.junit.Test

            class FailingAppTest {
                @Test
                fun `would fail if generateAfsmMmd ran every unit test`() {
                    fail("generateAfsmMmd must not run unrelated app unit tests")
                }
            }
            """.trimIndent(),
        )
        projectDir.writeAfsmCoreTestStubs()

        val result = gradle(projectDir)
            .withArguments(":app:generateAfsmMmd", "--stacktrace")
            .build()

        val graphFile = projectDir.resolve("app/build/generated/afsm/mmd/FixtureGraph.mmd")
        assertTrue(graphFile.isFile)
        assertTrue(graphFile.readText().startsWith("stateDiagram-v2"))
        assertFalse(result.output.contains("generateAfsmMmd must not run unrelated app unit tests"))
    }

    @Test
    fun `plugin fails with a clear message when ksp plugin is missing`() {
        val projectDir = createAndroidProject(applyKsp = false)
        projectDir.writeAfsmCoreTestStubs()

        val result = gradle(projectDir)
            .withArguments(":app:tasks", "--stacktrace")
            .buildAndFail()

        assertContains(result.output, "Afsm graph generation requires the com.google.devtools.ksp plugin.")
    }

    @Test
    fun `generateAfsmMmd supports Android library modules`() {
        val projectDir = createAndroidProject(
            androidPlugin = "com.android.library",
            applyKsp = true,
            configureAfsmGraph = "addProcessorDependency.set(false)",
        )
        projectDir.writeAfsmCoreTestStubs()

        gradle(projectDir)
            .withArguments(":app:generateAfsmMmd", "--stacktrace")
            .build()

        val graphFile = projectDir.resolve("app/build/generated/afsm/mmd/FixtureGraph.mmd")
        assertTrue(graphFile.isFile)
    }

    @Test
    fun `normal unit tests do not run generated graph export test`() {
        val projectDir = createAndroidProject(
            applyKsp = true,
            configureAfsmGraph = "addProcessorDependency.set(false)",
        )
        projectDir.writeAfsmCoreTestStubs(includeRegistry = false)
        projectDir.writeTextFile(
            "app/src/test/kotlin/example/OrdinaryTest.kt",
            """
            package example

            import org.junit.Assert.assertTrue
            import org.junit.Test

            class OrdinaryTest {
                @Test
                fun passes() {
                    assertTrue(true)
                }
            }
            """.trimIndent(),
        )

        gradle(projectDir)
            .withArguments(":app:testDebugUnitTest", "--stacktrace")
            .build()

        val graphFile = projectDir.resolve("app/build/generated/afsm/mmd/FixtureGraph.mmd")
        assertFalse(graphFile.exists())
    }

    @Test
    fun `generateAfsmMmd fails clearly when no graph registry was generated`() {
        val projectDir = createAndroidProject(
            applyKsp = true,
            configureAfsmGraph = "addProcessorDependency.set(false)",
        )
        projectDir.writeAfsmCoreTestStubs(includeRegistry = false)

        val result = gradle(projectDir)
            .withArguments(":app:generateAfsmMmd", "--stacktrace")
            .buildAndFail()

        assertContains(result.output, "No Afsm graph registry was generated.")
    }

    private fun gradle(projectDir: File): GradleRunner {
        return GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .forwardOutput()
    }

    private fun createAndroidProject(
        androidPlugin: String = "com.android.application",
        applyKsp: Boolean,
        configureAfsmGraph: String = "",
    ): File {
        val projectDir = createTempDirectory(prefix = "afsm-graph-plugin-test-").toFile()
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

            rootProject.name = "afsm-graph-plugin-test"
            include(":app")
            """.trimIndent(),
        )
        projectDir.writeTextFile(
            "local.properties",
            "sdk.dir=${androidSdkDir().invariantSeparatorsPath}\n",
        )
        projectDir.writeTextFile(
            "build.gradle.kts",
            """
            plugins {
                id("com.android.application") version "8.10.1" apply false
                id("com.android.library") version "8.10.1" apply false
                id("com.google.devtools.ksp") version "2.0.21-1.0.28" apply false
                kotlin("android") version "2.0.21" apply false
            }
            """.trimIndent(),
        )
        projectDir.writeTextFile(
            "app/build.gradle.kts",
            """
            plugins {
                id("$androidPlugin")
                ${if (applyKsp) "id(\"com.google.devtools.ksp\")" else ""}
                id("io.github.afsm.graph")
                kotlin("android")
            }

            android {
                namespace = "example.afsm.graph"
                compileSdk = 36

                defaultConfig {
                    minSdk = 23
                    ${if (androidPlugin == "com.android.application") "applicationId = \"example.afsm.graph\"" else ""}
                }

                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }

                kotlinOptions {
                    jvmTarget = "17"
                }
            }

            afsmGraph {
                $configureAfsmGraph
            }
            """.trimIndent(),
        )
        projectDir.writeTextFile(
            "app/src/main/AndroidManifest.xml",
            "<manifest />",
        )
        return projectDir
    }

    private fun File.writeAfsmCoreTestStubs(
        includeRegistry: Boolean = true,
    ) {
        writeTextFile(
            "app/src/test/kotlin/afsm/core/AfsmCoreStubs.kt",
            """
            package afsm.core

            import java.io.File

            sealed interface AfsmMmdOptions {
                data object Flow : AfsmMmdOptions
            }

            data class AfsmTopology(
                val graph: String,
            )

            data class AfsmGraphEntry(
                val id: String,
                val fileName: String,
                val createTopology: () -> AfsmTopology,
            )

            interface AfsmGraphRegistry {
                val entries: List<AfsmGraphEntry>
            }

            object AfsmMmdWriter {
                fun writeAll(
                    registry: AfsmGraphRegistry,
                    outputDir: File,
                    options: AfsmMmdOptions,
                ) {
                    check(options == AfsmMmdOptions.Flow)
                    registry.entries.forEach { entry ->
                        val file = outputDir.resolve(entry.fileName)
                        file.parentFile.mkdirs()
                        file.writeText(entry.createTopology().graph)
                    }
                }
            }
            """.trimIndent(),
        )
        if (!includeRegistry) {
            return
        }

        writeTextFile(
            "app/src/test/kotlin/afsm/generated/AfsmGeneratedGraphRegistry.kt",
            """
            package afsm.generated

            import afsm.core.AfsmGraphEntry
            import afsm.core.AfsmGraphRegistry
            import afsm.core.AfsmTopology

            internal object AfsmGeneratedGraphRegistry : AfsmGraphRegistry {
                override val entries: List<AfsmGraphEntry> = listOf(
                    AfsmGraphEntry(
                        id = "FixtureGraph",
                        fileName = "FixtureGraph.mmd",
                        createTopology = {
                            AfsmTopology(
                                graph = "stateDiagram-v2\\n    [*] --> Ready\\n",
                            )
                        },
                    )
                )
            }
            """.trimIndent(),
        )
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

    private fun androidSdkDir(): File {
        val fromEnvironment = System.getenv("ANDROID_HOME")
            ?: System.getenv("ANDROID_SDK_ROOT")
        if (!fromEnvironment.isNullOrBlank()) {
            return File(fromEnvironment)
        }

        val rootLocalProperties = File("..").resolve("local.properties")
        val sdkPath = rootLocalProperties
            .takeIf { it.isFile }
            ?.readLines()
            ?.firstOrNull { it.startsWith("sdk.dir=") }
            ?.substringAfter("sdk.dir=")

        require(!sdkPath.isNullOrBlank()) {
            "Android SDK path is required for Afsm graph plugin functional tests."
        }
        return File(sdkPath)
    }

    private val File.invariantSeparatorsPath: String
        get() {
            return absolutePath.replace(File.separatorChar, '/')
        }
}
