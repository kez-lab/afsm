# Changelog

All notable Afsm changes are documented here. Afsm is pre-release; `0.x` APIs
may change, but every public API change must update API dumps, docs, examples,
and verification in the same change.

## Unreleased

### Build and tooling

- Upgraded the build to Gradle 9.1.0, Kotlin 2.3.21, KSP 2.3.10, and Android
  Gradle Plugin 8.13.2 so Gradle can run on Android Studio's embedded JBR 25.
  Kotlin and Java bytecode targets remain JVM 17.
- Fixed the graph plugin's default KSP processor dependency registration so it
  reaches Android variant processor classpaths with KSP 2.3.

## 0.1.0 - 2026-08-09

Initial internal-beta candidate.

The source and Maven Local candidate use Apache-2.0. Remote publication was not
completed for this version.

### Runtime resilience

- `AfsmConfig` now records failures by default. `invalidTransitionPolicy` and
  `commandFailurePolicy` default to `Record`, so an invalid transition or an
  unmodelled command failure no longer stops the host and no longer reaches an
  Android `viewModelScope` as an uncaught exception.
- `AfsmConfig.strict()` selects the previous fail-fast behavior for debug builds
  and tests.
- Added `AfsmOverflowPolicy` (default `Record`): a full event or command queue
  drops the rejected work with a diagnostic instead of stopping the host.
- Reducer exceptions are caught and reported as
  `AfsmDiagnosticCode.ReducerFailure` instead of ending event processing.
- Phase-owned invocations run under a `SupervisorJob`, so one failing invocation
  no longer cancels sibling invocations or the host.
- A duplicate active invocation key cancels the stale invocation and records
  `AfsmDiagnosticCode.DuplicateInvocationKey` instead of throwing.
- Added `AfsmHost.isActive` and the `AfsmDiagnosticCode.HostStopped` diagnostic,
  so a stopped host is never silent.
- Added `AfsmConfig.commandContext` for command handlers that call work which is
  not main-safe.
- `AfsmDiagnostic.decision` and `AfsmDiagnostic.eventType` are nullable for
  diagnostics that are not tied to one reduced event.

### Graph verification

- Added `verifyAfsmMmd` and `updateAfsmMmd` Gradle tasks and the
  `afsmGraph.checkedInDir` baseline. `verifyAfsmMmd` joins `check` when a
  baseline exists, so a changed machine with a stale committed diagram fails the
  build with a diff.
- `generateAfsmMmd` is a `JavaExec` task running `afsm.core.AfsmMmdExport`. The
  plugin no longer injects a JUnit 4 dependency, no longer generates a test class
  into the consuming project's test source set, and no longer reflects into the
  Android Gradle plugin DSL.
- Committed graph baselines for `sample-shop` and `consumer-smoke`.

### Definition validation

- Enum phases are labelled by entry name. Previously every entry of one enum
  collapsed into a single label and the machine failed to build.
- An `on<Event>` handler that can never run because an earlier handler in the
  same phase matches a supertype now fails the build.

### Project

- Added a CI workflow running unit tests, `apiCheck`, graph verification, and the
  Maven Local consumer smoke build on every pull request.

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
