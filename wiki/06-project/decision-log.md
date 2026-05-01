---
title: Decision Log
updated: 2026-05-01
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
