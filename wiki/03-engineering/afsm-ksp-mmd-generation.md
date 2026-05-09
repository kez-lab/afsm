---
title: Afsm KSP MMD Generation
updated: 2026-05-09
---

# Afsm KSP MMD Generation

This page designs KSP-based automatic `.mmd` generation for Afsm state machines.

Goal:

- A developer defines an Afsm machine.
- The machine is registered once with a small annotation.
- The build generates one `.mmd` file per registered machine.
- No prose documentation is generated beside the graph.
- The generated `.mmd` comes from the real executable `AfsmMachine.topology`, not from a separate graph-only model.

References:

- [Kotlin Symbol Processing API overview](https://kotlinlang.org/docs/ksp-overview.html)
- [KSP quickstart](https://kotlinlang.org/docs/ksp-quickstart.html)

## Key Constraint

KSP can inspect Kotlin symbols and generate code, but it should not be treated as a runtime executor for app code.

Therefore Afsm should not ask the KSP processor to execute:

```kotlin
ProductEditorStateMachine().topology.toMmd()
```

Instead:

```text
KSP discovers annotated machine providers
-> KSP generates registry code
-> Gradle/test task runs compiled registry code
-> registry creates machines
-> machine.topology.toMmd()
-> write .mmd files
```

This keeps topology synchronized with runtime behavior while avoiding fragile source-code AST inference.

## User-Facing API

The recommended MVP API is explicit annotation registration.

```kotlin
@AfsmGraph(
    id = "ProductEditor",
    fileName = "ProductEditorStateMachine.mmd",
)
internal fun productEditorMachine(): AfsmMachine<
    ProductEditorPhase,
    ProductEditorContext,
    ProductEditorEvent,
    ProductEditorCommand,
    ProductEditorEffect,
> {
    return afsmMachine {
        // real executable machine definition
    }
}
```

Supported annotation targets:

```kotlin
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
)
@Retention(AnnotationRetention.BINARY)
public annotation class AfsmGraph(
    val id: String = "",
    val fileName: String = "",
)
```

Rules:

- Annotated symbols must return `AfsmMachine<*, *, *, *, *>`.
- Annotated functions must have no parameters.
- Annotated properties must have no receiver and must be readable from generated code.
- Private top-level providers are rejected because generated code cannot call them.
- If `id` is empty, use the provider simple name.
- If `fileName` is empty, use `<id>.mmd`.

Why annotation instead of scanning every `AfsmMachine`:

- It avoids accidentally publishing internal/test machines.
- It gives stable file names.
- It makes duplicate graph ids a compile-time error.
- It keeps the build predictable for Android teams.

## Generated Code

For each module using the processor, KSP generates a registry.

Example:

```kotlin
package afsm.generated

import afsm.core.AfsmGraphEntry
import afsm.core.AfsmGraphRegistry
import afsm.sample.shop.feature.editor.productEditorMachine

public object AfsmGeneratedGraphRegistry : AfsmGraphRegistry {
    override val entries: List<AfsmGraphEntry> = listOf(
        AfsmGraphEntry(
            id = "ProductEditor",
            fileName = "ProductEditorStateMachine.mmd",
            createMachine = { productEditorMachine() },
        ),
    )
}
```

Core support types:

```kotlin
public data class AfsmGraphEntry(
    val id: String,
    val fileName: String,
    val createMachine: () -> AfsmMachine<*, *, *, *, *>,
)

public interface AfsmGraphRegistry {
    val entries: List<AfsmGraphEntry>
}
```

The registry is ordinary Kotlin code. It does not contain graph text. It only knows how to instantiate registered machines.

## MMD Writer

The writer should be small and Kotlin/JVM-only:

```kotlin
public object AfsmMmdWriter {
    public fun writeAll(
        registry: AfsmGraphRegistry,
        outputDir: File,
    ) {
        registry.entries.forEach { entry ->
            val machine = entry.createMachine()
            val file = outputDir.resolve(entry.fileName)
            file.parentFile.mkdirs()
            file.writeText(machine.topology.toMmd() + "\n")
        }
    }
}
```

This belongs in a small module such as `afsm-graph-runtime` or `afsm-core` depending on how much public surface we want.

Initial recommendation:

- Put `AfsmGraph`, `AfsmGraphEntry`, `AfsmGraphRegistry`, and `AfsmMmdWriter` in `afsm-core` while the project is still small.
- Move graph tooling to `afsm-graph` later if the API grows.

## Gradle Integration

There are two viable paths.

### MVP Path: Generated Unit Test

For Android modules, unit tests already have a JVM runtime that can instantiate app classes.

KSP can generate a test source:

```kotlin
class AfsmGeneratedMmdExportTest {
    @Test
    fun writeAfsmMmd() {
        val outputDir = File(
            System.getProperty("afsm.mmd.outputDir")
                ?: "build/generated/afsm/mmd",
        )

        AfsmMmdWriter.writeAll(
            registry = AfsmGeneratedGraphRegistry,
            outputDir = outputDir,
        )
    }
}
```

Then `generateAfsmMmd` depends on the relevant unit-test task:

```kotlin
tasks.register("generateAfsmMmd") {
    group = "documentation"
    description = "Generates Afsm state machine .mmd graph files."
    dependsOn("testDebugUnitTest")
}
```

Pros:

- Works naturally for Android app/library modules.
- Avoids custom classpath engineering for Android variants.
- Matches the current sample-shop proof.

Cons:

- Graph generation rides on a test task.
- Teams may want graph generation without running the full unit test suite.

### Later Path: Afsm Gradle Plugin

Add an `afsm-graph-gradle-plugin`:

```kotlin
plugins {
    id("io.github.afsm.graph")
}

afsmGraph {
    outputDir.set(layout.buildDirectory.dir("generated/afsm/mmd"))
}
```

The plugin should:

- apply/configure KSP dependencies,
- generate registries per source set or Android variant,
- register `generateAfsmMmd`,
- wire the correct compiled classpath,
- support multi-module aggregation later.

This is better public UX, but it is more work than the first proof needs.

## Module Layout

Recommended implementation order:

```text
afsm-core
  AfsmGraph annotation
  AfsmGraphEntry
  AfsmGraphRegistry
  AfsmMmdWriter

afsm-graph-ksp
  AfsmGraphProcessorProvider
  AfsmGraphProcessor
  registry code generation
  generated mmd export test

sample-shop
  applies ksp
  annotates productEditorMachine()
  generateAfsmMmd writes all registered graphs

future:
  afsm-graph-gradle-plugin
```

## Processor Behavior

Processing steps:

1. Find symbols annotated with `@AfsmGraph`.
2. Validate each symbol:
   - function or property only,
   - no-arg function if function,
   - return type is assignable to `AfsmMachine<*, *, *, *, *>`,
   - callable from generated code,
   - unique graph id,
   - `fileName` ends with `.mmd`.
3. Generate one registry per module.
4. Optionally generate one mmd export test per module.
5. Emit KSP errors for invalid declarations.

Important diagnostics:

```text
@AfsmGraph provider must return AfsmMachine<*, *, *, *, *>.
@AfsmGraph provider must not be private.
@AfsmGraph function must not declare parameters.
Duplicate Afsm graph id: ProductEditor.
Afsm graph fileName must end with .mmd.
```

## Multi-Module Policy

MVP:

- Generate `.mmd` files per module.
- Each module writes only its own annotated machines.

Later:

- A root aggregation task can collect outputs from subprojects.
- Avoid cross-module KSP aggregation at first; KSP processors run inside a compilation unit and should not become a project-wide indexer.

Output convention:

```text
<module>/build/generated/afsm/mmd/<fileName>.mmd
```

Future root aggregation can copy to:

```text
build/generated/afsm/mmd/<module>/<fileName>.mmd
```

## Why Not Static DSL Parsing

Do not make KSP parse `afsmMachine { state { on { ... } } }` bodies to extract transitions.

Reasons:

- Kotlin function-body analysis is fragile for this use case.
- Helper functions and variables would hide transition targets.
- It duplicates the executable DSL interpreter.
- It creates risk that the diagram differs from runtime behavior.

The executable machine already has topology. Generate code that runs it.

## Implementation Spike Acceptance Criteria

The next spike should prove:

- `@AfsmGraph` on `productEditorMachine()` compiles.
- KSP generates `AfsmGeneratedGraphRegistry`.
- `generateAfsmMmd` writes `ProductEditorStateMachine.mmd`.
- Adding a second annotated machine writes a second `.mmd`.
- Invalid providers fail compilation with useful messages.
- No explanatory markdown is generated as graph output.

Initial test targets:

```bash
./gradlew :afsm-core:test
./gradlew :afsm-graph-ksp:test
./gradlew :sample-shop:generateAfsmMmd
```

## Current Verdict

Use KSP for discovery and registry generation, not for graph interpretation.

The machine definition remains the single source of truth. The generated `.mmd` files are build artifacts derived from `AfsmMachine.topology.toMmd()`.
