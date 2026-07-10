---
title: Current State
updated: 2026-07-11
---

# Current State

Afsm is an implemented Android/Kotlin FSM toolkit in private internal beta. It
is no longer a proposal or a project that is merely preparing to build a
library. The current repository contains the runtime, Android integrations,
graph tooling, test helpers, reference app, public documentation, and an
external Maven Local consumer check.

## Product Position

- Afsm's primary product goal is to turn implicit business-flow state changes
  scattered across complex Android ViewModels into explicit `Phase`/`Event`
  rules that can be read, tested, graphed, and verified from one executable
  machine definition.
- Afsm targets complex Android transaction and multi-step flows with meaningful
  phases, async results, retries, and invalid transitions.
- It is not a general `ViewModel` replacement and should not be added to simple
  data-display or loading/content/error screens when `ViewModel + StateFlow` is
  clearer.
- The current audience is controlled internal beta pilots. No public artifact
  has been released, so every API, DSL term, module boundary, sample, and test
  fixture remains redesignable. Usability, readability, safety, and Android fit
  take priority over compatibility with the current implementation.
- The source repository is the private GitHub repository `kez-lab/afsm`.
- Current distribution is Maven Local snapshot or direct project-module use.
  Stable OSS/Maven Central publication is not configured.

## Architecture Contract

- A feature machine is plain Kotlin and owns deterministic flow rules.
- Graphable feature code uses
  `AfsmMachine<State, Event, Command, Effect>`. Static flows use the
  `AfsmDefaultMachine` subtype with a genuine default state; dynamic flows use
  `afsmMachine(initialPhase = ...)` and require host-supplied runtime state.
- `AfsmState<Phase, Data>` separates finite business phases from durable data.
- `Event` represents user input or an async result. `Command` represents
  host-executed work. `Effect` is optional best-effort UI output.
- Android `ViewModel` remains the lifecycle adapter and owns `StateFlow`,
  `viewModelScope`, `SavedStateHandle` conversion, command execution, and the
  bridge from UI events to the machine.
- Required product progress belongs in state. Navigation, snackbar, focus,
  scroll, and animation behavior stays in the UI unless it changes business
  flow; lifecycle-sensitive UI work uses state plus acknowledgement when it
  must survive a collection gap.
- Afsm is recommended only when the state diagram improves flow readability or
  verification. Ordinary ViewModel state remains the preferred anti-example
  for simple screens.

## Implemented Surface

| Surface | Current role |
|---|---|
| `afsm-core` | Pure Kotlin reducer/machine contracts, executable phase/data DSL, decisions, topology, and Mermaid output |
| `afsm-runtime` | Serialized bounded event processing, sequential command execution that does not block later reduction, effects, diagnostics, and host policies |
| `afsm-viewmodel` | `ViewModel.afsmHost(...)` integration using `viewModelScope`, including explicit initial-state overloads |
| `afsm-compose` | Lifecycle-aware `CollectAfsmEffects(...)` route helper |
| `afsm-test` | Kotlin-only transition assertion helpers |
| `afsm-graph-ksp` | `@AfsmGraph` discovery for stable top-level machine properties and eligible classes/objects, plus generated graph registry |
| `io.github.afsm.graph` | Included-build Gradle plugin that wires KSP and `generateAfsmMmd` for one selected Android unit-test variant |
| `sample-shop` | Compose + Room reference app using Afsm for Auth, Checkout, and cancellable ProductEditor upload while leaving simple data screens on ordinary ViewModels |
| `consumer-smoke` | Separate Android Gradle build that consumes Maven Local artifacts, mirrors Draft, verifies diagnostics/invocation behavior, runs machine/ViewModel tests, and exports a graph |

The six library modules tracked by explicit API mode and binary API validation
are `afsm-core`, `afsm-runtime`, `afsm-test`, `afsm-viewmodel`, `afsm-compose`,
and `afsm-graph-ksp`. `sample-shop` is intentionally excluded from API dumps.

## Authoring and Runtime Policy

- The canonical DSL vocabulary is `phase`, `on`, named `case`, phase-only
  `transitionTo`, `updateData`, `onEnter`, `onExit`, `command`, phase-owned
  `invoke`, `effect`, `ignore`, and `invalid`.
- A 2026-07-10 first-use experiment rejected partial generic calls and inferred
  generic feature superclasses, then implemented the smallest viable shape:
  graphable features expose one explicitly typed lower-camel top-level `val`
  machine.
  Draft, Auth, Checkout, ProductEditor, and consumer fixtures no longer need a
  machine alias, delegated object, or factory. Fresh human preference remains
  unverified, so this is a pre-release authoring candidate rather than an API
  freeze.
- Checkout no longer declares placeholder `productId = 0` data. Its base
  `AfsmMachine` has graph initial phase `Idle` but no default state, so the
  ViewModel must call the explicit-state host overload at compile time.
- `case(condition = ...)` and payload phase factories are read-only scopes.
- A handled event stays in its phase by omitting `transitionTo(...)`.
  `ignore(...)` is reserved for expected harmless no-ops; omitted impossible
  handlers are invalid by default.
- Phase-changing execution order is
  `onExit -> case actions -> target phase factory -> onEnter`.
- Initial state construction does not run `onEnter`. Restoration reconstructs
  minimal stable state and deliberately starts work through an event only when
  safe. Checkout restores unresolved payment to `PaymentStatusUnknown` rather
  than retrying automatically.
- `AfsmInvalidTransitionPolicy.Throw` and
  `AfsmCommandFailurePolicy.Throw` are the defaults. Event and command queues
  default to capacity `64` and fail fast on overflow.
- Ordinary commands execute sequentially on a separate processor, so a
  suspended command does not block later event reduction. Long-running work
  owned by one phase can use `onEnter { invoke(key, label) { command } }`; the
  runtime tracks it separately and cancels its cooperative coroutine on phase
  exit or host closure. Cancelled invocation callbacks cannot dispatch late
  results. Request ids and idempotency remain required for remote or
  non-cooperative work.
- Effects have no replay by default. Late collectors do not receive old effects.
- Runtime diagnostics are types-only by default. They expose stable codes,
  decision categories, fixed messages, type names, and Afsm-owned metadata.
  Raw state/event/command/reason/throwable values require explicit
  `AfsmDiagnosticDataPolicy.IncludeValues` and grouped `diagnostic.values`
  access.
- ProductEditor phase-owned cancellation is proven against the runtime and a
  ViewModel delay, but the Android adapter still hardcodes that delay. A
  feature-owned suspend uploader is the selected next experiment so repository
  cancellation/failure mapping can be tested without timing races.

## Examples and Documentation

The supported learning order is:

1. Draft in `docs/getting-started.md` and `consumer-smoke`.
2. Auth as the first real Android form screen.
3. Checkout for dynamic initial state, loading, retry, request ids, stale result
   handling, durable completion, process restoration, and optional navigation
   effect.
4. ProductEditor as the advanced graph, transition-order, and phase-owned upload
   cancellation stress test.
5. Ordinary catalog/detail/like/review screens as examples where Afsm is not
   needed.

A 2026-07-10 constrained Checkout review could reconstruct the main path,
recovery, request-id safety, commands, and completion from only the machine,
generated graph, and transition tests. Focused tests now cover the remaining
graph-invisible handled/ignored/invalid policies, so the repository-based review
passes. Real Android developer comprehension and preference remain unverified.

`README.md` is the quick map. `docs/getting-started.md` is the first-use
copy/paste source and is mirrored by the external consumer fixture.

## Verification and Distribution

- Current coordinates are `io.github.afsm:*:0.1.0-SNAPSHOT`; the group id is
  explicitly temporary.
- `scripts/verify-release-local.sh` is the authoritative local release gate. It
  runs graph plugin tests, module and sample tests, graph generation, `apiCheck`,
  Maven Local publication, and the clean external consumer smoke build.
- The full local release gate passed on 2026-07-11 after the diagnostic privacy
  and phase-owned invocation redesigns. The known Kotlin Gradle
  plugin POM rewriting deprecation warning remains non-blocking.
- Hosted GitHub Actions CI was removed for cost control. No
  `.github/workflows/ci.yml` exists; maintainers run the relevant local checks
  before merge and the full local gate for release-facing work.
- Historical Android CLI layout/screenshot evidence remains under
  `raw/verification/`; it proves the dated sample journeys it records, not the
  state of every later commit.
- A no-coaching Checkout first-use task and facilitator rubric are ready, but no
  real Android developer session has been recorded yet. The post-restoration
  facilitator setup check passes; provisional time and score gates remain
  assumptions until the first human run.
- `CheckoutViewModelTest` now proves dynamic product id, product loading,
  repository command-result events, session failure, durable completion, and
  active effect delivery through the real sample ViewModel and production
  repositories over fake DAO boundaries.
- Checkout now implements representative feature-owned process restoration with
  minimal stable/pending `SavedStateHandle` keys and an explicit
  `PaymentStatusUnknown` phase instead of serializing the full machine state or
  silently retrying interrupted payment work. JVM, graph, APK assemble, and full
  release gates pass; on-device launch is unverified because Android CLI could
  not discover the booted emulator.

## Remaining Decisions

The unresolved release, module-boundary, advanced-runtime, restoration-helper,
and graph-aggregation decisions are maintained in
[[open-questions|Open Questions]]. Public release gates and metadata blockers
are maintained in `docs/release-readiness.md`.

## Canonical References

- [[../01-product/android-fsm-library-strategy|Android FSM Library Strategy]]
- [[../03-engineering/android-fsm-architecture|Android FSM Architecture]]
- [[../03-engineering/android-official-guidance|Android Official Guidance]]
- [[../03-engineering/afsm-v3-executable-dsl|Afsm v3 Executable DSL]]
- [[../03-engineering/afsm-example-catalog|Afsm Example Catalog]]
- [[../03-engineering/testing-strategy|Testing Strategy]]
- [[../07-llm/ai-engineering-guardrails|AI Engineering Guardrails]]
- [[../07-llm/codex-project-workflow|Codex Project Workflow]]

## Source Evidence

- [Android ViewModel FSM Discussion](../../raw/conversations/2026-05-01-android-viewmodel-fsm-discussion.md)
- [Android Official Docs Research](../../raw/sources/2026-05-01-android-official-docs-fsm-research.md)
- [LLM Wiki Pattern](../../raw/sources/2026-05-01-llm-wiki-pattern.md)
