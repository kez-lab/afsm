# Sample Shop Afsm Guide

`sample-shop` is a reference Android app, not a template for forcing every
screen into a machine.

## Feature split

| Feature | Architecture |
|---|---|
| Auth | Afsm; conditional form submit and async authentication |
| Checkout | Afsm; dynamic input, payment safety, restoration |
| Product Editor | Afsm; long workflow and cancellable upload |
| Catalog and simple screens | ordinary Android state handling where clearer |

## File roles

Each Afsm feature uses:

```text
FeatureFlow.kt          State, Event, Command, render model
FeatureStateMachine.kt  pure business-flow rules and graph topology
FeatureViewModel.kt     Android dependencies and command execution
FeatureScreen.kt        Compose state collection and feature callbacks
FeatureStateMachineTest.kt
```

The UI calls verb-named ViewModel methods. It does not expose a generic MVI
`onEvent` function.

## Output policy

- Commands request external work from the host.
- Business completion stays in state.
- UI-only actions remain UI callbacks.
- Routes may react to durable completion state for navigation.

## Review workflow

Generate diagrams:

```bash
./gradlew :sample-shop:generateAfsmMmd
```

Review each feature in this order:

1. graph for full topology,
2. machine for exact rules,
3. tests for edge cases,
4. ViewModel for repository and saved-state integration,
5. screen for Android API shape.

## Verification

```bash
./gradlew :sample-shop:testDebugUnitTest :sample-shop:generateAfsmMmd
```

The graph registry test ensures Auth, Checkout, and Product Editor remain
registered. See [Examples](examples.md) for the learning path.
