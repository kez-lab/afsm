---
name: use-afsm
description: Design, implement, migrate, review, and test complex Android ViewModel flows with Afsm, including State/Phase/Data/Event/Command modeling, the afsmMachine DSL, afsmHost integration, restoration, transition tests, and Mermaid graph generation. Use when adopting Afsm in a Kotlin or Android project, adding or changing an Afsm machine, refactoring branching ViewModel flow into Afsm, diagnosing Afsm behavior, or reviewing an Afsm integration. Do not use for simple screens where ordinary ViewModel plus StateFlow is clearer or for changing Afsm library internals.
---

# Use Afsm

Use Afsm to make one complex Android feature flow explicit without replacing
`ViewModel` or imposing an app-wide architecture.

## Load the Contract

- Read [references/current-contract.md](references/current-contract.md) before
  modeling, implementing, debugging, or reviewing an Afsm flow.
- Read
  [references/implementation-pattern.md](references/implementation-pattern.md)
  before editing Gradle, machine, ViewModel, test, or graph files.
- When working in an Afsm source checkout, also follow its `AGENTS.md` and
  current repository documentation. Treat source, tests, and API dumps as more
  current than this bundled pre-release reference.

## Follow the Adoption Workflow

### 1. Inspect Before Editing

1. Read the consuming repository's applicable agent instructions.
2. Locate the Afsm version and modules already used by Gradle. Do not invent a
   version, repository, or public release coordinate.
3. Inspect the current `ViewModel`, UI callbacks, state holders, repositories,
   saved-state logic, tests, and navigation behavior for the target feature.
4. Write a compact behavior inventory covering:
   - business phases and completion states,
   - user intents and external result inputs,
   - repository, database, timer, network, or SDK work,
   - validation and retry rules,
   - duplicate and stale-result handling,
   - restoration behavior and unsafe work that must not restart.
5. Preserve observable behavior unless the request explicitly changes it.

If Afsm is not already available to the consumer, report the distribution
boundary. The bundled contract targets the repository's internal-beta snapshot;
do not silently add Maven Local or a guessed remote repository.

### 2. Decide Whether Afsm Fits

Use Afsm when validity depends on the current business phase, especially for
multi-step work, retries, correlated async results, or restoration-sensitive
flows. Keep ordinary `ViewModel + StateFlow` for simple state production when a
machine would add ceremony without improving reviewability.

### 3. Design the Flow

1. Define `Phase` as the active business step.
2. Define durable business `Data` separately and combine them with
   `AfsmState<Phase, Data>`.
3. Define `Event` as user intent or an external-work result.
4. Define `Command` as typed work for the Android host.
5. Sketch the important transitions before writing the DSL.
6. Record expected `Handled`, `Ignored`, and `Invalid` outcomes that may not be
   visible in the graph.

Keep Android framework objects, repositories, coroutine scopes, and UI-only
state out of the pure machine model.

### 4. Specify Behavior With Tests

Add or preserve focused pure-transition tests before changing behavior. Cover
the happy path and the applicable validation, failure, retry, duplicate,
stale-result, invalid-event, and restoration cases.

Use the `afsm-test` version that matches the consumer's Afsm artifacts. Never
weaken an existing test merely to fit the new machine.

### 5. Implement the Machine

- Write unconditional actions directly inside `on<Event>`.
- Use named `case` branches only for real conditions that reviewers should see
  in the graph.
- Use `ignore` only for expected harmless duplicates or stale results.
- Leave impossible event/phase pairs invalid, or use `invalid` when an explicit
  rejection reason improves the contract.
- Emit `Command` values for external work; never call Android or repository
  APIs from the reducer.
- Prefer `onEnter` commands when entering a phase means work must begin.
- Keep request identifiers in active state or phase payloads when async results
  can arrive late.

### 6. Integrate the Android Boundary

Host the machine with `ViewModel.afsmHost(...)`, expose its `StateFlow`, execute
commands through the injected Android dependencies, and return results through
the command handler's `dispatchEvent` capability.

Expose verb-named methods such as `save()`, `retry()`, or
`updateTitle(value)` to UI. Do not make Compose construct machine `Event`
objects or expose a generic `onEvent(Event)` API solely because Afsm is used.

Restore the smallest safe business state. Initial-state construction does not
run `onEnter`; do not add observation-driven work that accidentally resubmits
unsafe operations after recreation.

### 7. Generate the Review Graph

For non-trivial machines, annotate a stable graphable machine value, configure
matching KSP and Afsm graph-plugin versions, and run the consumer's
`generateAfsmMmd` task.

Review graph, machine, and tests together:

- graph for whole-flow topology and named conditions,
- machine for exact data, work, guards, and ordering,
- tests for payloads and graph-invisible decisions.

Never hand-edit generated build output.

### 8. Verify and Report

Run, in order:

1. the smallest focused transition tests,
2. focused ViewModel wiring tests,
3. graph generation and inspection,
4. the affected module's broader tests and compile checks,
5. the consuming repository's normal verification gate.

Review the final diff for boundary violations: no Effect output type, no
Android work inside the reducer, no UI-exposed machine events, no unsafe
restoration restart, no broad `ignore`, and no mismatched Afsm versions.

Report the modeled flow, preserved or intentionally changed behavior, commands
run, generated graph path, and any unverified runtime or distribution boundary.
