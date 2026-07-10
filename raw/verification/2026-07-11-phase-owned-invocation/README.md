# Phase-Owned Invocation Verification

Date: 2026-07-11

Specification commit: `8146e65`

Implementation commit: `b0fd60a`

## Contradiction Found

The previous public guidance said to emit a cancel command from `onExit`, but
`AfsmHost` executes ordinary commands in one sequential processor. A cancel
command emitted after a suspended upload therefore waits until the upload has
already finished. The existing cleanup-command test did not exercise active
work interruption.

## Red Evidence

Core and runtime tests were added before production changes. The first focused
run failed compilation because these accepted contract elements did not exist:

- `AfsmInvocationKey`,
- `AfsmCommandInvocation.Start/Cancel`,
- `AfsmTransition.commandInvocations`,
- `onEnter { invoke(...) }`,
- low-level `commandInvocations` transition output.

Two fixture-only corrections followed: reified DSL events and phase labels use
sealed `data object` types rather than enum entries. No accepted behavior was
weakened.

## Implemented Contract

- Ordinary `command(...)` output remains sequential and bounded.
- `onEnter { invoke(key, label) { command } }` starts phase-owned work in a
  tracked child job.
- Exiting the owning phase emits `Cancel(key)` before target-phase invocation
  starts.
- Cooperative cancellation is not a command failure diagnostic.
- A cancelled invocation cannot use the Afsm-owned callback to dispatch a late
  result, including from a `NonCancellable` cleanup block.
- Host closure cancels active invocation jobs.
- Duplicate keys in one phase fail machine definition validation.
- ProductEditor image upload exposes a real cancel action and returns to
  `EditingDraft` without a ViewModel `Job` registry or cancel command.
- The generated graph shows invocation entry, automatic exit cancellation, and
  the `CancelUploadClicked` edge.

The exact generated Flow graph is preserved beside this report as
`ProductEditorStateMachine.mmd`.

## Passing Verification

```bash
./gradlew :afsm-core:test :afsm-runtime:test :afsm-test:test \
  :sample-shop:testDebugUnitTest --no-daemon
./gradlew :sample-shop:generateAfsmMmd --no-daemon
./gradlew :afsm-core:apiCheck :afsm-runtime:apiCheck \
  :afsm-test:apiCheck --no-daemon
./scripts/verify-release-local.sh --no-daemon
```

Result: passed, including API validation, ProductEditor ViewModel cancellation,
Maven Local publication, and a clean external Android consumer test that uses
the published invocation DSL, runtime, and test helper.

One combined `apiDump + apiCheck` invocation failed Gradle task validation
because the plugin does not declare that mixed task dependency. Running dump
and check in separate invocations passed; this was classified as a harness
usage issue, not an API failure.

## Evidence Boundary

This proves local cooperative coroutine cancellation. It does not prove a
remote server, payment SDK, callback API, or blocking call stopped work.
Cancellation is requested without joining non-cancellable cleanup, and
invocation jobs are not counted against the ordinary command queue capacity.
Request ids, idempotency, and application-specific cancellation contracts
remain required when work can outlive the local coroutine.
