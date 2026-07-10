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
- `AfsmDecision` with `Transitioned`, `Handled`, `Ignored`, and `Invalid`.
- `AfsmNoCommand` marker for machines that do not emit host-executed work.
- `AfsmNoEffect` marker for machines that do not emit UI-side effects.
- `AfsmMachine<S, E, C, F>` graphable transition/topology boundary without an
  assumed default state.
- `AfsmDefaultMachine<S, E, C, F>` for static flows with a genuine default
  state and the concise ViewModel host overload.
- `AfsmState<P, D>` phase/data state model.
- `afsmMachine { ... }` executable DSL that returns
  `AfsmDefaultMachine<AfsmState<Phase, Data>, Event, Command, Effect>` with
  topology metadata.
- `afsmMachine(initialPhase = ...) { ... }` for graphable dynamic flows whose
  host must supply runtime data.
- DSL helpers including `initial`, `phase`, `on`, `case`, `transitionTo`,
  `ignore`, `invalid`, `onEnter`, `onExit`, `updateData`, `command`, and
  `effect`.
- `AfsmTopology`, `AfsmTopologyTransition`, `AfsmMmdOptions`, and Mermaid `.mmd` export support.
- `@AfsmGraph`, `AfsmGraphSource`, `AfsmGraphRegistry`, and `AfsmMmdWriter`.
- `afsm-runtime` coroutine host with serialized event dispatch.
- `afsm-test` Kotlin test assertion helpers for Afsm transition behavior.
- Sequential command execution that does not block later event reduction.
- `AfsmHost.tryDispatch(event)` for non-throwing event queue attempts.
- Bounded default event queue capacity through `AfsmConfig.eventQueueCapacity`.
- Bounded default command queue capacity through `AfsmConfig.commandQueueCapacity`.
- Configurable invalid transition and command failure policies.
- Best-effort one-shot effect flow delivery.
- `afsm-viewmodel` with `ViewModel.afsmHost(...)`, including machine and dynamic initial state overloads.
- `afsm-compose` with `CollectAfsmEffects(...)`.
- `afsm-graph-ksp` KSP processor for automatic graph registry generation.
- `@AfsmGraph` discovery for stable top-level machine properties, allowing
  feature declarations to avoid delegated class/object wrappers and factories.
- `io.github.afsm.graph` Gradle plugin that wires KSP graph export and registers `generateAfsmMmd`.
- `phase(phase)` DSL convenience for terminal or marker phases with no handlers.
- `sample-shop` Android reference app using Afsm for auth, product editor, and checkout flows.
- Checkout ViewModel integration fixtures for dynamic navigation state,
  production repository command-result wiring, durable completion, and active
  effect delivery.
- `consumer-smoke` external Android build that verifies Maven Local consumption.
- Public example documentation for Auth, Checkout, and ProductEditor walkthroughs.
- External app-module graph generation setup guide.
- Maven Local publication for `afsm-core`, `afsm-runtime`, `afsm-test`, `afsm-viewmodel`, `afsm-compose`, `afsm-graph-ksp`, and the Afsm graph Gradle plugin.
- Kotlin explicit API mode for public library modules.
- Binary API validation baseline for public library modules.

### Removed

- Pre-release compatibility aliases before first publication:
  - `AfsmStateMachine`
  - `AfsmStateChart`
  - `afsmStateChart`
  - `AfsmStateChartMachine`
  - `AfsmChartState`
  - `AfsmGraphReducer`
- Superseded pre-release DSL names before first publication:
  - `AfsmDecision.Stayed`
  - `AfsmTransition.stayed(...)`
  - `AfsmPhaseMachine`
  - `state(...)`
  - `updateContext(...)`
  - `AfsmState.context`
  - `stay(...)`
  - `otherwise(...)`
- Temporary `AfsmMachineAdapter` base before first publication; graphable
  machines now use `AfsmState<Phase, Data>` directly.
- Hosted GitHub Actions CI workflow after the cost-control decision; local
  verification through `scripts/verify-release-local.sh` remains the release
  gate.

### Known Issues

- `publishToMavenLocal --warning-mode all` reports a Kotlin Gradle plugin POM rewriting deprecation warning for a project dependency. Afsm does not call the deprecated Gradle API directly.
- Final remote publishing metadata is not configured yet. License, final coordinates, SCM metadata, signing, and repository target remain product decisions.
