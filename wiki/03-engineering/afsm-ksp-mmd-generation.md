---
title: Afsm KSP MMD Generation
updated: 2026-05-19
---

# Afsm KSP MMD Generation

This page designs KSP + Gradle-plugin based automatic `.mmd` generation for
Afsm state machines.

Correction: graph registration should happen on the `StateMachine` class, not on a separate machine-provider function.

Goal:

- A developer defines a `StateMachine` class.
- The developer adds one annotation to that class.
- The build discovers all annotated state machines.
- The build writes one `.mmd` file per discovered machine.
- No prose documentation is generated beside the graph.
- The generated graph comes from the real executable topology, not from source-code parsing.

References:

- [Kotlin Symbol Processing API overview](https://kotlinlang.org/docs/ksp-overview.html)
- [KSP quickstart](https://kotlinlang.org/docs/ksp-quickstart.html)

## Recommended API

Use a class annotation plus a small topology contract.

```kotlin
@AfsmGraph(
    id = "ProductEditor",
    fileName = "ProductEditorStateMachine.mmd",
)
object ProductEditorStateMachine : ProductEditorMachine by productEditorMachine()
```

Core types:

```kotlin
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
public annotation class AfsmGraph(
    val id: String = "",
    val fileName: String = "",
)

public interface AfsmGraphSource {
    public val topology: AfsmTopology
}

public interface AfsmMachine<S : Any, E : Any, C : Any, F : Any> :
    AfsmReducer<S, E, C, F>,
    AfsmGraphSource {
    public val initialState: S
}
```

Why keep graphability separate from plain reducers:

- `AfsmReducer<S, E, C, F>` should stay small and should not force every simple reducer to expose graph metadata.
- Only graphable state machines opt in.
- KSP can validate a clear type contract.
- The writer can work with `AfsmTopology`, not with the generic machine internals.
- The processor validates the underlying `AfsmReducer + AfsmGraphSource` supertypes rather than only the nominal `AfsmMachine` type so typealias-based declarations are supported.

## User Experience

The desired sample-shop usage should become:

```kotlin
@AfsmGraph
object ProductEditorStateMachine : ProductEditorMachine by productEditorMachine()
```

Then this should generate:

```text
sample-shop/build/generated/afsm/mmd/ProductEditorStateMachine.mmd
```

With a second class:

```kotlin
@AfsmGraph(fileName = "CheckoutStateMachine.mmd")
object CheckoutStateMachine : CheckoutMachine by checkoutMachine()
```

The same task should also generate:

```text
sample-shop/build/generated/afsm/mmd/CheckoutStateMachine.mmd
```

No Gradle task should need to know `ProductEditorStateMachine` by name.

## KSP Role

KSP should discover classes and generate registry code. It should not interpret the state machine.

```text
KSP discovers @AfsmGraph classes
-> KSP validates they implement AfsmGraphSource
-> KSP generates AfsmGeneratedGraphRegistry
-> Gradle/test runner executes compiled registry code
-> registry instantiates each StateMachine
-> topology.toMmd()
-> write .mmd files
```

Do not make KSP parse `afsmMachine { ... }` bodies.

Reasons:

- Function-body parsing is fragile.
- Helper functions would hide transitions.
- It duplicates the executable DSL interpreter.
- It can drift from runtime behavior.

The runtime machine already owns topology. KSP should only find graph sources.

## Constructor Policy

KSP can generate code that instantiates annotated state-machine classes only if they are constructible.

MVP rule:

- Annotated classes must be Kotlin `object`s, or
- annotated classes must have a constructor with no required parameters.

Valid:

```kotlin
@AfsmGraph
class ProductEditorStateMachine(
    private val machine: ProductEditorMachine = productEditorMachine(),
) : AfsmMachine<ProductEditorState, ProductEditorEvent, ProductEditorCommand, ProductEditorEffect> {
    override val initialState = machine.initialState
    override val topology = machine.topology
    override fun transition(state: ProductEditorState, event: ProductEditorEvent) =
        machine.transition(state, event)
}
```

Valid:

```kotlin
@AfsmGraph
object CheckoutStateMachine : AfsmMachine<...>
```

Invalid:

```kotlin
@AfsmGraph
class CheckoutStateMachine(
    private val repository: ProductRepository,
) : AfsmGraphSource
```

Reason: graph generation should not need repositories, Android framework objects, DI containers, or network setup. Business work belongs in commands/actions handled by the host, not in the state machine constructor.

## Generated Code

For each module, KSP generates a registry.

Example:

```kotlin
package afsm.generated

import afsm.core.AfsmGraphEntry
import afsm.core.AfsmGraphRegistry
internal object AfsmGeneratedGraphRegistry : AfsmGraphRegistry {
    override val entries: List<AfsmGraphEntry> = listOf(
        AfsmGraphEntry(
            id = "ProductEditor",
            fileName = "ProductEditorStateMachine.mmd",
            createTopology = { afsm.sample.shop.feature.editor.ProductEditorStateMachine.topology },
        ),
    )
}
```

Core support:

```kotlin
public data class AfsmGraphEntry(
    val id: String,
    val fileName: String,
    val createTopology: () -> AfsmTopology,
)

public interface AfsmGraphRegistry {
    public val entries: List<AfsmGraphEntry>
}
```

The registry contains no graph text. It only knows how to instantiate graph sources and retrieve topology.

## MMD Writer

The writer should be small and Kotlin/JVM-only.

```kotlin
public object AfsmMmdWriter {
    public fun writeAll(
        registry: AfsmGraphRegistry,
        outputDir: File,
        options: AfsmMmdOptions = AfsmMmdOptions.Flow,
    ) {
        registry.entries.forEach { entry ->
            val file = outputDir.resolve(entry.fileName)
            file.parentFile.mkdirs()
            file.writeText(entry.createTopology().toMmd(options) + "\n")
        }
    }
}
```

`AfsmMmdOptions.Flow` is the default public output because it hides ordinary
internal self-loops such as text changes. `AfsmMmdOptions.Full` keeps the
complete topology for debugging.

Output convention:

```text
<module>/build/generated/afsm/mmd/<fileName>
```

## Validation Rules

The KSP processor should fail compilation when:

- `@AfsmGraph` is used on a non-class symbol.
- The class does not implement `AfsmGraphSource`.
- The class is private.
- The class has required constructor parameters.
- `fileName` does not end with `.mmd`.
- Two graphs in the same module have the same `id`.
- Two graphs in the same module write the same `fileName`.

Example diagnostics:

```text
@AfsmGraph can only be used on StateMachine classes.
@AfsmGraph class must implement AfsmGraphSource.
@AfsmGraph class must be constructible with no required parameters or be an object.
Duplicate Afsm graph id: ProductEditor.
Afsm graph fileName must end with .mmd.
```

## Gradle Integration

### Current: Gradle Plugin

The first `afsm-graph-gradle-plugin` slice now exists as an included build for
repo-local use and as a Maven Local Gradle plugin for consumer smoke tests.

Target usage:

```kotlin
plugins {
    id("com.google.devtools.ksp")
    id("io.github.afsm.graph")
}

afsmGraph {
    outputDir.set(layout.buildDirectory.dir("generated/afsm/mmd"))
}
```

The plugin:

- add the KSP processor by default when the `ksp` configuration exists,
- generate an Android unit-test source that loads `AfsmGeneratedGraphRegistry`
  at runtime,
- register `generateAfsmMmd` as the user-facing task,
- wire generated test sources into the selected Android unit-test variant,
- exclude the generated graph export test from the ordinary Android unit-test
  task,
- support Android app/library modules,
- publish to Maven Local for external consumer verification.

### Superseded Spike: Registry + Existing Generate Task

The earlier proof removed ProductEditor-specific knowledge from a handwritten
task:

```text
generateAfsmMmd
-> ProductEditorStateMachine.topology.toMmd()
```

That spike has been replaced by the plugin-driven flow:

```text
generateAfsmMmd
-> AfsmGeneratedGraphRegistry.entries
-> write every entry as .mmd
```

Deferred plugin work:

- multi-variant output,
- multi-module aggregation,
- graph API/module split decisions.

## Module Layout

Recommended implementation order:

```text
afsm-core
  AfsmGraph
  AfsmGraphSource
  AfsmGraphEntry
  AfsmGraphRegistry
  AfsmMmdWriter

afsm-graph-ksp
  AfsmGraphProcessorProvider
  AfsmGraphProcessor
  generated registry

afsm-graph-gradle-plugin
  io.github.afsm.graph
  generated AfsmGeneratedMmdExportTest
  generateAfsmMmd task

sample-shop
  @AfsmGraph on ProductEditorStateMachine
  @AfsmGraph on another sample state machine if graphable
  io.github.afsm.graph plugin
  generateAfsmMmd writes all registry entries
```

## Multi-Module Policy

MVP:

- Generate `.mmd` per module.
- Each module writes only its own annotated state machines.

Future:

- Root aggregation can copy module outputs into:

```text
build/generated/afsm/mmd/<module>/<fileName>
```

Avoid cross-module KSP aggregation at first. KSP should stay module-local.

## Acceptance Criteria For The Next Spike

Implementation status:

- Done: `@AfsmGraph` on `AuthStateMachine` and `ProductEditorStateMachine` compiles.
- Done: Auth, Checkout, and ProductEditor implement `AfsmGraphSource`.
- Done: KSP generates `AfsmGeneratedGraphRegistry`.
- Done: `generateAfsmMmd` no longer references ProductEditor directly.
- Done: `generateAfsmMmd` writes `AuthStateMachine.mmd`, `CheckoutStateMachine.mmd`, and `ProductEditorStateMachine.mmd`.
- Done: adding additional real annotated `StateMachine` objects writes additional `.mmd` files.
- Done: invalid annotated classes fail compilation with useful messages in processor functional tests.
- Done: `io.github.afsm.graph` removes app-maintained export test boilerplate.
- Done: `consumer-smoke` applies the published graph Gradle plugin and generates `.mmd` output from Maven Local artifacts.
- Done: no explanatory markdown is generated as graph output.
- Done: graph `fileName` values must be safe relative `.mmd` paths; absolute paths, traversal segments, empty segments, and non-`.mmd` files are rejected by both the KSP processor and runtime writer.
- Done: KSP processor functional tests cover valid object/default-arg class registry generation, non-machine annotations, required constructor parameters, unsafe file names, and duplicate ids/file names.
- Done: Gradle plugin functional tests cover Android app/library modules, KSP-missing failure messaging, normal unit-test separation from graph export, and no-registry `generateAfsmMmd` failure messaging.

Initial test targets:

```bash
./gradlew :afsm-core:test
./gradlew :afsm-graph-ksp:test
./gradlew :sample-shop:generateAfsmMmd
```

## Current Verdict

Yes, annotating the `StateMachine` class is possible and preferable.

The correct design is:

```text
@AfsmGraph StateMachine class
+ AfsmGraphSource topology contract
+ KSP-generated registry
+ generated unit-test writer from io.github.afsm.graph
+ runtime mmd writer
```

KSP discovers graphable state-machine classes. The real machine topology still generates the `.mmd`.
