# Afsm Current Contract

This reference describes the repository's current internal-beta contract. Afsm
is not publicly released. Verify the consuming project's exact version and use
matching source, docs, tests, and API dumps when they are available.

## Contents

1. [Fit](#fit)
2. [Vocabulary](#vocabulary)
3. [Layer boundaries](#layer-boundaries)
4. [DSL rules](#dsl-rules)
5. [Runtime and safety](#runtime-and-safety)
6. [Restoration](#restoration)
7. [Graph and tests](#graph-and-tests)
8. [Review checklist](#review-checklist)

## Fit

Use Afsm for complex feature-local business flows with meaningful phases,
branching validity, retries, async result correlation, or restoration rules.
Prefer ordinary Android state production for simple loading/content/error or
form screens when it is easier to read.

Afsm is not a `ViewModel` replacement and does not require app-wide MVI.

## Vocabulary

| Term | Contract |
|---|---|
| `State` | Current `Phase` plus durable business `Data` |
| `Phase` | The active business step |
| `Data` | Values that survive across phases |
| `Event` | User intent or an external-work result entering the machine |
| `Command` | Typed work that the Android host executes |

Use a phase payload only when a value exists because that phase exists, such as
`PaymentInProgress(requestId)`. Keep shared product data in `Data` instead of
duplicating it across phase constructors.

There is no Effect type, effect DSL operation, effect stream, or Compose effect
module. Durable completion belongs in state. UI-originated UI-only behavior
stays a direct UI callback.

## Layer Boundaries

| Layer | Owns |
|---|---|
| Pure machine | validity, phase/data changes, commands, duplicate/stale/invalid policy |
| `ViewModel` | `StateFlow`, `viewModelScope`, dependencies, command execution, result events, `SavedStateHandle`, UI verbs |
| Compose/UI | lifecycle-aware collection, rendering, navigation callbacks, focus, scroll, animation, sheets, snackbar state |

Do not place Android objects, repositories, suspend work, or coroutine scopes in
machine state or reducer rules.

## DSL Rules

- Use `initial(phase, data)` only for a genuine static default.
- Use a dynamic `AfsmMachine` plus explicit host `initialState` when runtime
  input or restoration supplies state.
- Write unconditional `updateData`, `command`, and `transitionTo` statements
  directly inside `on<Event>`.
- Use `case(label, condition)` only for named graph-relevant alternatives.
- Do not mix unconditional direct actions with `case`, `ignore`, or `invalid`
  decisions inside the same event handler.
- Use `ignore` for expected duplicates and stale results, not to enumerate
  every impossible event.
- Missing handlers are invalid by default. Use `invalid` when an explicit
  rejection reason is useful.
- Prefer an `onEnter` command when entering a phase means host work starts.
- The phase-changing order is `onExit`, branch actions, target phase factory,
  then `onEnter`.
- Initial-state construction does not run `onEnter`.

Decision meanings:

| Decision | Meaning |
|---|---|
| `Transitioned` | Accepted rule changed phase |
| `Handled` | Accepted rule stayed in phase and may update data or emit work |
| `Ignored` | Recognized event intentionally did nothing |
| `Invalid` | No valid rule exists, or a rule explicitly rejected the event |

## Runtime and Safety

- Events are reduced serially in FIFO order.
- Accepted state is published before command work is scheduled.
- Ordinary commands execute sequentially without blocking later event
  reduction.
- Invalid transitions, reducer failures, unmodelled command failures, and queue
  overflow record a diagnostic and keep the host usable by default. Configure
  `AfsmConfig.logger`, or those failures are silent. Use `AfsmConfig.strict()`
  in debug builds and tests to fail fast instead.
- `AfsmHost.isActive` reports a stopped host, and a host stopped by a throwing
  policy records a `HostStopped` diagnostic.
- Command handlers run on the hosting scope. Set `AfsmConfig.commandContext`
  when a command calls work that is not main-safe.
- Event and command queues are bounded; do not assume dispatch always succeeds
  when using `tryDispatch`.
- Diagnostics retain types only by default. Raw values are an explicit privacy
  risk.
- `invoke(key, label)` owns cooperative local work for one phase; phase exit and
  host closure cancel it.
- Local cancellation does not guarantee remote cancellation. Use request ids,
  idempotency, backend cancellation, or stale-result rejection as appropriate.

## Restoration

Restore minimal stable business state, not in-flight jobs. Do not resubmit
payment, upload, or equivalent unsafe work merely because the process was
recreated.

Convert `SavedStateHandle` into an explicit initial state in the `ViewModel`.
Use a safe recovery phase such as an unknown-status state when external work may
have completed remotely.

## Graph and Tests

Treat three artifacts as one review surface:

- generated `.mmd`: whole topology, named conditions, and phase work,
- machine: exact guards, data changes, commands, and ordering,
- tests: payload details plus `Handled`, `Ignored`, and `Invalid` proof.

The graph cannot replace transition tests because it intentionally omits some
no-op and rejection behavior.

## Review Checklist

- Afsm is justified by flow complexity.
- Existing behavior is inventoried and preserved unless intentionally changed.
- State contains business data, not Android/UI objects.
- Commands describe external work without executing it.
- Async results are correlated and stale results are tested.
- UI calls ViewModel verbs instead of constructing events.
- Restoration cannot restart unsafe work.
- Graph, machine, and tests agree.
- Afsm core, runtime, ViewModel, test, KSP, and plugin versions match.
