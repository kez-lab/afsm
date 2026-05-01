# LLM Wiki Pattern

Date: 2026-05-01

Source: user-provided concept note in chat.

## Essence

An LLM Wiki is a persistent, compounding markdown knowledge base maintained by an LLM. Instead of relying only on retrieval over raw documents at query time, the LLM incrementally reads new sources, extracts durable knowledge, updates related pages, records contradictions, maintains links, and logs operations.

## Layers

- Raw sources: immutable evidence and source-of-truth documents.
- Wiki: LLM-maintained markdown synthesis, summaries, entity pages, concept pages, comparisons, decisions, and indexes.
- Schema: agent instructions such as `AGENTS.md` that define conventions and workflows.

## Operations

- Ingest: add a source, summarize it, update related wiki pages, update the index, append the log.
- Query: answer from the wiki, then optionally file useful answers back into the wiki.
- Lint: periodically check contradictions, stale claims, orphan pages, missing concepts, and missing links.

## Required Support Files

- `index.md`: content-oriented map of the wiki.
- `log.md`: chronological append-only operation history.

## Project Adaptation

For this project, the LLM Wiki is used to preserve architecture reasoning for an Android-specific FSM approach. Raw conversations and external notes are kept in `raw/`; concise engineering decisions and implementation guidance live in `wiki/`.
