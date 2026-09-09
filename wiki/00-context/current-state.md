---
title: Current State
updated: 2026-09-09
---

# Current State

Afsm is an implemented Android/Kotlin FSM toolkit in private internal beta. It
has not been publicly released, so APIs remain redesignable when usability or
safety evidence supports a better shape.

## Product Position

- Afsm helps real Android teams make complex screen flows easier to read,
  verify, and change safely.
- It moves business-flow rules scattered across `ViewModel` state mutations,
  coroutines, callbacks, and tests into one plain Kotlin machine.
- Android `ViewModel` remains the lifecycle, `StateFlow`, `SavedStateHandle`,
  repository/SDK work, and UI adapter.
- Afsm targets meaningful multi-step flows, retries, async result correlation,
  and phase-dependent validity. Simple screens should keep ordinary Android
  state handling when clearer.
- Distribution is Maven Local snapshot or direct project modules. Public
  coordinates, license, signing, and release ownership remain undecided.

## Current Architecture

The public flow vocabulary is:

```text
State = current Phase plus durable Data
Event = user intent or external-work result entering the machine
Command = typed host-work request emitted by an accepted transition
```

- `AfsmMachine<State, Event, Command>` owns transition behavior and topology.
- `AfsmDefaultMachine` adds a genuine static default state.
- Dynamic features such as Checkout supply runtime initial state to
  `afsmHost(machine, initialState)`.
- The pure machine emits command values; `ViewModel` executes them and returns
  typed result events through the command handler's `send` capability.
- Product completion is state. UI-originated UI-only actions are direct UI
  callbacks. Routes may react to durable completion state for navigation.
- There is no Effect type/channel or `afsm-compose` module.

## Implemented Surface

| Surface | Current role |
|---|---|
| `afsm-core` | Pure machine contracts, phase/data DSL, decisions, topology, Mermaid rendering |
| `afsm-runtime` | Serialized bounded event processing, sequential commands, phase-owned invocation cancellation, diagnostics |
| `afsm-viewmodel` | `ViewModel.afsmHost(...)` and `viewModelScope` ownership |
| `afsm-test` | Transition decision/state/command assertion helpers |
| `afsm-graph-ksp` | `@AfsmGraph` discovery and generated registry |
| `io.github.afsm.graph` | Gradle `.mmd` export integration |
| `afsm-ide-plugin` | Separate Android Studio/IntelliJ inspection canvas; parses Afsm-generated MMD, uses ELK Layered for deterministic presentation layout, and renders Java2D without JCEF |
| `sample-shop` | Auth, Checkout, Product Editor reference flows plus ordinary non-Afsm screens |
| `consumer-smoke` | Separate Maven Local Android consumer, behavior tests, and graph generation |
| `docs/index.html` | Static English/Korean documentation hub with installation, Draft quickstart, Android integration, API reference, guides, full-text search with keyboard navigation, persistent light/dark themes, shareable guide sections, mobile navigation, and four user-driven examples with live Event/Data/phase traces |
| `.agents/skills/use-afsm` | English Codex skill for fit assessment, external-project modeling, ViewModel integration, restoration safety, tests, and graph review |

Five library modules use explicit API mode/API validation: `afsm-core`,
`afsm-runtime`, `afsm-test`, `afsm-viewmodel`, and `afsm-graph-ksp`.

## Authoring and Runtime Policy

- DSL vocabulary is `phase`, `on`, `updateData`, phase-only `transitionTo`,
  `command`, conditional `case`, `ignore`, `invalid`, `onEnter`, `onExit`, and
  phase-owned `invoke`.
- `case` requires a condition and is used only for named graph-relevant
  alternatives. Unconditional behavior uses direct statements.
- Missing impossible handlers are invalid by default. `ignore` is reserved for
  expected duplicates and stale results.
- Phase-changing order is `onExit -> branch actions -> target phase factory ->
  onEnter`.
- Initial state construction does not run `onEnter`. Features restore minimal
  safe business state and start work through explicit events only when safe.
- The getting-started path now teaches this as a first-feature stop rule:
  define the safe restored state before restoring any in-flight command phase.
- Checkout restores interrupted payment as
  `PaymentStatusUnknown(requestId)`, never automatic resubmission.
- Events are serialized FIFO. State is published before accepted command work.
  Commands execute sequentially without blocking later event reduction.
- `invoke(key, label)` owns a cooperative long-running job for one phase;
  phase exit and host closure cancel it. Remote work still needs request ids,
  idempotency, or backend cancellation.
- Invalid transitions, reducer failures, unexpected command failures, and queue
  overflow record a diagnostic by default and keep the host usable.
  `AfsmConfig.strict()` selects fail-fast behavior for debug builds and tests.
  Queue capacities default to 64. Diagnostics retain types only unless raw
  values are explicitly enabled.
- A stopped host is observable through `AfsmHost.isActive` and always records a
  `HostStopped` diagnostic. Phase-owned invocations are supervised, so one
  failing invocation cannot cancel siblings or the host.
- `AfsmConfig.commandContext` moves command handler execution off the hosting
  dispatcher when a command calls work that is not main-safe.

## Android Sample Shape

- Sample role files use `*Flow.kt`, not MVI-style `*Contract.kt`.
- Compose screens construct zero Auth, Checkout, or ProductEditor machine Event
  objects.
- ViewModels expose verbs such as `submit()`, `pay()`, `retry()`,
  `updateTitle(value)`, and `cancelUpload()`.
- Auth and Checkout routes react to durable authenticated/completed state.
- Product Editor is documented as the final sample after Checkout, not the
  first feature template; its `invoke` usage is limited to phase-owned local
  cancellable upload work.
- Product Editor Done directly calls the UI callback because publication is
  already the durable result and closing the surface is not a business rule.

## Reading Contract

The phase-local DSL favors exact local rules over one-screen topology scanning.
Three artifacts are therefore one product view:

- generated `.mmd`: whole topology, conditions, and entry work,
- machine: exact data, commands, guards, and ordering,
- tests: payload and graph-invisible Handled/Ignored/Invalid proof.

The graph is generated from the executable machine and is a first-class review
artifact, not manually maintained decoration. Generated diagrams are committed
as a baseline; `verifyAfsmMmd` compares the baseline against the machine on
`check`, so graph drift fails the build instead of being regenerated silently.

## Public Documentation

- The English and Korean root READMEs now lead with the JCEF-free IDE Graph
  Preview and an image rendered by the plugin's visual test. They describe the
  current pre-release Maven Local/source-build boundary instead of claiming an
  unverified remote artifact.
- The bilingual documentation hub is publicly available at
  <https://kez-lab.org/afsm/>.
- GitHub Pages deployment remains available through the manual
  `.github/workflows/pages.yml` dispatch, but PRs and pushes no longer start
  hosted Actions automatically.
- Deployment run `29631426699` published commit `6b900e0` successfully; public
  desktop, 390px mobile, and live Draft Event/Data/Command interaction checks
  pass with zero console warnings or errors.
- HTTPS content is reachable through the custom domain, but GitHub-managed
  `https_enforced` remains false because its Pages certificate was not yet
  available during verification.

## Current Evidence

- Every pull request runs unit tests, `apiCheck`, graph verification, and the
  Maven Local consumer smoke build through `.github/workflows/ci.yml`.
- The full local release gate passes with Gradle 9.1.0 running on the embedded
  JBR 25.0.2, AGP 8.13.2, Kotlin 2.3.21, and KSP 2.3.10 while production
  bytecode remains targeted at JVM 17.
- Core/runtime/ViewModel/test/sample/KSP tests and API checks pass after the
  Effect-free migration.
- Generated Auth, Checkout, and Product Editor graphs match the current
  executable machines.
- A clean separate Android consumer compiles published Maven Local artifacts,
  runs Draft ViewModel/machine and invocation tests, and generates graphs.
- The repo-scoped `use-afsm` skill packages the current Effect-free adoption
  contract and an external implementation pattern without guessing a public
  artifact coordinate.
- The IDE plugin tests, packaging checks, and Plugin Verifier pass for Android
  Studio AI-261 and IntelliJ IDEA IU-261. Android Studio-specific tests execute
  ELK layout, full-width floating canvas chrome, transition direction markers,
  and Java2D rendering without a classpath conflict. The final install/click/
  Refresh/interaction smoke remains a human verification step.
- Prior Android CLI sample evidence proves the dated Product Editor cancellation
  journey, not every later commit.
- One relayed human response identified the Command/Effect vocabulary cost,
  graph-role discoverability problem, and MVI-heavy samples. Metadata is
  insufficient to classify it as a controlled first-use session.
- Prior constrained Checkout review is AI evidence, not human evidence.
- A commit-pinned two-stage Effect-free Checkout input now separates
  machine/graph/tests comprehension from README/Android-boundary review. Its
  manifest and preparation checks pass, but no human has completed it.
- The 2026-07-17 Wiki lint reports zero legacy API hits in canonical current
  pages/public docs, zero ambiguous historical page heads, zero unindexed or
  broken wiki pages, and preserves old APIs only in explicitly historical or
  removal-explanation contexts.

## Goal Status

This redesign cycle is implemented, locally verified, and ready for a
controlled two-stage human review. The long-term product goal is still
incomplete because no controlled Effect-free human result and no
production-like Android pilot exist. The current requirement-by-requirement
proof boundary is recorded in the 2026-07-17 completion audit.

## Canonical References

- [[../01-product/android-fsm-library-strategy|Android FSM Library Strategy]]
- [[../03-engineering/afsm-output-model-simplification|Afsm Output Model Simplification]]
- [[../03-engineering/afsm-v3-executable-dsl|Afsm v3 Executable DSL]]
- [[../03-engineering/afsm-runtime-dispatch-loop|Afsm Runtime Dispatch Loop]]
- [[../03-engineering/afsm-viewmodel-integration|Afsm ViewModel Integration]]
- [[../03-engineering/afsm-ide-graph-preview|Afsm IDE Graph Preview]]
- [[../06-project/long-term-goal|Afsm Long-Term Goal]]
- [[../06-project/goal-completion-audit-2026-07-17|Afsm Goal Completion Audit 2026-07-17]]
- [[open-questions|Open Questions]]
