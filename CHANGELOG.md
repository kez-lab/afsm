# Changelog

All notable Afsm changes are documented here. Afsm is pre-release; `0.x` APIs
may change, but every public API change must update API dumps, docs, examples,
and verification in the same change.

## 0.1.0 - Unreleased

Initial internal-beta candidate.

### Added

- `afsm-core` pure Kotlin `State`, `Event`, and `Command` machine model.
- `AfsmState<Phase, Data>` and executable `afsmMachine { ... }` DSL.
- `AfsmMachine<S, E, C>` for runtime-supplied initial state and
  `AfsmDefaultMachine<S, E, C>` for static defaults.
- Phase-local `on`, `updateData`, `transitionTo`, `command`, `case`, `ignore`,
  `invalid`, `onEnter`, and `onExit` APIs.
- `AfsmInvocationKey`, `invoke`, and phase-owned command cancellation.
- `AfsmDecision` with `Transitioned`, `Handled`, `Ignored`, and `Invalid`.
- `AfsmNoCommand` for machines without external work.
- `afsm-runtime` with serialized FIFO event processing, bounded queues,
  sequential command execution, failure policies, and privacy-safe diagnostics.
- `afsm-viewmodel` with default and dynamic-initial-state `afsmHost` overloads.
- `afsm-test` transition assertions.
- Generated Mermaid topology through `@AfsmGraph`, KSP registry generation, and
  the `io.github.afsm.graph` Gradle plugin.
- Auth, Checkout, and Product Editor reference flows in `sample-shop`.
- External Maven Local consumer smoke build and binary API validation.

### Changed

- Public machine vocabulary was reduced to `State`, `Event`, and `Command`.
- Android sample UI now calls verb-named ViewModel methods instead of exposing a
  generic `onEvent(Event)` MVI boundary.
- Sample role files were renamed from `*Contract.kt` to `*Flow.kt`.
- Auth and Checkout navigation now derives from durable completion state.
- Product Editor Done is a direct UI callback because it does not change
  business flow.
- `case` requires a condition and is reserved for named conditional branches;
  unconditional rules use direct DSL statements.
- `AfsmCommandHandler` calls its result capability `dispatchEvent`.

### Removed

- The pre-release `Effect` generic, DSL operation, transition output, runtime
  stream, buffering policy, assertion helpers, and marker type.
- `afsm-compose`; ordinary Compose state observation now covers the supported
  UI integration model.
- Superseded pre-release aliases and DSL names before first publication.
