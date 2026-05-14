---
title: Afsm Example Catalog
updated: 2026-05-14
---

# Afsm Example Catalog

This page records the canonical example ladder for public usability.

The public docs version is [docs/examples.md](../../docs/examples.md).

## Example Ladder

| Level | Example | Purpose |
|---|---|---|
| 1 | README minimal Draft | Smallest mental model: phase, context, event, command, `onEnter` |
| 2 | Auth | Smallest real Android screen: validation, command result events, navigation effect |
| 3 | Checkout | Mid-size Android lifecycle flow: navigation argument initial state, loading, payment, retry, stale results, durable completion |
| 4 | ProductEditor | Complex transaction flow: draft, upload, review rejection/resubmission, approval, publishing |
| Anti-example | Catalog/Product/Reviews | Screens that should remain ordinary `ViewModel + Flow` |

## Why Checkout Was Promoted

Checkout used to be a custom `AfsmReducer` escape hatch. It is now a graphable
`AfsmMachine<CheckoutState, CheckoutEvent, CheckoutCommand, CheckoutEffect>`.

The promotion matters because Checkout demonstrates the exact concerns Android
teams ask about:

- dynamic initial state from navigation `productId`,
- phase/context separation,
- command emission from `onEnter`,
- request-id stale result handling,
- durable completion state plus optional navigation effect,
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

The public onboarding should send developers to Checkout before ProductEditor.
ProductEditor is stronger, but Checkout is easier to finish reading and shows
Android lifecycle concerns more directly.
