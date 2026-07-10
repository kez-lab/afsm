# Project Instructions

This project is building an Android-specific finite state machine architecture that keeps Android `ViewModel` as the lifecycle/UI adapter while moving screen flow rules into plain Kotlin state machines.

## Codex LLM Wiki Workflow

Use the LLM Wiki for every project task, including basic questions, code reading,
documentation edits, refactors, bug fixes, and implementation work. The wiki is
the retrieval and intent layer; current code, tests, Git state, and command output
are the implementation evidence layer.

Before broad repository search, a project-level answer, or any edit, read:

- `wiki/index.md`
- `wiki/00-context/current-state.md`
- `wiki/07-llm/codex-project-workflow.md`
- the task-specific canonical wiki page linked from the index

Then inspect the relevant code, tests, public docs, build files, and current Git
state. Read `raw/` only when source provenance, original wording, verification
evidence, or a contradiction needs to be checked. Do not use `raw/` as the normal
first retrieval layer.

For architecture, ViewModel, UDF, MVI, Compose state handling, or FSM work, also
read:

- `wiki/00-context/open-questions.md`
- `wiki/07-llm/ai-engineering-guardrails.md`
- `wiki/03-engineering/android-official-guidance.md`
- `wiki/03-engineering/android-fsm-architecture.md`

Follow `wiki/07-llm/codex-project-workflow.md` to decide which wiki, decision,
implementation, QA, raw evidence, index, and log files must be synchronized after
a change. Always consult the wiki, but do not create wiki churn for a purely
mechanical change that adds no durable behavior, decision, evidence, or reusable
learning.

## LLM Wiki Rules

- Treat `raw/` as source material. Do not rewrite raw conversation/source files except to add new evidence or indexes.
- Treat `wiki/` as the maintained synthesis layer. Keep pages short, linked, and current.
- Use `wiki/index.md` as the first retrieval map instead of scanning every wiki page or starting from `raw/`.
- For intended behavior, follow the accepted wiki/spec/decision hierarchy. For claims about what is implemented now, verify the current code, tests, Git state, or command output.
- If implementation evidence and the maintained wiki disagree, report and resolve the drift; do not silently choose one and leave the other stale.
- When the user corrects a design direction, update the canonical wiki page so it states the current conclusion directly; do not only append a new log or decision entry.
- Keep rejected or superseded ideas in a clearly labeled section such as `Superseded Ideas` when they remain useful, but do not let them obscure the current recommendation.
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

- Treat the directions below as the current working model, not a compatibility
  promise. Afsm has not been publicly released; an evidence-backed usability or
  safety improvement may replace APIs, DSL terms, module boundaries, or this
  model after the canonical product/spec/decision is updated first.
- Prefer plain Kotlin FSMs with no Android dependencies.
- Keep `ViewModel` responsible for `StateFlow`, `viewModelScope`, `SavedStateHandle`, command execution, and bridging UI events to the FSM.
- Model business flow with `State`, `Event`, and `Command`; add `Effect` only when a UI-side one-shot action is genuinely needed.
- Put UI-only state such as focus, sheet animation state, list scroll state, and `SnackbarHostState` in the UI layer unless it changes the business flow.
- Favor explicit state machines for multi-step or high-branching screens. Avoid FSM ceremony for simple loading/content/error screens unless it improves clarity.
