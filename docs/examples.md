# Afsm Example Catalog

Use this page to choose the right example before copying code.

Afsm should feel useful when a screen has a real flow. It should not make simple
data screens heavier. The examples are ordered from smallest to most persuasive.

## Example Map

| Example | Read when | Shows | Docs | Source |
|---|---|---|---|---|
| Minimal Draft | You want the smallest possible machine | `Phase`, `Context`, `Event`, `Command`, `onEnter`, `ViewModel.afsmHost` | [README.md](../README.md) | README-only copy-paste example |
| Auth | You need login/register form submission | form context, validation guards, command result events, navigation effect | [auth-walkthrough.md](auth-walkthrough.md) | [AuthStateMachine.kt](../sample-shop/src/main/kotlin/afsm/sample/shop/feature/auth/AuthStateMachine.kt), [AuthViewModel.kt](../sample-shop/src/main/kotlin/afsm/sample/shop/feature/auth/AuthViewModel.kt), [AuthStateMachineTest.kt](../sample-shop/src/test/kotlin/afsm/sample/shop/feature/auth/AuthStateMachineTest.kt) |
| Checkout | You need async loading, payment, retry, stale results, and durable completion | graphable payment flow, request ids, state plus optional effect, render mapping | [checkout-walkthrough.md](checkout-walkthrough.md) | [CheckoutStateMachine.kt](../sample-shop/src/main/kotlin/afsm/sample/shop/feature/checkout/CheckoutStateMachine.kt), [CheckoutViewModel.kt](../sample-shop/src/main/kotlin/afsm/sample/shop/feature/checkout/CheckoutViewModel.kt), [CheckoutStateMachineTest.kt](../sample-shop/src/test/kotlin/afsm/sample/shop/feature/checkout/CheckoutStateMachineTest.kt) |
| ProductEditor | You need a high-branching transaction flow | save draft, upload, review reject/resubmit, approve, publish, generated graph | [product-editor-walkthrough.md](product-editor-walkthrough.md) | [ProductEditorStateMachine.kt](../sample-shop/src/main/kotlin/afsm/sample/shop/feature/editor/ProductEditorStateMachine.kt), [ProductEditorViewModel.kt](../sample-shop/src/main/kotlin/afsm/sample/shop/feature/editor/ProductEditorViewModel.kt), [ProductEditorStateMachineTest.kt](../sample-shop/src/test/kotlin/afsm/sample/shop/feature/editor/ProductEditorStateMachineTest.kt) |
| Catalog/Product/Reviews | You need to know when not to use Afsm | ordinary `ViewModel + Flow` for data screens | This page | [CatalogViewModel.kt](../sample-shop/src/main/kotlin/afsm/sample/shop/feature/catalog/CatalogViewModel.kt), [ProductDetailViewModel.kt](../sample-shop/src/main/kotlin/afsm/sample/shop/feature/product/ProductDetailViewModel.kt) |

## Recommended Reading Path

1. Start with the README minimal Draft machine.
2. Read [modeling-rules.md](modeling-rules.md).
3. Read [auth-walkthrough.md](auth-walkthrough.md) for the smallest Android screen.
4. Read [checkout-walkthrough.md](checkout-walkthrough.md) for lifecycle and retry policy.
5. Read [product-editor-walkthrough.md](product-editor-walkthrough.md) only after Checkout; it is intentionally larger.

## What Each Example Proves

### Minimal Draft

The README example proves the core API can be understood without Android,
Room, KSP, or graph generation.

The README then adds the smallest `ViewModel.afsmHost(...)` bridge. Treat those
as two steps: first make the pure machine work, then host it from Android.

It is the onboarding shape:

```text
Editing -- SaveClicked --> Saving -- DraftSaveCompleted --> Saved
```

### Auth

Auth is the smallest real Android example.

It proves:

- UI input can stay as context updates,
- validation can be expressed with guarded transitions,
- repository calls stay in the command handler,
- successful auth can be durable phase plus optional navigation effect.

Generated graph:

```text
sample-shop/build/generated/afsm/mmd/AuthStateMachine.mmd
```

### Checkout

Checkout is the best mid-size example for Android teams.

It proves:

- navigation argument state can be supplied through `afsmHost(machine, initialState)`,
- product loading and payment are explicit phases,
- payment retry is visible in the graph,
- stale command results are safe with request ids,
- completed payment is durable state, while navigation remains an optional effect,
- UI can render a `CheckoutRenderState` instead of knowing every internal phase.

Generated graph:

```text
sample-shop/build/generated/afsm/mmd/CheckoutStateMachine.mmd
```

### ProductEditor

ProductEditor is the complex-flow reference.

It proves:

- long flows remain readable when phases are named by business condition,
- context carries form data without stuffing every phase constructor,
- `onEnter` is a good fit for phase-owned work,
- generated `.mmd` output can explain the flow before reading Kotlin.

Generated graph:

```text
sample-shop/build/generated/afsm/mmd/ProductEditorStateMachine.mmd
```

### Non-Afsm Screens

The sample intentionally keeps catalog, product detail, likes, and review lists
outside Afsm.

That is part of the library design. Afsm should earn its complexity by making a
flow more traceable. If ordinary `ViewModel + StateFlow` is clearer, use that.

## Generate Graphs

For external app-module setup, read [graph-generation.md](graph-generation.md).

```bash
./gradlew :sample-shop:generateAfsmMmd
```

Expected outputs:

```text
sample-shop/build/generated/afsm/mmd/AuthStateMachine.mmd
sample-shop/build/generated/afsm/mmd/CheckoutStateMachine.mmd
sample-shop/build/generated/afsm/mmd/ProductEditorStateMachine.mmd
```

## Verify Examples

```bash
./gradlew :sample-shop:testDebugUnitTest :sample-shop:generateAfsmMmd --warning-mode all --no-daemon
```

Use the tests as executable documentation. If a test reads like a user journey,
the example is probably working as intended.
