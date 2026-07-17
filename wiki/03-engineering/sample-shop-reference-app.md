---
title: Sample Shop Reference App
updated: 2026-07-17
---

# Sample Shop Reference App

## Purpose

The Compose + Room sample demonstrates both where Afsm helps and where ordinary
ViewModel state is clearer.

## Afsm Features

- Auth: form validation, login/register commands, async completion.
- Checkout: dynamic product id, loading, payment, retry, stale-result safety,
  restoration, durable completion.
- Product Editor: multi-step review/publish flow and cancellable upload.

Catalog and simpler data screens remain ordinary Android state holders.

## File Shape

```text
FeatureFlow.kt
FeatureStateMachine.kt
FeatureViewModel.kt
FeatureScreen.kt
FeatureStateMachineTest.kt
```

`Flow.kt` groups the sample's product-flow types without implying a mandatory
MVI contract convention.

## Android Boundary

Screens call verb-named ViewModel functions and construct zero feature machine
events. ViewModels translate calls and command results into internal events.

Auth/Checkout routes observe durable completion state. ProductEditor Done is a
direct route callback. No sample declares a separate UI output type.

## Graph Role

Auth, Checkout, and ProductEditor are annotated with `@AfsmGraph`. Generated
`.mmd` diagrams are the whole-flow review surface; machine source supplies
exact rules and tests prove edge policies.

## Verification

- feature transition tests,
- Checkout/ProductEditor ViewModel wiring tests,
- graph registry/topology tests,
- debug compilation and graph export,
- local release gate and external consumer.

Prior emulator evidence covers ProductEditor cancellation on its dated commit.
The current Effect-free UI redesign still needs a new controlled human review;
repository tests do not substitute for that evidence.
