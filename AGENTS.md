# Project Instructions

This project is building an Android-specific finite state machine architecture that keeps Android `ViewModel` as the lifecycle/UI adapter while moving screen flow rules into plain Kotlin state machines.

Before working on architecture, ViewModel, UDF, MVI, Compose state handling, or FSM implementation, read:

- `wiki/index.md`
- `wiki/00-context/current-state.md`
- `wiki/00-context/open-questions.md`
- `wiki/07-llm/ai-engineering-guardrails.md`
- `wiki/03-engineering/android-official-guidance.md`
- `wiki/03-engineering/android-fsm-architecture.md`

## LLM Wiki Rules

- Treat `raw/` as source material. Do not rewrite raw conversation/source files except to add new evidence or indexes.
- Treat `wiki/` as the maintained synthesis layer. Keep pages short, linked, and current.
- Update `wiki/index.md` when durable wiki pages are added or renamed.
- Append chronological changes to `wiki/log.md`.
- Append architecture decisions to `wiki/06-project/decision-log.md`.
- Mark assumptions explicitly as `Assumption` or `가정`.

## Spec and TDD Guardrails

- Treat tests as executable specification, not as obstacles to make green.
- Do not delete, skip, weaken, or rewrite a failing test just because implementation does not pass.
- When a test fails, classify the cause before editing anything: implementation bug, test harness issue, stale spec, or intentionally changed requirement.
- If behavior is intentionally changing, update the relevant wiki/spec/decision first, then update tests to match the new accepted behavior, then update implementation.
- For bug fixes, add or preserve a failing regression test before changing production code.
- For new behavior, prefer the loop: document expected behavior, add/adjust the focused test, observe the expected failure when practical, implement the smallest production change, run the focused test, then run the relevant broader suite.
- If a test itself is wrong, preserve the original intent in the replacement test and explain why the old assertion contradicted the accepted spec.
- Do not call a task complete while relevant tests are failing unless the failure is explicitly documented as an accepted blocker.

## Task Handoff Rule

- When finishing a task, always recommend the next concrete task unless the user explicitly asks not to.
- Keep the recommendation short and actionable.
- Prefer the next task that naturally advances the current project direction.
- If there are multiple reasonable next tasks, recommend one primary next task and optionally mention one alternative.

## Engineering Direction

- Prefer plain Kotlin FSMs with no Android dependencies.
- Keep `ViewModel` responsible for `StateFlow`, `viewModelScope`, `SavedStateHandle`, command execution, and bridging UI events to the FSM.
- Model business flow with `State`, `Event`, and `Command`; add `Effect` only when a UI-side one-shot action is genuinely needed.
- Put UI-only state such as focus, sheet animation state, list scroll state, and `SnackbarHostState` in the UI layer unless it changes the business flow.
- Favor explicit state machines for multi-step or high-branching screens. Avoid FSM ceremony for simple loading/content/error screens unless it improves clarity.
