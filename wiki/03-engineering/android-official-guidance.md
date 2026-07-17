---
title: Android Official Guidance
updated: 2026-07-17
---

# Android Official Guidance

This page summarizes official Android guidance that constrains the project FSM architecture.

Raw research notes: [Android Official Docs Research](../../raw/sources/2026-05-01-android-official-docs-fsm-research.md)

## Core Fit

The planned FSM architecture fits Android guidance if the FSM is treated as the transition model behind a screen-level business state holder.

The official direction is:

- UI displays state and sends user events.
- `ViewModel` is the common screen-level business state holder.
- Business logic belongs in ViewModel/domain/data layers, not in UI rendering code.
- UI behavior logic such as navigation, snackbar display, focus, scroll, and UI element state normally stays in UI or UI-scoped plain state holders.
- State should be exposed through observable holders such as `StateFlow`, Compose State, or LiveData.

## Mapping to This Project

| Android concept | Project mapping |
|---|---|
| Screen-level business state holder | `ViewModel` |
| State production rule engine | Plain Kotlin `StateMachine` |
| User event | External `Event` |
| Repository/use case response | Internal `Event` |
| Async work request | Sequential `Command` or phase-owned command invocation |
| UI-renderable output | `StateFlow<State>` |
| UI behavior after a business outcome | UI reacts to durable state; acknowledge in feature state only when needed |

## Design Constraints

### ViewModel remains Android-facing

`ViewModel` should:

- expose screen state,
- receive events,
- provide command execution through an `AfsmHost` owned by `viewModelScope`,
- integrate `SavedStateHandle`,
- call use cases or repositories,
- feed results back to the FSM as events.

It should not hold references to Views, `Context`, `Resources`, or lifecycle-scoped UI objects.

### FSM remains plain Kotlin

`StateMachine.transition` should:

- be synchronous,
- be deterministic,
- avoid Android dependencies,
- avoid direct use case/repository calls,
- return `AfsmTransition<State, Command>`.

### UI owns UI behavior

Compose/View code should:

- collect state lifecycle-aware,
- render state,
- expose user events upward,
- perform navigation and other UI behavior from UI callbacks or observed state,
- keep UI element state local unless business logic needs it.

## Event and State Rule

Official Android state production guidance separates events and state:

- events are transient inputs,
- state is the durable output consumed by UI.

For this project, that means:

- model user actions and async results as `Event`,
- model current business phase as `State`,
- do not use one-off event streams as the default way to represent meaningful state,
- prefer state changes that are reproducible after configuration changes.

## Navigation Rule

Navigation is generally UI behavior logic.

FSM/ViewModel may expose a state such as `LoggedIn`, `CheckoutCompleted`, or `DobValidated`. The UI decides whether and how to navigate from that state, especially when back stack behavior or form factor affects navigation.

Afsm does not provide a best-effort Effect stream. If a business outcome should
change navigation, represent the outcome in state and let the UI decide how to
navigate. If a UI-originated action only changes UI behavior, handle it directly
in the UI. Model pending/acknowledged state explicitly when repeated handling
would be unsafe.

## Compose Side-Effect Rule

Compose side effects should be UI-related and lifecycle-aware.

Use:

- `LaunchedEffect` for suspend work tied to composition keys,
- `rememberUpdatedState` when an effect needs the latest callback without restarting,
- `rememberCoroutineScope` for UI-triggered coroutine work tied to composition,
- `snapshotFlow` to convert Compose state reads to Flow,
- `derivedStateOf` only when update frequency is higher than UI recomposition needs.

Do not move animation or UI element state operations into `viewModelScope` when they require a composition-scoped coroutine.

## Coroutine Rule

Command execution in this project should follow Android coroutine guidance:

- The ViewModel-owned `AfsmHost` creates coroutines for business work triggered
  by UI events.
- Use `viewModelScope` for work that should live as long as the screen-level state holder.
- Use lifecycle-aware collection such as `collectAsStateWithLifecycle` in Compose or `repeatOnLifecycle` in Views/Fragments.
- Inject dispatchers into data/domain classes instead of hardcoding them.
- Suspend functions exposed by data/domain layers should be main-safe.
- Do not use `GlobalScope` for screen-bound work.
- Do not swallow `CancellationException`; rethrow it if caught.
- Phase-owned `invoke` work may use cooperative cancellation on phase exit, but
  local coroutine cancellation does not prove a remote operation stopped.

## Testing Rule

FSM transition tests should avoid Android entirely.

ViewModel command execution tests should use:

- `runTest`,
- `TestDispatcher`,
- a shared `TestCoroutineScheduler`,
- `Dispatchers.setMain`/`resetMain` or a reusable main dispatcher rule when `viewModelScope` is involved.

For `StateFlow`, prefer asserting the current `value` unless the exact emission sequence is important. If a `StateFlow` is created with `stateIn` and `SharingStarted.WhileSubscribed` or lazy sharing, keep at least one collector active during the test.

## State Restoration Rule

ViewModel memory covers configuration changes, but not system-initiated process death.

For this FSM architecture:

- Use `SavedStateHandle` only for small, transient state needed by business logic.
- Prefer saving IDs, selected filters, text input, current step keys, or minimal FSM restoration keys.
- Do not serialize large or complex screen state into `SavedStateHandle`.
- Rebuild complex screen state from repositories or persistent storage after process recreation.
- Use `rememberSaveable` for UI-scoped element state that does not belong to business logic.
- Test `SavedStateHandle` by constructing it directly with test values.
