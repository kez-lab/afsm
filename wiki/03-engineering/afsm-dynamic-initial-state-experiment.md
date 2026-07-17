---
title: Afsm Dynamic Initial State Experiment
updated: 2026-07-17
status: implemented-current
---

# Afsm Dynamic Initial State Experiment

This experiment asks whether a graphable machine can model transition rules
and its initial phase without inventing runtime data that Android must replace.

## Evidence-Backed Problem

Checkout requires a navigation `productId`, but `AfsmMachine` always exposes a
complete `initialState`. The current machine therefore declares:

```kotlin
initial(
    phase = CheckoutPhase.Idle,
    data = CheckoutData(productId = 0),
)
```

`CheckoutViewModel` correctly overrides it with
`checkoutState(productId = productId)`. However, the convenient
`afsmHost(machine = checkoutStateMachine)` overload remains callable and would
start a valid-looking load for product `0`.

This is not only documentation debt. The type system currently claims that a
usable default state exists when the domain says it does not.

## Invariants

- The executable transition rules and graph topology remain one definition.
- Graph generation still knows the initial phase without needing Android input.
- Static features such as Draft keep a concise default-state path.
- Dynamic features such as Checkout cannot be hosted without an explicit state
  created from navigation, deep-link, restoration, or repository input.
- The core stays plain Kotlin and does not own `SavedStateHandle` or navigation.
- No null state, unchecked cast, magic sentinel, or runtime-only guard replaces
  compile-time safety.

## Candidate A: Keep A Placeholder Default

Keep `AfsmMachine.initialState` and document that dynamic ViewModels must always
pass an override.

Verdict: baseline only. It leaves the unsafe convenience overload callable and
makes topology needs dictate fake business data.

## Candidate B: Split Rules From Optional Default

Make `AfsmMachine<State, Event, Command>` own reducer behavior and
topology only. Add a subtype for machines that genuinely own a default state:

```kotlin
interface AfsmMachine<S, E, C> : AfsmReducer<S, E, C>, AfsmGraphSource

interface AfsmDefaultMachine<S, E, C> : AfsmMachine<S, E, C> {
    val initialState: S
}
```

Static declaration:

```kotlin
val draftStateMachine: AfsmDefaultMachine<...> = afsmMachine {
    initial(DraftPhase.Editing, DraftData())
    // phase rules
}
```

Dynamic declaration:

```kotlin
val checkoutStateMachine: AfsmMachine<...> = afsmMachine(
    initialPhase = CheckoutPhase.Idle,
) {
    // flow
}
```

Only `AfsmDefaultMachine` receives the no-`initialState` ViewModel host
overload. All `AfsmMachine` values retain the explicit-state overload.

Questions:

- Is `AfsmDefaultMachine` the clearest name?
- Can expected-type inference preserve `CheckoutPhase` and `CheckoutData` from
  the declared machine type?
- Can core validation use an initial phase without manufacturing data?
- Does moving `initial(...)` from the DSL body into the constructor improve or
  fragment flow readability?

## Candidate C: Require Explicit State For Every Host

Remove `initialState` from `AfsmMachine` and remove the convenient ViewModel
overload. Every feature passes `initialState`, including Draft.

Benefit: one machine type and uniformly explicit Android initialization.

Cost: static flows repeat an initial-state value at every host and may lose the
single obvious default used by tests, previews, and simple ViewModels.

## Candidate D: Parameterized Initial-State Factory

Let a machine own an input-to-state factory, for example
`machine.initialState(productId)`.

Verdict: high risk. It adds an input generic or erased parameter channel to the
machine, couples domain construction to runtime inputs, and complicates graph
and host overloads. Prototype only if Candidates B/C fail.

## Prototype Requirements

1. Draft compiles with a genuine default state and the concise host overload.
2. Checkout compiles with an initial phase but no placeholder product id.
3. Checkout ViewModel must pass `checkoutState(productId)`.
4. A temporary negative compile probe confirms that omitting Checkout's
   explicit state has no applicable overload.
5. Draft, Auth, Checkout, ProductEditor, graph registry, API checks, and clean
   external consumer tests remain green.
6. Transition tests can construct scenario states directly without depending
   on a fake machine default.

## Acceptance Criteria

- No dynamic feature contains sentinel initial business data.
- Missing dynamic initialization fails at compile time near the host call.
- Static first use does not gain more ceremony than the removed wrapper shape.
- Initial graph phase and runtime initial data are explained as separate
  concerns without introducing Android dependencies.
- The public type names and compiler errors survive a fresh-use review.
- Human and pilot evidence remain separate from repository verification.

## Implementation Result

Candidate B is implemented with the minimal static migration:

- `AfsmMachine` now owns reducer behavior and topology only,
- `AfsmDefaultMachine` owns `initialState`,
- the existing `afsmMachine { initial(...) }` static DSL returns
  `AfsmDefaultMachine`,
- `afsmMachine(initialPhase = ...)` returns a base `AfsmMachine` without
  runtime data,
- only `AfsmDefaultMachine` is accepted by the no-state ViewModel host overload,
- Checkout removed `CheckoutData(productId = 0)` and keeps its real
  `checkoutState(productId)` in the ViewModel,
- a negative compiler probe at the host call reported that
  `AfsmDefaultMachine` was expected when the explicit Checkout-like state was
  removed,
- the full local release gate and clean Maven Local external consumer passed.

The result is the current pre-release direction, not an API freeze. The type
name and split still require a repository-based fresh-use review and eventual
human evidence.
