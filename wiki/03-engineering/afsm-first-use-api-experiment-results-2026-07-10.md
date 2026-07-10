---
title: Afsm First-Use API Experiment Results 2026-07-10
updated: 2026-07-10
status: candidate-e-implemented
---

# Afsm First-Use API Experiment Results 2026-07-10

The first prototype round did not accept a new production API. It eliminated
two attractive but invalid Kotlin shapes and showed that compiling alternatives
must be judged by concepts and diagnostics, not only by removed lines.

## Executable Evidence

`AfsmFirstUseApiExperimentTest` defines test-only declaration helpers and runs
equivalent Draft, Auth, and Checkout machines through:

- a staged three-channel type set,
- named event/command/effect type tokens,
- a composed feature value that owns a machine.

The prototypes preserve guarded form transitions, entry commands, failure and
retry, invalid results, UI effects, payload phases, dynamic Checkout data,
request-id matching, stale-result ignores, durable completion, and topology.

Verification:

```bash
./gradlew :afsm-core:test --tests 'afsm.core.AfsmFirstUseApiExperimentTest'
./gradlew :afsm-core:check
```

Result: passed.

## Comparison

| Shape | Compiles | Setup result | New concepts | Graph implication | Verdict |
|---|---:|---|---|---|---|
| A: alias + delegated object + factory | Yes | Repeats framework plumbing before flow | machine alias, delegation, factory | Works with current class-only KSP | Baseline only |
| B: three explicit types on one five-type function | No | Kotlin does not support partial explicit type arguments | None | Not reached | Reject exact syntax |
| B2: staged `afsmTypes().machine()` | Yes | Removes wrapper/factory but adds a staged call | type-set object | Still needs property discovery | Do not promote yet |
| C: named type channels | Yes | Roles are named, but three token helpers remain | event/command/effect tokens | Still needs property discovery | Do not promote yet |
| D: inferred generic feature superclass | No | Kotlin requires all five supertype arguments | feature base class | Not reached | Reject exact syntax |
| D2: composed feature value | Yes | Owns machine identity but adds `.machine` and a container | feature value plus type tokens | Could be discoverable as a property | Capability does not justify cost yet |

All compiling candidates keep a single `DraftState`-style alias. That alias is
not merely boilerplate: it names the Android-visible state boundary and keeps
the sealed phase root from narrowing to the initial singleton value.

## Compiler Diagnostics

The exact Candidate B call produced `Inapplicable candidate(s)` because the
function has five type parameters while the call supplied three. The exact
Candidate D inheritance produced `5 type arguments expected for class
'ExperimentalFeature'`.

A wrong event inside a Draft builder produced the useful diagnostic `Type
argument is not within its bounds: should be subtype of DraftEvent`. By
contrast, supplying the wrong command or effect token to Candidates C/D made
the compiler report an unresolved `draftFlow()` receiver mismatch. This is
type-safe, but it points farther from the mistaken channel and is weaker for
authoring.

## Android, Tests, and Graphs

- ViewModel hosting stays simplest when the declaration itself is an
  `AfsmMachine`; token and feature wrappers do not improve `afsmHost(machine =
  ...)`.
- Transition test call sites are effectively identical after machine creation.
- Checkout already proves that dynamic navigation data belongs in the host's
  explicit `initialState`; the graphable machine can keep a stable default.
- Before Candidate E, `@AfsmGraph` accepted only classes and objects. That
  constraint was the direct reason feature code needed delegation wrappers.

## Candidate E Implementation

Candidate E is now implemented as an explicitly typed, annotated top-level
machine property. It removes the wrapper and factory with ordinary Kotlin
expected-type inference and requires no new runtime concept.

Passing evidence now covers:

- KSP direct-property discovery and generated registry references,
- focused rejection diagnostics for unsafe property shapes,
- Draft, Auth, Checkout, ProductEditor, and external consumer declarations,
- graph generation and registry tests,
- Checkout dynamic host initialization,
- clean Maven Local external consumer compilation, tests, and MMD output.

This makes Candidate E the current pre-release authoring candidate. A fresh-use
review and real-user evidence remain necessary before API freeze.

## Evidence Boundary

These results prove Kotlin feasibility and behavior parity in repository tests.
They do not prove that a human Android developer finds any candidate easier to
learn or author.
