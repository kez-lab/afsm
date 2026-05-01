---
title: Wiki Maintenance Guide
updated: 2026-05-01
---

# Wiki Maintenance Guide

This project uses an LLM Wiki pattern.

## Directory Roles

- `raw/`: immutable source material, including conversations and source notes.
- `wiki/`: maintained synthesis pages.
- `AGENTS.md`: instructions for Codex and other coding agents.

## Ingest Workflow

When new architecture discussion, implementation notes, test evidence, or source material arrives:

1. Add the raw source under `raw/`.
2. Update relevant wiki pages with concise durable synthesis.
3. Add or update links in `wiki/index.md`.
4. Append a chronological entry to `wiki/log.md`.
5. Append durable decisions to `wiki/06-project/decision-log.md`.
6. Add unresolved items to `wiki/00-context/open-questions.md`.

## Query Workflow

When answering project architecture questions:

1. Read `wiki/index.md`.
2. Read `wiki/00-context/current-state.md`.
3. Read the relevant engineering page.
4. Answer from the wiki first, then inspect code if implementation details are needed.
5. If the answer creates durable insight, file it back into `wiki/`.

## Task Handoff Workflow

At the end of each completed task, recommend the next concrete task.

The recommendation should be:

- short,
- actionable,
- connected to the current project direction,
- omitted only when the user explicitly asks not to recommend follow-up work.

For this project, prefer recommendations that move from wiki/context work toward an Android FSM implementation, tests, or verification.

## Lint Workflow

Periodically check:

- stale claims,
- contradictions,
- pages missing inbound links,
- open questions that have been resolved,
- decisions not reflected in engineering pages,
- implementation that diverges from documented architecture.
