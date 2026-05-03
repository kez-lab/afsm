---
title: Current State
updated: 2026-05-01
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

## Current Source Material

- Raw discussion: [Android ViewModel FSM Discussion](../../raw/conversations/2026-05-01-android-viewmodel-fsm-discussion.md)
- Raw pattern note: [LLM Wiki Pattern](../../raw/sources/2026-05-01-llm-wiki-pattern.md)
- Official Android docs research: [Android Official Docs Research](../../raw/sources/2026-05-01-android-official-docs-fsm-research.md)
