# Wiki Log

## [2026-05-25] documentation | Render state boundary guidance

- Source: First-use render-state boundary review.
- Action: Documented when to pass `DraftState` directly and when to add a
  feature-owned render state, then linked the rule to Auth and Checkout
  examples.
- Updated: `README.md`, `docs/getting-started.md`,
  `docs/modeling-rules.md`, `docs/auth-walkthrough.md`,
  `docs/examples.md`, `wiki/00-context/current-state.md`,
  `wiki/06-project/implementation-log.md`, `wiki/log.md`.

## [2026-05-25] documentation | No-effect Compose route quickstart bridge

- Source: First-use route wiring review after the Draft ViewModel path.
- Action: Added a no-effect Compose route example that collects
  `viewModel.state`, passes state to a stateless screen, and sends UI callbacks
  through `viewModel.onEvent(...)`.
- Updated: `README.md`, `docs/getting-started.md`, `docs/examples.md`,
  `wiki/00-context/current-state.md`,
  `wiki/06-project/implementation-log.md`, `wiki/log.md`.

## [2026-05-25] test+docs | SavedStateHandle initial state quickstart bridge

- Source: First-use dynamic initial-state review.
- Action: Added a consumer-smoke `SavedStateHandle` to explicit Draft
  `initialState` test and documented the same path in the getting-started,
  testing, README, consumer-smoke, and release-readiness docs.
- Updated: `consumer-smoke/`, `docs/getting-started.md`,
  `docs/testing-guide.md`, `docs/release-readiness.md`, `README.md`,
  `wiki/00-context/current-state.md`,
  `wiki/06-project/implementation-log.md`, `wiki/log.md`.

## [2026-05-25] documentation | First effect onboarding bridge

- Source: First-use effect/Compose onboarding review.
- Action: Added a concrete `AfsmNoEffect` to feature `Effect` migration path,
  including durable state plus optional effect emission, ViewModel exposure,
  route-level collection, and a testing-guide reminder to assert effects in
  pure transition tests.
- Updated: `docs/getting-started.md`, `docs/testing-guide.md`,
  `docs/examples.md`, `README.md`, `wiki/00-context/current-state.md`,
  `wiki/06-project/implementation-log.md`, `wiki/log.md`.

## [2026-05-01] ingest | Android ViewModel FSM discussion

- Source: `raw/conversations/2026-05-01-android-viewmodel-fsm-discussion.md`
- Action: Captured the conversation that established the need for an Android-specific FSM approach on top of ViewModel.
- Updated: `wiki/00-context/current-state.md`, `wiki/03-engineering/android-fsm-architecture.md`, `wiki/03-engineering/state-event-command-effect.md`, `wiki/03-engineering/viewmodel-fsm-boundaries.md`, `wiki/03-engineering/testing-strategy.md`, `wiki/06-project/decision-log.md`.

## [2026-05-01] llm-ops | Initialize LLM Wiki structure

- Source: `raw/sources/2026-05-01-llm-wiki-pattern.md`
- Action: Created project wiki structure, index, maintenance guide, and agent instructions.
- Updated: `AGENTS.md`, `wiki/index.md`, `wiki/07-llm/wiki-maintenance-guide.md`, `wiki/log.md`.

## [2026-05-01] planning | Android FSM runtime roadmap

- Source: `raw/conversations/2026-05-01-android-viewmodel-fsm-discussion.md`
- Action: Added a phased roadmap for moving from per-screen FSMs to a small Android-specific FSM runtime.
- Updated: `wiki/03-engineering/fsm-runtime-roadmap.md`, `wiki/index.md`, `wiki/log.md`.

## [2026-05-01] ingest | Android official architecture guidance

- Source: `raw/sources/2026-05-01-android-official-docs-fsm-research.md`
- Action: Researched official Android docs through Android CLI and distilled constraints for ViewModel-backed FSM architecture.
- Updated: `wiki/03-engineering/android-official-guidance.md`, `wiki/index.md`, `wiki/log.md`.

## [2026-05-01] ingest | Android coroutine and flow testing guidance

- Source: `raw/sources/2026-05-01-android-official-docs-fsm-research.md`
- Action: Added lifecycle-aware coroutine, dispatcher injection, coroutine testing, and Flow/StateFlow testing guidance for FSM command execution.
- Updated: `wiki/03-engineering/android-official-guidance.md`, `wiki/03-engineering/testing-strategy.md`, `wiki/03-engineering/fsm-runtime-roadmap.md`, `wiki/log.md`.

## [2026-05-01] ingest | Android saved state guidance

- Source: `raw/sources/2026-05-01-android-official-docs-fsm-research.md`
- Action: Added official saved state and `SavedStateHandle` constraints for FSM restoration policy.
- Updated: `wiki/03-engineering/android-official-guidance.md`, `wiki/03-engineering/fsm-runtime-roadmap.md`, `wiki/00-context/open-questions.md`, `wiki/log.md`.

## [2026-05-01] llm-ops | Current state updated after Android research

- Source: `wiki/03-engineering/android-official-guidance.md`
- Action: Updated current project state to reflect that official Android architecture, coroutine, testing, and saved state research has been ingested.
- Updated: `wiki/00-context/current-state.md`, `wiki/log.md`.

## [2026-05-01] llm-ops | Add task handoff recommendation rule

- Source: user instruction
- Action: Added a rule that completed tasks should end with a short recommendation for the next concrete task.
- Updated: `AGENTS.md`, `wiki/07-llm/wiki-maintenance-guide.md`, `wiki/log.md`.

## [2026-05-01] decision | Android FSM library product goal

- Source: user instruction from CEO
- Action: Promoted the project from architecture exploration to a reusable Android FSM library goal and added product/delivery planning pages.
- Updated: `wiki/01-product/android-fsm-library-strategy.md`, `wiki/03-engineering/library-delivery-plan.md`, `wiki/index.md`, `wiki/00-context/current-state.md`, `wiki/00-context/open-questions.md`, `wiki/06-project/decision-log.md`, `wiki/log.md`.

## [2026-05-01] planning | Signup identity retry reference flow

- Source: user request
- Action: Selected signup + identity verification + retry as the first reference flow and documented State/Event/Command/Effect policy.
- Updated: `wiki/03-engineering/reference-flow-signup-identity-retry.md`, `wiki/index.md`, `wiki/03-engineering/library-delivery-plan.md`, `wiki/00-context/current-state.md`, `wiki/00-context/open-questions.md`, `wiki/06-project/decision-log.md`, `wiki/log.md`.

## [2026-05-01] planning | Afsm public API draft

- Source: user request
- Action: Added initial Afsm public API proposal with naming, dependency policy, module boundaries, core/runtime/ViewModel/test APIs, and MVP exclusions.
- Updated: `wiki/03-engineering/afsm-public-api-draft.md`, `wiki/index.md`, `wiki/00-context/current-state.md`, `wiki/00-context/open-questions.md`, `wiki/01-product/android-fsm-library-strategy.md`, `wiki/03-engineering/library-delivery-plan.md`, `wiki/06-project/decision-log.md`, `wiki/log.md`.

## [2026-05-01] planning | Signup StateMachine pseudo implementation

- Source: user request
- Action: Added Kotlin-like pseudo implementation for the signup identity retry reference flow to validate `AfsmTransition<S, C, F>` ergonomics.
- Updated: `wiki/03-engineering/signup-state-machine-pseudo-implementation.md`, `wiki/index.md`, `wiki/log.md`.

## [2026-05-01] meeting | Afsm API pseudo implementation review

- Source: Android architecture and Kotlin API reviewer agent outputs
- Action: Recorded review consensus that the direction is promising but not implementation-ready until `Stayed`, effect delivery, dispatch serialization, command behavior, and saved state restoration semantics are clarified.
- Updated: `wiki/08-meetings/2026-05-01-afsm-api-pseudo-implementation-review.md`, `wiki/00-context/open-questions.md`, `wiki/index.md`, `wiki/log.md`.

## [2026-05-03] planning | Afsm public API draft v2

- Source: user request
- Action: Added implementation-candidate API draft v2 covering `Stayed`, `AfsmNoEffect`, effect delivery semantics, dispatch serialization, and MVP command execution policy.
- Updated: `wiki/03-engineering/afsm-public-api-draft-v2.md`, `wiki/index.md`, `wiki/00-context/current-state.md`, `wiki/00-context/open-questions.md`, `wiki/06-project/decision-log.md`, `wiki/log.md`.

## [2026-05-03] implementation | afsm-core minimal Kotlin skeleton

- Source: user request
- Action: Added Gradle wrapper, root Kotlin project, `afsm-core` module, v2 core public API source files, and compile-check usage for effectful and no-effect flows.
- Updated: `settings.gradle.kts`, `build.gradle.kts`, `afsm-core/`, `wiki/03-engineering/afsm-core-compile-validation.md`, `wiki/06-project/implementation-log.md`, `wiki/00-context/current-state.md`, `wiki/00-context/open-questions.md`, `wiki/06-project/decision-log.md`, `wiki/index.md`, `wiki/log.md`.

## [2026-05-09] implementation | afsm-runtime dispatch loop

- Source: user request
- Action: Added `afsm-runtime` with `AfsmHost`, command handling, effect delivery, diagnostics, invalid transition policy, and tests proving serialized dispatch and decision behavior.
- Updated: `settings.gradle.kts`, `afsm-runtime/`, `wiki/03-engineering/afsm-runtime-dispatch-loop.md`, `wiki/06-project/implementation-log.md`, `wiki/00-context/current-state.md`, `wiki/00-context/open-questions.md`, `wiki/06-project/decision-log.md`, `wiki/index.md`, `wiki/log.md`.

## [2026-05-09] llm-ops | AI engineering guardrails

- Source: user request
- Action: Added project-scoped AI engineering and TDD guardrails that protect tests as executable specification and define failure triage before test edits.
- Updated: `AGENTS.md`, `wiki/07-llm/ai-engineering-guardrails.md`, `wiki/03-engineering/testing-strategy.md`, `wiki/07-llm/wiki-maintenance-guide.md`, `wiki/00-context/current-state.md`, `wiki/06-project/decision-log.md`, `wiki/index.md`, `wiki/log.md`.

## [2026-05-09] implementation | afsm-viewmodel helper

- Source: user request
- Action: Added `afsm-viewmodel` as an Android library module with `ViewModel.afsmHost(...)`, AndroidX Lifecycle dependency, ViewModel usage tests, and Gradle Android configuration.
- Updated: `settings.gradle.kts`, `build.gradle.kts`, `gradle/wrapper/gradle-wrapper.properties`, `gradle.properties`, `afsm-viewmodel/`, `wiki/03-engineering/afsm-viewmodel-integration.md`, `wiki/06-project/implementation-log.md`, `wiki/00-context/current-state.md`, `wiki/00-context/open-questions.md`, `wiki/06-project/decision-log.md`, `wiki/index.md`, `wiki/log.md`.

## [2026-05-09] implementation | sample-shop reference app

- Source: user request
- Action: Added `:sample-shop` Compose + Room app with Afsm-backed auth and checkout flows, ordinary ViewModel-backed catalog/product/review screens, JVM state machine tests, and sample usage documentation.
- Updated: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `sample-shop/`, `docs/sample-shop-afsm-guide.md`, `wiki/03-engineering/sample-shop-reference-app.md`, `wiki/06-project/implementation-log.md`, `wiki/00-context/current-state.md`, `wiki/00-context/open-questions.md`, `wiki/06-project/decision-log.md`, `wiki/index.md`, `wiki/log.md`.

## [2026-05-09] implementation | sample-shop sealed FSM rewrite

- Source: user request
- Action: Rewrote auth as a sealed phase FSM and rewrote product registration as a richer Afsm review/publish flow with tests and documentation.
- Updated: `sample-shop/`, `docs/sample-shop-afsm-guide.md`, `wiki/03-engineering/sample-shop-reference-app.md`, `wiki/06-project/decision-log.md`, `wiki/06-project/implementation-log.md`, `wiki/00-context/current-state.md`, `wiki/00-context/open-questions.md`, `wiki/log.md`.

## [2026-05-09] verification | sample-shop FSM smoke test

- Source: `raw/verification/2026-05-09-sample-shop-fsm-smoke/`
- Action: Ran Android CLI smoke test for signup and product registration review/publish flow on `emulator-5556`; captured layout JSON and final screenshot evidence.
- Updated: `raw/verification/2026-05-09-sample-shop-fsm-smoke/`, `raw/README.md`, `wiki/05-qa/verification-report-2026-05-09-sample-shop-fsm-smoke.md`, `wiki/index.md`, `wiki/log.md`.

## [2026-05-09] planning | v3 topology-first API comparison

- Source: user request
- Action: Added ProductEditor-based v2 vs v3 design comparison for a possible `transition<From, Event, To>` API that can generate state diagrams from declared edge metadata.
- Updated: `wiki/03-engineering/afsm-v3-topology-first-api.md`, `wiki/index.md`, `wiki/00-context/current-state.md`, `wiki/00-context/open-questions.md`, `wiki/06-project/decision-log.md`, `wiki/06-project/implementation-log.md`, `wiki/log.md`.

## [2026-05-09] planning | v3 transition action terminology

- Source: user discussion
- Action: Clarified that Afsm commands are transition actions emitted by the machine, documented Event/Action/Effect directionality, and defined ProductEditor naming candidates.
- Updated: `wiki/03-engineering/afsm-v3-terminology-transition-actions.md`, `wiki/03-engineering/state-event-command-effect.md`, `wiki/03-engineering/afsm-v3-topology-first-api.md`, `wiki/index.md`, `wiki/00-context/current-state.md`, `wiki/00-context/open-questions.md`, `wiki/06-project/decision-log.md`, `wiki/06-project/implementation-log.md`, `wiki/log.md`.

## [2026-05-09] implementation | ProductEditor transition action naming cleanup

- Source: user request
- Action: Renamed ProductEditor phase states and commands to `ImageUploadInProgress`/`StartImageUpload`, `ReviewSubmissionInProgress`/`StartReviewSubmission`, and `PublishInProgress`/`StartProductPublish`; updated tests and documentation.
- Updated: `sample-shop/src/main/kotlin/afsm/sample/shop/feature/editor/`, `sample-shop/src/test/kotlin/afsm/sample/shop/feature/editor/ProductEditorStateMachineTest.kt`, `docs/sample-shop-afsm-guide.md`, `wiki/03-engineering/sample-shop-reference-app.md`, `wiki/03-engineering/afsm-v3-topology-first-api.md`, `wiki/03-engineering/afsm-v3-terminology-transition-actions.md`, `wiki/06-project/implementation-log.md`, `wiki/log.md`.

## [2026-05-09] verification | ProductEditor transition action rename smoke

- Source: `raw/verification/2026-05-09-product-editor-transition-action-rename-smoke/`
- Action: Ran Gradle unit/build verification and Android CLI smoke journey for signup plus product registration after ProductEditor naming cleanup.
- Updated: `raw/verification/2026-05-09-product-editor-transition-action-rename-smoke/`, `raw/README.md`, `wiki/05-qa/verification-report-2026-05-09-product-editor-transition-action-rename-smoke.md`, `wiki/index.md`, `wiki/00-context/current-state.md`, `wiki/log.md`.

## [2026-05-09] planning | v3 topology-first API correction

- Source: user feedback on `wiki/03-engineering/afsm-v3-topology-first-api.md`
- Action: Corrected the v3 topology-first pseudo API away from `transition<From, Event, To>` plus `goTo(state, commands, effects)` and toward a `from<FromState> { on<Event>().to<ToState>() }` topology companion with plain Kotlin typed receiver reducers.
- Updated: `wiki/03-engineering/afsm-v3-topology-first-api.md`, `wiki/index.md`, `wiki/00-context/current-state.md`, `wiki/00-context/open-questions.md`, `wiki/06-project/decision-log.md`, `wiki/06-project/implementation-log.md`, `wiki/log.md`.

## [2026-05-09] planning | v3 typed-handler API correction

- Source: user feedback recalling the prior no-DSL discussion
- Action: Corrected the v3 topology-first page again to prefer plain Kotlin `when`, concrete State/Event handler signatures, and `transitionTo` next-state extraction instead of a `from/on/to` DSL companion.
- Updated: `wiki/03-engineering/afsm-v3-topology-first-api.md`, `wiki/index.md`, `wiki/00-context/current-state.md`, `wiki/00-context/open-questions.md`, `wiki/06-project/decision-log.md`, `wiki/06-project/implementation-log.md`, `wiki/log.md`.

## [2026-05-09] maintenance | v3 canonical synthesis cleanup

- Source: user feedback about poor conversation/wiki sync and fragmented v3 documentation
- Action: Rewrote the v3 page as the canonical `Afsm v3 Typed Handler API` synthesis and added rules requiring future design corrections to update canonical pages, not only append logs.
- Updated: `AGENTS.md`, `wiki/03-engineering/afsm-v3-topology-first-api.md`, `wiki/03-engineering/afsm-v3-terminology-transition-actions.md`, `wiki/07-llm/wiki-maintenance-guide.md`, `wiki/index.md`, `wiki/00-context/current-state.md`, `wiki/00-context/open-questions.md`, `wiki/06-project/decision-log.md`, `wiki/06-project/implementation-log.md`, `wiki/log.md`.

## [2026-05-09] decision | v3 phased state with hidden entry policy

- Source: user discussion on abstract state diagrams, phase-only transitions, and hidden context update rules
- Action: Reworked the canonical v3 direction from typed sealed-state handlers to a phased-state profile where reducers call `transitionTo(Phase)` and feature-local `PhaseEntryPolicy` owns context normalization plus command/effect entry rules.
- Updated: `wiki/03-engineering/afsm-v3-topology-first-api.md`, `wiki/03-engineering/afsm-v3-terminology-transition-actions.md`, `wiki/index.md`, `wiki/00-context/current-state.md`, `wiki/00-context/open-questions.md`, `wiki/06-project/decision-log.md`, `wiki/06-project/implementation-log.md`, `wiki/log.md`.

## [2026-05-09] implementation | afsm-core phased-state spike

- Source: user request to validate `AfsmPhasedState`, `AfsmPhaseEntryPolicy`, `transitionTo(Phase)`, and `updateContext` as real Kotlin code
- Action: Added a minimal phased-state API spike to `afsm-core`, executable ProductEditor-like tests, and verification notes.
- Updated: `afsm-core/`, `wiki/03-engineering/afsm-phased-core-spike.md`, `wiki/index.md`, `wiki/00-context/current-state.md`, `wiki/00-context/open-questions.md`, `wiki/06-project/implementation-log.md`, `wiki/log.md`.

## [2026-05-09] implementation | AfsmPhasedStateMachine helper

- Source: user request to hide direct `Afsm.phased(...)` setup and let reducers call `transitionTo(Phase)` directly
- Action: Added `AfsmPhasedStateMachine`, refactored the phased compile check to use the helper, and documented the API ergonomics finding.
- Updated: `afsm-core/`, `wiki/03-engineering/afsm-phased-core-spike.md`, `wiki/00-context/current-state.md`, `wiki/00-context/open-questions.md`, `wiki/06-project/implementation-log.md`, `wiki/log.md`.

## [2026-05-09] implementation | ProductEditor phased-state helper spike

- Source: user request and correction that only real data should be separated from state
- Action: Refactored ProductEditor to `State = Phase + Context`, kept flow states such as `SavingDraft` and `DraftSaved` as phases, moved `ProductDraft` into context, and made reducers call `transitionTo(ProductEditorPhase.X)` while phase entry policy updates context and emits commands.
- Updated: `sample-shop/src/main/kotlin/afsm/sample/shop/feature/editor/`, `sample-shop/src/test/kotlin/afsm/sample/shop/feature/editor/ProductEditorStateMachineTest.kt`, `docs/sample-shop-afsm-guide.md`, `wiki/03-engineering/sample-shop-reference-app.md`, `wiki/03-engineering/afsm-phased-core-spike.md`, `wiki/00-context/current-state.md`, `wiki/00-context/open-questions.md`, `wiki/06-project/decision-log.md`, `wiki/06-project/implementation-log.md`, `wiki/log.md`.

## [2026-05-09] decision | v3 executable statechart DSL

- Source: user request to rely on deeper FSM analysis and external references for the best Android FSM structure
- Action: Added the canonical `Afsm v3 Executable DSL` plan, superseded the phased-state helper as the public v3 recommendation, and recorded the implementation plan for DSL compile/interpreter/graph/ProductEditor migration spikes.
- Updated: `wiki/03-engineering/afsm-v3-executable-dsl.md`, `wiki/03-engineering/afsm-v3-topology-first-api.md`, `wiki/03-engineering/afsm-v3-terminology-transition-actions.md`, `wiki/03-engineering/afsm-phased-core-spike.md`, `wiki/index.md`, `wiki/00-context/current-state.md`, `wiki/00-context/open-questions.md`, `wiki/06-project/decision-log.md`, `wiki/06-project/implementation-log.md`, `wiki/log.md`.

## [2026-05-09] implementation | Afsm executable DSL core spike

- Source: `afsm-core/src/main/kotlin/afsm/core/AfsmMachineDsl.kt`
- Action: Added a minimal executable DSL builder/interpreter and ProductEditor-like tests for phase transitions, context assignment, entry actions, guard fallback, typed payload phases, and effect emission.
- Updated: `afsm-core/`, `wiki/03-engineering/afsm-v3-executable-dsl.md`, `wiki/00-context/current-state.md`, `wiki/00-context/open-questions.md`, `wiki/06-project/implementation-log.md`, `wiki/log.md`.

## [2026-05-09] implementation | Afsm executable DSL topology spike

- Source: `afsm-core/src/main/kotlin/afsm/core/AfsmTopology.kt`
- Action: Added static topology metadata and Mermaid export, then refined the DSL branch shape so graph edges are declared at build time rather than inferred from runtime-only `transitionTo` calls.
- Updated: `afsm-core/`, `wiki/03-engineering/afsm-v3-executable-dsl.md`, `wiki/00-context/current-state.md`, `wiki/00-context/open-questions.md`, `wiki/06-project/implementation-log.md`, `wiki/log.md`.

## [2026-05-09] implementation | ProductEditor executable DSL migration

- Source: `sample-shop/src/main/kotlin/afsm/sample/shop/feature/editor/ProductEditorStateMachine.kt`
- Action: Migrated ProductEditor from the phased helper to the executable DSL, preserved `State = Phase + Context`, added topology assertions, and updated sample documentation.
- Updated: `sample-shop/src/main/kotlin/afsm/sample/shop/feature/editor/`, `sample-shop/src/test/kotlin/afsm/sample/shop/feature/editor/ProductEditorStateMachineTest.kt`, `docs/sample-shop-afsm-guide.md`, `wiki/03-engineering/sample-shop-reference-app.md`, `wiki/03-engineering/afsm-v3-executable-dsl.md`, `wiki/03-engineering/afsm-phased-core-spike.md`, `wiki/00-context/current-state.md`, `wiki/00-context/open-questions.md`, `wiki/06-project/implementation-log.md`, `wiki/log.md`.

## [2026-05-09] verification | ProductEditor executable DSL smoke

- Source: `raw/verification/2026-05-09-product-editor-executable-dsl-smoke/`
- Action: Ran Android CLI smoke journey after the ProductEditor executable DSL migration and captured layout/screenshot evidence for register, review rejection, resubmission, approval, publish, and catalog return.
- Updated: `raw/verification/2026-05-09-product-editor-executable-dsl-smoke/`, `raw/README.md`, `wiki/05-qa/verification-report-2026-05-09-product-editor-executable-dsl-smoke.md`, `wiki/index.md`, `wiki/00-context/current-state.md`, `wiki/06-project/implementation-log.md`, `wiki/log.md`.

## [2026-05-09] implementation | Afsm DSL API cleanup and mmd generation

- Source: user feedback on `.mmd`-only graph output, `assign`, `AfsmPhasedStateMachine`, and `AfsmEventBuilder`.
- Action: Removed the phased helper API from current core code, renamed unclear DSL terms, switched topology rendering to `toMmd()`, and added a sample Gradle task that writes the ProductEditor state graph as a `.mmd` file.
- Updated: `afsm-core/`, `sample-shop/build.gradle.kts`, `sample-shop/src/test/kotlin/afsm/sample/shop/feature/editor/ProductEditorMmdExportTest.kt`, `docs/sample-shop-afsm-guide.md`, `wiki/03-engineering/afsm-v3-executable-dsl.md`, `wiki/03-engineering/afsm-phased-core-spike.md`, `wiki/03-engineering/sample-shop-reference-app.md`, `wiki/00-context/current-state.md`, `wiki/00-context/open-questions.md`, `wiki/06-project/decision-log.md`, `wiki/06-project/implementation-log.md`, `wiki/log.md`.

## [2026-05-09] planning | KSP mmd generation design

- Source: user request to design KSP-based automatic `.mmd` discovery for multiple state machines.
- Action: Corrected the KSP graph generation design to annotate `StateMachine` classes, generate a registry, execute real compiled topology through `AfsmGraphSource`, and write one `.mmd` per registered machine.
- Updated: `wiki/03-engineering/afsm-ksp-mmd-generation.md`, `wiki/index.md`, `wiki/00-context/current-state.md`, `wiki/00-context/open-questions.md`, `wiki/03-engineering/afsm-v3-executable-dsl.md`, `wiki/06-project/decision-log.md`, `wiki/06-project/implementation-log.md`, `wiki/log.md`.

## [2026-05-09] implementation | KSP mmd generation slice

- Source: `afsm-graph-ksp/`, `sample-shop/src/main/kotlin/afsm/sample/shop/feature/editor/ProductEditorStateMachine.kt`
- Action: Implemented `@AfsmGraph` class discovery, generated graph registry code, registry-driven `.mmd` writing, and ProductEditor annotation-based graph export.
- Updated: `afsm-core/`, `afsm-graph-ksp/`, `sample-shop/build.gradle.kts`, `sample-shop/src/main/kotlin/afsm/sample/shop/feature/editor/ProductEditorStateMachine.kt`, `sample-shop/src/test/kotlin/afsm/sample/shop/feature/editor/ProductEditorMmdExportTest.kt`, `docs/sample-shop-afsm-guide.md`, `wiki/03-engineering/afsm-ksp-mmd-generation.md`, `wiki/03-engineering/sample-shop-reference-app.md`, `wiki/00-context/current-state.md`, `wiki/00-context/open-questions.md`, `wiki/06-project/implementation-log.md`, `wiki/log.md`.

## [2026-05-09] implementation | Executable DSL ignored and invalid branches

- Source: `afsm-core/src/main/kotlin/afsm/core/AfsmMachineDsl.kt`
- Action: Added `ignore(...)` and `invalid(...)` DSL branches so reducers can preserve non-graph transition decisions while keeping diagrams focused on real state transitions.
- Updated: `afsm-core/`, `wiki/03-engineering/afsm-v3-executable-dsl.md`, `wiki/06-project/implementation-log.md`, `wiki/log.md`.

## [2026-05-09] implementation | Auth graphable DSL migration

- Source: `sample-shop/src/main/kotlin/afsm/sample/shop/feature/auth/AuthStateMachine.kt`
- Action: Migrated Auth to the executable DSL, annotated it with `@AfsmGraph`, and verified `generateAfsmMmd` emits both Auth and ProductEditor `.mmd` files from the generated registry.
- Updated: `sample-shop/src/main/kotlin/afsm/sample/shop/feature/auth/AuthStateMachine.kt`, `sample-shop/src/test/kotlin/afsm/sample/shop/feature/editor/ProductEditorMmdExportTest.kt`, `docs/sample-shop-afsm-guide.md`, `wiki/03-engineering/sample-shop-reference-app.md`, `wiki/00-context/current-state.md`, `wiki/00-context/open-questions.md`, `wiki/03-engineering/afsm-ksp-mmd-generation.md`, `wiki/06-project/implementation-log.md`, `wiki/log.md`.

## [2026-05-09] implementation | Afsm statechart naming cleanup

- Source: user feedback on `AfsmStateMachine`/`AfsmMachine`, single-state Android usage, topology forwarding, `ignore(...)`, and repeated generic lists.
- Action: Renamed the executable DSL concept to `AfsmStateChart`, introduced `AfsmChartState`, added `AfsmStateChartMachine`, migrated Auth/ProductEditor adapters, and synchronized the canonical v3 wiki guidance.
- Updated: `afsm-core/`, `sample-shop/src/main/kotlin/afsm/sample/shop/feature/auth/AuthStateMachine.kt`, `sample-shop/src/main/kotlin/afsm/sample/shop/feature/editor/ProductEditorStateMachine.kt`, `docs/sample-shop-afsm-guide.md`, `wiki/03-engineering/afsm-v3-executable-dsl.md`, `wiki/03-engineering/afsm-ksp-mmd-generation.md`, `wiki/00-context/current-state.md`, `wiki/00-context/open-questions.md`, `wiki/06-project/decision-log.md`, `wiki/06-project/implementation-log.md`, `wiki/log.md`.

## [2026-05-10] implementation | AfsmState phase/context model validation

- Source: user request to verify whether all Afsm state should use `phase + context`.
- Action: Added `AfsmState<P, X>` as the standard phase/context state data class, deprecated `AfsmChartState`, made `AfsmStateChart` implement `AfsmStateMachine<AfsmState<P, X>, ...>` and `AfsmGraphSource`, migrated ProductEditor to a typealias plus chart delegation, and verified `.mmd` generation still works.
- Updated: `afsm-core/`, `sample-shop/src/main/kotlin/afsm/sample/shop/feature/auth/AuthStateMachine.kt`, `sample-shop/src/main/kotlin/afsm/sample/shop/feature/editor/`, `sample-shop/src/test/kotlin/afsm/sample/shop/feature/editor/ProductEditorStateMachineTest.kt`, `afsm-core/src/test/kotlin/afsm/core/AfsmExecutableDslCompileCheckTest.kt`, `docs/sample-shop-afsm-guide.md`, `wiki/03-engineering/afsm-v3-executable-dsl.md`, `wiki/03-engineering/sample-shop-reference-app.md`, `wiki/00-context/current-state.md`, `wiki/00-context/open-questions.md`, `wiki/06-project/decision-log.md`, `wiki/06-project/implementation-log.md`, `wiki/log.md`.

## [2026-05-10] planning | Afsm reference architecture review

- Source: user request to compare Afsm against external state-machine and reducer references.
- Action: Added a reference review comparing current Afsm to XState, SCXML, Tinder StateMachine, KStateMachine, Redux, Elm, Square Workflow, and Android guidance; identified API hardening priorities before public release.
- Updated: `wiki/03-engineering/afsm-reference-architecture-review.md`, `wiki/index.md`, `wiki/00-context/current-state.md`, `wiki/00-context/open-questions.md`, `wiki/log.md`.

## [2026-05-10] implementation | Afsm API hardening loop

- Source: user request to unify command terminology, rename `AfsmStateMachine`/DSL concepts, add DSL validation, add `onExit`, expand topology metadata, and define command failure/cancellation policy.
- Action: Implemented `AfsmReducer`, `AfsmMachine`, `command(...)`, `onExit`, `AfsmDefinitionException`, richer topology metadata, `AfsmCommandFailurePolicy`, sample migrations, and verification for core/runtime/ViewModel/sample-shop.
- Updated: `afsm-core/`, `afsm-runtime/`, `afsm-viewmodel/`, `afsm-graph-ksp/`, `sample-shop/src/main/kotlin/`, `docs/sample-shop-afsm-guide.md`, `wiki/03-engineering/afsm-v3-executable-dsl.md`, `wiki/03-engineering/afsm-reference-architecture-review.md`, `wiki/00-context/current-state.md`, `wiki/00-context/open-questions.md`, `wiki/06-project/decision-log.md`, `wiki/06-project/implementation-log.md`, `wiki/log.md`.

## [2026-05-11] implementation | Public API alias removal and README

- Source: user request to decide when to remove deprecated aliases and prepare public README/API docs.
- Action: Removed pre-release aliases from source, added root README and public API reference, and synchronized canonical wiki pages with the current `AfsmReducer` / `AfsmMachine` API vocabulary.
- Updated: `README.md`, `docs/afsm-public-api.md`, `afsm-core/`, `wiki/00-context/current-state.md`, `wiki/00-context/open-questions.md`, `wiki/01-product/android-fsm-library-strategy.md`, `wiki/03-engineering/android-fsm-architecture.md`, `wiki/03-engineering/afsm-viewmodel-integration.md`, `wiki/03-engineering/afsm-ksp-mmd-generation.md`, `wiki/03-engineering/afsm-reference-architecture-review.md`, `wiki/03-engineering/afsm-v3-executable-dsl.md`, `wiki/06-project/decision-log.md`, `wiki/06-project/implementation-log.md`, `wiki/log.md`.

## [2026-05-11] implementation | Maven local publication setup

- Source: release-readiness autopilot loop after public README/API docs.
- Action: Added Maven Local publication metadata for the four library modules and verified `publishToMavenLocal`.
- Updated: `build.gradle.kts`, `afsm-core/build.gradle.kts`, `afsm-runtime/build.gradle.kts`, `afsm-viewmodel/build.gradle.kts`, `afsm-graph-ksp/build.gradle.kts`, `README.md`, `docs/afsm-public-api.md`, `wiki/00-context/current-state.md`, `wiki/06-project/decision-log.md`, `wiki/06-project/implementation-log.md`, `wiki/log.md`.

## [2026-05-11] implementation | Maven Local consumer smoke

- Source: release-readiness autopilot loop after Maven Local publication setup.
- Action: Added a separate Android consumer build that resolves Afsm from Maven Local, compiles ViewModel integration, and runs the KSP graph processor.
- Updated: `consumer-smoke/`, `scripts/verify-consumer-smoke.sh`, `README.md`, `docs/afsm-public-api.md`, `wiki/00-context/current-state.md`, `wiki/06-project/decision-log.md`, `wiki/06-project/implementation-log.md`, `wiki/log.md`.

## [2026-05-11] implementation | Release readiness warning triage

- Source: release-readiness autopilot loop after consumer smoke.
- Action: Investigated Gradle deprecation warnings and documented the current local release gate, remaining product decisions, and warning policy.
- Updated: `docs/release-readiness.md`, `README.md`, `wiki/00-context/current-state.md`, `wiki/06-project/implementation-log.md`, `wiki/log.md`.

## [2026-05-11] implementation | Kotlin explicit API gate

- Source: public API stability autopilot loop.
- Action: Enabled Kotlin explicit API mode for all Afsm library modules and documented it as a release gate.
- Updated: `afsm-core/build.gradle.kts`, `afsm-runtime/build.gradle.kts`, `afsm-viewmodel/build.gradle.kts`, `afsm-graph-ksp/build.gradle.kts`, `docs/release-readiness.md`, `wiki/00-context/current-state.md`, `wiki/06-project/decision-log.md`, `wiki/06-project/implementation-log.md`, `wiki/log.md`.

## [2026-05-11] implementation | Binary API validation gate

- Source: public release-readiness autopilot loop and JetBrains binary compatibility validator documentation.
- Action: Added binary API validation, generated module API dumps, trimmed avoidable `@PublishedApi` ABI exposure, and added `apiCheck` to release verification.
- Updated: `build.gradle.kts`, `afsm-core/src/main/kotlin/afsm/core/AfsmMachineDsl.kt`, `afsm-core/api/`, `afsm-runtime/api/`, `afsm-viewmodel/api/`, `afsm-graph-ksp/api/`, `README.md`, `docs/release-readiness.md`, `wiki/00-context/current-state.md`, `wiki/06-project/decision-log.md`, `wiki/06-project/implementation-log.md`, `wiki/log.md`.

## [2026-05-11] implementation | Pre-release changelog

- Source: release-readiness autopilot loop after binary API validation.
- Action: Added the initial changelog and linked it from public docs.
- Updated: `CHANGELOG.md`, `README.md`, `docs/release-readiness.md`, `wiki/00-context/current-state.md`, `wiki/06-project/implementation-log.md`, `wiki/log.md`.

## [2026-05-11] implementation | Contribution guardrails

- Source: user emphasis on TDD/spec-based project flow and release-readiness autopilot loop.
- Action: Added public contribution rules covering engineering principles, test integrity, API changes, verification, and documentation.
- Updated: `CONTRIBUTING.md`, `README.md`, `docs/release-readiness.md`, `wiki/00-context/current-state.md`, `wiki/06-project/implementation-log.md`, `wiki/log.md`.

## [2026-05-11] implementation | One-command local release verification

- Source: release-readiness autopilot loop after contribution guardrails.
- Action: Added a canonical one-command local release verification script and updated public docs to use it.
- Updated: `scripts/verify-release-local.sh`, `README.md`, `CONTRIBUTING.md`, `docs/release-readiness.md`, `wiki/00-context/current-state.md`, `wiki/06-project/implementation-log.md`, `wiki/log.md`.

## [2026-05-11] implementation | Maven publication metadata audit

- Source: release-readiness autopilot loop after local verification script.
- Action: Inspected generated Maven Local POMs and documented which remote-publication metadata remains blocked on product decisions.
- Updated: `docs/release-readiness.md`, `wiki/00-context/current-state.md`, `wiki/06-project/implementation-log.md`, `wiki/log.md`.

## [2026-05-11] implementation | ProductEditor submit transition readability cleanup

- Source: user feedback that `submitDraft` hurt readability in `ProductEditorStateMachine.kt`.
- Action: Inlined submit/resubmit phase transitions and limited helpers to context transformations.
- Updated: `sample-shop/src/main/kotlin/afsm/sample/shop/feature/editor/ProductEditorStateMachine.kt`, `docs/sample-shop-afsm-guide.md`, `wiki/03-engineering/sample-shop-reference-app.md`, `wiki/00-context/current-state.md`, `wiki/06-project/implementation-log.md`, `wiki/log.md`.

## [2026-05-11] implementation | ProductEditor guarded transition cleanup

- Source: user feedback that two consecutive `transitionTo` calls looked unlike a state machine.
- Action: Replaced invalid saved-draft submit transition with an `otherwise` stayed branch, surfaced the validation error in `DraftSaved`, and added unit coverage.
- Updated: `sample-shop/src/main/kotlin/afsm/sample/shop/feature/editor/ProductEditorStateMachine.kt`, `sample-shop/src/main/kotlin/afsm/sample/shop/feature/editor/ProductEditorScreen.kt`, `sample-shop/src/test/kotlin/afsm/sample/shop/feature/editor/ProductEditorStateMachineTest.kt`, `docs/sample-shop-afsm-guide.md`, `wiki/03-engineering/sample-shop-reference-app.md`, `wiki/00-context/current-state.md`, `wiki/06-project/implementation-log.md`, `wiki/log.md`.

## [2026-05-11] implementation | Afsm DSL KDoc expansion

- Source: user feedback that Afsm DSL KDoc did not explain function parameters.
- Action: Added detailed KDoc for public DSL builders, branch APIs, scopes, runtime parameters, and topology metadata parameters.
- Updated: `afsm-core/src/main/kotlin/afsm/core/AfsmMachineDsl.kt`, `wiki/00-context/current-state.md`, `wiki/06-project/implementation-log.md`, `wiki/log.md`.

## [2026-05-11] implementation | AfsmMachineAdapter removal

- Source: user request to remove `AfsmMachineAdapter` and report any remaining boilerplate.
- Action: Removed the adapter from `afsm-core`, migrated Auth to direct `AfsmState<AuthPhase, AuthContext>` usage, refreshed API dump, and synchronized public/wiki docs.
- Updated: `afsm-core/`, `sample-shop/src/main/kotlin/afsm/sample/shop/feature/auth/`, `sample-shop/src/test/kotlin/afsm/sample/shop/feature/auth/AuthStateMachineTest.kt`, `CHANGELOG.md`, `docs/afsm-public-api.md`, `docs/sample-shop-afsm-guide.md`, `wiki/00-context/current-state.md`, `wiki/00-context/open-questions.md`, `wiki/03-engineering/afsm-ksp-mmd-generation.md`, `wiki/03-engineering/afsm-v3-executable-dsl.md`, `wiki/03-engineering/sample-shop-reference-app.md`, `wiki/06-project/decision-log.md`, `wiki/06-project/implementation-log.md`, `wiki/log.md`.

## [2026-05-11] decision | AfsmState factory spike

- Source: user request to validate whether `authState()` / `productEditorState()` boilerplate should be commonized.
- Action: Spiked `AfsmStateFactory`, found the required explicit singleton phase type arguments too costly, rejected the public API, and documented the conclusion.
- Updated: `docs/sample-shop-afsm-guide.md`, `wiki/00-context/current-state.md`, `wiki/00-context/open-questions.md`, `wiki/06-project/decision-log.md`, `wiki/06-project/implementation-log.md`, `wiki/log.md`.

## [2026-05-11] implementation | AfsmGraphReducer feature-boundary cleanup

- Source: user request to reduce `ProductEditorMachine` / `AuthMachine` five-generic typealias repetition and continue senior-level API hardening.
- Action: Added `AfsmGraphReducer`, made `AfsmMachine` extend it, migrated Auth/ProductEditor/consumer-smoke graphable machines to state-based aliases and singleton objects, updated docs/wiki guidance, and re-ran the local release gate including Maven Local consumer smoke.
- Updated: `afsm-core/`, `sample-shop/src/main/kotlin/afsm/sample/shop/feature/auth/`, `sample-shop/src/main/kotlin/afsm/sample/shop/feature/editor/`, `sample-shop/src/test/kotlin/afsm/sample/shop/feature/auth/AuthStateMachineTest.kt`, `sample-shop/src/test/kotlin/afsm/sample/shop/feature/editor/ProductEditorStateMachineTest.kt`, `consumer-smoke/`, `README.md`, `docs/afsm-public-api.md`, `docs/sample-shop-afsm-guide.md`, `wiki/00-context/current-state.md`, `wiki/00-context/open-questions.md`, `wiki/01-product/android-fsm-library-strategy.md`, `wiki/03-engineering/android-fsm-architecture.md`, `wiki/03-engineering/afsm-ksp-mmd-generation.md`, `wiki/03-engineering/afsm-v3-executable-dsl.md`, `wiki/06-project/decision-log.md`, `wiki/06-project/implementation-log.md`, `wiki/log.md`.

## [2026-05-11] meeting | Afsm Public API Usability Review

- Source: user request to run five sub-agent reviews of public API complexity and usability.
- Action: Recorded the review verdict, common findings, runtime risks, and usability hardening plan.
- Updated: `wiki/08-meetings/2026-05-11-afsm-public-api-usability-review.md`, `wiki/index.md`, `wiki/log.md`.

## [2026-05-11] implementation | Public API usability hardening

- Source: Afsm public API usability review and user request to prioritize simple, understandable usage.
- Action: Added `ViewModel.afsmHost(machine = ...)`, changed standard sample usage to the simpler overload, hardened `AfsmHost` command/event/invalid defaults, refreshed README onboarding, and updated API docs/wiki.
- Updated: `afsm-runtime/`, `afsm-viewmodel/`, `sample-shop/`, `consumer-smoke/`, `README.md`, `docs/afsm-public-api.md`, `docs/sample-shop-afsm-guide.md`, `wiki/00-context/current-state.md`, `wiki/00-context/open-questions.md`, `wiki/03-engineering/afsm-runtime-dispatch-loop.md`, `wiki/03-engineering/afsm-v3-executable-dsl.md`, `wiki/06-project/decision-log.md`, `wiki/06-project/implementation-log.md`, `wiki/log.md`.

## [2026-05-11] meeting | Afsm 10-agent usability POC

- Source: user request to run 10 Android developer POC reviews before deciding the next public structure.
- Action: Recorded cross-agent adoption verdict, common usability risks, release blockers, and next structure direction.
- Updated: `wiki/08-meetings/2026-05-11-afsm-10-agent-usability-poc.md`, `wiki/index.md`, `wiki/00-context/current-state.md`, `wiki/00-context/open-questions.md`, `wiki/06-project/decision-log.md`, `wiki/log.md`.

## [2026-05-11] implementation | Afsm usability hardening v2

- Source: user request to execute the full improvement plan after the 10-agent POC review.
- Action: Added `afsm-compose`, renamed the graphable public boundary to `AfsmMachine`, introduced `AfsmPhaseMachine`, added dynamic initial-state hosting, bounded command queue capacity, documented request-id stale command handling through Checkout, improved MMD output, and refreshed public onboarding docs.
- Updated: `afsm-core/`, `afsm-runtime/`, `afsm-viewmodel/`, `afsm-compose/`, `sample-shop/`, `consumer-smoke/`, `README.md`, `CHANGELOG.md`, `docs/`, `wiki/00-context/current-state.md`, `wiki/00-context/open-questions.md`, `wiki/01-product/android-fsm-library-strategy.md`, `wiki/03-engineering/`, `wiki/06-project/decision-log.md`, `wiki/06-project/implementation-log.md`, `wiki/log.md`.

## [2026-05-14] meeting | Afsm 10-agent CTO review

- Source: user request to keep looping until global Android developers would feel Afsm is attractive for complex screens.
- Action: Recorded the follow-up 10-agent review and CTO synthesis, including internal-beta verdict, release blockers, accepted/deferred feedback, and execution order.
- Updated: `wiki/08-meetings/2026-05-14-afsm-10-agent-cto-review.md`, `wiki/index.md`, `wiki/00-context/current-state.md`, `wiki/00-context/open-questions.md`, `wiki/06-project/decision-log.md`, `wiki/log.md`.

## [2026-05-14] implementation | Afsm adoption hardening loop

- Source: `wiki/08-meetings/2026-05-14-afsm-10-agent-cto-review.md`.
- Action: Hardened public ABI, guarded Checkout completion, hardened MMD output paths, added modeling rules, updated public docs, and verified the local release gate.
- Updated: `afsm-core/`, `afsm-graph-ksp/`, `afsm-runtime/`, `afsm-viewmodel/`, `sample-shop/`, `README.md`, `docs/`, `wiki/00-context/current-state.md`, `wiki/00-context/open-questions.md`, `wiki/03-engineering/afsm-ksp-mmd-generation.md`, `wiki/03-engineering/sample-shop-reference-app.md`, `wiki/06-project/implementation-log.md`, `wiki/log.md`.

## [2026-05-14] implementation | Runtime pressure and effect lifecycle hardening

- Source: CTO execution order item for runtime pressure and lifecycle tests.
- Action: Added fail-fast command queue overflow handling, documented the policy, and added tests for command queue saturation plus default no-replay effect delivery.
- Updated: `afsm-runtime/`, `README.md`, `docs/afsm-public-api.md`, `docs/modeling-rules.md`, `wiki/00-context/current-state.md`, `wiki/00-context/open-questions.md`, `wiki/03-engineering/afsm-runtime-dispatch-loop.md`, `wiki/06-project/decision-log.md`, `wiki/06-project/implementation-log.md`, `wiki/log.md`.

## [2026-05-14] documentation | Restoration, effect, and command policy

- Source: CTO execution order item for restoration/effect/command policy guide.
- Action: Added a public Android-facing policy guide and linked it from onboarding docs.
- Updated: `docs/restoration-effect-command-policy.md`, `README.md`, `docs/afsm-public-api.md`, `docs/modeling-rules.md`, `docs/sample-shop-afsm-guide.md`, `wiki/00-context/current-state.md`, `wiki/00-context/open-questions.md`, `wiki/06-project/implementation-log.md`, `wiki/log.md`.

## [2026-05-14] implementation | Graphable Checkout and curated examples

- Source: user request to improve and curate usage examples until Afsm feels attractive for complex Android screens.
- Action: Converted Checkout to a graphable phase/context machine, added generated graph coverage, created an example catalog and Checkout walkthrough, and updated public onboarding docs.
- Updated: `sample-shop/src/main/kotlin/afsm/sample/shop/feature/checkout/`, `sample-shop/src/test/kotlin/afsm/sample/shop/feature/checkout/CheckoutStateMachineTest.kt`, `README.md`, `docs/examples.md`, `docs/checkout-walkthrough.md`, `docs/modeling-rules.md`, `docs/sample-shop-afsm-guide.md`, `docs/testing-guide.md`, `wiki/03-engineering/afsm-example-catalog.md`, `wiki/03-engineering/sample-shop-reference-app.md`, `wiki/03-engineering/afsm-ksp-mmd-generation.md`, `wiki/00-context/current-state.md`, `wiki/00-context/open-questions.md`, `wiki/06-project/decision-log.md`, `wiki/06-project/implementation-log.md`, `wiki/log.md`.

## [2026-05-14] documentation | Primary example walkthroughs

- Source: follow-up review of the example catalog after Checkout was promoted.
- Action: Added dedicated Auth and ProductEditor walkthroughs, then linked all three primary walkthroughs from README, the example catalog, the sample guide, and wiki.
- Updated: `docs/auth-walkthrough.md`, `docs/product-editor-walkthrough.md`, `docs/examples.md`, `README.md`, `docs/sample-shop-afsm-guide.md`, `wiki/03-engineering/afsm-example-catalog.md`, `wiki/00-context/current-state.md`, `wiki/06-project/implementation-log.md`, `wiki/log.md`.

## [2026-05-16] implementation | GitHub README and CI

- Source: user request to proceed with GitHub first-screen polish after creating the private repository.
- Action: Added GitHub Actions CI, README badges/quickstart/status, release-readiness CI documentation, and Gradle-argument forwarding in verification scripts.
- Updated: `.github/workflows/ci.yml`, `README.md`, `docs/release-readiness.md`, `CONTRIBUTING.md`, `CHANGELOG.md`, `scripts/verify-release-local.sh`, `scripts/verify-consumer-smoke.sh`, `wiki/00-context/current-state.md`, `wiki/06-project/decision-log.md`, `wiki/06-project/implementation-log.md`, `wiki/log.md`.

## [2026-05-19] implementation | Six-agent usability hardening loop

- Source: user request to continue `/goal 12` style usability loops with six Android developer subagents per round.
- Action: Ran six initial usability reviewers and six post-change reviewers, added terminal-state DSL convenience, simplified README first-use onboarding, clarified graph generation setup, aligned Auth/Checkout render-state sample boundaries, documented ProductEditor execution order, and added internal beta pilot criteria.
- Updated: `afsm-core/src/main/kotlin/afsm/core/AfsmMachineDsl.kt`, `afsm-core/api/afsm-core.api`, `sample-shop/src/main/kotlin/afsm/sample/shop/feature/auth/`, `sample-shop/src/main/kotlin/afsm/sample/shop/feature/checkout/`, `README.md`, `CHANGELOG.md`, `consumer-smoke/README.md`, `docs/`, `wiki/08-meetings/2026-05-19-afsm-6-agent-usability-loop.md`, `wiki/index.md`, `wiki/00-context/current-state.md`, `wiki/06-project/decision-log.md`, `wiki/06-project/implementation-log.md`, `wiki/log.md`.

## [2026-05-19] implementation | Graph plugin and ProductEditor render-state loop

- Source: follow-up six-agent usability review finding that `.mmd` generation still required app-owned export wiring and ProductEditor exposed internal FSM phase details to Compose.
- Action: Added `io.github.afsm.graph` Gradle plugin, migrated `sample-shop` and `consumer-smoke` graph generation to the plugin, added ProductEditor render-state mapping, and refreshed public docs/wiki.
- Updated: `afsm-graph-gradle-plugin/`, `settings.gradle.kts`, `sample-shop/build.gradle.kts`, `sample-shop/src/main/kotlin/afsm/sample/shop/feature/editor/`, `consumer-smoke/`, `scripts/verify-release-local.sh`, `scripts/verify-consumer-smoke.sh`, `README.md`, `CHANGELOG.md`, `docs/`, `wiki/00-context/current-state.md`, `wiki/00-context/open-questions.md`, `wiki/03-engineering/afsm-ksp-mmd-generation.md`, `wiki/06-project/decision-log.md`, `wiki/06-project/implementation-log.md`, `wiki/log.md`.

## [2026-05-19] implementation | Graph tooling verification hardening

- Source: fourth six-agent usability review after graph Gradle plugin introduction.
- Action: Added KSP processor functional tests, graph Gradle plugin TestKit functional tests, normal unit-test/export-test separation, no-registry failure messaging, and consumer-smoke `.mmd` file assertions.
- Updated: `afsm-graph-ksp/`, `afsm-graph-gradle-plugin/`, `scripts/verify-consumer-smoke.sh`, `docs/graph-generation.md`, `docs/release-readiness.md`, `wiki/00-context/current-state.md`, `wiki/00-context/open-questions.md`, `wiki/03-engineering/afsm-ksp-mmd-generation.md`, `wiki/06-project/implementation-log.md`, `wiki/08-meetings/2026-05-19-afsm-6-agent-usability-loop.md`, `wiki/log.md`.

## [2026-05-20] implementation | Version alignment and command-result pressure hardening

- Source: fifth six-agent usability review focused on external setup, graph version behavior, runtime pressure, and sample adoption clarity.
- Action: Added shared `afsmVersion`, generated the graph plugin's default processor coordinate from that version, made `consumer-smoke` verify the current version with a clean dependency-refreshed fixture build, added command-result event overflow fail-fast handling, and improved README/example adoption guidance.
- Updated: `gradle.properties`, `build.gradle.kts`, `afsm-runtime/`, `afsm-graph-gradle-plugin/`, `consumer-smoke/`, `scripts/verify-consumer-smoke.sh`, `README.md`, `docs/`, `wiki/00-context/current-state.md`, `wiki/00-context/open-questions.md`, `wiki/06-project/decision-log.md`, `wiki/06-project/implementation-log.md`, `wiki/08-meetings/2026-05-19-afsm-6-agent-usability-loop.md`, `wiki/log.md`.

## [2026-05-21] implementation | Case-oriented DSL usability loop

- Source: user feedback that the public DSL direction is good, but `stay`,
  `otherwise`, and `transitionTo { updateContext(...) }` feel hard for Android
  developers.
- Action: Accepted breaking API cleanup, added the `case(...)` event-branch API,
  kept context changes under `updateContext(...)` including an event-aware
  `updateContext { context, event -> ... }` overload, added direct effect
  helpers, and began migrating samples to the style where `transitionTo` only
  means phase change.
- Updated: `afsm-core/src/main/kotlin/afsm/core/AfsmMachineDsl.kt`,
  `afsm-core/src/test/kotlin/afsm/core/AfsmExecutableDslCompileCheckTest.kt`,
  `consumer-smoke/app/src/main/kotlin/afsm/consumer/smoke/ConsumerSmoke.kt`,
  `sample-shop/src/main/kotlin/afsm/sample/shop/feature/editor/ProductEditorStateMachine.kt`,
  `sample-shop/src/main/kotlin/afsm/sample/shop/feature/auth/AuthStateMachine.kt`,
  `sample-shop/src/main/kotlin/afsm/sample/shop/feature/checkout/CheckoutStateMachine.kt`,
  `wiki/03-engineering/afsm-v3-executable-dsl.md`,
  `wiki/06-project/decision-log.md`, `wiki/06-project/implementation-log.md`,
  `wiki/log.md`.

## [2026-05-21] implementation | Public condition terminology cleanup

- Source: follow-up review of the case-oriented DSL public API vocabulary.
- Action: Renamed public topology metadata from `guardLabel` to
  `conditionLabel`, aligned `ignore`/`invalid` optional predicates with
  `condition = { ... }`, and updated canonical docs/wiki wording.
- Updated: `afsm-core/`, `sample-shop/`, `docs/`, `wiki/00-context/current-state.md`,
  `wiki/00-context/open-questions.md`, `wiki/03-engineering/afsm-v3-executable-dsl.md`,
  `wiki/06-project/decision-log.md`, `wiki/06-project/implementation-log.md`,
  `wiki/log.md`.

## [2026-05-21] implementation | Explicit validation branch conditions

- Source: follow-up review of validation cases after removing DSL-level
  `otherwise`.
- Action: Made Auth and ProductEditor invalid validation branches declare
  explicit `condition = ...` predicates, and updated public examples/wiki.
- Updated: `sample-shop/`, `afsm-core/src/test/kotlin/afsm/core/AfsmExecutableDslCompileCheckTest.kt`,
  `docs/`, `wiki/00-context/current-state.md`,
  `wiki/03-engineering/afsm-v3-executable-dsl.md`,
  `wiki/03-engineering/sample-shop-reference-app.md`,
  `wiki/06-project/decision-log.md`, `wiki/06-project/implementation-log.md`,
  `wiki/log.md`.

## [2026-05-21] implementation | Payload factory ordering and graph visibility hardening

- Source: six-agent Android developer usability review of the case-oriented
  DSL.
- Action: Moved payload phase factory execution after exit/case actions, made
  named no-transition condition cases visible in Flow `.mmd`, fixed Checkout
  missing-product conditions/tests, and corrected public docs.
- Updated: `afsm-core/`, `sample-shop/`, `README.md`, `docs/`,
  `wiki/index.md`,
  `wiki/00-context/current-state.md`, `wiki/03-engineering/afsm-v3-executable-dsl.md`,
  `wiki/03-engineering/afsm-v3-terminology-transition-actions.md`,
  `wiki/06-project/decision-log.md`, `wiki/06-project/implementation-log.md`,
  `wiki/08-meetings/2026-05-21-afsm-6-agent-dsl-usability-review.md`,
  `wiki/log.md`.

## [2026-05-21] implementation | Graph plugin Flow and Full options

- Source: six-agent graph usability review finding that `Full` was available in
  core but not from `generateAfsmMmd`.
- Action: Added `afsmGraph.mmdOptions`, `-PafsmMmdOptions=Full`, generated test
  option mapping, TestKit coverage, and docs.
- Updated: `afsm-graph-gradle-plugin/`, `README.md`, `docs/afsm-public-api.md`,
  `docs/graph-generation.md`, `wiki/00-context/current-state.md`,
  `wiki/06-project/decision-log.md`, `wiki/06-project/implementation-log.md`,
  `wiki/log.md`.

## [2026-05-21] implementation | ProductEditor save event naming cleanup

- Source: six-agent DSL usability review finding that `DraftSaved` was both a
  phase and an event.
- Action: Renamed the ProductEditor save-result event to `DraftSaveCompleted`
  while keeping `DraftSaved` as the phase.
- Updated: `sample-shop/`, `afsm-core/src/test/kotlin/afsm/core/AfsmExecutableDslCompileCheckTest.kt`,
  `docs/product-editor-walkthrough.md`, `wiki/00-context/current-state.md`,
  `wiki/03-engineering/afsm-v3-executable-dsl.md`,
  `wiki/06-project/implementation-log.md`, `wiki/log.md`.

## [2026-05-23] implementation | Entry command label usability cleanup

- Source: user review that Afsm still does not feel natural for Android
  developers and that separate `commandLabels` metadata is hard to justify.
- Action: Changed entry/exit DSL actions so graph labels are declared on the
  actual `command(label = ...) { ... }` / `effect(label = ...) { ... }`
  statements, removed beginner-facing `Afsm.stay(...)`, simplified Auth no-op
  enumeration, and renamed sample condition helpers toward domain intent.
- Updated: `afsm-core/`, `sample-shop/`, `README.md`, `docs/`,
  `wiki/00-context/current-state.md`, `wiki/00-context/open-questions.md`,
  `wiki/03-engineering/afsm-v3-executable-dsl.md`,
  `wiki/06-project/decision-log.md`, `wiki/log.md`.

## [2026-05-23] implementation | First-use API terminology cleanup

- Source: six-agent review from the perspective of Android developers seeing
  Afsm for the first time.
- Action: Renamed first-use public vocabulary to `phase`, `data`, and
  `handled`; removed public `AfsmPhaseMachine`; added an Android-first getting
  started guide; updated samples, tests, docs, and canonical wiki pages.
- Updated: `afsm-core/`, `afsm-runtime/`, `afsm-viewmodel/`,
  `afsm-graph-ksp/`, `sample-shop/`, `consumer-smoke/`, `README.md`, `docs/`,
  `wiki/00-context/current-state.md`, `wiki/00-context/open-questions.md`,
  `wiki/03-engineering/`, `wiki/06-project/decision-log.md`,
  `wiki/06-project/implementation-log.md`,
  `wiki/08-meetings/2026-05-23-afsm-6-agent-first-use-review.md`,
  `wiki/index.md`, `wiki/log.md`.

## [2026-05-23] implementation | Second first-use usability hardening

- Source: follow-up six-agent review from first-time Android developer,
  architecture, Kotlin API, sample POC, docs, and CTO/adoption perspectives.
- Action: Made DSL branch predicates and payload phase factories read-only,
  moved getting-started to a minimal Draft-first flow, clarified API choice and
  phase payload rules, reduced Checkout `ignore(...)` enumeration, and made
  sample ViewModel state/effect types explicit.
- Updated: `afsm-core/`, `sample-shop/`, `README.md`, `docs/`,
  `wiki/00-context/current-state.md`, `wiki/00-context/open-questions.md`,
  `wiki/06-project/decision-log.md`, `wiki/06-project/implementation-log.md`,
  `wiki/08-meetings/2026-05-23-afsm-6-agent-second-first-use-review.md`,
  `wiki/index.md`, `wiki/log.md`.

## [2026-05-25] implementation | Draft quickstart consumer compile check

- Source: API usability review of whether a first-time Android developer can
  paste the getting-started Draft machine and ViewModel without hitting API
  drift.
- Action: Mirrored the Draft quickstart in `consumer-smoke`, annotated it for
  graph export, and extended consumer smoke verification to check its `.mmd`
  output.
- Updated: `consumer-smoke/`, `scripts/verify-consumer-smoke.sh`, `README.md`,
  `docs/getting-started.md`, `docs/release-readiness.md`,
  `wiki/00-context/current-state.md`, `wiki/06-project/implementation-log.md`,
  `wiki/log.md`.

## [2026-05-25] implementation | Direct command handler lambda examples

- Source: API usability review of the first ViewModel code Android developers
  copy from README/getting-started.
- Action: Replaced beginner-facing `AfsmCommandHandler { ... }` wrapper usage
  with direct `commandHandler = { command: Command, dispatch -> ... }` lambdas
  in docs, sample ViewModels, and consumer smoke fixtures.
- Updated: `sample-shop/`, `consumer-smoke/`, `README.md`, `docs/`,
  `wiki/00-context/current-state.md`, `wiki/06-project/implementation-log.md`,
  `wiki/log.md`.

## [2026-05-25] documentation | Getting-started paste checklist

- Source: API usability review of where first-time Android developers pause
  before pasting the Draft machine into an app module.
- Action: Added dependency, AndroidX, file-layout, and import checklists at the
  start of `docs/getting-started.md`, and explicitly deferred graph generation
  until after the machine is useful.
- Updated: `docs/getting-started.md`, `wiki/00-context/current-state.md`,
  `wiki/06-project/implementation-log.md`, `wiki/log.md`.

## [2026-05-25] documentation | Draft command failure path

- Source: API usability review of the first repository-backed Draft example.
- Action: Added `DraftSaveFailed(message)` to the quickstart contract, modeled
  `Saving -> Editing` failure recovery with `errorMessage`, and changed the
  ViewModel command handler to dispatch success/failure result events.
- Updated: `README.md`, `docs/getting-started.md`, `docs/examples.md`,
  `consumer-smoke/`, `scripts/verify-consumer-smoke.sh`,
  `wiki/00-context/current-state.md`, `wiki/06-project/implementation-log.md`,
  `wiki/log.md`.

## [2026-05-25] documentation | Getting-started ViewModel scaffold

- Source: API usability review of the `DraftViewModel.kt` paste path in
  `docs/getting-started.md`.
- Action: Replaced the partial host-only snippet with a full `DraftViewModel`
  scaffold including repository contract, `StateFlow<DraftState>`, and
  `onEvent(event)` forwarding.
- Updated: `docs/getting-started.md`, `wiki/00-context/current-state.md`,
  `wiki/06-project/implementation-log.md`, `wiki/log.md`.

## [2026-05-25] documentation | Getting-started initial entry note

- Source: API usability review of how first-time users start initial repository
  work from a machine.
- Action: Added a note that initial state construction does not run `onEnter`;
  startup work should come from an explicit event such as `ScreenEntered`.
- Updated: `docs/getting-started.md`, `wiki/00-context/current-state.md`,
  `wiki/06-project/implementation-log.md`, `wiki/log.md`.

## [2026-05-25] documentation | Command failure test guidance

- Source: API usability review of the first machine test path after adding
  `DraftSaveFailed`.
- Action: Added command failure result testing to `docs/testing-guide.md` and
  README's feature-test checklist.
- Updated: `README.md`, `docs/testing-guide.md`,
  `wiki/00-context/current-state.md`, `wiki/06-project/implementation-log.md`,
  `wiki/log.md`.

## [2026-05-25] api-docs | Command handler default behavior

- Source: API usability review of `ViewModel.afsmHost(...)` signatures and the
  `AfsmCommandHandler.none()` default.
- Action: Added KDoc and public API documentation that the default command
  handler intentionally ignores commands and is only for no-command machines;
  Kotlin callers should normally pass a direct command handler lambda.
- Updated: `afsm-runtime/`, `afsm-viewmodel/`, `docs/afsm-public-api.md`,
  `wiki/00-context/current-state.md`, `wiki/06-project/implementation-log.md`,
  `wiki/log.md`.

## [2026-05-25] documentation | Graph generation setup checklist

- Source: API usability review of the first graph/KSP setup path after the
  getting-started guide defers graph generation.
- Action: Added a preflight checklist and documented Maven Local repository
  setup for both plugin and dependency resolution in `docs/graph-generation.md`.
- Updated: `docs/graph-generation.md`, `wiki/00-context/current-state.md`,
  `wiki/06-project/implementation-log.md`, `wiki/log.md`.

## [2026-05-25] documentation | Checkout ViewModel startup snippet

- Source: API usability review of the Checkout walkthrough after adding the
  getting-started initial-entry note.
- Action: Replaced `commandHandler = ...` with a complete ViewModel startup
  bridge that dispatches `ScreenEntered` and shows command result handling.
- Updated: `docs/checkout-walkthrough.md`,
  `wiki/00-context/current-state.md`, `wiki/06-project/implementation-log.md`,
  `wiki/log.md`.

## [2026-05-25] wiki | ViewModel integration sync

- Source: Stale API check after documenting `AfsmCommandHandler.none()` and
  direct command handler lambdas.
- Action: Updated `wiki/03-engineering/afsm-viewmodel-integration.md` to match
  the current reducer overload order and command-handler default guidance.
- Updated: `wiki/03-engineering/afsm-viewmodel-integration.md`,
  `wiki/06-project/implementation-log.md`, `wiki/log.md`.

## [2026-05-25] documentation | Auth ViewModel wiring snippet

- Source: API usability review of the smallest real Android walkthrough.
- Action: Replaced the command-handler comment with a full `AuthViewModel`
  wiring snippet that dispatches typed success/failure events.
- Updated: `docs/auth-walkthrough.md`,
  `wiki/00-context/current-state.md`, `wiki/06-project/implementation-log.md`,
  `wiki/log.md`.

## [2026-05-25] documentation | Minimal Draft example links

- Source: API usability review of the example catalog after strengthening the
  getting-started and consumer-smoke Draft path.
- Action: Updated the Minimal Draft row and description to point to
  `docs/getting-started.md` and the consumer-smoke `DraftQuickstart.kt` mirror.
- Updated: `docs/examples.md`, `wiki/00-context/current-state.md`,
  `wiki/06-project/implementation-log.md`, `wiki/log.md`.

## [2026-05-25] documentation | Modeling rules reading order

- Source: API usability review of stale first-reading links after the Draft
  quickstart became the canonical first-use path.
- Action: Updated `docs/modeling-rules.md` to start with
  `docs/getting-started.md`, then examples, Auth, and Checkout.
- Updated: `docs/modeling-rules.md`, `wiki/00-context/current-state.md`,
  `wiki/06-project/implementation-log.md`, `wiki/log.md`.

## [2026-05-25] documentation | Sample guide API feedback sync

- Source: Stale wording scan after aligning public onboarding around
  `AfsmMachine<State, Event, Command, Effect>`.
- Action: Reframed `AfsmReducer` as a lower-level runtime contract in
  `docs/sample-shop-afsm-guide.md`.
- Updated: `docs/sample-shop-afsm-guide.md`,
  `wiki/00-context/current-state.md`, `wiki/06-project/implementation-log.md`,
  `wiki/log.md`.

## [2026-05-25] verification | Consumer smoke Draft tests

- Source: API usability review of whether the Draft quickstart failure path was
  executable in the external consumer gate.
- Action: Added consumer-smoke JVM tests for Draft command emission and save
  failure recovery, made `verify-consumer-smoke.sh` run them, and fixed the
  quickstart failure branch so `updateData` and `transitionTo` run in the same
  `case`.
- Updated: `consumer-smoke/`, `scripts/verify-consumer-smoke.sh`,
  `README.md`, `docs/getting-started.md`, `docs/release-readiness.md`,
  `wiki/00-context/current-state.md`, `wiki/06-project/implementation-log.md`,
  `wiki/log.md`.

## [2026-05-25] documentation | Case composition rule

- Source: Follow-up after consumer-smoke tests exposed the sibling
  `updateData` / `transitionTo` quickstart bug.
- Action: Documented that multi-action event handling must use one
  `case { ... }`, and that top-level shorthand calls are separate alternatives.
- Updated: `afsm-core/`, `docs/getting-started.md`,
  `docs/modeling-rules.md`, `docs/afsm-public-api.md`,
  `wiki/00-context/current-state.md`, `wiki/06-project/implementation-log.md`,
  `wiki/log.md`.

## [2026-05-25] documentation | Effect boundary in first-use docs

- Source: Follow-up API usability review of the first-time Android developer
  path.
- Action: Documented that `AfsmNoEffect` is the starting point for screens with
  no UI one-shot output, and that `afsm-compose` is only needed when a Compose
  route collects effects.
- Updated: `README.md`, `docs/getting-started.md`, `docs/afsm-public-api.md`,
  `wiki/00-context/current-state.md`, `wiki/06-project/implementation-log.md`,
  `wiki/log.md`.

## [2026-05-25] documentation | First quickstart JVM tests

- Source: Follow-up API usability review of the first-time Android developer
  path.
- Action: Added the first two Draft transition tests to getting-started before
  ViewModel wiring, matching the consumer-smoke executable quickstart tests.
- Updated: `docs/getting-started.md`, `docs/testing-guide.md`,
  `wiki/00-context/current-state.md`, `wiki/06-project/implementation-log.md`,
  `wiki/log.md`.

## [2026-05-25] docs+fixture | Quickstart test dependency

- Source: Follow-up after adding first JUnit quickstart tests.
- Action: Added explicit JUnit dependency guidance to quickstart docs and the
  consumer-smoke fixture.
- Updated: `README.md`, `docs/getting-started.md`,
  `docs/release-readiness.md`, `consumer-smoke/`, `wiki/00-context/current-state.md`,
  `wiki/06-project/implementation-log.md`, `wiki/log.md`.

## [2026-05-25] fixture | Draft quickstart mirror formatting

- Source: Follow-up mirror review after updating getting-started snippets.
- Action: Reformatted the consumer-smoke Draft ViewModel command-handler block
  to match the public getting-started snippet.
- Updated: `consumer-smoke/app/src/main/kotlin/afsm/consumer/smoke/DraftQuickstart.kt`,
  `wiki/06-project/implementation-log.md`, `wiki/log.md`.

## [2026-05-25] api-docs | Low-level Afsm helper KDoc

- Source: Follow-up API surface review for first-time Android developers.
- Action: Added KDoc that frames `Afsm` transition factory helpers as custom
  reducer support and directs normal feature code to `afsmMachine { ... }`.
- Updated: `afsm-core/src/main/kotlin/afsm/core/Afsm.kt`,
  `wiki/00-context/current-state.md`, `wiki/06-project/implementation-log.md`,
  `wiki/log.md`.

## [2026-05-25] wiki | Effect policy open-question cleanup

- Source: Follow-up stale-question review after first-use effect documentation.
- Action: Moved UI one-shot modeling and required navigation durability from
  open questions to resolved policy.
- Updated: `wiki/00-context/open-questions.md`,
  `wiki/00-context/current-state.md`, `wiki/06-project/implementation-log.md`,
  `wiki/log.md`.

## [2026-05-25] documentation | Example reading path test loop

- Source: Follow-up after adding first quickstart JVM tests.
- Action: Added the quickstart test loop to example and modeling-rule reading
  orders before larger sample adoption.
- Updated: `docs/examples.md`, `docs/modeling-rules.md`,
  `wiki/00-context/current-state.md`, `wiki/06-project/implementation-log.md`,
  `wiki/log.md`.

## [2026-05-25] documentation | README first-use test step

- Source: Follow-up after aligning example reading order with first JVM tests.
- Action: Added plain JVM transition tests to README's first-use short version.
- Updated: `README.md`, `wiki/00-context/current-state.md`,
  `wiki/06-project/implementation-log.md`, `wiki/log.md`.

## [2026-05-25] test | Case composition regression

- Source: Follow-up after the Draft quickstart sibling `updateData` /
  `transitionTo` bug.
- Action: Added an `afsm-core` DSL regression test that locks top-level
  shorthand branches as alternatives, not merged actions.
- Updated: `afsm-core/src/test/kotlin/afsm/core/AfsmExecutableDslCompileCheckTest.kt`,
  `wiki/00-context/current-state.md`, `wiki/06-project/implementation-log.md`,
  `wiki/log.md`.

## [2026-05-25] test | Draft quickstart validation branch

- Source: Follow-up quickstart test coverage review.
- Action: Added a missing-title validation test to getting-started and
  consumer-smoke Draft quickstart tests.
- Updated: `docs/getting-started.md`,
  `consumer-smoke/app/src/test/kotlin/afsm/consumer/smoke/DraftQuickstartTest.kt`,
  `wiki/00-context/current-state.md`, `wiki/06-project/implementation-log.md`,
  `wiki/log.md`.

## [2026-05-25] documentation | README and release quickstart sync

- Source: Follow-up after expanding Draft quickstart transition tests.
- Action: Reformatted the README Draft ViewModel snippet and updated
  release-readiness to include validation branch coverage.
- Updated: `README.md`, `docs/release-readiness.md`,
  `wiki/00-context/current-state.md`, `wiki/06-project/implementation-log.md`,
  `wiki/log.md`.

## [2026-05-25] api+test | Afsm test helper quickstart dogfood

- Source: First-use testing ergonomics review.
- Action: Added `afsm-test` transition assertion helpers and dogfooded them in
  Draft quickstart tests through `consumer-smoke`.
- Updated: `settings.gradle.kts`, `afsm-test/`, `consumer-smoke/`,
  `scripts/verify-release-local.sh`, `README.md`, `docs/getting-started.md`,
  `docs/testing-guide.md`, `docs/afsm-public-api.md`,
  `docs/release-readiness.md`, `CHANGELOG.md`,
  `wiki/00-context/current-state.md`, `wiki/00-context/open-questions.md`,
  `wiki/06-project/decision-log.md`, `wiki/06-project/implementation-log.md`,
  `wiki/log.md`.

## [2026-05-25] implementation | Sample-shop test helper dogfood

- Source: Follow-up after adding `afsm-test`.
- Action: Updated sample-shop state-machine tests to use `afsm-test` helpers
  for transition assertions while leaving render-state and topology checks
  explicit.
- Updated: `sample-shop/build.gradle.kts`,
  `sample-shop/src/test/kotlin/afsm/sample/shop/feature/auth/AuthStateMachineTest.kt`,
  `sample-shop/src/test/kotlin/afsm/sample/shop/feature/checkout/CheckoutStateMachineTest.kt`,
  `sample-shop/src/test/kotlin/afsm/sample/shop/feature/editor/ProductEditorStateMachineTest.kt`,
  `wiki/00-context/current-state.md`, `wiki/06-project/implementation-log.md`,
  `wiki/log.md`.

## [2026-05-25] documentation | Changelog API vocabulary cleanup

- Source: API usability review of release notes for first-time Android
  developers.
- Action: Updated `CHANGELOG.md` so the unreleased Added section uses the
  current `phase`, `data`, and `Handled` vocabulary, while superseded
  pre-release DSL names are listed only under Removed.
- Updated: `CHANGELOG.md`, `wiki/00-context/current-state.md`,
  `wiki/06-project/implementation-log.md`, `wiki/log.md`.

## [2026-05-25] test+docs | Draft ViewModel consumer test

- Source: First-use testing review after adding machine transition helpers.
- Action: Added external-consumer Draft ViewModel wiring tests with a test main
  dispatcher and documented the same pattern in the testing guide.
- Updated: `consumer-smoke/app/build.gradle.kts`,
  `consumer-smoke/app/src/test/kotlin/afsm/consumer/smoke/DraftViewModelTest.kt`,
  `consumer-smoke/README.md`, `docs/testing-guide.md`,
  `docs/release-readiness.md`, `wiki/00-context/current-state.md`,
  `wiki/06-project/implementation-log.md`, `wiki/log.md`.

## [2026-05-25] documentation | Getting-started ViewModel test link

- Source: Review after adding external-consumer Draft ViewModel tests.
- Action: Added a getting-started bridge from pure Draft machine tests to the
  first ViewModel wiring test pattern and its executable consumer-smoke mirror.
- Updated: `docs/getting-started.md`, `wiki/00-context/current-state.md`,
  `wiki/06-project/implementation-log.md`, `wiki/log.md`.

## [2026-05-25] documentation | README ViewModel test link

- Source: Review of the README first-use short path after adding ViewModel
  consumer tests.
- Action: Added a ViewModel wiring test step and linked README readers to the
  testing guide and executable `consumer-smoke` Draft ViewModel test.
- Updated: `README.md`, `wiki/00-context/current-state.md`,
  `wiki/06-project/implementation-log.md`, `wiki/log.md`.
