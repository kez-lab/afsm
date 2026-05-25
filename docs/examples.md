# Afsm Example Catalog

Use this page to choose the right example before copying code.

Afsm should feel useful when a screen has a real flow. It should not make simple
data screens heavier. The examples are ordered from smallest to most persuasive.

## Example Map

| Example | Read when | Shows | Docs | Source |
|---|---|---|---|---|
| Minimal Draft | You want the smallest possible machine | `Phase`, `Data`, `Event`, `Command`, command success/failure events, `onEnter`, `ViewModel.afsmHost`, route state collection | [getting-started.md](getting-started.md) | [DraftQuickstart.kt](../consumer-smoke/app/src/main/kotlin/afsm/consumer/smoke/DraftQuickstart.kt), [README.md](../README.md) |
| Auth | You need login/register form submission | form data, validation guards, command result events, navigation effect | [auth-walkthrough.md](auth-walkthrough.md) | [AuthStateMachine.kt](../sample-shop/src/main/kotlin/afsm/sample/shop/feature/auth/AuthStateMachine.kt), [AuthViewModel.kt](../sample-shop/src/main/kotlin/afsm/sample/shop/feature/auth/AuthViewModel.kt), [AuthStateMachineTest.kt](../sample-shop/src/test/kotlin/afsm/sample/shop/feature/auth/AuthStateMachineTest.kt) |
| Checkout | You need async loading, payment, retry, stale results, and durable completion | graphable payment flow, request ids, state plus optional effect, render mapping | [checkout-walkthrough.md](checkout-walkthrough.md) | [CheckoutStateMachine.kt](../sample-shop/src/main/kotlin/afsm/sample/shop/feature/checkout/CheckoutStateMachine.kt), [CheckoutViewModel.kt](../sample-shop/src/main/kotlin/afsm/sample/shop/feature/checkout/CheckoutViewModel.kt), [CheckoutStateMachineTest.kt](../sample-shop/src/test/kotlin/afsm/sample/shop/feature/checkout/CheckoutStateMachineTest.kt) |
| ProductEditor | You need an advanced graph stress test | save draft, upload, review reject/resubmit, approve, publish, generated graph | [product-editor-walkthrough.md](product-editor-walkthrough.md) | [ProductEditorStateMachine.kt](../sample-shop/src/main/kotlin/afsm/sample/shop/feature/editor/ProductEditorStateMachine.kt), [ProductEditorViewModel.kt](../sample-shop/src/main/kotlin/afsm/sample/shop/feature/editor/ProductEditorViewModel.kt), [ProductEditorStateMachineTest.kt](../sample-shop/src/test/kotlin/afsm/sample/shop/feature/editor/ProductEditorStateMachineTest.kt) |
| Catalog/Product/Reviews | You need to know when not to use Afsm | ordinary `ViewModel + Flow` for data screens | This page | [CatalogViewModel.kt](../sample-shop/src/main/kotlin/afsm/sample/shop/feature/catalog/CatalogViewModel.kt), [ProductDetailViewModel.kt](../sample-shop/src/main/kotlin/afsm/sample/shop/feature/product/ProductDetailViewModel.kt) |

## Adoption Decision Examples

Use this table before copying a sample. Afsm should make a flow easier to
review, test, or diagram; it should not be the default for every screen.

| Screen shape | Recommendation | Why |
|---|---|---|
| Auth | Use as a syntax tutorial or when auth includes real multi-step policy | The sample is intentionally small, so it teaches guards, commands, render state, and effects without a large domain. A basic login-only form can stay ordinary `ViewModel + StateFlow`. |
| Checkout / ProductEditor | Strong Afsm candidates | They have visible phases, invalid transitions, async command results, retry or resubmission, and graph value during review. |
| Catalog / ProductDetail / Review list | Prefer ordinary ViewModel | They are mostly data projection, filtering, likes, or form submission without a meaningful business-flow state diagram. |

## Recommended Reading Path

1. Start with [getting-started.md](getting-started.md).
2. Run the first Draft JVM tests, then use [testing-guide.md](testing-guide.md)
   when you need broader transition or ViewModel coverage.
3. Read [modeling-rules.md](modeling-rules.md).
4. Read [auth-walkthrough.md](auth-walkthrough.md) for the smallest Android screen.
5. Read [checkout-walkthrough.md](checkout-walkthrough.md) for lifecycle and retry policy.
6. Read [product-editor-walkthrough.md](product-editor-walkthrough.md) only after Checkout; it is intentionally larger.

## What Each Example Proves

### Minimal Draft

The getting-started Draft example proves the core API can be understood before
Room, KSP, or graph generation.

The guide then adds the smallest `ViewModel.afsmHost(...)` bridge. Treat those
as two steps: first make the pure machine work, then host it from Android. The
consumer-smoke mirror compiles that shape against published Maven Local
artifacts.

After the ViewModel works, the first Compose route is still ordinary UI:
collect `viewModel.state` with `collectAsStateWithLifecycle()` and send UI
callbacks back through `viewModel.onEvent(...)`.

Pass `DraftState` directly at first. Add a render state only when UI code starts
deriving visible behavior from multiple phases; Auth and Checkout show that
next step.

It is the onboarding shape:

```text
Editing -- SaveClicked --> Saving -- DraftSaveCompleted --> Saved
Saving -- DraftSaveFailed --> Editing
```

### Auth

Auth is the smallest real Android example.

It proves:

- UI input can stay as data updates,
- validation can be expressed with guarded transitions,
- repository calls stay in the command handler,
- render state can hide small phase/data details from Compose,
- successful auth can be durable phase plus optional navigation effect
- route code can collect `host.effects` with `CollectAfsmEffects(...)`

For the first `AfsmNoEffect` to real effect migration, read
`AuthStateMachine.kt` together with `AuthScreen.kt`.

Generated graph:

```text
sample-shop/build/generated/afsm/mmd/AuthStateMachine.mmd
```

### Checkout

Checkout is the best production-style mid-size example for Android teams.

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

ProductEditor is the advanced graph stress test.

It proves:

- long flows remain readable when phases are named by business condition,
- data carries form data without stuffing every phase constructor,
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
