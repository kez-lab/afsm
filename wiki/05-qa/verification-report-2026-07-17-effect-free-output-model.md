---
title: Effect-Free Output Model Verification 2026-07-17
updated: 2026-07-17
status: passed-repository-gate
---

# Effect-Free Output Model Verification 2026-07-17

## Result

The accepted three-concept output model is implemented and passes repository,
API, graph, Maven Local, and clean external-consumer checks.

## Verified Changes

- Effect generic/channel/DSL/topology/config/test helpers removed.
- `afsm-compose` module removed.
- Auth, Checkout, and ProductEditor use no feature Effect type.
- sample screens construct no feature Events.
- sample ViewModels expose Android-style verbs.
- completion is durable state; ProductEditor Done is a direct UI callback.
- sample role files use `Flow.kt`.
- generated graphs and current public docs explain the machine/graph/tests
  division of labor.

## Verification

- core, runtime, ViewModel, test helper, KSP, and sample tests: pass,
- API checks: pass,
- graph generation: pass,
- Maven Local publication: pass,
- refreshed separate Android consumer compile/tests/graph generation: pass,
- static acceptance audit: zero maintained Effect surface and zero feature
  Event construction in sample screens.

## Boundary

This closes the implementation portion of the 2026-07-17 redesign cycle. It
does not close the long-term product goal. A controlled first-use review of the
current revision and a production-like Android pilot remain required.

Raw command/evidence record:
[effect-free output model](../../raw/verification/2026-07-17-effect-free-output-model/README.md).
