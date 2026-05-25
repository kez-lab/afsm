---
title: Current State
updated: 2026-05-25
---

# Current State

This project is preparing to build an Android-specific FSM library.

The current direction is:

- Treat the final deliverable as a reusable Android library, not only an app-local convention.
- Keep Android `ViewModel`.
- Make complex screen flows explicit with plain Kotlin finite state machines.
- Use the `ViewModel` as the Android lifecycle and UI integration layer, not as the place where all transition rules live.
- Store durable architecture reasoning in this LLM Wiki so future Codex sessions can continue without rediscovering the same context.
- Use official Android UI layer, ViewModel, Compose state, coroutine, Flow testing, and saved state guidance as constraints for the FSM design.
- A minimal Kotlin/JVM `afsm-core` project now exists and compiles with the v2 core API shape.
- `AfsmNoEffect` and `AfsmTransition<S, C, F>` were validated through compile-time signup/login reference usage.
- A minimal coroutine-based `afsm-runtime` module now exists.
- `AfsmHost` serializes non-suspending `dispatch(event)` calls through a bounded FIFO event queue, exposes `StateFlow<S>` state, exposes best-effort `Flow<F>` effects, and executes commands sequentially on a separate command processor so suspended commands do not block later event reduction.
- Project-scoped AI engineering guardrails now require spec-first/TDD-oriented work and prohibit weakening tests merely to make implementation pass.
- A thin AndroidX `afsm-viewmodel` module now exists with `ViewModel.afsmHost(...)`, wiring `AfsmHost` to `viewModelScope`; graphable machines can use `afsmHost(machine = StateMachine, ...)`, while navigation-argument screens can use `afsmHost(machine = StateMachine, initialState = ...)`.
- A thin `afsm-compose` module now exists with `CollectAfsmEffects(...)` for lifecycle-aware one-shot effect collection in Compose routes.
- A `:sample-shop` Android app module now exists to validate Afsm in a realistic Compose + Room shopping app.
- The sample app uses Afsm for auth, product registration review/publish, and checkout retry flows, while keeping product list/detail/likes/reviews on ordinary ViewModel + Flow to avoid unnecessary FSM ceremony.
- Public sample documentation now lives in `docs/sample-shop-afsm-guide.md`.
- Auth now uses the executable DSL directly with `typealias AuthState = AfsmState<AuthPhase, AuthData>`.
- Product registration is now the stronger FSM reference flow: draft editing, mock image upload, review rejection, resubmission, approval, publishing, and close effect.
- Android CLI smoke verification passed for signup and product registration, with layout/screenshot evidence under `raw/verification/2026-05-09-sample-shop-fsm-smoke/`.
- The canonical v3 API direction is now a scoped executable machine DSL: `phase`, `on`, named `case`, phase-only `transitionTo`, `updateData`, `onEnter`, `onExit`, `command`, and `effect` in one machine definition.
- Afsm DSL public KDoc now explains phase/data/event/command/effect type parameters, runtime parameters, topology-only metadata parameters, condition behavior, branch ordering, and entry/exit execution order.
- `afsm-core` now distinguishes `AfsmReducer<S, E, C, F>` as the low-level host-facing contract and `AfsmMachine<S, E, C, F>` as the graphable feature-boundary machine. The DSL builder `afsmMachine<P, D, E, C, F> { ... }` returns `AfsmMachine<AfsmState<P, D>, E, C, F>` directly.
- The pre-release `AfsmGraphReducer` name was removed from the public API before release docs; new graphable code should use `AfsmMachine<State, Event, Command, Effect>`.
- Deprecated pre-release aliases and the temporary `AfsmMachineAdapter` base were removed from the public source surface. New graphable code uses `AfsmReducer`, `AfsmMachine`, `afsmMachine`, and `AfsmState`.
- The executable DSL spike passes ProductEditor-like core tests for phase transitions, data updates, `onExit -> transition -> onEnter` ordering, typed payload phases, named conditions, DSL build validation, and UI-side effect emission.
- The executable DSL now exposes `AfsmMachine.topology` plus `AfsmTopology.toMmd()`; event branches are declared with graphable named `case(...)` blocks and phase-only `transitionTo(...)`.
- `AfsmState<P, D>` is now the standard phase/data state value. It exposes `phase` and `data`; `Data` intentionally avoids the Android `Context` naming collision.
- Root `README.md` and `docs/afsm-public-api.md` now document only the current public API names.
- Maven local publishing now works for `afsm-core`, `afsm-runtime`, `afsm-viewmodel`, `afsm-compose`, `afsm-graph-ksp`, and the `io.github.afsm.graph` Gradle plugin using `io.github.afsm:*:0.1.0-SNAPSHOT` pre-release coordinates.
- `consumer-smoke` now exists as a separate Android Gradle build that consumes those Maven Local coordinates, compiles a ViewModel-hosted Afsm machine, applies the graph Gradle plugin, runs the KSP graph processor, and generates `.mmd` output without project-module dependencies.
- The Draft quickstart machine and ViewModel from `docs/getting-started.md`
  are mirrored in `consumer-smoke`, so the first-use documentation compiles
  against Maven Local artifacts and exports a graph during release verification.
- `docs/release-readiness.md` now defines the local release gate, remaining product decisions, engineering gates, and the known Kotlin Gradle plugin POM deprecation warning.
- Kotlin explicit API mode is enabled for `afsm-core`, `afsm-runtime`, `afsm-viewmodel`, `afsm-compose`, and `afsm-graph-ksp` so public declarations must be intentional.
- Binary API validation is enabled with API dumps for the five Afsm library modules; `sample-shop` is excluded because it is a sample app, not published API.
- `CHANGELOG.md` now contains the initial `0.1.0 - Unreleased` release notes
  using current `phase`, `data`, and `Handled` vocabulary while documenting
  superseded pre-release names only under removed aliases.
- `CONTRIBUTING.md` now captures the project development flow, test-integrity rules, public API change policy, and local release gate.
- `scripts/verify-release-local.sh` now runs the full local release gate, including tests, sample graph generation, `apiCheck`, Maven Local publication, and external consumer smoke.
- Maven Local generated POMs now have documented metadata audit status: packaging, internal dependency coordinates, names/descriptions are present; URL, license, SCM, and developer metadata remain product-owned decisions.
- The graph-generation slice now exists: annotated `StateMachine` classes implement `AfsmGraphSource`, `afsm-graph-ksp` generates `AfsmGeneratedGraphRegistry`, the `io.github.afsm.graph` Gradle plugin generates a JUnit4-compatible export test, and `generateAfsmMmd` writes one `.mmd` per registry entry without running the app's whole unit-test suite.
- Graph tooling verification now includes KSP functional tests for registry generation and invalid annotation diagnostics, plus Gradle TestKit functional tests for Android app/library plugin usage, KSP-missing failure messaging, ordinary unit-test separation, and no-registry `generateAfsmMmd` failure messaging.
- The graph Gradle plugin now generates its default `afsm-graph-ksp` processor dependency from the shared Afsm version, and `consumer-smoke` receives the same `afsmVersion` from the root verification script so local release checks do not accidentally use stale Maven Local artifacts.
- `consumer-smoke` now runs with dependency refresh and a clean fixture build so generated `.mmd` validation cannot pass on stale build outputs.
- Command result events now fail fast with `AfsmEventQueueOverflowException` if a full bounded event queue rejects them, while command results after host close are dropped and logged as lifecycle completion.
- `AuthStateMachine`, `CheckoutStateMachine`, and `ProductEditorStateMachine` are annotated graph sources; `generateAfsmMmd` writes `AuthStateMachine.mmd`, `CheckoutStateMachine.mmd`, and `ProductEditorStateMachine.mmd`.
- The phased-state helper spike has been removed from `afsm-core`; it remains only as superseded design history.
- Afsm terminology now consistently treats `Command` as host-executed work emitted by the machine and executed by the host, not as another input event; v3 naming should distinguish phase states like `ImageUploadInProgress` from commands like `StartImageUpload`.
- ProductEditor now uses transition-action naming in code: `ImageUploadInProgress` with `StartImageUpload`, `ReviewSubmissionInProgress` with `StartReviewSubmission`, and `PublishInProgress` with `StartProductPublish`.
- ProductEditor now maps `ProductEditorState` to `ProductEditorRenderState` before Compose rendering, so the most complex sample no longer branches on internal `ProductEditorPhase` values in the UI layer; published render state hides draft fields and shows only completion-focused UI.
- Android CLI regression smoke verification passed after the ProductEditor naming cleanup, with evidence under `raw/verification/2026-05-09-product-editor-transition-action-rename-smoke/`.
- ProductEditor was briefly refactored to the phased-state helper as an intermediate spike: state was split into `ProductEditorPhase + ProductEditorData`, `ProductDraft` lived in data, and reducers called `transitionTo(ProductEditorPhase.X)`.
- The failed intermediate idea of hiding `SavingDraft`/`DraftSaved` as data flags was rejected; meaningful flow states must remain phases so the state diagram stays visible.
- ProductEditor has now been migrated from the phased helper to the executable DSL while keeping `State = Phase + Data`; `ProductEditorStateMachine.topology` exposes `.mmd` graph metadata from the real sample implementation.
- ProductEditor submit/resubmit transitions now stay inline inside each event branch; only data transformations are helperized so the FSM flow remains readable in the machine body.
- ProductEditor validation failure is modeled as a named no-transition `case` that updates data with an error, not as a second `transitionTo` competing with the success transition.
- Android CLI smoke verification passed after the ProductEditor executable DSL migration, with layout/screenshot evidence under `raw/verification/2026-05-09-product-editor-executable-dsl-smoke/`.
- ProductEditor now uses `typealias ProductEditorState = AfsmState<ProductEditorPhase, ProductEditorData>` and delegates `ProductEditorStateMachine` directly to the DSL machine, removing the previous phase/data adapter mapping.
- Kotlin `typealias` cannot share a same-named factory with the aliased constructor, so ProductEditor uses a lowercase `productEditorState()` factory for default initial state construction.
- A shared `AfsmStateFactory` API was spiked and rejected for now; Kotlin singleton phase inference requires explicit `<Phase, Data>` arguments, so the small feature-local factory function remains clearer.
- Auth, ProductEditor, Checkout, and consumer smoke now expose graphable feature state machines as singleton `object`s through `AfsmMachine<State, Event, Command, Effect>` aliases, avoiding repeated five-parameter `AfsmMachine<Phase, Data, Event, Command, Effect>` aliases at feature boundaries.
- A five-perspective public API usability review concluded that Afsm should be presented as an Android executable extended statechart DSL for complex flows, with `afsmMachine { ... }` as the primary onboarding path and `AfsmReducer`/graph metadata as advanced reference concepts.
- Runtime hardening now makes invalid transitions throw by default, adds `tryDispatch(event)`, bounds the default event and command queues to 64 items, and keeps command execution sequential without blocking event reduction.
- A reference architecture review compared Afsm against XState, SCXML, Tinder StateMachine, KStateMachine, Redux, Elm, Square Workflow, and Android guidance. The first hardening pass is now implemented: naming uses `AfsmReducer`/`AfsmMachine`, DSL output terminology is `command`, `onExit` exists, DSL build validation exists, topology metadata is richer, `AfsmHost` has a configurable command failure policy, and command cancellation remains explicit in feature commands/events.
- A ten-agent Android developer POC review concluded that the current executable DSL direction should continue, but Afsm must be positioned as a complex transaction/flow screen toolkit rather than a general ViewModel replacement.
- The first public usability hardening pass is now implemented: README is minimal-first, `afsm-compose` provides `CollectAfsmEffects`, `AfsmConfig.commandQueueCapacity` exists, `afsmHost(machine, initialState)` exists, Checkout demonstrates request-id stale result handling, and MMD output has initial nodes, entry/exit metadata notes, labels, and `AfsmMmdOptions.Flow`/`Full`.
- The remaining graph tooling release concerns are multi-variant/multi-module graph generation policy and eventual graph API/module-boundary decisions before broad external adoption.
- A 2026-05-14 ten-agent follow-up plus CTO review approved Afsm for internal beta only and blocked broad OSS/stable release until API/ABI, runtime pressure, restoration/effect policy, graph tooling, and OSS release identity are hardened.
- The first follow-up hardening loop is complete: internal DSL helper functions no longer appear in the public API dump, `AfsmTransition` is factory-based, `AfsmDslMarker` exists, graphable `machine + initialState` is separated from custom `reducer + initialState`, Checkout guards completed payment, `.mmd` output paths are validated, and `docs/modeling-rules.md` documents modeling choices.
- Runtime pressure hardening now fails fast with `AfsmCommandQueueOverflowException` when accepted commands exceed the bounded command queue, instead of suspending the event processor indefinitely. Tests also lock down that default effects are not replayed to late collectors.
- `docs/restoration-effect-command-policy.md` now documents the Android-facing rules for restoreable state, `onEnter`, effect durability, state-plus-acknowledgement UI work, command result events, request ids, explicit cancellation, and queue pressure.
- Checkout is now a graphable phase/data `AfsmMachine` sample instead of a custom reducer escape hatch. It demonstrates dynamic initial state from navigation `productId`, product loading, payment retry, request-id stale result handling, durable completion state, optional navigation effect, render-state mapping, and generated `CheckoutStateMachine.mmd`.
- Public example onboarding is now organized as a ladder: README minimal Draft, Auth, Checkout, ProductEditor, plus ordinary non-Afsm data screens as anti-examples. Public docs live in `docs/examples.md`, `docs/auth-walkthrough.md`, `docs/checkout-walkthrough.md`, and `docs/product-editor-walkthrough.md`.
- The project is now pushed to the private GitHub repository `kez-lab/afsm`. README has GitHub-facing status badges, a quickstart, and internal-beta positioning. `.github/workflows/ci.yml` runs the same local release gate on push, pull request, and manual dispatch.
- A 2026-05-19 six-agent usability loop simplified first-use onboarding, added terminal-state `phase(phase)` convenience, moved Auth to a render-state UI boundary, made Checkout primary UI actions explicit, documented ProductEditor transition execution order, added `docs/graph-generation.md`, and clarified the internal beta adoption contract.
- The first graph Gradle plugin slice, KSP/Gradle functional verification, plugin/processor version synchronization, and external consumer version alignment are implemented. The remaining graph-tooling concerns are multi-variant/multi-module aggregation and eventual graph API/module-boundary decisions before broad external adoption.
- A case-oriented DSL usability pass is complete. Public event-branch helpers let examples use `case(label, condition = ...) { updateData(...); transitionTo(...) }`, direct `updateData(...)`, event-aware `updateData { data, event -> ... }`, and `effect(label) { ... }`. Public examples now treat `transitionTo` as phase change only and avoid `stay`/`otherwise` usage in the graphable DSL.
- Public topology transition metadata now uses `conditionLabel`, matching the DSL's `condition = { ... }` vocabulary. The earlier `guardLabel` name is superseded before release.
- ProductEditor and Auth examples now make validation failure branches explicit with named `case(..., condition = ...)` blocks instead of relying on a final unconditional fallback case.
- Payload phase factories now run after source `onExit` and accepted case actions, so `transitionTo<PayloadPhase> { ... }` observes data updates declared earlier in the same case.
- Flow `.mmd` output now includes named no-transition condition cases such as validation failures and missing-data branches, while still hiding unlabeled data-only self-loops.
- A 2026-05-23 six-agent first-use review found that the remaining understanding cost came mainly from public terminology and onboarding order. The accepted breaking cleanup renamed public DSL `state(...)` scopes to `phase(...)`, renamed the standard extended state property from `context` to `data`, replaced public `Stayed` wording with `Handled`, removed `AfsmPhaseMachine` from the public surface, and added `docs/getting-started.md` as the Android-first entry point.
- The graph Gradle plugin now supports `afsmGraph { mmdOptions.set("Flow"|"Full") }` and `-PafsmMmdOptions=Full`, so Android modules can choose between review-friendly and complete topology output.
- ProductEditor now uses `DraftSaveCompleted` for the save-result event while keeping `DraftSaved` as the phase name, matching the naming rule that events describe what happened and phases describe the current condition.
- Entry/exit command and effect graph labels are now declared in the same statement as runtime outputs, for example `command(label = "SaveDraft") { ... }`; public examples should not use separate `commandLabels` / `effectLabels` parameters.
- The public `Afsm` helper no longer exposes `stay(...)`; graphable DSL code stays in phase by handling an event without `transitionTo(...)`, while low-level reducers can use `AfsmTransition.handled(...)` when they intentionally need a handled transition output.
- Auth and ProductEditor samples now use domain-named condition helpers such as `canSubmitLoginRequest()` and `canStartReviewSubmission()` so event branches read closer to product rules than raw validation predicates.
- A follow-up six-agent first-use review tightened the remaining onboarding costs: `docs/getting-started.md` now teaches a minimal Draft flow before Checkout, public docs include an everyday API choice table, `case(...)` is explained as a graphable `if` branch, and ProductEditor is labeled as an advanced graph stress test.
- DSL predicates and payload phase factories are now read-only scopes. `case(condition = ...)` can inspect `phase`, `event`, and `data` but cannot update data or emit outputs; `transitionTo<PayloadPhase> { ... }` can create the target phase but cannot mutate the transition.
- Checkout now omits ordinary invalid event/phase combinations and uses `ignore(...)` only for expected harmless duplicates, stale async results, and terminal duplicate actions.
- Sample ViewModels now expose explicit `StateFlow<State>` and `Flow<Effect>` types so Android integration remains visible to first-time readers.
- Beginner docs, sample ViewModels, and consumer smoke now pass command handlers
  as direct Kotlin lambdas, keeping `AfsmCommandHandler` as the underlying API
  type without making the first-use examples import the wrapper.
- `docs/getting-started.md` now starts with dependency, AndroidX, file-layout,
  and import checklists before teaching the Draft machine, so first-time Android
  developers do not need to jump to README before pasting code.
- The Draft quickstart now models repository save failure as
  `DraftSaveFailed(message)` from the command handler, returning from `Saving`
  to `Editing` with `errorMessage` instead of leaving first-time users to invent
  their own failure path outside the machine.
- `docs/getting-started.md` now shows the complete `DraftViewModel` scaffold
  instead of only the `private val host` snippet, including repository contract,
  `StateFlow<DraftState>`, and `onEvent(event)`.
- `docs/getting-started.md` now calls out that initial state construction does
  not run `onEnter`; startup work should be triggered by an explicit event such
  as `ScreenEntered`.
- `docs/testing-guide.md` now includes command failure result testing as a
  first-use test category, using the Draft save failure path as the concrete
  example.
- `AfsmCommandHandler.none()` and the `ViewModel.afsmHost(...)` KDoc now state
  that the default command handler is only for machines that never emit
  commands; public API docs also show Kotlin callers should normally pass a
  direct command handler lambda.
- `docs/graph-generation.md` now starts with a setup checklist and shows Maven
  Local snapshot consumers must configure both `pluginManagement.repositories`
  and `dependencyResolutionManagement.repositories`.
- `docs/checkout-walkthrough.md` now shows the complete ViewModel startup
  bridge for the mid-size sample, including direct command handler lambda,
  explicit `ScreenEntered` dispatch, `StateFlow`, and effects exposure.
- `docs/auth-walkthrough.md` now shows the complete small-sample ViewModel
  wiring, including login/register command handling, session persistence,
  success/failure result events, `StateFlow`, effects, and `onEvent(event)`.
- `docs/examples.md` now points Minimal Draft readers to
  `docs/getting-started.md` and the consumer-smoke `DraftQuickstart.kt` mirror
  instead of describing the first sample as README-only.
- `docs/modeling-rules.md` now starts its first reading order with
  `docs/getting-started.md` and the Auth walkthrough instead of the older
  README/sample-shop wording.
- `docs/sample-shop-afsm-guide.md` now frames `AfsmReducer` as a lower-level
  runtime contract while keeping graphable `AfsmMachine<State, Event, Command,
  Effect>` objects as the normal feature-code boundary.
- `consumer-smoke` now runs focused Draft quickstart JVM tests in addition to
  compiling and exporting graphs, so command emission and save failure recovery
  are verified against Maven Local artifacts.
- Public DSL docs and `updateData(...)` KDoc now state that multi-action event
  handling must use one `case { ... }`; top-level shorthand calls are separate
  alternatives and are not merged.
- First-use docs now make the effect boundary explicit: start no-effect
  machines with `AfsmNoEffect`, and add `afsm-compose` only when a Compose route
  collects real UI one-shot effects.
- `docs/getting-started.md` now shows the first two Draft JVM transition tests
  before ViewModel wiring, matching the external `consumer-smoke` quickstart
  tests.
- The quickstart dependency checklist and `consumer-smoke` fixture now include
  explicit JUnit wiring for those first JVM tests.
- `Afsm` low-level transition helper KDoc now directs ordinary Android feature
  code back to graphable `afsmMachine { ... }` and frames the helpers as custom
  reducer support.
- `wiki/00-context/open-questions.md` now treats UI one-shot modeling and
  required navigation durability as resolved policy instead of open questions.
- Example and modeling-rule reading orders now include the quickstart JVM test
  loop before broader sample adoption.
- README's first-use short version now includes plain JVM transition tests
  before ViewModel hosting.
- `afsm-core` now has a regression test proving top-level shorthand event
  branches are alternatives and not merged transition actions.
- Draft quickstart JVM tests now cover the missing-title data-only validation
  branch in addition to command emission and save failure recovery.
- README and release-readiness now match the current quickstart ViewModel
  snippet formatting and consumer-smoke test coverage.
- A new `afsm-test` module provides Kotlin transition assertion helpers, and
  the Draft quickstart tests dogfood it through `consumer-smoke` against Maven
  Local artifacts.
- `sample-shop` Auth, Checkout, and ProductEditor state-machine tests now use
  `afsm-test` helpers for transition decisions, phases, commands, and effects,
  while keeping render-state and topology assertions explicit.
- `consumer-smoke` now includes Draft ViewModel wiring tests with
  `kotlinx-coroutines-test`, proving command-handler repository calls and
  command-result state updates from Maven Local artifacts.
- `docs/getting-started.md` now links the first pure machine tests to the first
  ViewModel wiring test pattern, including the coroutine test dependency and
  the executable `consumer-smoke` mirror.
- README's first-use short path now also tells readers to add one ViewModel
  wiring test after pure machine tests, linking to the testing guide and
  executable `consumer-smoke` test.
- `docs/getting-started.md` now includes the first `AfsmNoEffect` to real
  feature `Effect` migration path, showing the effect type change, durable
  state plus optional effect emission, ViewModel `Flow<Effect>` exposure, and
  route-level `CollectAfsmEffects(...)` collection.
- The Draft quickstart ViewModel now accepts an explicit initial state, and
  `consumer-smoke` verifies a `SavedStateHandle` title key can seed Draft state
  without starting work; docs now show this as the first navigation/deep-link
  restoration path before moving to Checkout.
- `docs/getting-started.md` now shows the first no-effect Compose route:
  collect `viewModel.state` with `collectAsStateWithLifecycle()`, pass state
  into a stateless screen, and send user callbacks back to `viewModel.onEvent`.
- First-use docs now state the render-state boundary directly: pass
  `DraftState` to tiny screens at first, then add a feature-owned render state
  when Compose would otherwise infer UI behavior from multiple phases.

## Core Architecture Position

The project should combine:

- MVVM's lifecycle fit and state ownership through `ViewModel`.
- UDF's one-way event/state flow.
- FSM's explicit state transition model.
- Plain Kotlin unit tests for transition behavior.

Primary engineering page: [[../03-engineering/android-fsm-architecture|Android FSM Architecture]].

Official Android constraints: [[../03-engineering/android-official-guidance|Android Official Guidance]].

Product strategy: [[../01-product/android-fsm-library-strategy|Android FSM Library Strategy]].

Delivery plan: [[../03-engineering/library-delivery-plan|Library Delivery Plan]].

First reference flow: [[../03-engineering/reference-flow-signup-identity-retry|Reference Flow - Signup Identity Retry]].

Public API draft: [[../03-engineering/afsm-public-api-draft|Afsm Public API Draft]].

Implementation-candidate API draft: [[../03-engineering/afsm-public-api-draft-v2|Afsm Public API Draft v2]].

v3 executable DSL direction: [[../03-engineering/afsm-v3-executable-dsl|Afsm v3 Executable DSL]].

KSP `.mmd` generation: [[../03-engineering/afsm-ksp-mmd-generation|Afsm KSP MMD Generation]].

Reference architecture review: [[../03-engineering/afsm-reference-architecture-review|Afsm Reference Architecture Review]].

Example catalog: [[../03-engineering/afsm-example-catalog|Afsm Example Catalog]].

10-agent CTO review: [[../08-meetings/2026-05-14-afsm-10-agent-cto-review|Afsm 10-Agent CTO Review]].

Phased core spike: [[../03-engineering/afsm-phased-core-spike|Afsm Phased Core Spike]].

Transition action terminology: [[../03-engineering/afsm-v3-terminology-transition-actions|Afsm v3 Terminology and Transition Actions]].

Core compile validation: [[../03-engineering/afsm-core-compile-validation|Afsm Core Compile Validation]].

Runtime dispatch validation: [[../03-engineering/afsm-runtime-dispatch-loop|Afsm Runtime Dispatch Loop]].

AI engineering guardrails: [[../07-llm/ai-engineering-guardrails|AI Engineering Guardrails]].

ViewModel integration validation: [[../03-engineering/afsm-viewmodel-integration|Afsm ViewModel Integration]].

Sample app validation: [[../03-engineering/sample-shop-reference-app|Sample Shop Reference App]].

## Current Source Material

- Raw discussion: [Android ViewModel FSM Discussion](../../raw/conversations/2026-05-01-android-viewmodel-fsm-discussion.md)
- Raw pattern note: [LLM Wiki Pattern](../../raw/sources/2026-05-01-llm-wiki-pattern.md)
- Official Android docs research: [Android Official Docs Research](../../raw/sources/2026-05-01-android-official-docs-fsm-research.md)
