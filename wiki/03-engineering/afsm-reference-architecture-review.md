---
title: Afsm Reference Architecture Review
updated: 2026-07-11
reviewed: 2026-05-11
status: historical-review
---

# Afsm Reference Architecture Review

This dated review compares the May 2026 Afsm structure against established
state-machine, statechart, reducer, and Android UI-state references. Its old API
names and gap table are design history; current contracts live in
`current-state.md` and the canonical engineering pages.

References:

- [XState transitions](https://statelyai-xstate.mintlify.app/concepts/transitions)
- [XState actions](https://statelyai-xstate.mintlify.app/guides/actions)
- [W3C SCXML](https://www.w3.org/TR/scxml/)
- [Tinder StateMachine](https://github.com/Tinder/StateMachine)
- [KStateMachine transitions](https://kstatemachine.github.io/kstatemachine/pages/transitions/transitions.html)
- [KStateMachine README](https://github.com/KStateMachine/kstatemachine)
- [Redux reducers](https://redux.js.org/tutorials/fundamentals/part-3-state-actions-reducers)
- [Redux overview](https://redux.js.org/tutorials/essentials/part-1-overview-concepts)
- [Elm commands and subscriptions](https://guide.elm-lang.org/effects/)
- [Square Workflow API](https://square.github.io/workflow/kotlin/api/workflow/com.squareup.workflow1/)

## Current Afsm Shape

Afsm currently has two layers:

- Reducer/runtime layer: `AfsmReducer<S, E, C, F>`, `AfsmTransition<S, C, F>`, `AfsmHost`, `ViewModel.afsmHost(...)`.
- Executable machine layer: `AfsmState<P, X>`, `AfsmMachine<P, X, E, C, F>`, `afsmMachine { ... }`, `state`, `on`, `transitionTo`, `stay`, `otherwise`, `onEnter`, `onExit`, `updateContext`, `command`, `effect`, and `topology.toMmd()`.

This is directionally sound. The reducer/runtime layer resembles Elm/Redux: pure state update plus deferred outputs. The statechart layer resembles XState/SCXML/KStateMachine/Tinder: state scopes, event branches, guards, entry actions, and graphable targets.

The main problem is not the architecture direction. The main problem is that naming, lifecycle semantics, graph metadata, and validation are not yet sharp enough for a public library.

## Reference Comparison

| Reference | Relevant pattern | Afsm match | Gap |
|---|---|---|---|
| XState | Finite state plus extended `context`; transitions with guards and actions | `AfsmState<Phase, Context>`, `transitionTo`, `guard`, `updateContext` | Initial entry behavior, invoked actors/services, inspection semantics |
| SCXML | `datamodel`, transition executable content, ordered exit -> transition -> entry, `invoke` | Context, transition block, `onExit`, `onEnter` | No invoke/cancel model, no hierarchy/parallel semantics |
| Tinder StateMachine | Kotlin DSL with `state`, `on`, `transitionTo`, side effect | Afsm DSL shape is close | Tinder is simpler; Afsm adds context and graph export, so validation matters more |
| KStateMachine | Kotlin DSL, guards, targetless transitions, nested/parallel states, transition listeners | `stay` maps to targetless/internal transition; guards align | Afsm lacks nested/parallel, transition type, global listeners |
| Redux | Pure reducer: state + action -> state; no side effects inside reducer | `AfsmReducer.transition` remains pure and Android-free | Afsm DSL lambdas can still hide impure work unless documented/tested |
| Elm | `update : Msg -> Model -> (Model, Cmd Msg)`; runtime executes commands | `AfsmTransition(state, commands, effects)` plus `AfsmHost` | Need clearer command naming and command exception/cancellation policy |
| Square Workflow | State, render output, sinks, workers, snapshot/restoration | ViewModel as adapter plus command handler is compatible | Afsm has no first-class worker/subscription/restoration API yet |
| Android official guidance | ViewModel state holder, lifecycle-aware collection, UI-local behavior stays UI | `afsm-viewmodel` and sample app align | SavedStateHandle and effect collection helpers remain incomplete |

## What Should Stay

- Keep `ViewModel` as the Android lifecycle adapter. This aligns with Android guidance and avoids turning Afsm into a UI framework.
- Keep the core state machine Android-free and synchronous.
- Keep `AfsmState<Phase, Context>` as the standard state shape for graphable machines.
- Keep command execution outside transition logic. This preserves reducer purity and testability.
- Keep `.mmd` generation from executable topology, not from a second graph-only DSL.
- Keep Afsm optional for complex flows only. Ordinary data screens should stay ViewModel + Flow.

## Changes Needed Before Public API Freeze

### 1. Unify Command/Action Naming

Current code mixes terms:

- `AfsmTransition` exposes `commands`.
- Runtime uses `AfsmCommandHandler`.
- DSL exposes `command(...)`.
- `AfsmMachine<P, X, E, C, F>` uses generic `C`, and sample features pass `ProductEditorCommand`.

This is confusing. References split in two directions:

- XState/SCXML say actions/executable content.
- Elm says commands.
- Redux says actions for input events, so `Action` is risky for Android developers who already know UDF/MVI.

Recommended Afsm direction:

- Use `Command` consistently for host-executed outputs.
- Keep DSL `command(...)` as the public spelling.
- Keep `AfsmMachine<P, X, E, C, F>` generic `C`.
- Keep documentation wording: “command is a transition action/output.”

### 2. Add Builder Validation

The DSL currently allows invalid definitions that become hard-to-debug runtime behavior:

- duplicate `state(...)` labels,
- duplicate `on<Event>` handlers in the same state,
- transition targets without matching state definitions,
- ambiguous graph labels,
- initial phase without a state definition.

References with declarative machine definitions generally validate structure early. Afsm should fail at chart build time for these cases.

Recommended next implementation:

- Add `AfsmDefinitionException`.
- Validate duplicate state labels.
- Validate duplicate event labels per state.
- Validate initial and target labels are declared.
- Add focused tests before changing sample code.

### 3. Clarify Initial Entry Semantics

Current `onEnter` runs only when a transition targets a phase. It does not run for the initial state passed to `AfsmHost`.

This is surprising from a statechart perspective. SCXML and XState-style machines treat initial entry as a real entry path. KStateMachine also distinguishes machine start.

Recommended Afsm decision:

- For Android MVP, keep `ScreenEntered` as the explicit Android-friendly start event.
- But document that `onEnter` does not run for `initialState` unless the host starts the chart through a future chart-specific API.
- Later add an `AfsmHost` overload for `AfsmStateChart` that can execute a start transition if the product wants statechart-pure startup semantics.

### 4. Add `onExit` Before Claiming Statechart Completeness

Afsm has `onEnter` but no `onExit`.

SCXML explicitly orders exit handlers, transition executable content, and entry handlers. KStateMachine and XState both model entry/exit behavior. Without `onExit`, Afsm cannot cleanly model timer cancellation, subscription cleanup, or leaving-state diagnostics.

Recommended scope:

- Add flat-state `onExit` before nested states.
- Define order as: transition block context/commands -> exit? or exit -> transition block -> entry.
- Prefer SCXML-compatible order for public semantics: exit, transition block, entry.
- Add tests for same-state `stay` not running exit/entry and external transition running both.

### 5. Improve Topology Metadata Before Graph API Stabilizes

`AfsmTopologyTransition` currently stores only `from`, `event`, and `to`.

That is enough for a first `.mmd`, but not enough for production diagrams:

- guarded branches need labels,
- `otherwise` needs better visual treatment,
- self transitions should be optionally hidden or grouped,
- states need human labels separate from ids,
- future hierarchy needs parent/child metadata,
- Mermaid ids need escaping/sanitization.

Recommended change:

```kotlin
data class AfsmTopologyState(
    val id: String,
    val label: String = id,
    val parentId: String? = null,
)

data class AfsmTopologyTransition(
    val from: String,
    val event: String,
    val to: String,
    val guardLabel: String? = null,
    val commandLabels: List<String> = emptyList(),
    val kind: AfsmTopologyTransitionKind = AfsmTopologyTransitionKind.External,
)
```

Do this before external release because topology is part of the public graph contract.

### 6. Add Command Failure and Cancellation Policy

`AfsmHost` currently executes commands sequentially. If a command handler throws, the processor coroutine can fail and the host stops accepting events.

For demos this is acceptable. For an Android library it is too sharp.

Recommended MVP improvement:

- Add command exception diagnostics.
- Keep the default fail-fast or record behavior explicit in `AfsmConfig`.
- Do not invent generic error events in core because event types are feature-specific.
- Document that command handlers should catch domain failures and dispatch typed failure events.

Cancellation and long-running services should remain a later design, but the API should reserve space for it.

### 7. Rename Public Type Boundaries

`AfsmStateChart` is technically accurate, but the user feedback shows it feels awkward.

The deeper issue is that Afsm has two machine-like concepts:

- host-facing reducer contract,
- DSL-built executable machine definition.

Recommended candidate:

- Rename low-level `AfsmStateMachine<S, E, C, F>` to `AfsmReducer<S, E, C, F>` or hide it behind runtime APIs.
- Rename `AfsmStateChart<P, X, E, C, F>` to the main product type, likely `Afsm<P, X, E, C, F>` or `AfsmMachine<P, X, E, C, F>`.
- Remove the helper object `Afsm` if `Afsm` becomes the main type; replace helper methods with top-level functions or `AfsmTransition.transitionTo(...)`.

This should be decided before more public docs are written.

## Changes To Defer

- Nested states and parallel states: important, but too much for MVP. Preserve topology extensibility now; implement later.
- Invoked actors/services: valuable for timers, polling, websocket, upload, and location flows, but requires cancellation semantics and Android lifecycle policy.
- Generated visual previews beyond `.mmd`: useful, but the user expectation is `.mmd` first.
- Full Redux/Workflow-style global store behavior: not aligned with Afsm’s Android screen-flow focus.

## Bottom Line

Afsm is currently on the right architectural track.

The most urgent changes are not conceptual rewrites. They are API hardening:

1. unify `Command`/`Action` naming,
2. add DSL definition validation,
3. define initial entry semantics,
4. add `onExit` and execution-order tests,
5. enrich topology metadata before graph APIs become public,
6. add command error diagnostics,
7. settle the public machine/reducer names.

The current `AfsmState<Phase, Context>` direction should remain.

## Follow-up Implementation

2026-05-10 hardening pass:

- `AfsmStateMachine` was renamed to `AfsmReducer` for new code.
- The executable DSL public name is now `AfsmMachine` / `afsmMachine`.
- DSL output terminology is now `command(...)`.
- Flat `onExit` exists, with `onExit -> transition block -> onEnter` ordering tests.
- Initial state construction does not run `onEnter`; startup work should be explicit through an event such as `ScreenEntered`.
- `afsmMachine { ... }` now validates missing initial declarations, duplicate declarations, and undeclared transition targets.
- `AfsmTopologyTransition` now carries guard, command, effect, kind, and fallback metadata.
- `AfsmHost` now has `AfsmCommandFailurePolicy`; `CancellationException` is always rethrown.
- Superseded on 2026-07-11: queued feature cancel commands cannot interrupt the
  active sequential command. Bounded keyed `onEnter { invoke(...) }` now owns
  local cooperative phase-exit cancellation; full actor/service semantics
  remain deferred.
