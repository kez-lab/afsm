---
title: Afsm Output Model Simplification
updated: 2026-07-17
status: implemented-verified
---

# Afsm Output Model Simplification

## Decision

Remove `Effect` from the Afsm public model before release. Do not rename it and
do not merge it into `Command`.

The accepted public flow vocabulary is:

```text
State = current business-significant Phase + durable Data
Event = something that happened and enters the machine
Command = host work requested by an accepted machine transition
```

UI behavior is not a fourth machine output channel:

- A business outcome belongs in `Phase`/`Data` and is observed as state.
- A UI-originated UI action such as closing after a Done click calls the route
  callback directly.
- UI behavior following an async business outcome reacts to state.
- If repeating that reaction is unsafe, the feature models explicit pending/
  acknowledged state and an acknowledgement event; Afsm does not hide the
  delivery contract in a best-effort stream.

## Why Command Remains

`Command` has a distinct and necessary ownership boundary. A pure synchronous
machine cannot call repositories or suspend use cases. The host executes the
typed command, owns queue/cancellation/failure policy, and dispatches typed
result events back to the machine.

Removing Command would force one of two worse contracts:

- execute side effects inside transition code, coupling the machine to runtime
  and Android concerns; or
- observe state to infer work, risking duplicate execution after collection or
  restoration.

`Command` is therefore the single machine output for external work. It is not a
UI event and not a general MVI action type.

## Why Effect Does Not Remain

The current channel adds an Effect generic to every reducer/machine/transition,
an `AfsmNoEffect` marker, `effect(...)` DSL, topology labels, host buffering,
delivery config, test helpers, a Compose module, and feature effect types.

The three representative effects are redundant:

- Auth already has `Authenticated(session)`.
- Checkout already has durable `Completed(orderId)`.
- ProductEditor already receives the UI-originated Done click that can close
  the editor directly.

The current best-effort stream can also lose UI behavior while inactive. A
framework-level helper makes that risk look like the recommended path even
though current Android guidance prefers ViewModel-originated actions to update
UI state.

## Rejected Alternatives

### Documentation only

Rejected because it does not reduce concepts or fix sample behavior. The human
needed a verbal explanation despite existing Command/Effect documentation.

### Rename Effect to UiAction

Rejected because the type argument, DSL operation, runtime channel, delivery
policy, module, and lifecycle ambiguity remain.

### One merged Output or Action type

Rejected because the host would need to distinguish work it executes from UI
behavior it cannot safely deliver. The distinction would move into sealed
subtypes or conventions rather than disappear.

## Android Integration Shape

Sample UI must not be required to construct machine Event objects. The sample
ViewModels expose ordinary verb-named functions such as `submit()`, `pay()`,
`retry()`, and `updateTitle(value)`, then translate those calls to internal
machine events. Async command results remain typed events inside the
ViewModel-machine boundary.

Sample files should be named for their product role rather than using an MVI
`Contract.kt` convention. Grouping flow model types in one file remains a sample
choice, not a library requirement.

## Machine, Graph, and Tests Reading Contract

The phase-local DSL intentionally optimizes exact local questions: in this
phase, which events are accepted, ignored, or invalid, and what data/work/phase
change follows? It does not optimize a one-screen scan of every edge.

The generated Mermaid graph is therefore a first-class product view, not
decorative output:

- Graph: whole-flow topology and named conditions.
- Machine: exact local rules, data changes, commands, and ordering.
- Tests: executable proof for payloads and graph-invisible handled/ignored/
  invalid behavior.

The graph is generated from the same executable machine so this division of
labor does not create a second manually synchronized model.

## Accepted API Shape

```kotlin
interface AfsmReducer<S : Any, E : Any, C : Any> {
    fun transition(state: S, event: E): AfsmTransition<S, C>
}

interface AfsmMachine<S : Any, E : Any, C : Any> :
    AfsmReducer<S, E, C>,
    AfsmGraphSource

class AfsmTransition<S : Any, C : Any> {
    val state: S
    val commands: List<C>
    val commandInvocations: List<AfsmCommandInvocation<C>>
    val decision: AfsmDecision
}
```

The DSL becomes `afsmMachine<P, D, E, C> { ... }`. The `effect(...)` operation,
Effect topology metadata, `AfsmNoEffect`, `AfsmEffectDelivery`, `host.effects`,
Effect assertions, `CollectAfsmEffects`, and the `afsm-compose` module are
removed without compatibility aliases.

## Success Criteria

- Public reducer/machine/transition/host APIs have no Effect type parameter or
  channel.
- The six maintained library modules become five by removing `afsm-compose`.
- Auth, Checkout, and ProductEditor declare no Effect type and emit no Effect.
- Their screen files construct zero feature Event objects.
- Product completion remains represented by state; UI-only clicks remain UI
  callbacks.
- Generated graphs, machine tests, API dumps, docs, and external consumer all
  agree with the smaller model.
- README and onboarding explicitly explain the graph/machine/tests reading
  contract and show ordinary Android ViewModel methods.
- A fresh-use review checks whether the Command role is now clear without an
  Effect comparison and whether samples still feel like MVI.

## Evidence

- [Informal human feedback](../../raw/verification/2026-07-17-human-usability-feedback/README.md)
- [Output model audit](../../raw/verification/2026-07-17-output-model-audit/README.md)
- [Android guidance refresh](../../raw/sources/2026-07-17-android-ui-event-guidance-refresh.md)

## Implementation Result

Implemented on 2026-07-17:

- core, runtime, ViewModel, test helpers, topology, and API dumps use the
  three-type model;
- `afsm-compose` and every maintained feature Effect type were removed;
- sample screens construct zero feature Event objects and call verb-named
  ViewModel methods;
- Auth and Checkout navigation derive from durable state; ProductEditor Done is
  a direct callback;
- `Contract.kt` feature files became `Flow.kt`;
- README and public docs explain Command ownership and the graph/machine/tests
  reading contract;
- module tests, sample tests, KSP tests, graph generation, API checks, Maven
  Local publication, and clean external consumer tasks pass.

A new controlled human first-use session remains the next evidence gate.
The commit-pinned two-stage participant input is prepared and verified in
[[../05-qa/verification-report-2026-07-17-effect-free-first-use-preparation|Effect-Free First-Use Preparation 2026-07-17]], but no human result exists yet.
