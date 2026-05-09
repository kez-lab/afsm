---
title: Wiki Index
updated: 2026-05-09
---

# Wiki Index

## Context

- [[00-context/current-state|Current State]] - Current project direction and durable summary.
- [[00-context/open-questions|Open Questions]] - Unresolved architecture and implementation questions.

## Product

- [[01-product/android-fsm-library-strategy|Android FSM Library Strategy]] - Product goal, users, positioning, MVP, and success criteria.

## Engineering

- [[03-engineering/afsm-public-api-draft|Afsm Public API Draft]] - Initial public API proposal, naming, modules, dependency policy, runtime, ViewModel, and test APIs.
- [[03-engineering/afsm-public-api-draft-v2|Afsm Public API Draft v2]] - Implementation-candidate API draft covering Stayed, AfsmNoEffect, effect delivery, and dispatch serialization.
- [[03-engineering/afsm-v3-topology-first-api|Afsm v3 Topology-First API]] - ProductEditor-based comparison of v2 reducer-style state machines and a graph-friendly `transition<From, Event, To>` authoring layer.
- [[03-engineering/afsm-v3-terminology-transition-actions|Afsm v3 Terminology and Transition Actions]] - Clarifies Command as a transition action, separates Event/Action/Effect directionality, and defines ProductEditor naming policy.
- [[03-engineering/afsm-core-compile-validation|Afsm Core Compile Validation]] - Minimal `afsm-core` Kotlin project setup and compile validation for `AfsmNoEffect` and `AfsmTransition<S, C, F>`.
- [[03-engineering/afsm-runtime-dispatch-loop|Afsm Runtime Dispatch Loop]] - Minimal coroutine runtime implementation and verification for serialized dispatch, decisions, commands, and effects.
- [[03-engineering/afsm-viewmodel-integration|Afsm ViewModel Integration]] - Thin AndroidX Lifecycle module that wires `AfsmHost` to `viewModelScope` through `ViewModel.afsmHost(...)`.
- [[03-engineering/sample-shop-reference-app|Sample Shop Reference App]] - Complex Compose + Room sample app that validates where Afsm helps and where ordinary ViewModel state is preferable.
- [[03-engineering/android-official-guidance|Android Official Guidance]] - Official Android documentation distilled into constraints for this FSM architecture.
- [[03-engineering/android-fsm-architecture|Android FSM Architecture]] - Core architecture for ViewModel-backed plain Kotlin FSMs.
- [[03-engineering/fsm-runtime-roadmap|FSM Runtime Roadmap]] - Suggested build order for the Android FSM foundation.
- [[03-engineering/library-delivery-plan|Library Delivery Plan]] - End-to-end plan for turning the FSM architecture into a usable Android library.
- [[03-engineering/reference-flow-signup-identity-retry|Reference Flow - Signup Identity Retry]] - First reference flow design with State/Event/Command/Effect policy.
- [[03-engineering/signup-state-machine-pseudo-implementation|Signup StateMachine Pseudo Implementation]] - Kotlin-like pseudo implementation used to validate Afsm API ergonomics.
- [[03-engineering/state-event-command-effect|State, Event, Command, Effect]] - Terms and boundaries for the FSM model.
- [[03-engineering/viewmodel-fsm-boundaries|ViewModel and FSM Boundaries]] - Responsibility split across View, ViewModel, FSM, and UseCase layers.
- [[03-engineering/testing-strategy|Testing Strategy]] - How to test transitions, ViewModel command execution, and UI rendering.

## Project

- [[06-project/decision-log|Decision Log]] - Durable architecture decisions.
- [[06-project/implementation-log|Implementation Log]] - Chronological implementation changes and verification commands.

## QA

- [[05-qa/verification-report-2026-05-09-sample-shop-fsm-smoke|Sample Shop FSM Smoke Verification]] - Android CLI layout/screenshot verification for signup and product registration FSM flows.
- [[05-qa/verification-report-2026-05-09-product-editor-transition-action-rename-smoke|ProductEditor Transition Action Rename Smoke Verification]] - Android CLI regression evidence after ProductEditor state/command naming cleanup.

## LLM Operations

- [[07-llm/ai-engineering-guardrails|AI Engineering Guardrails]] - Project-scoped software engineering, TDD, and verification integrity rules for AI agents.
- [[07-llm/wiki-maintenance-guide|Wiki Maintenance Guide]] - How this project uses the LLM Wiki pattern.

## Meetings

- [[08-meetings/2026-05-01-afsm-api-pseudo-implementation-review|Afsm API Pseudo Implementation Review]] - Review of signup pseudo-implementation and Afsm API ergonomics.

## Raw Sources

- [Android ViewModel FSM Discussion](../raw/conversations/2026-05-01-android-viewmodel-fsm-discussion.md)
- [Android Official Docs Research](../raw/sources/2026-05-01-android-official-docs-fsm-research.md)
- [LLM Wiki Pattern](../raw/sources/2026-05-01-llm-wiki-pattern.md)
