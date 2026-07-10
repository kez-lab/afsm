---
title: Wiki Index
updated: 2026-07-10
---

# Wiki Index

## Context

- [[00-context/current-state|Current State]] - Current project direction and durable summary.
- [[00-context/open-questions|Open Questions]] - Unresolved architecture and implementation questions.

## Product

- [[01-product/android-fsm-library-strategy|Android FSM Library Strategy]] - Product goal, users, positioning, MVP, and success criteria.

## Engineering

- [[03-engineering/afsm-public-api-draft|Historical Afsm Public API Draft]] - Superseded initial API and module proposal retained as design history.
- [[03-engineering/afsm-public-api-draft-v2|Historical Afsm Public API Draft v2]] - Superseded implementation-candidate draft retained to explain the path to v3.
- [[03-engineering/afsm-v3-executable-dsl|Afsm v3 Executable DSL]] - Canonical v3 direction: a scoped executable statechart DSL that is runtime definition, graph source, and test target.
- [[03-engineering/afsm-reference-architecture-review|Afsm Reference Architecture Review]] - Comparison against XState, SCXML, Kotlin state-machine libraries, Redux/Elm, Workflow, and Android guidance.
- [[03-engineering/afsm-example-catalog|Afsm Example Catalog]] - Canonical example ladder from minimal Draft to Auth, Checkout, ProductEditor, and non-Afsm data screens.
- [[03-engineering/afsm-ksp-mmd-generation|Afsm KSP MMD Generation]] - KSP-based automatic discovery and `.mmd` generation design for multiple state machines.
- [[03-engineering/afsm-v3-topology-first-api|Superseded Afsm v3 Phased State API]] - Historical phased-state helper direction superseded by the executable DSL plan.
- [[03-engineering/afsm-phased-core-spike|Afsm Phased Core Spike]] - Historical compile/test validation for phased state helpers that were later removed from `afsm-core`.
- [[03-engineering/afsm-v3-terminology-transition-actions|Afsm v3 Terminology and Transition Actions]] - Clarifies Command as a transition action, separates Event/Action/Effect directionality, and defines ProductEditor naming policy.
- [[03-engineering/afsm-core-compile-validation|Afsm Core Compile Validation]] - Minimal `afsm-core` Kotlin project setup and compile validation for `AfsmNoEffect` and `AfsmTransition<S, C, F>`.
- [[03-engineering/afsm-runtime-dispatch-loop|Afsm Runtime Dispatch Loop]] - Minimal coroutine runtime implementation and verification for serialized dispatch, decisions, commands, and effects.
- [[03-engineering/afsm-viewmodel-integration|Afsm ViewModel Integration]] - Thin AndroidX Lifecycle module that wires `AfsmHost` to `viewModelScope` through `ViewModel.afsmHost(...)`.
- [[03-engineering/sample-shop-reference-app|Sample Shop Reference App]] - Complex Compose + Room sample app that validates where Afsm helps and where ordinary ViewModel state is preferable.
- [[03-engineering/android-official-guidance|Android Official Guidance]] - Official Android documentation distilled into constraints for this FSM architecture.
- [[03-engineering/android-fsm-architecture|Android FSM Architecture]] - Core architecture for ViewModel-backed plain Kotlin FSMs.
- [[03-engineering/fsm-runtime-roadmap|Historical FSM Runtime Roadmap]] - Superseded build-order plan retained as implementation history.
- [[03-engineering/library-delivery-plan|Historical Library Delivery Plan]] - Staged plan retained as history; current gates live in release readiness and open questions.
- [[03-engineering/reference-flow-signup-identity-retry|Reference Flow - Signup Identity Retry]] - First reference flow design with State/Event/Command/Effect policy.
- [[03-engineering/signup-state-machine-pseudo-implementation|Signup StateMachine Pseudo Implementation]] - Kotlin-like pseudo implementation used to validate Afsm API ergonomics.
- [[03-engineering/state-event-command-effect|State, Event, Command, Effect]] - Terms and boundaries for the FSM model.
- [[03-engineering/viewmodel-fsm-boundaries|ViewModel and FSM Boundaries]] - Responsibility split across View, ViewModel, FSM, and UseCase layers.
- [[03-engineering/testing-strategy|Testing Strategy]] - How to test transitions, ViewModel command execution, and UI rendering.

## Project

- [[06-project/long-term-goal|Afsm Long-Term Goal]] - Active usability-first product goal with full pre-release design freedom and an evidence-driven improvement loop.
- [[06-project/goal-evidence-baseline-2026-07-10|Afsm Goal Evidence Baseline 2026-07-10]] - Outcome-by-outcome proof audit and first redesign-cycle selection.
- [[06-project/decision-log|Decision Log]] - Durable architecture decisions.
- [[06-project/implementation-log|Implementation Log]] - Chronological implementation changes and verification commands.
- [[log|Wiki Log]] - Append-only chronological record of wiki operations and durable documentation changes.

## QA

- [[05-qa/verification-report-2026-05-09-sample-shop-fsm-smoke|Sample Shop FSM Smoke Verification]] - Android CLI layout/screenshot verification for signup and product registration FSM flows.
- [[05-qa/verification-report-2026-05-09-product-editor-transition-action-rename-smoke|ProductEditor Transition Action Rename Smoke Verification]] - Android CLI regression evidence after ProductEditor state/command naming cleanup.
- [[05-qa/verification-report-2026-05-09-product-editor-executable-dsl-smoke|ProductEditor Executable DSL Smoke Verification]] - Android CLI regression evidence after migrating ProductEditor to the executable DSL.

## LLM Operations

- [[07-llm/ai-engineering-guardrails|AI Engineering Guardrails]] - Project-scoped software engineering, TDD, and verification integrity rules for AI agents.
- [[07-llm/codex-project-workflow|Codex Project Workflow]] - Required LLM Wiki retrieval, current-evidence verification, and change synchronization workflow for all Codex project tasks.
- [[07-llm/wiki-lint-2026-07-10|Wiki Lint 2026-07-10]] - Code-backed lint report for current state, open questions, CI policy drift, and index health.
- [[07-llm/wiki-maintenance-guide|Wiki Maintenance Guide]] - How this project uses the LLM Wiki pattern.

## Meetings

- [[08-meetings/2026-05-01-afsm-api-pseudo-implementation-review|Afsm API Pseudo Implementation Review]] - Review of signup pseudo-implementation and Afsm API ergonomics.
- [[08-meetings/2026-05-11-afsm-public-api-usability-review|Afsm Public API Usability Review]] - Five-perspective review of public API complexity, runtime risks, graph semantics, and onboarding.
- [[08-meetings/2026-05-11-afsm-10-agent-usability-poc|Afsm 10-Agent Usability POC]] - Android developer POC review covering onboarding, TDD, runtime, graph generation, commerce sample, and OSS adoption.
- [[08-meetings/2026-05-14-afsm-10-agent-cto-review|Afsm 10-Agent CTO Review]] - Follow-up Android developer review and CTO synthesis that sets the internal-beta verdict and hardening order.
- [[08-meetings/2026-05-19-afsm-6-agent-usability-loop|Afsm 6-Agent Usability Loop]] - Two-round usability review covering first-use onboarding, render-state sample boundaries, ProductEditor explanation, graph generation, and beta adoption contract.
- [[08-meetings/2026-05-21-afsm-6-agent-dsl-usability-review|Afsm 6-Agent DSL Usability Review]] - Review of the case-oriented DSL, payload phase factory order, no-transition graph visibility, and documentation drift.
- [[08-meetings/2026-05-23-afsm-6-agent-first-use-review|Afsm 6-Agent First-Use Review]] - First-time Android developer review that drove the phase/data/handled terminology cleanup and Android-first onboarding page.
- [[08-meetings/2026-05-23-afsm-6-agent-second-first-use-review|Afsm 6-Agent Second First-Use Review]] - Follow-up review that moved onboarding to Draft-first docs, made DSL predicates read-only, reduced Checkout ignore usage, and clarified phase payload rules.

## Raw Sources

- [Android ViewModel FSM Discussion](../raw/conversations/2026-05-01-android-viewmodel-fsm-discussion.md)
- [Android Official Docs Research](../raw/sources/2026-05-01-android-official-docs-fsm-research.md)
- [LLM Wiki Pattern](../raw/sources/2026-05-01-llm-wiki-pattern.md)
