---
title: Testing Strategy
updated: 2026-05-09
---

# Testing Strategy

## Test Integrity

Tests in this project are executable specification.

Do not change a failing test merely because production code does not pass. First classify the failure:

- implementation bug,
- test harness issue,
- stale spec,
- ambiguous requirement.

When behavior intentionally changes, update the relevant wiki/spec/decision first, then update tests, then update implementation.

For bug fixes, add or preserve a failing regression test before changing production code.

Detailed AI workflow: [[../07-llm/ai-engineering-guardrails|AI Engineering Guardrails]].

## FSM Unit Tests

FSM tests should be plain JVM tests with no Android dependency.

Test:

- valid transitions,
- invalid transitions,
- command emission,
- retry behavior,
- failure behavior,
- terminal state behavior,
- preservation of useful data across states.

Example scenario name:

```text
Editing에서 SubmitRequested를 받으면 Submitting으로 전이되고 ExecuteLogin command가 발생한다
```

## ViewModel Tests

ViewModel tests should verify Android integration behavior.

Test:

- events are passed into the FSM,
- returned state is exposed through `StateFlow`,
- commands call the correct use case,
- use case success feeds back success event,
- use case failure feeds back failure event,
- coroutine cancellation behavior if defined,
- coroutine command execution uses test dispatchers deterministically,
- `CancellationException` is not accidentally consumed,
- `stateIn` flows have an active collector when required.

Use official coroutine testing patterns:

- wrap coroutine tests with `runTest`,
- replace `Dispatchers.Main` for local ViewModel tests that use `viewModelScope`,
- share a single `TestCoroutineScheduler` across test dispatchers,
- prefer `StandardTestDispatcher` for precise scheduling and `UnconfinedTestDispatcher` for simple eager tests,
- assert `StateFlow.value` when final/current state matters more than every intermediate emission.

## UI Tests

UI tests should focus on rendering and event mapping.

Test:

- each state renders the expected UI,
- user interactions call the expected event callback,
- navigation/snackbar behavior is triggered according to the chosen state/effect policy.

## Documentation Value

Scenario-style test names are part of the wiki strategy: they make the flow searchable and readable without forcing every future reader to reconstruct the transition graph from implementation code.

## Official References

- `kb://android/kotlin/coroutines/test`
- `kb://android/kotlin/flow/test`
- `kb://android/topic/libraries/architecture/coroutines`
