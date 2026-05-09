---
title: Current State
updated: 2026-05-09
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
- `AfsmHost` serializes non-suspending `dispatch(event)` calls through a FIFO event queue, exposes `StateFlow<S>` state, exposes best-effort `Flow<F>` effects, and executes commands sequentially.
- Project-scoped AI engineering guardrails now require spec-first/TDD-oriented work and prohibit weakening tests merely to make implementation pass.
- A thin AndroidX `afsm-viewmodel` module now exists with `ViewModel.afsmHost(...)`, wiring `AfsmHost` to `viewModelScope`.
- A `:sample-shop` Android app module now exists to validate Afsm in a realistic Compose + Room shopping app.
- The sample app uses Afsm for auth, product registration review/publish, and checkout retry flows, while keeping product list/detail/likes/reviews on ordinary ViewModel + Flow to avoid unnecessary FSM ceremony.
- Public sample documentation now lives in `docs/sample-shop-afsm-guide.md`.
- Auth now uses sealed phases (`Editing`, `Submitting`, `Authenticated`) so text edits read as self-transitions and submit reads as a real phase transition.
- Product registration is now the stronger FSM reference flow: draft editing, mock image upload, review rejection, resubmission, approval, publishing, and close effect.
- Android CLI smoke verification passed for signup and product registration, with layout/screenshot evidence under `raw/verification/2026-05-09-sample-shop-fsm-smoke/`.
- A v3 topology-first API idea is now documented using `ProductEditorStateMachine`: v2 remains a low-level reducer-style engine, while v3 explores an optional `from<FromState> { on<Event>().to<ToState>() }` topology companion for state diagram generation.
- Afsm terminology now treats `Command` as a transition action emitted by the machine and executed by the host, not as another input event; v3 naming should distinguish phase states like `ImageUploadInProgress` from actions like `StartImageUpload`.
- ProductEditor now uses transition-action naming in code: `ImageUploadInProgress` with `StartImageUpload`, `ReviewSubmissionInProgress` with `StartReviewSubmission`, and `PublishInProgress` with `StartProductPublish`.
- Android CLI regression smoke verification passed after the ProductEditor naming cleanup, with evidence under `raw/verification/2026-05-09-product-editor-transition-action-rename-smoke/`.

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

Topology-first API exploration: [[../03-engineering/afsm-v3-topology-first-api|Afsm v3 Topology-First API]].

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
