# Sample Shop Afsm Guide

This sample app validates Afsm against a small but realistic Android shopping app.

The point is not to force every screen into a finite state machine. The app uses Afsm only where flow correctness matters, and keeps ordinary data screens on standard ViewModel + Flow.

## Module

- Gradle module: `:sample-shop`
- Package: `afsm.sample.shop`
- App entry point: `sample-shop/src/main/kotlin/afsm/sample/shop/MainActivity.kt`
- Manual DI: `sample-shop/src/main/kotlin/afsm/sample/shop/app/ShopAppContainer.kt`
- Database: `sample-shop/src/main/kotlin/afsm/sample/shop/core/database/ShopDatabase.kt`
- Example catalog: [examples.md](examples.md)
- Getting started: [getting-started.md](getting-started.md)
- Modeling rules: [modeling-rules.md](modeling-rules.md)
- Restoration/effect/command policy: [restoration-effect-command-policy.md](restoration-effect-command-policy.md)
- Graph generation: [graph-generation.md](graph-generation.md)
- Auth walkthrough: [auth-walkthrough.md](auth-walkthrough.md)
- Checkout walkthrough: [checkout-walkthrough.md](checkout-walkthrough.md)
- ProductEditor walkthrough: [product-editor-walkthrough.md](product-editor-walkthrough.md)

## Dependencies

The sample intentionally stays close to the Android/Kotlin stack:

- Afsm modules: `afsm-core`, `afsm-runtime`, `afsm-viewmodel`, `afsm-compose`
- Compose Material 3
- Navigation Compose
- AndroidX Lifecycle Compose
- Room with KSP
- Kotlin coroutines

No external DI, networking, MVI framework, or reducer framework is used.

## Architecture Rule

Use Afsm for screens where state transitions are the behavior:

- signup/login editing, submitting, and completion
- product draft, mock upload, review rejection, resubmission, approval, publishing, and completion
- payment loading, failure, retry, and completion

Use ordinary ViewModel state for screens where the behavior is mostly data observation:

- product list
- product detail
- likes
- review registration
- review list

This split is intentional. A usable Android FSM library must make complex flows clearer without making simple screens ceremonial.

## Auth Flow

Files:

- `feature/auth/AuthContract.kt`
- `feature/auth/AuthStateMachine.kt`
- `feature/auth/AuthViewModel.kt`
- `feature/auth/AuthScreen.kt`
- Walkthrough: [auth-walkthrough.md](auth-walkthrough.md)

Flow:

```text
TextField/Button interaction
-> AuthEvent
-> AuthStateMachine.transition(state, event)
-> AuthState + AuthCommand/AuthEffect
-> AuthViewModel command handler
-> AuthRepository
-> AuthEvent.AuthSucceeded/AuthFailed
-> AuthState/AuthEffect.OpenCatalog
-> AuthRoute navigation callback
```

Contract:

- `AuthState` is a feature-local typealias for `AfsmState<AuthPhase, AuthData>`.
- `AuthPhase` contains the finite graph states: `Editing`, `Submitting`, and `Authenticated`.
- `AuthData` keeps mode, form, and error data outside the finite phase; the terminal `Authenticated` phase carries its `UserSession` payload.
- `AuthStateMachine` uses the executable DSL directly with `AuthPhase + AuthData`.
- `AuthStateMachine` is annotated with `@AfsmGraph`; KSP discovers it and a Gradle export task writes `AuthStateMachine.mmd` through the generated registry.
- `AuthForm` keeps input data separate from the phase.
- `AuthEvent` models user input and command results.
- `AuthCommand` models async work the ViewModel must execute.
- `AuthEffect.OpenCatalog` models one-shot navigation after successful auth.

Key usage shape:

```kotlin
private val host = afsmHost(
    machine = AuthStateMachine,
    commandHandler = { command: AuthCommand, dispatch ->
        // Execute repository work, then dispatch result events.
    },
)

val state = host.state
val effects = host.effects

fun onEvent(event: AuthEvent) {
    host.dispatch(event)
}
```

## Product Registration Flow

Files:

- `feature/editor/ProductEditorContract.kt`
- `feature/editor/ProductEditorStateMachine.kt`
- `feature/editor/ProductEditorViewModel.kt`
- `feature/editor/ProductEditorScreen.kt`
- Walkthrough: [product-editor-walkthrough.md](product-editor-walkthrough.md)

Flow:

```text
EditingDraft
-> SaveDraftClicked: SavingDraft -> DraftSaved
-> SubmitClicked: ImageUploadInProgress
-> ImageUploadSucceeded: ReviewSubmissionInProgress
-> first ReviewRejected: Rejected
-> ResubmitClicked: ImageUploadInProgress
-> second ReviewApproved: Approved
-> PublishClicked: PublishInProgress
-> PublishSucceeded: Published
-> DoneClicked: CloseEditor effect
```

Policy:

- ProductEditor uses the executable DSL with `typealias ProductEditorState = AfsmState<ProductEditorPhase, ProductEditorData>` at the state-machine boundary.
- `ProductEditorState.toRenderState()` maps internal phases to ordinary Compose render data so `ProductEditorScreen` does not branch on `ProductEditorPhase`.
- Flow phases stay explicit: `SavingDraft`, `DraftSaved`, `ImageUploadInProgress`, `ReviewSubmissionInProgress`, `Rejected`, `Approved`, `PublishInProgress`, and `Published`.
- Actual draft data lives in `ProductEditorData`, not in every phase constructor.
- Event branches use named `case(...)` blocks when there are domain alternatives; `transitionTo(...)` only changes phase.
- ProductEditor keeps submit/resubmit phase transitions inline in each event branch; helper functions are limited to data transformations so graph-relevant flow remains visible.
- Validation failure is modeled as an explicit no-transition `case(label = "invalid ...", condition = ...)` that updates data, not as a second competing `transitionTo`.
- `onEnter` emits commands such as `SaveDraft`, `StartImageUpload`, `StartReviewSubmission`, and `StartProductPublish`.
- `ProductEditorStateMachine` is the annotated executable machine property; no
  delegated object or separate factory is required.
- KSP generates `AfsmGeneratedGraphRegistry` from annotated stable machine
  properties (and still supports eligible classes/objects).
- The `io.github.afsm.graph` Gradle plugin generates the export test and registers `./gradlew :sample-shop:generateAfsmMmd`.
- `./gradlew :sample-shop:generateAfsmMmd` writes registry entries such as `sample-shop/build/generated/afsm/mmd/ProductEditorStateMachine.mmd`.
- Text changes inside `EditingDraft` and `Rejected` are no-transition handlers that update data with `updateData { data, event -> ... }`.
- Long-running phases use phase names like `ImageUploadInProgress`; host work uses command names like `StartImageUpload`.
- Review attempt count is part of `ProductDraft`, so mock rejection/approval behavior is deterministic.
- The product is inserted into Room only after `PublishSucceeded`.
- `DoneClicked` emits a close effect instead of making navigation a state machine dependency.

## Checkout Flow

Files:

- `feature/checkout/CheckoutContract.kt`
- `feature/checkout/CheckoutStateMachine.kt`
- `feature/checkout/CheckoutViewModel.kt`
- `feature/checkout/CheckoutScreen.kt`
- Walkthrough: [checkout-walkthrough.md](checkout-walkthrough.md)

Flow:

```text
CheckoutRoute enters with productId
-> CheckoutEvent.ScreenEntered
-> CheckoutPhase.ProductLoading
-> CheckoutCommand.LoadProduct
-> ProductRepository.findProduct(productId)
-> CheckoutEvent.ProductLoaded/ProductUnavailable
-> CheckoutPhase.ProductReady/ProductUnavailable
-> PayClicked
-> CheckoutPhase.PaymentInProgress(requestId)
-> CheckoutCommand.SubmitPayment
-> PaymentRepository.submitPayment(...)
-> PaymentSucceeded/PaymentFailed
-> CheckoutPhase.Completed(orderId) + PaymentCompleted effect
   or CheckoutPhase.PaymentFailed retry state
```

Policy:

- Checkout is now a graphable `AfsmMachine<CheckoutState, CheckoutEvent,
  CheckoutCommand, CheckoutEffect>` built with the executable DSL.
- `CheckoutState` is `AfsmState<CheckoutPhase, CheckoutData>`.
- Product loading and payment commands are emitted from phase `onEnter`
  handlers and serialized by `AfsmHost`.
- Duplicate enter/pay events are ignored while work is already running.
- First mock payment attempt fails for higher-priced products, so retry can be exercised.
- Payment completion is durable state plus an effect. The state renders
  completion if the effect is missed; the effect lets the route navigate.
- Payment commands include a request id and result events echo that id. Late
  results from older payment attempts are treated as stale `Ignored` events
  instead of invalid programmer errors.
- Checkout demonstrates `afsmHost(machine = ..., initialState = ...)` for a
  graphable machine whose starting state depends on a navigation argument.
- `CheckoutState.toRenderState()` keeps Compose rendering simple while the
  internal graph stays precise.
- `./gradlew :sample-shop:generateAfsmMmd` now writes
  `sample-shop/build/generated/afsm/mmd/CheckoutStateMachine.mmd`.

## Compose Effect Collection

Route composables use `CollectAfsmEffects(...)` from `afsm-compose` instead of
hand-writing `LaunchedEffect { effects.collect { ... } }`.

Effects remain UI one-shots. Durable business information must stay in state.

## Testing Policy

State machine tests are plain JVM tests:

- `AuthStateMachineTest`
- `CheckoutStateMachineTest`
- `ProductEditorStateMachineTest`

These tests are executable specs. If a test fails, treat it as a product behavior signal first. Do not weaken tests just to make implementation pass.

Current verification:

```bash
./gradlew :sample-shop:testDebugUnitTest :sample-shop:generateAfsmMmd :sample-shop:assembleDebug --warning-mode all --no-daemon
```

## Early API Feedback

The current sample suggests:

- `AfsmTransition<S, C, F>` should rarely appear in app code now that primary samples expose graphable `AfsmMachine<State, Event, Command, Effect>` objects.
- `ViewModel.afsmHost(...)` reads naturally in real Android ViewModels.
- `afsmHost(machine = ..., initialState = ...)` keeps graphable state machines
  usable for navigation-argument screens like Checkout.
- `Command` is easier to explain than making transition functions suspend.
- `Effect` should stay rare and focused on UI-side one-shot work.
- Flow states must remain phases. Hiding `SavingDraft` or `DraftSaved` as data flags made the state machine less readable and less graphable.
- `ProductDraft` belongs in data; phase constructors should carry only flow-specific edge data such as `uploadToken`, rejection reason, or published product metadata.
- The executable DSL is more graph-friendly than the phased helper because branch targets are declared at build time and exported through `AfsmMachine.topology` / `AfsmTopology.toMmd()`.
- `AfsmReducer` remains the lower-level runtime contract, but feature code
  should normally expose graphable `AfsmMachine<State, Event, Command, Effect>`
  objects. The executable DSL builds that machine directly on the standard
  `AfsmState<Phase, Data>` data class.
- `AfsmMachine<State, Event, Command, Effect>` is the feature-boundary alias
  shape for graphable machines, so sample code keeps the internal `Phase/Data`
  split behind a named state type.
- The standard `AfsmState<Phase, Data>` model now removes Auth/ProductEditor adapter boilerplate while keeping state diagrams focused on phases.
- Custom sealed UI states require an explicit feature-owned `AfsmReducer`; the core API no longer ships an adapter base.
- Kotlin typealias constructors cannot have a same-named default factory, so ProductEditor uses `productEditorState()` for initial/default state creation.
- A shared `AfsmStateFactory` was spiked but rejected for now because singleton
  phase inference requires explicit `<Phase, Data>` arguments and the extra
  public API is heavier than a small feature-local factory function.
- Simple data screens should not be forced into Afsm, but product registration became a better reference after being expanded into review/publish phases.
- Checkout is now the mid-size reference for Android lifecycle, retry, request
  id, durable completion, and render-state mapping policy.
- `io.github.afsm.graph` should remain the public graph-generation entry point;
  sample modules should not own copy-paste MMD export tests.

Open follow-up:

- Add instrumentation smoke tests once an emulator target is available.
