---
title: Afsm First-Use API Experiment
updated: 2026-07-10
status: candidate-e-implemented-review-pending
---

# Afsm First-Use API Experiment

This experiment tests whether Afsm can make a feature machine easier to declare
without hiding the domain flow or weakening Kotlin type safety.

It is not constrained by the current unpublished API. No candidate is accepted
until realistic Draft, Auth, and Checkout prototypes compile and are compared.

## Evidence-Backed Problem

The current first-use path requires framework type plumbing before the machine
body:

```kotlin
typealias DraftState = AfsmState<DraftPhase, DraftData>

typealias DraftMachine =
    AfsmMachine<DraftState, DraftEvent, DraftCommand, AfsmNoEffect>

object DraftStateMachine : DraftMachine by draftMachine()

private fun draftMachine(): DraftMachine = afsmMachine {
    // product flow starts here
}
```

The 2026-05-19 usability review already identified heavy generics in the minimal
Draft path. Later improvements reduced graph metadata, empty terminal blocks,
terminology confusion, and unsafe receiver scopes, but the feature-boundary
alias/delegation/factory pattern remains in Draft, Auth, Checkout, and
ProductEditor.

The problem is not the raw line count. The hypothesis is that developers must
understand Afsm's generic plumbing before they can focus on their business
flow.

## Invariants During The Experiment

Every candidate must keep these behaviors visible and typed:

- state/phase and durable data,
- event root and typed event branches,
- host-executed work versus UI one-shot output,
- invalid and expected no-op behavior,
- deterministic transition tests,
- ViewModel hosting,
- graph topology and discovery,
- dynamic initial state.

The terms and exact type model may change. The behavior must not become hidden
behind reflection, string keys, unchecked casts at feature call sites, or code
generation that developers must inspect to understand the flow.

## Candidate A: Current Baseline

Keep the current state alias, four-channel machine alias, delegated singleton,
and factory function.

Advantages:

- current graph annotation and KSP discovery work,
- explicit state/event/command/effect roots,
- machine body has a strongly expected receiver type.

Costs:

- two aliases plus delegation plus factory before the flow,
- the machine's public shape is repeated across declaration and hosting docs,
- first-use readers see framework generics before transitions.

## Candidate B: Direct Machine Value

Let initial phase/data values infer their types and declare event, command, and
effect roots once:

```kotlin
val DraftStateMachine = afsmMachine<
    DraftEvent,
    DraftCommand,
    AfsmNoEffect,
>(
    initialPhase = DraftPhase.Editing,
    initialData = DraftData(),
) {
    // product flow
}
```

Expected benefits:

- removes the machine alias, delegated object, and factory function,
- reduces the main declaration from five generic dimensions to three,
- keeps event/command/effect boundaries visible.

Questions:

- can Kotlin inference preserve the sealed `Phase` root rather than the
  singleton initial-phase type?
- should `DraftState` remain an optional Android-facing alias?
- can KSP discover an annotated top-level property safely and generate a stable
  registry reference?
- does a `val` machine have acceptable identity, visibility, and test ergonomics?

Prototype finding: the shown syntax is not legal Kotlin. A function with five
type parameters cannot be called by explicitly supplying only the first three;
the compiler does not infer the remaining `Phase` and `Data` parameters. A
staged `afsmTypes<Event, Command, Effect>().machine(...)` fallback compiles, but
adds a new type-set concept and an extra call stage.

## Candidate C: Named Type Channels

Infer phase/data from initial values and make the other roots named arguments
instead of a generic tuple:

```kotlin
val DraftStateMachine = afsmMachine(
    initialPhase = DraftPhase.Editing,
    initialData = DraftData(),
    events = afsmEvents<DraftEvent>(),
    commands = afsmCommands<DraftCommand>(),
    effects = noAfsmEffects(),
) {
    // product flow
}
```

Expected benefits:

- the declaration names the role of every type channel,
- no five-type generic list appears at the entry point,
- no-effect/no-command choices can read as product decisions.

Risks:

- token/helper objects may replace generic noise with framework ceremony,
- staged builders may produce worse compiler errors,
- tokens must not introduce reflection or runtime type registries,
- graph property discovery remains necessary unless graph identity moves into
  the machine declaration.

## Candidate D: Feature Declaration Object

Evaluate a more radical declaration that owns machine identity and graph
metadata without delegation boilerplate:

```kotlin
object DraftFeature : AfsmFeature(
    initialPhase = DraftPhase.Editing,
    initialData = DraftData(),
    events = afsmEvents<DraftEvent>(),
    commands = afsmCommands<DraftCommand>(),
    effects = noAfsmEffects(),
) {
    val machine = define {
        // product flow
    }
}
```

Expected benefits:

- one discoverable feature declaration can own machine, initial state, topology,
  and graph identity,
- may remove KSP's current class/object delegation requirement,
- gives docs and tooling one feature entry point.

Risks:

- adds an `AfsmFeature` concept and possible framework inheritance,
- can hide the ordinary `AfsmMachine` boundary,
- may make multiple machines per feature awkward,
- must not become a framework-owned ViewModel or DI container.

Prototype finding: Kotlin also requires all five generic superclass arguments
for `object DraftFeature : AfsmFeature(...)`; constructor values do not infer a
generic supertype. A composed `afsmFeature(...)` value compiles, but the new
feature container is not yet justified by a product capability.

## Candidate E: Direct Typed Property

Test the smallest language-native declaration that needs no new runtime type
tokens or feature container:

```kotlin
@AfsmGraph(
    id = "DraftQuickstart",
    fileName = "DraftQuickstart.mmd",
)
val draftStateMachine:
    AfsmMachine<DraftState, DraftEvent, DraftCommand, AfsmNoEffect> =
    afsmMachine {
        initial(
            phase = DraftPhase.Editing,
            data = DraftData(),
        )

        // product flow
    }
```

Expected benefits:

- removes the machine alias, delegated singleton, and factory function,
- uses the existing `AfsmMachine` boundary and ordinary expected-type inference,
- gives graph tooling a stable top-level symbol without adding token helpers,
- preserves `machine = draftStateMachine` in ViewModel and tests,
- follows Kotlin property naming conventions so the machine reads as a value,
  not a type or singleton declaration.

Required proof:

- extend `@AfsmGraph` and KSP discovery to a non-private top-level `val`,
- reject local, member, mutable, private, or non-graph-source properties with
  useful diagnostics,
- generate a direct property reference rather than instantiate a wrapper,
- migrate Draft, Auth, and Checkout and keep their graph, machine, ViewModel,
  and external-consumer checks green,
- verify Checkout dynamic initial state still comes from the host's explicit
  `initialState`, not from rebuilding the machine.

Implementation result:

- `@AfsmGraph` now accepts a safe top-level machine property and generates a
  direct registry reference,
- focused KSP tests reject private, member, mutable, computed, and non-machine
  properties,
- Draft, Auth, Checkout, ProductEditor, and both external consumer graph
  fixtures now use direct machine properties,
- machine/ViewModel tests, graph generation, API checks, and the clean Maven
  Local consumer build pass,
- Checkout still supplies navigation data through `afsmHost(initialState =
  ...)`.

Candidate E is the current pre-release authoring candidate. Human first-use
preference remains unverified, so this is not an API-freeze decision. See
[[afsm-first-use-api-experiment-results-2026-07-10|the experiment results]].

## Prototype Requirements

Build compile-only or test-only prototypes before changing production API:

1. Draft: form update, guarded save, entry command, failure recovery, no effect.
2. Auth: multiple form events, command branches, invalid result, UI effect,
   payload terminal phase.
3. Checkout: dynamic initial data, loading command, request-id payload phase,
   stale result ignores, retry, durable completion, optional effect.

For each candidate record:

- declarations before the first phase,
- explicit generic arguments and aliases,
- additional framework concepts,
- compiler diagnostics for a wrong event/command/effect type,
- ViewModel host signature,
- transition test call site,
- graph annotation/discovery implications,
- any casts, reflection, or hidden generated API.

## Acceptance Criteria

A candidate may replace the current API only if:

- Draft, Auth, and Checkout compile without weakening their behavior tests,
- the primary machine body is at least as readable as the current DSL,
- event, host-work, and UI-output boundaries remain clear,
- first-use setup removes meaningful conceptual or syntactic ceremony rather
  than moving it elsewhere,
- graph and ViewModel integration have a coherent migration design,
- a fresh-use review prefers it for reasons tied to comprehension and authoring,
  not novelty.

If none of the candidates materially improves the baseline, keep the current
shape and record that result rather than forcing an API change.
