# Afsm Graph Generation

Afsm graph generation is a pre-release KSP plus Gradle plugin workflow.

The goal is:

```text
@AfsmGraph machine
-> KSP-generated AfsmGeneratedGraphRegistry
-> io.github.afsm.graph generated export test
-> generateAfsmMmd writes .mmd files
```

The plugin does not parse Kotlin function bodies and does not need sample
events. It runs the compiled graph registry and writes the topology declared by
the executable `afsmMachine { ... }` DSL.

## 1. Configure Plugins

Root or app build:

```kotlin
pluginManagement {
    repositories {
        mavenLocal() // only needed for Maven Local snapshots
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
```

```kotlin
plugins {
    id("com.google.devtools.ksp") version "2.0.21-1.0.28" apply false
    id("io.github.afsm.graph") version "0.1.0-SNAPSHOT" apply false
}
```

App module:

```kotlin
plugins {
    id("com.android.application")
    id("com.google.devtools.ksp")
    id("io.github.afsm.graph")
    kotlin("android")
}
```

The graph plugin adds the `afsm-graph-ksp` processor to the app module by
default and registers:

```text
generateAfsmMmd
generateAfsmMmdExportTest
```

Repository-local development can keep the processor dependency as a project
dependency:

```kotlin
afsmGraph {
    addProcessorDependency.set(false)
}

dependencies {
    ksp(project(":afsm-graph-ksp"))
}
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

## 3. Generate MMD

Run:

```bash
./gradlew :app:generateAfsmMmd
```

Expected output:

```text
app/build/generated/afsm/mmd/CheckoutStateMachine.mmd
```

For `sample-shop`:

```bash
./gradlew :sample-shop:generateAfsmMmd
```

```text
sample-shop/build/generated/afsm/mmd/AuthStateMachine.mmd
sample-shop/build/generated/afsm/mmd/CheckoutStateMachine.mmd
sample-shop/build/generated/afsm/mmd/ProductEditorStateMachine.mmd
```

## 4. Plugin Options

```kotlin
afsmGraph {
    variant.set("debug")
    outputDir.set(layout.buildDirectory.dir("generated/afsm/mmd"))

    // Defaults to io.github.afsm:afsm-graph-ksp:0.1.0-SNAPSHOT.
    processorDependency.set("io.github.afsm:afsm-graph-ksp:0.1.0-SNAPSHOT")
    addProcessorDependency.set(true)

    // The generated export test is JUnit4-compatible and does not change
    // the app module's existing unit-test runner.
    junitDependency.set("junit:junit:4.13.2")
    addJunitDependency.set(true)
}
```

The plugin currently targets one Android unit-test variant, `debug` by default.
That keeps the MVP predictable and avoids premature multi-variant aggregation.

## What The Plugin Generates

The plugin generates a test source under:

```text
build/generated/afsm/graph-test/kotlin/afsm/generated/AfsmGeneratedMmdExportTest.kt
```

The generated test:

- loads the KSP-generated `AfsmGeneratedGraphRegistry` at runtime,
- calls `AfsmMmdWriter.writeAll(...)`,
- asserts that the registry is not empty,
- asserts that every registered `.mmd` file exists.

This replaces app-maintained copy-paste export tests.

`generateAfsmMmd` is a dedicated `Test` task that runs only the generated graph
export test class. It reuses the selected Android unit-test variant classpath,
but it does not run the app module's whole unit-test suite and does not force
JUnit Platform on existing tests. The ordinary Android unit-test task excludes
the generated graph export test, so normal `testDebugUnitTest` runs are not
turned into graph-generation runs.

## Troubleshooting

- `No Afsm graph registry was generated`: check that at least one machine is
  annotated with `@AfsmGraph`. Normal unit tests can still run before graphs
  exist; this failure is specific to `generateAfsmMmd`.
- `Afsm graph generation requires the com.google.devtools.ksp plugin`: apply
  `com.google.devtools.ksp`, or set `addProcessorDependency=false` and add the
  processor manually.
- No `.mmd` files: run the module's `generateAfsmMmd` task and check that the
  annotated machine is constructible as an `object` or no-required-arg class.
- Diagram labels appear but no command runs: labels such as `commandLabels` and
  `effectLabels` are metadata only. Runtime work still requires `command(...)`
  or `effect(...)` in the machine DSL.
