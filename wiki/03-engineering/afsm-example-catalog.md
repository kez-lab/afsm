---
title: Afsm Example Catalog
updated: 2026-07-17
---

# Afsm Example Catalog

This page records the canonical example ladder for public usability.

The public docs version is [docs/examples.md](../../docs/examples.md).

## Example Ladder

| Level | Example | Purpose |
|---|---|---|
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

The public onboarding should send developers through dedicated walkthroughs in
this order:

1. Auth.
2. Checkout.
3. ProductEditor.

ProductEditor is stronger as a graph stress test, but Checkout is easier to
finish reading and shows Android lifecycle concerns more directly.
