# Afsm

Afsm is an Android-focused finite state machine toolkit for complex `ViewModel` flows.

It keeps Android architecture familiar:

```text
UI event
-> ViewModel
-> AfsmHost
-> AfsmReducer or AfsmMachine
-> new state + commands + optional effects
-> ViewModel executes commands
-> command results dispatch events
-> UI renders state
```

Afsm is useful when a screen has meaningful phases, retries, invalid transitions, async results, or multi-step behavior. It is intentionally not a full UI framework and should not be forced onto simple loading/content/error screens.

## Modules

| Module | Purpose | Android dependency |
|---|---|---|
| `afsm-core` | Pure Kotlin transition types, reducer contract, executable machine DSL, graph metadata | No |
| `afsm-runtime` | Coroutine host, serialized dispatch loop, command execution, effect delivery | No |
| `afsm-viewmodel` | Thin `ViewModel.afsmHost(...)` adapter backed by `viewModelScope` | Yes |
| `afsm-graph-ksp` | KSP discovery for `@AfsmGraph` machines | No Android runtime dependency |
| `sample-shop` | Compose + Room sample app proving real usage | Yes |
| `consumer-smoke` | Separate Android consumer build that resolves Afsm from Maven Local | Yes |

For repository-local development, depend on the project modules:

```kotlin
dependencies {
    implementation(project(":afsm-core"))
    implementation(project(":afsm-runtime"))
    implementation(project(":afsm-viewmodel"))
}
```

For local artifact evaluation, publish to Maven Local:

```bash
./gradlew publishToMavenLocal
```

Then consume the pre-release snapshot artifacts:

```kotlin
repositories {
    google()
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation("io.github.afsm:afsm-core:0.1.0-SNAPSHOT")
    implementation("io.github.afsm:afsm-runtime:0.1.0-SNAPSHOT")
    implementation("io.github.afsm:afsm-viewmodel:0.1.0-SNAPSHOT")

    ksp("io.github.afsm:afsm-graph-ksp:0.1.0-SNAPSHOT")
}
```

`io.github.afsm` is the current pre-release group id used for local publishing. Final Maven Central coordinates still need product approval.

## Core Concepts

| Concept | Meaning |
|---|---|
| `State` | Full UI/business state exposed to Android |
| `Phase` | Finite graph state inside an executable machine |
| `Context` | Extended data carried across phases |
| `Event` | User input or command result |
| `Command` | Host-executed work, such as repository calls or timers |
| `Effect` | Optional UI-side one-shot output |
| `AfsmReducer` | Low-level pure `state + event -> transition` contract |
| `AfsmMachine` | DSL-built phase/context machine with topology metadata |

## Minimal Reducer

```kotlin
sealed interface LoginState {
    data object Editing : LoginState
    data object Submitting : LoginState
    data object LoggedIn : LoginState
}

sealed interface LoginEvent {
    data object SubmitClicked : LoginEvent
    data object LoginSucceeded : LoginEvent
    data class LoginFailed(val message: String) : LoginEvent
}

sealed interface LoginCommand {
    data object SubmitLogin : LoginCommand
}

typealias LoginTransition =
    AfsmTransition<LoginState, LoginCommand, AfsmNoEffect>

class LoginReducer :
    AfsmReducer<LoginState, LoginEvent, LoginCommand, AfsmNoEffect> {

    override fun transition(
        state: LoginState,
        event: LoginEvent,
    ): LoginTransition {
        return when (state) {
            LoginState.Editing -> when (event) {
                LoginEvent.SubmitClicked -> Afsm.transitionTo(
                    state = LoginState.Submitting,
                    commands = listOf(LoginCommand.SubmitLogin),
                )

                LoginEvent.LoginSucceeded,
                is LoginEvent.LoginFailed -> Afsm.invalid(
                    state = state,
                    reason = "Login result before submit.",
                )
            }

            LoginState.Submitting -> when (event) {
                LoginEvent.LoginSucceeded -> Afsm.transitionTo(LoginState.LoggedIn)
                is LoginEvent.LoginFailed -> Afsm.transitionTo(LoginState.Editing)
                LoginEvent.SubmitClicked -> Afsm.ignore(
                    state = state,
                    reason = "Duplicate submit.",
                )
            }

            LoginState.LoggedIn -> Afsm.ignore(
                state = state,
                reason = "Login already completed.",
            )
        }
    }
}
```

## Graphable Machine DSL

Use `AfsmMachine` when the flow should produce a state diagram.

```kotlin
typealias ProductEditorState =
    AfsmState<ProductEditorPhase, ProductEditorContext>

private typealias ProductEditorMachine =
    AfsmMachine<
        ProductEditorPhase,
        ProductEditorContext,
        ProductEditorEvent,
        ProductEditorCommand,
        ProductEditorEffect,
        >

private fun productEditorMachine(): ProductEditorMachine {
    return afsmMachine {
        initial(
            phase = ProductEditorPhase.EditingDraft,
            context = ProductEditorContext(),
        )

        state(ProductEditorPhase.EditingDraft) {
            on<ProductEditorEvent.TitleChanged> {
                stay {
                    updateContext {
                        copy(draft = draft.withTitle(event.value))
                    }
                }
            }

            on<ProductEditorEvent.SubmitClicked> {
                transitionTo(
                    phase = ProductEditorPhase.ImageUploadInProgress,
                    guardLabel = "valid draft",
                    commandLabels = listOf("StartImageUpload"),
                    guard = { context.draft.isValid() },
                ) {
                    updateContext {
                        copy(draft = draft.normalized(), errorMessage = null)
                    }
                }

                otherwise {
                    updateContext {
                        copy(errorMessage = draft.validationMessage())
                    }
                }
            }
        }

        state(ProductEditorPhase.ImageUploadInProgress) {
            onEnter {
                command(ProductEditorCommand.StartImageUpload(context.draft))
            }

            onExit {
                command(ProductEditorCommand.CancelImageUpload)
            }

            on<ProductEditorEvent.ImageUploadSucceeded> {
                transitionTo<ProductEditorPhase.ReviewSubmissionInProgress>(
                    phase = {
                        ProductEditorPhase.ReviewSubmissionInProgress(
                            uploadToken = event.uploadToken,
                        )
                    },
                )
            }
        }
    }
}
```

`transitionTo(...)` changes phase. `stay(...)` handles an event without changing phase. Phase-changing transitions run in this order:

```text
onExit -> transition block -> onEnter
```

Initial state construction does not automatically run `onEnter`. Trigger startup work with an explicit event such as `ScreenEntered`.

## ViewModel Integration

```kotlin
class ProductEditorViewModel(
    private val repository: ProductRepository,
) : ViewModel() {
    private val host = afsmHost(
        initialState = productEditorState(),
        reducer = ProductEditorStateMachine(),
        commandHandler = { command: ProductEditorCommand, dispatch ->
            when (command) {
                is ProductEditorCommand.StartImageUpload -> {
                    val token = repository.upload(command.draft)
                    dispatch(ProductEditorEvent.ImageUploadSucceeded(token))
                }
            }
        },
    )

    val state: StateFlow<ProductEditorState> = host.state
    val effects: Flow<ProductEditorEffect> = host.effects

    fun onEvent(event: ProductEditorEvent) {
        host.dispatch(event)
    }
}
```

The UI renders `state` and sends events up. Navigation, snackbar display, focus, scroll, and animation state should stay in the UI unless they are part of the business flow.

## Graph Generation

Annotate graphable machines:

```kotlin
@AfsmGraph(
    id = "ProductEditor",
    fileName = "ProductEditorStateMachine.mmd",
)
class ProductEditorStateMachine(
    machine: ProductEditorMachine = productEditorMachine(),
) : ProductEditorMachine by machine
```

The sample app generates Mermaid state diagrams with:

```bash
./gradlew :sample-shop:generateAfsmMmd
```

Output:

```text
sample-shop/build/generated/afsm/mmd/AuthStateMachine.mmd
sample-shop/build/generated/afsm/mmd/ProductEditorStateMachine.mmd
```

## Runtime Policies

- Dispatch is non-suspending and serialized through FIFO event processing.
- Commands execute sequentially in the MVP runtime.
- Command results must dispatch typed events back into the host.
- Domain failures should become domain events, not thrown exceptions.
- Unexpected command exceptions use `AfsmCommandFailurePolicy`.
- `CancellationException` is always rethrown.
- Effects are best-effort one-shot outputs with no replay by default.

## Verification

Current baseline:

```bash
./gradlew :afsm-core:test :afsm-runtime:test :afsm-viewmodel:testDebugUnitTest
./gradlew :sample-shop:compileDebugKotlin :sample-shop:testDebugUnitTest :sample-shop:generateAfsmMmd
./gradlew publishToMavenLocal
./scripts/verify-consumer-smoke.sh
```

`consumer-smoke` is intentionally a separate Gradle build. It verifies that an Android project can resolve `afsm-core`, `afsm-runtime`, `afsm-viewmodel`, and `afsm-graph-ksp` from Maven Local without project-module shortcuts.

See [docs/afsm-public-api.md](docs/afsm-public-api.md) for the API reference and [docs/sample-shop-afsm-guide.md](docs/sample-shop-afsm-guide.md) for sample app notes.
