---
title: Android FSM Architecture
updated: 2026-07-17
---

# Android FSM Architecture

The target architecture keeps Android `ViewModel` while moving business flow transitions into plain Kotlin FSMs.

## Goal

Make user interaction -> business logic -> state update -> view rendering easier to trace without giving up Android lifecycle handling, Compose state collection, or ViewModel testability.

## Shape

```text
View / Compose
-> calls a verb-named ViewModel function
-> ViewModel translates the call into an Event
-> AfsmReducer.transition(currentState, event)
-> AfsmTransition(newState, commands, commandInvocations, decision)
-> AfsmHost publishes StateFlow
-> AfsmHost executes ordinary commands or tracks phase-owned invocations
-> command result is fed back as Event
-> View renders State
```

## Recommended Interfaces

```kotlin
class AfsmTransition<S : Any, C : Any> {
    val state: S
    val commands: List<C>
    val commandInvocations: List<AfsmCommandInvocation<C>>
    val decision: AfsmDecision
}

fun interface AfsmReducer<S, E, C> {
    fun transition(state: S, event: E): AfsmTransition<S, C>
}
```

`AfsmTransition` is created through factories or the executable DSL, not a
public constructor. `commands` are bounded sequential work. An
`AfsmCommandInvocation.Start/Cancel` pair represents cooperative long-running
work owned by a phase.

For graphable phase/data flows, expose static defaults as
`AfsmDefaultMachine<State, Event, Command>` and dynamic flows as the
base `AfsmMachine` with host-supplied state:

```kotlin
data class AfsmState<P : Any, D : Any>(
    val phase: P,
    val data: D,
)

typealias LoginState = AfsmState<LoginPhase, LoginData>

val loginStateMachine:
    AfsmDefaultMachine<LoginState, LoginEvent, LoginCommand> =
    afsmMachine {
        initial(LoginPhase.Editing, LoginData())
        // phase rules
    }
```

At feature boundaries, collapse `Phase + Data` into a feature state type and
refer to graphable machines through `AfsmMachine<State, Event, Command>`
and opt into `AfsmDefaultMachine` only when a real default exists.

## Screen File Layout

```text
feature/login/
  LoginRoute.kt
  LoginScreen.kt
  LoginViewModel.kt
  LoginFlow.kt
  LoginStateMachine.kt
```

## Responsibilities

- `LoginFlow.kt`: feature phases, durable data, machine events, commands, and
  render mapping. This grouping is a sample choice, not an MVI contract.
- `LoginStateMachine.kt`: pure transition rules.
- `LoginViewModel.kt`: host creation, command handler, `StateFlow`,
  `viewModelScope`, and saved-state integration.
- `LoginRoute.kt`: collect state with lifecycle and connect state-driven or
  direct UI navigation.
- `LoginScreen.kt`: stateless UI rendering and user event callbacks.

Sample UI calls verb-named ViewModel functions and does not construct machine
Event values. The ViewModel translates UI callbacks and command results into
events at the machine boundary.

## Adoption Rule

Use this FSM structure when the screen has meaningful flow phases, invalid transitions, retries, async results, or multi-step behavior.

Avoid forcing it onto simple screens where a plain `UiState` and clear ViewModel functions are easier to read.

## Async Work Rule

- Use ordinary `command(...)` for short host work that should execute
  sequentially.
- Use `onEnter { invoke(key, label) { command } }` for cooperative long-running
  work whose local coroutine lifetime belongs to one phase.
- Leaving the phase or clearing the `ViewModel` cancels that invocation through
  the `AfsmHost` child of `viewModelScope`.
- Local cancellation is not remote cancellation. Preserve request ids,
  idempotency, and SDK/repository cancellation contracts when work can outlive
  the coroutine.
