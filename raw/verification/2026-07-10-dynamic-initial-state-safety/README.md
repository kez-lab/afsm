# Dynamic Initial-State Safety Verification

Date: 2026-07-10

Purpose: remove fake business defaults from graphable machines whose state
depends on Android runtime input.

## Contract Under Test

- `AfsmMachine` owns transition rules and topology without `initialState`.
- `AfsmDefaultMachine` adds a genuine static `initialState`.
- `afsmMachine { initial(...) }` returns `AfsmDefaultMachine`.
- `afsmMachine(initialPhase = ...)` returns `AfsmMachine` without runtime data.
- The no-state ViewModel host overload accepts only `AfsmDefaultMachine`.
- Checkout has no `CheckoutData(productId = 0)` and must receive
  `checkoutState(productId)` from its ViewModel.

## Negative Compile Probe

The explicit state was temporarily removed from a ViewModel hosting a base
`AfsmMachine`. Compilation failed at the host call with:

```text
Argument type mismatch: actual type is DynamicCounterStateMachine,
but AfsmDefaultMachine<S, E, C, F> was expected.
```

The explicit state was restored after capturing the diagnostic.

## Passing Verification

```bash
./gradlew :afsm-core:test \
  :afsm-viewmodel:testDebugUnitTest \
  :sample-shop:testDebugUnitTest \
  :sample-shop:generateAfsmMmd \
  :afsm-graph-ksp:test
./gradlew :afsm-core:apiDump :afsm-viewmodel:apiDump
./scripts/verify-release-local.sh --no-daemon
```

Result: passed, including the clean Maven Local external consumer build.

The broader root `test` aggregate was also tried but hit the existing Gradle
validation issue where release unit-test KSP consumes the shared generated graph
test directory without an explicit task dependency. The authoritative local
release gate does not use that invalid aggregate path and passed. No test was
weakened or skipped to hide the configuration issue.

## Evidence Boundary

This proves compile-time host safety and repository coherence. It does not prove
human preference for the `AfsmDefaultMachine` name.
