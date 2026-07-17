---
title: Wiki Maintenance Guide
updated: 2026-07-17
---

# Wiki Maintenance Guide

This project uses an LLM Wiki pattern.

## Directory Roles

- `raw/`: immutable source material, including conversations and source notes.
- `wiki/`: maintained synthesis pages.
- `AGENTS.md`: instructions for Codex and other coding agents.
- `wiki/07-llm/ai-engineering-guardrails.md`: project-scoped software engineering, TDD, and verification integrity rules for AI agents.
- `wiki/07-llm/codex-project-workflow.md`: required retrieval, evidence, and synchronization workflow for all Codex project tasks.

## Ingest Workflow

When new architecture discussion, implementation notes, test evidence, or source material arrives:

1. Add the raw source under `raw/`.
2. Update relevant wiki pages with concise durable synthesis.
3. Add or update links in `wiki/index.md`.
4. Append a chronological entry to `wiki/log.md`.
5. Append durable decisions to `wiki/06-project/decision-log.md`.
6. Add unresolved items to `wiki/00-context/open-questions.md`.

## Canonical Synthesis Workflow

When the user corrects an architecture direction or a design conclusion changes:

1. Update the canonical engineering/product page so it reads as the current answer, not as a chronological argument.
2. Move rejected alternatives into a short `Superseded Ideas` or `Rejected Alternatives` section when they are still useful context.
3. Update `wiki/00-context/current-state.md` with only the current direction.
4. Update `wiki/00-context/open-questions.md` by removing or rewriting stale questions.
5. Append the change to `wiki/log.md` as history.
6. Append durable decisions to `wiki/06-project/decision-log.md`, but do not rely on the decision log as the only source of truth.

Do not leave future agents to reconstruct the current answer from a chain of corrections. The canonical page should be readable on its own.

## Project Query Workflow

When reading the project or answering any project question:

1. Read `wiki/index.md`.
2. Read `wiki/00-context/current-state.md`.
3. Read `wiki/07-llm/codex-project-workflow.md` and the relevant canonical page.
4. Use the wiki for project intent and inspect current code, tests, Git state, or command output for implementation claims.
5. Read `raw/` only when provenance, original evidence, or a contradiction must be checked.
6. If the answer creates durable insight, file it back into `wiki/`.

For the full task routing and change-type synchronization matrix, follow
[[codex-project-workflow|Codex Project Workflow]].

## Task Handoff Workflow

At the end of each completed task, recommend the next concrete task.

The recommendation should be:

- short,
- actionable,
- connected to the current project direction,
- omitted only when the user explicitly asks not to recommend follow-up work.

For this project, prefer recommendations that move from wiki/context work toward an Android FSM implementation, tests, or verification.

## Engineering Guardrail Workflow

When implementation and tests disagree:

1. Treat the test as executable spec until proven otherwise.
2. Classify the failure before editing tests.
3. If the accepted behavior changed, update wiki/spec/decision pages before changing tests.
4. Preserve regression coverage when fixing bugs.
5. Record durable process or behavior changes in `wiki/log.md`.

## Lint Workflow

Periodically check:

- stale claims,
- contradictions,
- pages that read like chat history instead of current synthesis,
- pages missing inbound links,
- open questions that have been resolved,
- decisions not reflected in engineering pages,
- implementation that diverges from documented architecture.

When a page preserves a removed API or superseded design, mark it in both
places:

- the `wiki/index.md` entry, so retrieval routes it as history;
- the page title/status/first screen, so a direct link cannot look current.

Canonical current pages may name a removed symbol when they explicitly explain
its removal. Otherwise a legacy symbol hit is drift that must be corrected or
reclassified, not silently ignored.
