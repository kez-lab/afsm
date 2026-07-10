---
title: Codex Project Workflow
updated: 2026-07-10
---

# Codex Project Workflow

This page defines how Codex uses the LLM Wiki when reading or changing any part
of Afsm. It applies to quick project questions, code exploration, documentation,
tests, refactors, bug fixes, implementation, verification, and architecture work.

The rule is simple: start from maintained synthesis, verify against current
implementation evidence, and write durable learning back to the correct layer.

## Knowledge Layers

| Layer | Role | Typical contents |
|---|---|---|
| `raw/` | Immutable source and evidence | Conversations, external research notes, layouts, screenshots, verification captures |
| `wiki/` | Maintained project synthesis and intent | Current state, architecture, decisions, open questions, QA conclusions, agent workflows |
| Repository | Current implementation evidence | Kotlin source, tests, public docs, Gradle files, API dumps, Git state, command output |
| `AGENTS.md` | Codex operating contract | Mandatory reading order, guardrails, handoff rules, engineering direction |

The wiki is not a substitute for inspecting the current repository. It explains
what the project intends and why; code, tests, Git state, and command output show
what is implemented now.

## Required Reading Path

Before broad search, a project-level answer, or an edit:

1. Read `AGENTS.md`.
2. Read `wiki/index.md` as the retrieval map.
3. Read `wiki/00-context/current-state.md` for the current project position.
4. Read the task-specific canonical page linked from the index.
5. Inspect the relevant code, tests, public docs, build files, and Git state.
6. Read `raw/` only when provenance, original wording, verification evidence, or
   a contradiction must be checked.

Do not read the full wiki or all raw evidence by default. Follow links from the
index and expand only as the task requires.

## Task Routing

| Task | Read first after the common path |
|---|---|
| Quick project question or status check | Relevant canonical page, then current code/Git/command evidence |
| Architecture, ViewModel, UDF, MVI, Compose, or FSM | `open-questions.md`, `ai-engineering-guardrails.md`, `android-official-guidance.md`, `android-fsm-architecture.md` |
| Public API or DSL change | Current API/DSL engineering page, public docs, API dumps, tests, decision log |
| Bug fix or test failure | `ai-engineering-guardrails.md`, accepted behavior page, focused tests, implementation |
| Sample or Android integration | Relevant example/integration page, public walkthrough, sample source and tests |
| Release or verification claim | `docs/release-readiness.md`, implementation/QA pages, raw verification evidence, current command output |
| New external source or user-provided evidence | `raw/README.md`, the new source, affected canonical wiki pages |
| Wiki or Codex process change | `wiki-maintenance-guide.md`, this page, `AGENTS.md`, index and logs |

## Resolve Intent and Reality Separately

For intended behavior, use the precedence defined in
`wiki/07-llm/ai-engineering-guardrails.md`: current user instruction, accepted
decisions, current context, engineering pages, tests, then implementation.

For claims such as "implemented", "passing", "merged", "published", or
"currently configured", verify current evidence. A wiki statement or old log is
not enough by itself.

If the layers disagree:

1. State the mismatch explicitly.
2. Classify it as stale synthesis, implementation drift, stale test/spec, or an
   unresolved decision.
3. Fix the authoritative layer first according to the accepted intent.
4. Synchronize the other affected layers in the same task when it is in scope.

## Before Editing

1. Check `git status` and preserve unrelated user changes.
2. Identify the canonical page that defines the behavior or area being changed.
3. Read the relevant tests as executable specification.
4. State assumptions explicitly when the wiki and code do not answer a material
   question.
5. For intentional behavior changes, update the accepted wiki/spec/decision
   before tests and implementation.

## Synchronization Matrix

| Change type | Required synchronization |
|---|---|
| New source or evidence | Preserve it under `raw/`; update affected synthesis; add new pages to `wiki/index.md`; append `wiki/log.md` |
| Product, architecture, API, behavior, or process decision | Update the canonical page first; append `decision-log.md`; update current state/open questions when affected; append `wiki/log.md` |
| Implementation that changes supported behavior or project status | Update the relevant engineering page and `current-state.md`; append `implementation-log.md` and `wiki/log.md` |
| Bug fix with a durable lesson | Preserve the regression test; update the relevant behavior page when needed; append implementation/log entries |
| Verification with reusable evidence | Store captures under `raw/verification/`; add or update a QA synthesis page; update readiness/current state if changed; append logs |
| Codex, documentation, or wiki workflow change | Update `AGENTS.md` and the relevant `wiki/07-llm/` page; update the index for new/renamed pages; append decision and wiki logs for durable policy |
| Purely mechanical internal edit | Re-check affected wiki pages for drift; no wiki edit is required when behavior, status, decisions, evidence, and reusable learning are unchanged |

When a canonical page changes materially, update its `updated` date. Keep
`wiki/log.md` and `wiki/06-project/implementation-log.md` chronological and
append-only.

## Completion Check

Before reporting completion:

- Verify the change with the smallest relevant test or check, then a broader
  check proportional to risk.
- Confirm no failing test was weakened or bypassed.
- Re-read the affected canonical page and compare it with the final diff.
- Confirm new or renamed wiki pages are in `wiki/index.md`.
- Confirm durable decisions, implementation changes, and evidence were written
  to their correct layers.
- Name what was verified and recommend the next concrete task.
