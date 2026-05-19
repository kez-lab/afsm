# Afsm Graph Generation

Afsm graph generation is a pre-release KSP plus test-task workflow.

The goal is:

```text
@AfsmGraph machine
-> KSP-generated AfsmGeneratedGraphRegistry
-> Gradle task writes .mmd files
```

A dedicated Gradle plugin is still future work. Until then, use this copy-paste
setup in the Android app module that owns the annotated machines.

## 1. Add KSP

```kotlin
plugins {
    id("com.google.devtools.ksp")
}

dependencies {
    ksp("io.github.afsm:afsm-graph-ksp:0.1.0-SNAPSHOT")
}
```

Repository-local development can use:

```kotlin
ksp(project(":afsm-graph-ksp"))
```

## 2. Annotate Machines

```kotlin
@AfsmGraph(
    id = "Checkout",
    fileName = "CheckoutStateMachine.mmd",
)
object CheckoutStateMachine : CheckoutMachine by checkoutMachine()
```

The annotated object must expose graph metadata. The normal
`AfsmMachine<State, Event, Command, Effect>` path already does this because
`AfsmMachine` is an `AfsmGraphSource`.

## 3. Add An Export Test

Place this in the app module test source set, for example:

```text
app/src/test/kotlin/.../AfsmMmdExportTest.kt
```

```kotlin
import afsm.core.AfsmMmdOptions
import afsm.core.AfsmMmdWriter
import afsm.generated.AfsmGeneratedGraphRegistry
import java.io.File
import kotlin.test.Test

class AfsmMmdExportTest {
    @Test
    fun `writes afsm graphs`() {
        val outputDir = File(
            System.getProperty("afsm.mmd.outputDir")
                ?: "build/generated/afsm/mmd",
        )

        AfsmMmdWriter.writeAll(
            registry = AfsmGeneratedGraphRegistry,
            outputDir = outputDir,
            options = AfsmMmdOptions.Flow,
        )
    }
}
```

`AfsmMmdOptions.Flow` hides ordinary internal self-loops such as text field
updates. Use `AfsmMmdOptions.Full` when you need every declared edge.

## 4. Add A Gradle Task

```kotlin
tasks.withType<Test>().configureEach {
    systemProperty(
        "afsm.mmd.outputDir",
        layout.buildDirectory.dir("generated/afsm/mmd").get().asFile.absolutePath,
    )
    outputs.dir(layout.buildDirectory.dir("generated/afsm/mmd"))
}

tasks.register("generateAfsmMmd") {
    group = "documentation"
    description = "Generates Afsm state machine .mmd graph files."
    dependsOn("testDebugUnitTest")
    outputs.dir(layout.buildDirectory.dir("generated/afsm/mmd"))
}
```

Run:

```bash
./gradlew :app:generateAfsmMmd
```

Expected output:

```text
app/build/generated/afsm/mmd/CheckoutStateMachine.mmd
```

## Troubleshooting

- `Unresolved reference: AfsmGeneratedGraphRegistry`: check that KSP is applied
  to the app module and that at least one machine is annotated with
  `@AfsmGraph`.
- No `.mmd` files: check that the export test is included in the test variant
  that `generateAfsmMmd` depends on.
- The task runs unit tests: this is expected in the current pre-release setup.
  A first-class Gradle plugin should replace this wiring before broad external
  adoption.
- Diagram labels appear but no command runs: labels such as `commandLabels` and
  `effectLabels` are metadata only. Runtime work still requires `command(...)`
  or `effect(...)` in the machine DSL.
