# Changelog

All notable Afsm changes will be documented here.

This project follows pre-1.0 semantic versioning discipline:

- `0.x` versions may still change public API.
- Every public API change must be reflected in binary API dumps.
- Breaking changes after the first public artifact require an explicit release note.

## 0.1.0 - Unreleased

Initial pre-release candidate.

### Added

- `afsm-core` pure Kotlin module.
- `AfsmReducer<S, E, C, F>` low-level transition contract.
- `AfsmTransition<S, C, F>` with state, commands, effects, and decision.
- `AfsmDecision` with `Transitioned`, `Stayed`, `Ignored`, and `Invalid`.
- `AfsmNoEffect` marker for machines that do not emit UI-side effects.
- `AfsmState<P, X>` phase/context state model.
- `AfsmMachine<P, X, E, C, F>` executable DSL machine with topology metadata.
- `afsmMachine { ... }` DSL with `initial`, `state`, `on`, `transitionTo`, `stay`, `otherwise`, `ignore`, `invalid`, `onEnter`, `onExit`, `updateContext`, `command`, and `effect`.
- `AfsmTopology`, `AfsmTopologyTransition`, and Mermaid `.mmd` export support.
- `@AfsmGraph`, `AfsmGraphSource`, `AfsmGraphRegistry`, and `AfsmMmdWriter`.
- `afsm-runtime` coroutine host with serialized event dispatch.
- Sequential command execution and command-result event dispatch.
- Configurable invalid transition and command failure policies.
- Best-effort one-shot effect flow delivery.
- `afsm-viewmodel` with `ViewModel.afsmHost(...)`.
- `afsm-graph-ksp` KSP processor for automatic graph registry generation.
- `sample-shop` Android reference app using Afsm for auth, product editor, and checkout flows.
- `consumer-smoke` external Android build that verifies Maven Local consumption.
- Maven Local publication for `afsm-core`, `afsm-runtime`, `afsm-viewmodel`, and `afsm-graph-ksp`.
- Kotlin explicit API mode for public library modules.
- Binary API validation baseline for public library modules.

### Removed

- Pre-release compatibility aliases before first publication:
  - `AfsmStateMachine`
  - `AfsmStateChart`
  - `afsmStateChart`
  - `AfsmStateChartMachine`
  - `AfsmChartState`

### Known Issues

- `publishToMavenLocal --warning-mode all` reports a Kotlin Gradle plugin POM rewriting deprecation warning for a project dependency. Afsm does not call the deprecated Gradle API directly.
- Final remote publishing metadata is not configured yet. License, final coordinates, SCM metadata, signing, and repository target remain product decisions.
