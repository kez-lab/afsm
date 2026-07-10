---
title: Afsm Long-Term Goal
updated: 2026-07-10
status: active
---

# Afsm Long-Term Goal

Make Afsm a product that helps real Android teams model complex screen flows
more readably, more safely, and with less cognitive overhead than scattered
`ViewModel` state mutations.

The product outcome is authoritative. The current API, DSL, terminology,
modules, samples, tests, graphs, and documentation are pre-release hypotheses.
Afsm has not been publicly released, so backward compatibility with the current
design is not a constraint. Any part may be renamed, removed, split, merged, or
rewritten when evidence shows that another design better serves readability,
usability, Android fit, or flow safety.

## Completion Outcomes

### 1. Readable Complex Flows

- An Android developer can open the primary Afsm definition and quickly
  understand current states, valid events, branching conditions, async work,
  failures, retries, completion, and important no-op/invalid behavior.
- Understanding a flow does not require reconstructing business rules by
  jumping repeatedly between UI, ViewModel, reducers, and collectors.
- The adopted vocabulary and DSL read like product flow rather than framework
  internals.

### 2. Useful Authoring Experience

- A first-time Android developer can decide whether Afsm fits a screen and can
  implement a representative complex flow without excessive type aliases,
  generic noise, boilerplate, hidden ordering, or graph-only metadata.
- Common paths are concise and unsurprising; advanced escape hatches do not
  dominate first use.
- Afsm is clearly more useful than ordinary `ViewModel + StateFlow` for its
  target flows and is not forced onto simple screens.

### 3. Safer Flow Behavior

- Invalid transitions, duplicate events, stale async results, retry identity,
  cancellation, durable completion, command failure, effect loss, queue
  pressure, and restoration are explicit where they matter.
- The API makes dangerous or ambiguous modeling harder and makes important
  behavior straightforward to test.
- Runtime diagnostics help developers locate flow errors without exposing
  sensitive data by default.

### 4. Natural Android Integration

- Afsm works with Android `ViewModel`, `StateFlow`, Compose, lifecycle,
  `SavedStateHandle`, repositories, and coroutine testing without replacing
  Android architecture or requiring a framework-owned base ViewModel.
- UI-only focus, scroll, animation, keyboard, and composition state remain in
  the UI unless they change business flow.

### 5. Coherent Verification and Visualization

- Runtime behavior, transition tests, generated diagrams, diagnostics, and
  public examples do not contradict one another.
- Machine, graph, and tests together expose the complete important flow; the
  implementation mechanism may change if a clearer design is found.
- Verification proves behavior and comprehension, not merely compilation or
  artifact generation.

### 6. Real Usability Evidence

- Repeated first-use reviews identify where Android developers hesitate,
  misread the DSL, add boilerplate, or cannot predict behavior, and those
  findings drive redesign.
- At least one real or production-like pilot beyond the repository fixtures
  records adoption benefit, friction, stop criteria, and rollback feasibility.
- AI reviews and sample success remain labeled as supporting evidence and are
  never presented as real customer validation.

### 7. Evidence-Based API Freeze

- No current public API, module boundary, or term is preserved merely because
  it already exists or has tests.
- Compatibility and API stability become goals only after usability, safety,
  Android fit, and real pilot evidence support a design worth stabilizing.
- Once that evidence exists, API/ABI checks, migration guidance, consumer tests,
  and release policy protect the deliberately selected surface.

### 8. Durable Product Knowledge

- Raw evidence, canonical wiki pages, decisions, implementation logs, open
  questions, public docs, and verification reports remain synchronized with the
  current product direction and code.
- Superseded APIs remain clearly historical and never constrain a better
  pre-release design.

## Pre-Release Design Freedom

During this goal, Codex is authorized to propose and implement breaking
repository changes when evidence shows a meaningful improvement, including:

- renaming or replacing public types, DSL operations, and terminology,
- changing the `State`/`Phase`/`Data` model,
- changing how Event, Command, Effect, ignore, invalid, or entry/exit behavior
  is expressed,
- restructuring, splitting, merging, adding, or removing modules,
- replacing graph-generation or test-helper APIs,
- rewriting samples, public docs, API dumps, and consumer fixtures to match the
  accepted design.

An intentional product change must follow this order:

1. Record the accepted usability or safety problem and desired behavior in the
   canonical wiki/spec/decision.
2. Update or add tests that express the newly accepted behavior while
   preserving unrelated regression intent.
3. Change production code with the smallest coherent redesign.
4. Update samples, docs, graphs, API dumps, changelog, and external consumer
   evidence in the same milestone.
5. Remove superseded APIs instead of retaining compatibility aliases by
   default.

Tests are executable specification for accepted behavior, but existing tests
do not freeze a product design that the user has intentionally changed.

## Continuous Product Loop

Each cycle must:

1. Audit current APIs, machine examples, tests, graphs, Android integration,
   docs, and available usability evidence.
2. Identify the largest evidence-backed obstacle to readability, authoring
   usefulness, safety, or Android fit.
3. Define a bounded experiment or redesign with an explicit hypothesis and
   proof method.
4. Compare the current design and proposed design on realistic complex flows,
   not toy syntax alone.
5. Update the accepted product/spec direction before intentionally changing
   tests and implementation.
6. Implement and verify the redesign, including focused tests, relevant broader
   suites, graph/doc parity, and external consumer checks where applicable.
7. Run a fresh-use comprehension review and record what became clearer, what
   became harder, and what remains unproven.
8. Synchronize durable evidence and immediately select the next largest gap.

Do not choose a small compatibility-preserving patch when a breaking redesign
would better serve the final product. Do not declare completion because the
current API is documented, tests are green, or a release artifact can be built.

## Decision Priority

When goals conflict, use this order:

1. Readability of real complex Android flows.
2. Safety and predictability of flow behavior.
3. Usefulness and low authoring friction for Android developers.
4. Idiomatic Android lifecycle and state integration.
5. Testability, visualization, diagnostics, and tooling coherence.
6. Implementation simplicity and maintenance cost.
7. Pre-release compatibility with the current API.

Pre-release compatibility is last and may be discarded.

## Boundaries

- Do not weaken tests merely to make implementation pass; change tests only
  after the accepted product/spec behavior changes.
- Do not preserve APIs, terminology, modules, or examples solely because work
  has already been invested in them.
- Do not add advanced FSM features without a demonstrated Android use case or
  usability benefit.
- Do not force Afsm onto simple screens to make adoption appear broader.
- Preserve unrelated user changes in a dirty worktree.
- Do not infer approval to push, merge, deploy, publish, or release.
- Do not treat AI review as real user or production validation.
- Ask for user direction when a product choice cannot be resolved by evidence
  or when external coordination and approval are required.

## Completion Standard

This goal is complete only when current evidence shows that real Android
developers can use Afsm to express representative complex flows more readably
and safely than the practical alternatives, with acceptable authoring cost and
idiomatic Android integration. API stabilization and release preparation follow
that proof; they do not replace it.

## Current Evidence

- [[goal-evidence-baseline-2026-07-10|Goal Evidence Baseline 2026-07-10]]
