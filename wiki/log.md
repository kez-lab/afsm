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
