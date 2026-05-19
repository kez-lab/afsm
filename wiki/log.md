# Wiki Log

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
