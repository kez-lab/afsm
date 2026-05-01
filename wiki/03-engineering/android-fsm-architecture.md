---
title: Android FSM Architecture
updated: 2026-05-01
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
-> StateMachine.transition(currentState, event)
-> TransitionResult(newState, commands)
-> ViewModel updates StateFlow
-> ViewModel executes commands
-> command result is fed back as Event
-> View renders State
```

## Recommended Interfaces

```kotlin
data class TransitionResult<S, C>(
    val state: S,
    val commands: List<C> = emptyList(),
)

interface StateMachine<S, E, C> {
    fun transition(state: S, event: E): TransitionResult<S, C>
}
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
