---
title: Afsm Example Catalog
updated: 2026-07-18
---

# Afsm Example Catalog

This page records the canonical example ladder for public usability.

The public docs version is [docs/examples.md](../../docs/examples.md).

## Example Ladder

| Level | Example | Purpose |
|---|---|---|
| 0 | [Bilingual documentation hub](../../docs/index.html) | English/Korean installation, quickstart, core concepts, Android integration, API lookup, and guide routing |
| 1 | README minimal Draft | Smallest mental model: phase, data, event, command, `onEnter` |
| 2 | [Auth](../../docs/auth-walkthrough.md) | Smallest real Android screen: validation, command result events, state-driven navigation |
| 3 | [Checkout](../../docs/checkout-walkthrough.md) | Mid-size Android lifecycle flow: navigation argument initial state, loading, payment, retry, stale results, durable completion |
| 4 | [ProductEditor](../../docs/product-editor-walkthrough.md) | Advanced graph stress test: draft, upload, review rejection/resubmission, approval, publishing |
| Anti-example | Catalog/Product/Reviews | Screens that should remain ordinary `ViewModel + Flow` |

## Why Checkout Was Promoted

Checkout used to be a custom `AfsmReducer` escape hatch. It is now a graphable
`AfsmMachine<CheckoutState, CheckoutEvent, CheckoutCommand>`.

The promotion matters because Checkout demonstrates the exact concerns Android
teams ask about:

- dynamic initial state from navigation `productId`,
- phase/data separation,
- command emission from `onEnter`,
- request-id stale result handling,
- durable completion state observed by route-owned navigation,
- `CheckoutState.toRenderState()` so Compose does not depend on every internal phase,
- generated `CheckoutStateMachine.mmd`.

## Canonical Generated Graphs

```text
sample-shop/build/generated/afsm/mmd/AuthStateMachine.mmd
sample-shop/build/generated/afsm/mmd/CheckoutStateMachine.mmd
sample-shop/build/generated/afsm/mmd/ProductEditorStateMachine.mmd
```

## Current Rule

Use examples to prove both sides of the product:

- Afsm is attractive for complex transaction/flow screens.
- Afsm is not a replacement for ordinary data `ViewModel`s.
- Afsm does not require UI composables to construct machine Event objects;
  ordinary verb-named ViewModel functions are the public screen boundary.

The documentation hub is the default first-use surface. It should expose the
installation and Draft quickstart before optional background, keep API and
guide navigation persistent, and send developers through dedicated
walkthroughs in this order:

1. Auth.
2. Checkout.
3. ProductEditor.

ProductEditor is stronger as a graph stress test, but Checkout is easier to
finish reading and shows Android lifecycle concerns more directly.

## Documentation Hub Contract

`docs/index.html` should use a conventional documentation information
architecture:

1. status and installation,
2. five-minute Draft quickstart,
3. State/Event/Command and Android boundary,
4. public API quick reference,
5. testing, graph, restoration, and modeling guides,
6. example ladder and next/previous navigation.

Persistent sidebar navigation, an on-page table of contents, working search,
copyable code, responsive mobile navigation, and English/Korean parity are
part of this entry contract. Large marketing copy, presentation-first motion,
and an interactive concept demo are not the primary entry experience.

The example ladder's primary action opens an embedded interactive example
instead of navigating directly to Markdown. The lab must make the learning
boundary explicit: it mirrors the maintained example machines in browser
JavaScript and does not execute the Kotlin/JVM Afsm runtime.

The user must cause Events through the same kind of feature action that an
Android screen exposes. Text input dispatches value-change Events immediately;
buttons dispatch submit, save, pay, retry, result, cancel, review, and publish
Events only when the current phase permits them. The lab must not teach the
flow through a fixed `Next Event` or pre-recorded main-path player.

For each user action it shows, in order:

- the dispatched Event,
- named guard/case results when relevant,
- `Data` field changes as before/after diffs,
- phase transitions,
- emitted Command or phase-owned invocation work,
- the resulting State snapshot.

The currently enabled controls are derived from the selected example's phase so
the user can explore validation, retry, cancellation, or completion paths
instead of only one happy path. The full Markdown walkthrough remains a
secondary source link. Draft, Auth, Checkout, and ProductEditor should all be
selectable from the same lab so the learning ladder stays in one documentation
context.
