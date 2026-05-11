---
title: Android FSM Architecture
updated: 2026-05-11
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

For graphable phase/context flows, use `AfsmMachine<P, X, E, C, F>`:

```kotlin
data class AfsmState<P : Any, X : Any>(
    val phase: P,
    val context: X,
)

interface AfsmMachine<P : Any, X : Any, E : Any, C : Any, F : Any> :
    AfsmReducer<AfsmState<P, X>, E, C, F>,
    AfsmGraphSource
```

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
