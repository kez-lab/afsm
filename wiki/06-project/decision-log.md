---
title: Decision Log
updated: 2026-05-03
---

# Decision Log

## [2026-05-01] Use ViewModel-backed FSM for complex Android flows

Decision: Keep Android `ViewModel`, but model complex screen/business flows with plain Kotlin finite state machines.

Rationale:

- MVVM/UDF improves separation and lifecycle behavior but fragments the local reading path.
- The traceability discomfort is real, not merely personal preference.
- FSMs make valid states, events, invalid transitions, and async command boundaries explicit.
- Keeping FSMs plain Kotlin preserves fast unit tests and reduces Android coupling.

Consequences:

- Future complex screens should define `State`, `Event`, `Command`, and optional `Effect`.
- `ViewModel` should execute the FSM and commands, not become a large transition table itself.
- Simple screens may continue using ordinary `UiState` and ViewModel functions.

Source: [Android ViewModel FSM Discussion](../../raw/conversations/2026-05-01-android-viewmodel-fsm-discussion.md)

## [2026-05-01] Use LLM Wiki for architecture memory

Decision: Maintain architecture context as an LLM Wiki with immutable raw sources and concise synthesized wiki pages.

Rationale:

- The project is early and architecture reasoning should compound across future Codex sessions.
- Raw conversations are too long for practical repeated reading.
- Wiki pages make the current direction, decisions, and open questions durable.

Consequences:

- Keep raw source material in `raw/`.
- Update `wiki/index.md` and `wiki/log.md` whenever durable knowledge changes.
- Use `AGENTS.md` to point Codex at the relevant wiki pages.

## [2026-05-01] Product goal is an Android FSM library

Decision: The final target is a reusable Android-focused FSM library, not merely documentation or an app-local pattern.

Rationale:

- The recurring pain is broad: MVVM/UDF improves separation but obscures flow traceability in complex Android screens.
- A library can standardize state/event/command modeling, ViewModel execution, transition diagnostics, and test helpers.
- The library should remain Android-aligned instead of replacing official ViewModel and UI layer guidance.

Consequences:

- Product strategy, API design, sample apps, documentation, testing utilities, and release planning are first-class workstreams.
- The first implementation should still start with a reference flow before extracting too much abstraction.
- Public API minimalism is a major design constraint.

## [2026-05-01] First reference flow is signup identity verification with retry

Decision: Use a signup + identity verification + retry flow as the first reference flow.

Rationale:

- It is complex enough to demonstrate meaningful FSM value beyond simple loading/content/error.
- It exercises external UI effects, async command results, retry policy, invalid transitions, process restoration questions, and navigation boundaries.
- It is common enough for Android teams to understand without deep domain explanation.

Consequences:

- Stage 1 implementation should build this flow before extracting reusable runtime helpers.
- API design questions should be evaluated against this flow first.
- Sample and documentation should use this flow to teach State/Event/Command/Effect boundaries.

## [2026-05-01] Use Afsm as the working library name

Decision: Use `Afsm` as the working product/API name, expanded as Android FSM.

Rationale:

- The name is short and maps directly to the product category.
- Kotlin public types can use `Afsm` idiomatically, while artifacts can use lowercase `afsm-*`.
- The risk is acronym ambiguity, so README and documentation must define it immediately.

Consequences:

- Public API drafts use `AfsmStateMachine`, `AfsmTransition`, `AfsmHost`, and related names.
- Package and artifact naming should use lowercase `afsm`.
- The name can still be revisited before external release if user testing shows confusion.

## [2026-05-03] Afsm public API v2 candidate

Decision: Draft v2 keeps the `Afsm` public prefix and proposes `AfsmDecision.Stayed`, `Afsm.stay(...)`, `AfsmNoEffect`, best-effort no-replay effect delivery, non-suspending serialized dispatch, and sequential command execution as MVP candidates.

Rationale:

- The CEO confirmed that the API should communicate Android State Machine directly.
- Signup pseudo-implementation review showed generic verbosity is acceptable with typealiases, but `Ignored` was semantically overloaded.
- Android lifecycle review showed effect delivery and dispatch semantics must be explicit before implementation.

Consequences:

- Implementation should not start until this v2 candidate is explicitly accepted.
- The next design/implementation pass should validate `AfsmNoEffect` and dispatch/effect behavior with real Kotlin tests.

## [2026-05-03] Start afsm-core as plain Kotlin core module

Decision: Create the first implementation skeleton as a Kotlin/JVM `afsm-core` module with Kotlin stdlib as the only core dependency.

Rationale:

- The v2 API candidate needs real compiler feedback before runtime or ViewModel work.
- `afsm-core` should remain free of Android, AndroidX, coroutine, Compose, serialization, DI, and code generation dependencies.
- Plain Kotlin compile checks are enough to validate public type ergonomics before adding runtime behavior.

Consequences:

- The initial source package is `afsm.core`.
- `AfsmTransition<S, C, F>`, `AfsmNoEffect`, `AfsmDecision`, `AfsmStateMachine`, and the `Afsm` builder object are now concrete public source files.
- Runtime dispatch, command execution, effect delivery, ViewModel integration, and test helper APIs remain outside `afsm-core` for later tasks.
