# Android Official Docs Research for FSM Architecture

Date: 2026-05-01

Research method: Android CLI Knowledge Base search and fetch via `android docs search` and `android docs fetch`.

## Queries

- `architecture ui layer ViewModel UI state events unidirectional data flow`
- `Compose state state hoisting ViewModel collectAsStateWithLifecycle`
- `Compose side effects LaunchedEffect snackbar navigation ViewModel events`
- `navigation events ViewModel UI state Android architecture`
- `viewModelScope repeatOnLifecycle collectAsStateWithLifecycle coroutine testing ViewModel`
- `Kotlin coroutines Android ViewModel lifecycle-aware repeatOnLifecycle`
- `test Kotlin coroutines Android ViewModel runTest TestDispatcher`
- `SavedStateHandle ViewModel save UI state process death Compose`
- `save UI state Compose SavedStateHandle rememberSaveable ViewModel`

## Primary Official Sources

### UI layer

- KB URL: `kb://android/topic/architecture/ui-layer/index`
- Public URL: `https://developer.android.com/topic/architecture/ui-layer`
- Relevant points:
  - UI is a visual representation of application state.
  - UI layer converts application data changes into renderable UI state.
  - UDF is framed as state flowing down and events flowing up.
  - ViewModel is the recommended implementation for screen-level UI state with data-layer access.
  - UI should consume and display state, and relay user intent.
  - Business logic should not live in the UI layer.
  - UI behavior logic such as navigation and user messages is UI responsibility.

### UI events

- KB URL: `kb://android/topic/architecture/ui-layer/events`
- Public URL: `https://developer.android.com/topic/architecture/ui-layer/events`
- Relevant points:
  - User events can be handled by UI or ViewModel depending on whether they need UI behavior logic or business logic.
  - ViewModel events should result in UI state updates.
  - One-off event streams from ViewModel can lose delivery when producer outlives consumer.
  - Navigation is UI logic, but business validation before navigation belongs in ViewModel/state.
  - Transient messages can be represented as UI state and cleared after UI consumes them.

### State holders and UI state

- KB URL: `kb://android/topic/architecture/ui-layer/stateholders`
- Public URL: `https://developer.android.com/topic/architecture/ui-layer/stateholders`
- Relevant points:
  - State holders can be `ViewModel` or plain classes.
  - Screen UI state and UI element state are separate concepts.
  - Business logic state holders are lifecycle-independent and typically implemented as ViewModels.
  - UI logic state holders are lifecycle-dependent and typically plain classes remembered by UI.
  - State should be held closest to where it is consumed while preserving correct ownership.
  - Do not pass ViewModel instances down through reusable UI components.

### UI state production and management

- KB URL: `kb://android/topic/architecture/ui-layer/state-production`
- Public URL: `https://developer.android.com/topic/architecture/ui-layer/state-production`
- Relevant points:
  - State always exists; events happen.
  - Events are inputs of state production; state is the output consumed by UI.
  - State production is a pipeline with inputs, state holders, and UI state output.
  - `MutableStateFlow`, Compose State, and `stateIn` are common output mechanisms.
  - Prefer lifecycle-aware state production and consumption.
  - Avoid launching async work in a ViewModel `init` block when possible; prefer lazy/idempotent initialization where appropriate.

### ViewModel overview

- KB URL: `kb://android/topic/libraries/architecture/viewmodel/index`
- Public URL: `https://developer.android.com/topic/libraries/architecture/viewmodel`
- Relevant points:
  - ViewModel is a business logic or screen-level state holder.
  - It exposes state to UI and encapsulates related business logic.
  - It persists state and triggered operations across configuration changes.
  - It is scoped to a `ViewModelStoreOwner`.
  - ViewModel should not reference Views, Lifecycle, Context, or Resources.
  - Use ViewModel close to screen-level composables or navigation destinations.

### Unidirectional data flow in Compose

- KB URL: `kb://android/develop/ui/compose/architecture`
- Public URL: `https://developer.android.com/develop/ui/compose/architecture`
- Relevant points:
  - Composables accept state and expose events.
  - UDF in Compose is event -> state update -> display state.
  - UDF improves testability, state encapsulation, and UI consistency.
  - Compose examples model sign-in states as sealed classes, which supports the FSM direction.

### State hoisting in Compose

- KB URL: `kb://android/develop/ui/compose/state-hoisting`
- Public URL: `https://developer.android.com/develop/ui/compose/state-hoisting`
- Relevant points:
  - Hoist state to the lowest common ancestor between readers and writers.
  - Keep state closest to where it is consumed.
  - Hoist to ViewModel when business logic is involved.
  - UI element state can stay in composables or plain UI state holders.
  - ViewModel is an implementation detail of a state holder.
  - Some UI element state exposes suspend animation APIs that require composition-scoped coroutines.

### Side-effects in Compose

- KB URL: `kb://android/develop/ui/compose/side-effects`
- Public URL: `https://developer.android.com/develop/ui/compose/side-effects`
- Relevant points:
  - Composables should ideally be side-effect free.
  - Use effect APIs for predictable UI-related side effects.
  - Effects can be overused and should not break UDF.
  - `LaunchedEffect`, `rememberCoroutineScope`, `rememberUpdatedState`, `DisposableEffect`, `derivedStateOf`, and `snapshotFlow` each have specific lifecycle semantics.

### Coroutines with lifecycle-aware components

- KB URL: `kb://android/topic/libraries/architecture/coroutines`
- Public URL: `https://developer.android.com/topic/libraries/architecture/coroutines`
- Relevant points:
  - `viewModelScope` is tied to each ViewModel and is canceled when the ViewModel is cleared.
  - `lifecycleScope` is tied to a Lifecycle and is canceled when it is destroyed.
  - `repeatOnLifecycle` starts collection when the lifecycle reaches a target state and cancels when it drops below that state.
  - Prefer `repeatOnLifecycle` over `launchWhenX` APIs for flow collection because it cancels upstream collection instead of merely suspending.

### Coroutines best practices

- KB URL: `kb://android/kotlin/coroutines/coroutines-best-practices`
- Public URL: `https://developer.android.com/kotlin/coroutines/coroutines-best-practices`
- Relevant points:
  - Inject dispatchers instead of hardcoding them.
  - Suspend functions should be main-safe.
  - ViewModels should create coroutines for business logic rather than exposing suspend functions to the UI.
  - Expose immutable types.
  - Data/business layers should expose suspend functions for one-shot work and Flow for streams.
  - Avoid `GlobalScope`.
  - Do not swallow `CancellationException`.

### Testing Kotlin coroutines

- KB URL: `kb://android/kotlin/coroutines/test`
- Public URL: `https://developer.android.com/kotlin/coroutines/test`
- Relevant points:
  - Use `runTest` for coroutine tests.
  - Use `TestDispatcher` and a shared `TestCoroutineScheduler`.
  - Replace `Dispatchers.Main` with a `TestDispatcher` for local ViewModel unit tests that use `viewModelScope`.
  - `StandardTestDispatcher` gives precise scheduling control; `UnconfinedTestDispatcher` is simpler but less production-like.

### Testing flows

- KB URL: `kb://android/kotlin/flow/test`
- Public URL: `https://developer.android.com/kotlin/flow/test`
- Relevant points:
  - Use fake producers when testing classes that consume flows.
  - Test exposed finite flows with `first`, `toList`, `take`, `drop`, or similar operators.
  - For continuous collection, use `backgroundScope`.
  - For `StateFlow`, often assert the current `value` rather than every emission because `StateFlow` is conflated.
  - When testing `stateIn` with lazy or while-subscribed sharing, ensure at least one collector is active.

### Saved State module for ViewModel

- KB URL: `kb://android/topic/libraries/architecture/viewmodel/viewmodel-savedstate`
- Public URL: `https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-savedstate`
- Relevant points:
  - ViewModel handles configuration changes, but not system-initiated process death.
  - `SavedStateHandle` is a key-value map for small saved state tied to the task stack.
  - State used in business logic should be held in ViewModel and saved with `SavedStateHandle`.
  - State used in UI logic should use View `onSaveInstanceState` or Compose `rememberSaveable`.
  - Saved state should be simple and lightweight; use local persistence for complex or large data.
  - `SavedStateHandle` supports `getStateFlow`.
  - In tests, create a `SavedStateHandle` with required test values and pass it to the ViewModel.

### Save UI state in Compose

- KB URL: `kb://android/develop/ui/compose/state-saving`
- Public URL: `https://developer.android.com/develop/ui/compose/state-saving`
- Relevant points:
  - State preservation depends on where state is hoisted and what logic requires it.
  - `rememberSaveable` is for UI-scoped state.
  - ViewModel handles configuration changes for business state.
  - `SavedStateHandle` handles process recreation for small ViewModel-held UI element state.
  - Do not store complex screen UI state in `SavedStateHandle`; store minimal keys and rebuild from data layer.

### Save UI states

- KB URL: `kb://android/topic/libraries/architecture/saving-states`
- Public URL: `https://developer.android.com/topic/libraries/architecture/saving-states`
- Relevant points:
  - ViewModel survives configuration changes but not system-initiated process death.
  - Saved instance state survives configuration changes and process death but is limited to small/simple data.
  - Persistent storage survives broader app lifecycle events and should hold durable app data.
  - Divide restore work across ViewModel, saved instance state, and persistence based on complexity and lifetime.

## Implications for This Project

- The FSM should be a business-flow state holder or transition model, not a UI widget state manager.
- The Android `ViewModel` remains the screen-level state holder and command executor.
- Compose UI should receive state and event lambdas, not raw ViewModel instances deep in the tree.
- One-off ViewModel event streams should not be the default escape hatch; prefer durable state or narrowly scoped UI effects.
- Navigation should usually be executed by UI, with ViewModel/FSM exposing the business state that makes navigation valid.
- State machines should be plain Kotlin and testable without Android dependencies.
- Command execution should use ViewModel-owned coroutines and be tested with coroutine test dispatchers.
- StateFlow-based ViewModel tests should account for conflation and `stateIn` collection requirements.
- FSM restoration should save only minimal transient keys or input state; complex screen state should be rebuilt from persistent/domain data.
