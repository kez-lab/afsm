---
title: Afsm KSP MMD Generation
updated: 2026-07-17
---

# Afsm KSP MMD Generation

## Decision

KSP discovers stable graphable machine declarations and generates an
`AfsmGraphRegistry`. The Gradle plugin renders Mermaid `.mmd` files from that
registry.

## Declaration Shape

```kotlin
@AfsmGraph(id = "Checkout", fileName = "CheckoutStateMachine.mmd")
internal val checkoutStateMachine:
    AfsmMachine<CheckoutState, CheckoutEvent, CheckoutCommand> =
    afsmMachine(initialPhase = CheckoutPhase.Idle) { ... }
```

A non-private stable top-level `val` is the preferred feature shape. Eligible
classes and objects remain supported. Mutable, computed, member, private, or
non-graphable property declarations fail with explicit diagnostics.

## Generated Boundary

The registry stores graph id, output filename, and a lambda returning
`AfsmTopology`. It does not duplicate reducer behavior or inspect Android
runtime objects.

## Gradle Integration

`io.github.afsm.graph` wires one selected Android unit-test variant, KSP, and
`generateAfsmMmd`. The processor dependency version is generated from the
plugin's Afsm version.

Current output:

```text
build/generated/afsm/mmd/*.mmd
```

## Rendering Contract

Flow mode favors phases, external edges, named conditions, and command work.
Full mode may show more internal transitions. The graph contains no separate UI
output labels.

## Open Scope

Multi-variant and multi-module aggregation remain open until real adoption
proves they are required.

## Verification

Functional tests compile temporary projects for valid top-level/class/object
sources and invalid declaration shapes. Sample registry and graph tests ensure
all three reference machines export current topology.
