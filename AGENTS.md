# Project Instructions

This project is building an Android-specific finite state machine architecture that keeps Android `ViewModel` as the lifecycle/UI adapter while moving screen flow rules into plain Kotlin state machines.

Before working on architecture, ViewModel, UDF, MVI, Compose state handling, or FSM implementation, read:

- `wiki/index.md`
- `wiki/00-context/current-state.md`
- `wiki/00-context/open-questions.md`
- `wiki/03-engineering/android-official-guidance.md`
- `wiki/03-engineering/android-fsm-architecture.md`

## LLM Wiki Rules

- Treat `raw/` as source material. Do not rewrite raw conversation/source files except to add new evidence or indexes.
- Treat `wiki/` as the maintained synthesis layer. Keep pages short, linked, and current.
- Update `wiki/index.md` when durable wiki pages are added or renamed.
- Append chronological changes to `wiki/log.md`.
- Append architecture decisions to `wiki/06-project/decision-log.md`.
- Mark assumptions explicitly as `Assumption` or `가정`.

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
