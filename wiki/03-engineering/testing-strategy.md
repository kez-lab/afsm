---
title: Testing Strategy
updated: 2026-07-17
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
- `Handled`, `Ignored`, and `Invalid` decisions,
- command emission,
- phase-owned invocation start/cancellation when used,
- retry behavior,
- duplicate and stale-result behavior,
- failure behavior,
- durable terminal state behavior,
- restoration-only phase behavior when defined,
- preservation of useful data across states.

Example scenario name:

```text
Editing에서 SubmitRequested를 받으면 Submitting으로 전이되고 ExecuteLogin command가 발생한다
```

## ViewModel Tests

ViewModel tests should verify Android integration behavior.

Test:

- feature verb methods translate UI calls into the expected internal machine
  events,
- accepted machine state is exposed through host `StateFlow`,
- commands call the correct use case,
- use case success feeds back success event,
- use case failure feeds back failure event,
- completion and restoration remain correct without a one-shot Effect stream,
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

UI tests should focus on rendering and the Android feature boundary.

Test:

- each state renders the expected UI,
- user interactions call the expected feature verb callback,
- UI composables do not need to construct machine Event values,
- navigation or other UI behavior follows direct UI callbacks or durable
  business state as specified by the feature.

## Documentation Value

Scenario-style test names are part of the wiki strategy: they make the flow searchable and readable without forcing every future reader to reconstruct the transition graph from implementation code.

## Official References

- `kb://android/kotlin/coroutines/test`
- `kb://android/kotlin/flow/test`
- `kb://android/topic/libraries/architecture/coroutines`
