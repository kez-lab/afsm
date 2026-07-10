---
title: Android FSM Architecture
updated: 2026-07-10
---

# Android FSM Architecture

The target architecture keeps Android `ViewModel` while moving business flow transitions into plain Kotlin FSMs.

## Goal

Make user interaction -> business logic -> state update -> view rendering easier to trace without giving up Android lifecycle handling, Compose state collection, or ViewModel testability.

## Shape

```text
View / Compose
-> sends Event
-> ViewModel.onEvent(event)
-> AfsmReducer.transition(currentState, event)
-> AfsmTransition(newState, commands, effects)
-> ViewModel updates StateFlow
-> ViewModel executes commands
-> command result is fed back as Event
-> View renders State
```

## Recommended Interfaces

```kotlin
data class AfsmTransition<S, C, F>(
    val state: S,
    val commands: List<C> = emptyList(),
    val effects: List<F> = emptyList(),
)

fun interface AfsmReducer<S, E, C, F> {
    fun transition(state: S, event: E): AfsmTransition<S, C, F>
}
```

For graphable phase/data flows, expose static defaults as
`AfsmDefaultMachine<State, Event, Command, Effect>` and dynamic flows as the
base `AfsmMachine` with host-supplied state:

```kotlin
data class AfsmState<P : Any, D : Any>(
    val phase: P,
    val data: D,
)

typealias LoginState = AfsmState<LoginPhase, LoginData>

val loginStateMachine:
    AfsmDefaultMachine<LoginState, LoginEvent, LoginCommand, LoginEffect> =
    afsmMachine {
        initial(LoginPhase.Editing, LoginData())
        // phase rules
    }
```

At feature boundaries, collapse `Phase + Data` into a feature state type and
refer to graphable machines through `AfsmMachine<State, Event, Command, Effect>`
and opt into `AfsmDefaultMachine` only when a real default exists.

## Screen File Layout

```text
feature/login/
  LoginRoute.kt
  LoginScreen.kt
  LoginViewModel.kt
  LoginContract.kt
  LoginStateMachine.kt
```

## Responsibilities

- `LoginContract.kt`: `State`, `Event`, `Command`, optional `Effect`.
- `LoginStateMachine.kt`: pure transition rules.
- `LoginViewModel.kt`: `StateFlow`, `viewModelScope`, command execution, saved state integration.
- `LoginRoute.kt`: collect state with lifecycle, connect navigation/effects.
- `LoginScreen.kt`: stateless UI rendering and user event callbacks.

## Adoption Rule

Use this FSM structure when the screen has meaningful flow phases, invalid transitions, retries, async results, or multi-step behavior.

Avoid forcing it onto simple screens where a plain `UiState` and clear ViewModel functions are easier to read.
