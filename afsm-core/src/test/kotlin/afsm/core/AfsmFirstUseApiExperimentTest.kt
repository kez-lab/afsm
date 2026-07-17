package afsm.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * First-use regression flows after the Effect channel was removed.
 *
 * The earlier version of this file compared staged E/C/F declaration
 * candidates. The accepted public shape is now one explicitly typed machine
 * property with State/Event/Command only, so these tests preserve the realistic
 * Draft, Auth, and Checkout behavior rather than the superseded candidate
 * syntax.
 */
class AfsmFirstUseApiExperimentTest {
    @Test
    fun `Draft declaration keeps command retry and topology behavior`() {
        val titleChanged = draftMachine.transition(
            state = draftMachine.initialState,
            event = DraftEvent.TitleChanged("Travel Mug"),
        )
        val saving = draftMachine.transition(titleChanged.state, DraftEvent.SaveClicked)

        assertEquals(DraftPhase.Saving, saving.state.phase)
        assertEquals(listOf(DraftCommand.Save("Travel Mug")), saving.commands)

        val failed = draftMachine.transition(saving.state, DraftEvent.SaveFailed("offline"))
        val retried = draftMachine.transition(failed.state, DraftEvent.RetryClicked)

        assertEquals(DraftPhase.Saving, retried.state.phase)
        assertEquals(listOf(DraftCommand.Save("Travel Mug")), retried.commands)
        assertEquals(
            listOf("Editing", "Saving", "Failed", "Saved"),
            draftMachine.topology.states.map { state -> state.id },
        )
    }

    @Test
    fun `Auth completion is durable state and impossible result is invalid`() {
        val emailChanged = authMachine.transition(
            state = authMachine.initialState,
            event = AuthEvent.EmailChanged("dev@example.com"),
        )
        val passwordChanged = authMachine.transition(
            state = emailChanged.state,
            event = AuthEvent.PasswordChanged("secret123"),
        )
        val submitted = authMachine.transition(passwordChanged.state, AuthEvent.SubmitClicked)

        assertEquals(AuthPhase.Submitting, submitted.state.phase)
        assertEquals(
            listOf(AuthCommand.Login("dev@example.com", "secret123")),
            submitted.commands,
        )

        val authenticated = authMachine.transition(
            state = submitted.state,
            event = AuthEvent.LoginSucceeded(userId = "user-7"),
        )
        assertEquals(AuthPhase.Authenticated(userId = "user-7"), authenticated.state.phase)

        val impossible = authMachine.transition(
            state = authMachine.initialState,
            event = AuthEvent.LoginSucceeded(userId = "stale"),
        )
        assertIs<AfsmDecision.Invalid>(impossible.decision)
    }

    @Test
    fun `Checkout keeps runtime data request identity and durable completion`() {
        val initialState = checkoutState(cartId = "cart-42")
        val loading = checkoutMachine.transition(initialState, CheckoutEvent.ScreenEntered)
        val ready = checkoutMachine.transition(
            loading.state,
            CheckoutEvent.CartLoaded(items = listOf("mug")),
        )
        val paying = checkoutMachine.transition(
            ready.state,
            CheckoutEvent.PayClicked(requestId = "request-2"),
        )

        assertEquals(listOf(CheckoutCommand.LoadCart("cart-42")), loading.commands)
        assertEquals(
            CheckoutPhase.PaymentInProgress(requestId = "request-2"),
            paying.state.phase,
        )
        assertEquals(
            listOf(CheckoutCommand.Pay("request-2", listOf("mug"))),
            paying.commands,
        )

        val stale = checkoutMachine.transition(
            paying.state,
            CheckoutEvent.PaymentSucceeded("request-1", "order-old"),
        )
        assertIs<AfsmDecision.Ignored>(stale.decision)
        assertEquals(paying.state, stale.state)

        val completed = checkoutMachine.transition(
            stale.state,
            CheckoutEvent.PaymentSucceeded("request-2", "order-9"),
        )
        assertEquals(CheckoutPhase.Completed(orderId = "order-9"), completed.state.phase)
    }
}

private typealias DraftState = AfsmState<DraftPhase, DraftData>

private val draftMachine: AfsmDefaultMachine<DraftState, DraftEvent, DraftCommand> =
    afsmMachine {
        initial(DraftPhase.Editing, DraftData())

        phase(DraftPhase.Editing) {
            on<DraftEvent.TitleChanged> {
                updateData { data, event -> data.copy(title = event.value, error = null) }
            }
            on<DraftEvent.SaveClicked> {
                case(condition = { data.title.isNotBlank() }) {
                    transitionTo(DraftPhase.Saving)
                }
                case(condition = { data.title.isBlank() }) {
                    updateData { copy(error = "Title is required.") }
                }
            }
        }

        phase(DraftPhase.Saving) {
            onEnter {
                command(label = "Save") { DraftCommand.Save(data.title) }
            }
            on<DraftEvent.SaveFailed> {
                updateData { data, event -> data.copy(error = event.message) }
                transitionTo(DraftPhase.Failed)
            }
            on<DraftEvent.SaveSucceeded> {
                transitionTo(DraftPhase.Saved)
            }
        }

        phase(DraftPhase.Failed) {
            on<DraftEvent.RetryClicked> {
                transitionTo(DraftPhase.Saving)
            }
        }
        phase(DraftPhase.Saved)
    }

private sealed interface DraftPhase {
    data object Editing : DraftPhase
    data object Saving : DraftPhase
    data object Failed : DraftPhase
    data object Saved : DraftPhase
}

private data class DraftData(
    val title: String = "",
    val error: String? = null,
)

private sealed interface DraftEvent {
    data class TitleChanged(val value: String) : DraftEvent
    data object SaveClicked : DraftEvent
    data object SaveSucceeded : DraftEvent
    data class SaveFailed(val message: String) : DraftEvent
    data object RetryClicked : DraftEvent
}

private sealed interface DraftCommand {
    data class Save(val title: String) : DraftCommand
}

private typealias AuthState = AfsmState<AuthPhase, AuthData>

private val authMachine: AfsmDefaultMachine<AuthState, AuthEvent, AuthCommand> =
    afsmMachine {
        initial(AuthPhase.Editing, AuthData())

        phase(AuthPhase.Editing) {
            on<AuthEvent.EmailChanged> {
                updateData { data, event -> data.copy(email = event.value) }
            }
            on<AuthEvent.PasswordChanged> {
                updateData { data, event -> data.copy(password = event.value) }
            }
            on<AuthEvent.SubmitClicked> {
                case(condition = { data.email.contains('@') && data.password.length >= 8 }) {
                    transitionTo(AuthPhase.Submitting)
                }
                case(condition = { !data.email.contains('@') || data.password.length < 8 }) {
                    updateData { copy(error = "Enter valid credentials.") }
                }
            }
            on<AuthEvent.LoginSucceeded> {
                invalid(reason = "Login result arrived before submission.")
            }
        }

        phase(AuthPhase.Submitting) {
            onEnter {
                command(label = "Login") { AuthCommand.Login(data.email, data.password) }
            }
            on<AuthEvent.LoginSucceeded> {
                transitionTo<AuthPhase.Authenticated> {
                    AuthPhase.Authenticated(event.userId)
                }
            }
        }
        phase<AuthPhase.Authenticated>()
    }

private sealed interface AuthPhase {
    data object Editing : AuthPhase
    data object Submitting : AuthPhase
    data class Authenticated(val userId: String) : AuthPhase
}

private data class AuthData(
    val email: String = "",
    val password: String = "",
    val error: String? = null,
)

private sealed interface AuthEvent {
    data class EmailChanged(val value: String) : AuthEvent
    data class PasswordChanged(val value: String) : AuthEvent
    data object SubmitClicked : AuthEvent
    data class LoginSucceeded(val userId: String) : AuthEvent
}

private sealed interface AuthCommand {
    data class Login(val email: String, val password: String) : AuthCommand
}

private typealias CheckoutState = AfsmState<CheckoutPhase, CheckoutData>

private fun checkoutState(cartId: String): CheckoutState =
    AfsmState(CheckoutPhase.Idle, CheckoutData(cartId = cartId))

private val checkoutMachine: AfsmMachine<CheckoutState, CheckoutEvent, CheckoutCommand> =
    afsmMachine(initialPhase = CheckoutPhase.Idle) {
        phase(CheckoutPhase.Idle) {
            on<CheckoutEvent.ScreenEntered> {
                transitionTo(CheckoutPhase.Loading)
            }
        }
        phase(CheckoutPhase.Loading) {
            onEnter {
                command(label = "LoadCart") { CheckoutCommand.LoadCart(data.cartId) }
            }
            on<CheckoutEvent.CartLoaded> {
                updateData { data, event -> data.copy(items = event.items) }
                transitionTo(CheckoutPhase.Ready)
            }
        }
        phase(CheckoutPhase.Ready) {
            on<CheckoutEvent.PayClicked> {
                transitionTo<CheckoutPhase.PaymentInProgress> {
                    CheckoutPhase.PaymentInProgress(event.requestId)
                }
            }
        }
        phase<CheckoutPhase.PaymentInProgress> {
            onEnter {
                command(label = "Pay") { CheckoutCommand.Pay(phase.requestId, data.items) }
            }
            on<CheckoutEvent.PaymentSucceeded> {
                case(condition = { event.requestId == phase.requestId }) {
                    transitionTo<CheckoutPhase.Completed> {
                        CheckoutPhase.Completed(event.orderId)
                    }
                }
                ignore(
                    reason = "Stale payment success.",
                    condition = { event.requestId != phase.requestId },
                )
            }
        }
        phase<CheckoutPhase.Completed>()
    }

private sealed interface CheckoutPhase {
    data object Idle : CheckoutPhase
    data object Loading : CheckoutPhase
    data object Ready : CheckoutPhase
    data class PaymentInProgress(val requestId: String) : CheckoutPhase
    data class Completed(val orderId: String) : CheckoutPhase
}

private data class CheckoutData(
    val cartId: String,
    val items: List<String> = emptyList(),
)

private sealed interface CheckoutEvent {
    data object ScreenEntered : CheckoutEvent
    data class CartLoaded(val items: List<String>) : CheckoutEvent
    data class PayClicked(val requestId: String) : CheckoutEvent
    data class PaymentSucceeded(val requestId: String, val orderId: String) : CheckoutEvent
}

private sealed interface CheckoutCommand {
    data class LoadCart(val cartId: String) : CheckoutCommand
    data class Pay(val requestId: String, val items: List<String>) : CheckoutCommand
}
