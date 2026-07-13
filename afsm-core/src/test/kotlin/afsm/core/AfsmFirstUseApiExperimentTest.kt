package afsm.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Test-only prototypes for the first-use API experiment.
 *
 * These helpers deliberately do not change the production API. They let the
 * repository compare declaration shapes against the same executable DSL and
 * runtime before choosing a public redesign.
 */
class AfsmFirstUseApiExperimentTest {
    @Test
    fun `candidate declarations preserve all Draft type channels and behavior`() {
        draftCandidates.forEach { machine ->
            val titleChanged = machine.transition(
                state = machine.initialState,
                event = DraftEvent.TitleChanged("  Travel Mug  "),
            )
            val saveStarted = machine.transition(
                state = titleChanged.state,
                event = DraftEvent.SaveClicked,
            )

            assertEquals(DraftPhase.Saving, saveStarted.state.phase)
            assertEquals(
                listOf(DraftCommand.Save(title = "  Travel Mug  ")),
                saveStarted.commands,
            )

            val failed = machine.transition(
                state = saveStarted.state,
                event = DraftEvent.SaveFailed("offline"),
            )
            assertEquals(DraftPhase.Failed, failed.state.phase)
            assertEquals("offline", failed.state.data.error)

            val retried = machine.transition(
                state = failed.state,
                event = DraftEvent.RetryClicked,
            )
            assertEquals(DraftPhase.Saving, retried.state.phase)
            assertEquals(listOf(DraftCommand.Save("  Travel Mug  ")), retried.commands)
            assertTrue(
                machine.topology.states.map { it.id }
                    .containsAll(listOf("Editing", "Saving", "Failed", "Saved")),
            )
        }
    }

    @Test
    fun `candidate declarations preserve Auth command invalid and effect boundaries`() {
        authCandidates.forEach { machine ->
            val emailChanged = machine.transition(
                state = machine.initialState,
                event = AuthEvent.EmailChanged("dev@example.com"),
            )
            val passwordChanged = machine.transition(
                state = emailChanged.state,
                event = AuthEvent.PasswordChanged("secret123"),
            )
            val submitted = machine.transition(
                state = passwordChanged.state,
                event = AuthEvent.SubmitClicked,
            )

            assertEquals(AuthPhase.Submitting, submitted.state.phase)
            assertEquals(
                listOf(AuthCommand.Login("dev@example.com", "secret123")),
                submitted.commands,
            )

            val authenticated = machine.transition(
                state = submitted.state,
                event = AuthEvent.LoginSucceeded(userId = "user-7"),
            )
            assertEquals(AuthPhase.Authenticated(userId = "user-7"), authenticated.state.phase)
            assertEquals(listOf(AuthEffect.OpenHome(userId = "user-7")), authenticated.effects)

            val impossibleResult = machine.transition(
                state = machine.initialState,
                event = AuthEvent.LoginSucceeded(userId = "stale"),
            )
            assertIs<AfsmDecision.Invalid>(impossibleResult.decision)
        }
    }

    @Test
    fun `candidate declarations preserve dynamic Checkout data and stale result policy`() {
        checkoutCandidates(initialData = CheckoutData(cartId = "cart-42"))
            .forEach { machine ->
                assertEquals("cart-42", machine.initialState.data.cartId)

                val loading = machine.transition(
                    state = machine.initialState,
                    event = CheckoutEvent.ScreenEntered,
                )
                assertEquals(CheckoutPhase.Loading, loading.state.phase)
                assertEquals(listOf(CheckoutCommand.LoadCart("cart-42")), loading.commands)

                val ready = machine.transition(
                    state = loading.state,
                    event = CheckoutEvent.CartLoaded(items = listOf("mug")),
                )
                val paying = machine.transition(
                    state = ready.state,
                    event = CheckoutEvent.PayClicked(requestId = "request-2"),
                )
                assertEquals(
                    CheckoutPhase.PaymentInProgress(requestId = "request-2"),
                    paying.state.phase,
                )
                assertEquals(
                    listOf(CheckoutCommand.Pay(requestId = "request-2", items = listOf("mug"))),
                    paying.commands,
                )

                val stale = machine.transition(
                    state = paying.state,
                    event = CheckoutEvent.PaymentSucceeded(
                        requestId = "request-1",
                        orderId = "order-old",
                    ),
                )
                assertIs<AfsmDecision.Ignored>(stale.decision)
                assertEquals(paying.state, stale.state)

                val completed = machine.transition(
                    state = stale.state,
                    event = CheckoutEvent.PaymentSucceeded(
                        requestId = "request-2",
                        orderId = "order-9",
                    ),
                )
                assertEquals(CheckoutPhase.Completed(orderId = "order-9"), completed.state.phase)
                assertEquals(listOf(CheckoutEffect.ShowReceipt("order-9")), completed.effects)
            }
    }
}

// Candidate B fallback: Kotlin cannot partially specify three of a five-type
// function. A staged type set keeps E/C/F explicit, then infers P/D from State.
private class ExperimentalMachineTypes<E : Any, C : Any, F : Any> {
    fun <P : Any, D : Any> machine(
        initialState: AfsmState<P, D>,
        build: AfsmMachineBuilder<P, D, E, C, F>.() -> Unit,
    ): AfsmDefaultMachine<AfsmState<P, D>, E, C, F> = experimentalMachine(
        initialState = initialState,
        build = build,
    )
}

private fun <E : Any, C : Any, F : Any> afsmTypes(): ExperimentalMachineTypes<E, C, F> =
    ExperimentalMachineTypes()

// Candidate C: named zero-runtime-behavior type channels.
private class ExperimentalTypeChannel<T : Any>

private fun <T : Any> afsmEvents(): ExperimentalTypeChannel<T> = ExperimentalTypeChannel()

private fun <T : Any> afsmCommands(): ExperimentalTypeChannel<T> = ExperimentalTypeChannel()

private fun <T : Any> afsmEffects(): ExperimentalTypeChannel<T> = ExperimentalTypeChannel()

private fun noAfsmEffects(): ExperimentalTypeChannel<AfsmNoEffect> = ExperimentalTypeChannel()

@Suppress("UNUSED_PARAMETER")
private fun <P : Any, D : Any, E : Any, C : Any, F : Any> namedAfsmMachine(
    initialState: AfsmState<P, D>,
    events: ExperimentalTypeChannel<E>,
    commands: ExperimentalTypeChannel<C>,
    effects: ExperimentalTypeChannel<F>,
    build: AfsmMachineBuilder<P, D, E, C, F>.() -> Unit,
): AfsmDefaultMachine<AfsmState<P, D>, E, C, F> = experimentalMachine(
    initialState = initialState,
    build = build,
)

// Candidate D fallback: generic superclass arguments are not inferred in an
// object declaration. A composed feature value can infer all five channels.
private class ExperimentalFeature<P : Any, D : Any, E : Any, C : Any, F : Any>(
    private val defaultInitialState: AfsmState<P, D>,
    private val build: AfsmMachineBuilder<P, D, E, C, F>.() -> Unit,
) {
    val machine: AfsmDefaultMachine<AfsmState<P, D>, E, C, F> = machine(defaultInitialState)

    fun machine(
        initialState: AfsmState<P, D>,
    ): AfsmDefaultMachine<AfsmState<P, D>, E, C, F> = experimentalMachine(
        initialState = initialState,
        build = build,
    )
}

@Suppress("UNUSED_PARAMETER")
private fun <P : Any, D : Any, E : Any, C : Any, F : Any> experimentalFeature(
    defaultInitialState: AfsmState<P, D>,
    events: ExperimentalTypeChannel<E>,
    commands: ExperimentalTypeChannel<C>,
    effects: ExperimentalTypeChannel<F>,
    build: AfsmMachineBuilder<P, D, E, C, F>.() -> Unit,
): ExperimentalFeature<P, D, E, C, F> = ExperimentalFeature(
    defaultInitialState = defaultInitialState,
    build = build,
)

private fun <P : Any, D : Any, E : Any, C : Any, F : Any> experimentalMachine(
    initialState: AfsmState<P, D>,
    build: AfsmMachineBuilder<P, D, E, C, F>.() -> Unit,
): AfsmDefaultMachine<AfsmState<P, D>, E, C, F> = afsmMachine {
    initial(
        phase = initialState.phase,
        data = initialState.data,
    )
    build()
}

private typealias DraftState = AfsmState<DraftPhase, DraftData>

private val draftCandidateB = afsmTypes<DraftEvent, DraftCommand, AfsmNoEffect>()
    .machine(
        initialState = DraftState(
            phase = DraftPhase.Editing,
            data = DraftData(),
        ),
    ) {
        draftFlow()
    }

private val draftCandidateC = namedAfsmMachine(
    initialState = DraftState(
        phase = DraftPhase.Editing,
        data = DraftData(),
    ),
    events = afsmEvents<DraftEvent>(),
    commands = afsmCommands<DraftCommand>(),
    effects = noAfsmEffects(),
) {
    draftFlow()
}

private val DraftFeature = experimentalFeature(
    defaultInitialState = DraftState(
        phase = DraftPhase.Editing,
        data = DraftData(),
    ),
    events = afsmEvents<DraftEvent>(),
    commands = afsmCommands<DraftCommand>(),
    effects = noAfsmEffects(),
) {
    draftFlow()
}

private val draftCandidates: List<
    AfsmDefaultMachine<DraftState, DraftEvent, DraftCommand, AfsmNoEffect>,
    > = listOf(
    draftCandidateB,
    draftCandidateC,
    DraftFeature.machine,
)

private fun AfsmMachineBuilder<
    DraftPhase,
    DraftData,
    DraftEvent,
    DraftCommand,
    AfsmNoEffect,
    >.draftFlow() {
    phase(DraftPhase.Editing) {
        on<DraftEvent.TitleChanged> {
            updateData { data, event ->
                data.copy(
                    title = event.value,
                    error = null,
                )
            }
        }

        on<DraftEvent.SaveClicked> {
            case(
                label = "title present",
                condition = { data.title.isNotBlank() },
            ) {
                transitionTo(DraftPhase.Saving)
            }
            case(
                label = "title missing",
                condition = { data.title.isBlank() },
            ) {
                updateData { copy(error = "Title is required.") }
            }
        }
    }

    phase(DraftPhase.Saving) {
        onEnter {
            command(label = "Save") {
                DraftCommand.Save(title = data.title)
            }
        }
        on<DraftEvent.SaveSucceeded> {
            transitionTo(DraftPhase.Saved)
        }
        on<DraftEvent.SaveFailed> {
            updateData { data, event -> data.copy(error = event.message) }
            transitionTo(DraftPhase.Failed)
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

private val authCandidateB = afsmTypes<AuthEvent, AuthCommand, AuthEffect>()
    .machine(
        initialState = AuthState(
            phase = AuthPhase.Editing,
            data = AuthData(),
        ),
    ) {
        authFlow()
    }

private val authCandidateC = namedAfsmMachine(
    initialState = AuthState(
        phase = AuthPhase.Editing,
        data = AuthData(),
    ),
    events = afsmEvents<AuthEvent>(),
    commands = afsmCommands<AuthCommand>(),
    effects = afsmEffects<AuthEffect>(),
) {
    authFlow()
}

private val AuthFeature = experimentalFeature(
    defaultInitialState = AuthState(
        phase = AuthPhase.Editing,
        data = AuthData(),
    ),
    events = afsmEvents<AuthEvent>(),
    commands = afsmCommands<AuthCommand>(),
    effects = afsmEffects<AuthEffect>(),
) {
    authFlow()
}

private val authCandidates: List<
    AfsmDefaultMachine<AuthState, AuthEvent, AuthCommand, AuthEffect>,
    > = listOf(
    authCandidateB,
    authCandidateC,
    AuthFeature.machine,
)

private fun AfsmMachineBuilder<
    AuthPhase,
    AuthData,
    AuthEvent,
    AuthCommand,
    AuthEffect,
    >.authFlow() {
    phase(AuthPhase.Editing) {
        on<AuthEvent.EmailChanged> {
            updateData { data, event -> data.copy(email = event.value, error = null) }
        }
        on<AuthEvent.PasswordChanged> {
            updateData { data, event -> data.copy(password = event.value, error = null) }
        }
        on<AuthEvent.SubmitClicked> {
            case(
                label = "valid credentials",
                condition = { data.email.contains('@') && data.password.length >= 8 },
            ) {
                transitionTo(AuthPhase.Submitting)
            }
            case(
                label = "invalid credentials",
                condition = { !data.email.contains('@') || data.password.length < 8 },
            ) {
                updateData { copy(error = "Enter valid credentials.") }
            }
        }
        on<AuthEvent.LoginSucceeded> {
            invalid(reason = "Login result arrived before submission.")
        }
    }

    phase(AuthPhase.Submitting) {
        onEnter {
            command(label = "Login") {
                AuthCommand.Login(
                    email = data.email,
                    password = data.password,
                )
            }
        }
        on<AuthEvent.LoginSucceeded> {
            transitionTo<AuthPhase.Authenticated> {
                AuthPhase.Authenticated(userId = event.userId)
            }
        }
        on<AuthEvent.LoginFailed> {
            updateData { data, event -> data.copy(error = event.message) }
            transitionTo(AuthPhase.Editing)
        }
    }

    phase<AuthPhase.Authenticated> {
        onEnter {
            effect(label = "OpenHome") {
                AuthEffect.OpenHome(userId = phase.userId)
            }
        }
    }
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
    data class LoginFailed(val message: String) : AuthEvent
}

private sealed interface AuthCommand {
    data class Login(
        val email: String,
        val password: String,
    ) : AuthCommand
}

private sealed interface AuthEffect {
    data class OpenHome(val userId: String) : AuthEffect
}

private typealias CheckoutState = AfsmState<CheckoutPhase, CheckoutData>

private fun checkoutCandidateB(initialData: CheckoutData) =
    afsmTypes<CheckoutEvent, CheckoutCommand, CheckoutEffect>()
        .machine(
            initialState = CheckoutState(
                phase = CheckoutPhase.Idle,
                data = initialData,
            ),
        ) {
            checkoutFlow()
        }

private fun checkoutCandidateC(initialData: CheckoutData) = namedAfsmMachine(
    initialState = CheckoutState(
        phase = CheckoutPhase.Idle,
        data = initialData,
    ),
    events = afsmEvents<CheckoutEvent>(),
    commands = afsmCommands<CheckoutCommand>(),
    effects = afsmEffects<CheckoutEffect>(),
) {
    checkoutFlow()
}

private val CheckoutFeature = experimentalFeature(
    defaultInitialState = CheckoutState(
        phase = CheckoutPhase.Idle,
        data = CheckoutData(cartId = "default"),
    ),
    events = afsmEvents<CheckoutEvent>(),
    commands = afsmCommands<CheckoutCommand>(),
    effects = afsmEffects<CheckoutEffect>(),
) {
    checkoutFlow()
}

private fun checkoutCandidates(
    initialData: CheckoutData,
): List<AfsmDefaultMachine<CheckoutState, CheckoutEvent, CheckoutCommand, CheckoutEffect>> = listOf(
    checkoutCandidateB(initialData),
    checkoutCandidateC(initialData),
    CheckoutFeature.machine(
        initialState = CheckoutState(
            phase = CheckoutPhase.Idle,
            data = initialData,
        ),
    ),
)

private fun AfsmMachineBuilder<
    CheckoutPhase,
    CheckoutData,
    CheckoutEvent,
    CheckoutCommand,
    CheckoutEffect,
    >.checkoutFlow() {
    phase(CheckoutPhase.Idle) {
        on<CheckoutEvent.ScreenEntered> {
            transitionTo(CheckoutPhase.Loading)
        }
    }

    phase(CheckoutPhase.Loading) {
        onEnter {
            command(label = "LoadCart") {
                CheckoutCommand.LoadCart(cartId = data.cartId)
            }
        }
        on<CheckoutEvent.CartLoaded> {
            updateData { data, event -> data.copy(items = event.items, error = null) }
            transitionTo(CheckoutPhase.Ready)
        }
        on<CheckoutEvent.LoadFailed> {
            updateData { data, event -> data.copy(error = event.message) }
            transitionTo<CheckoutPhase.Failed> {
                CheckoutPhase.Failed(message = event.message)
            }
        }
    }

    phase(CheckoutPhase.Ready) {
        on<CheckoutEvent.PayClicked> {
            case(
                label = "cart has items",
                condition = { data.items.isNotEmpty() },
            ) {
                transitionTo<CheckoutPhase.PaymentInProgress> {
                    CheckoutPhase.PaymentInProgress(requestId = event.requestId)
                }
            }
            invalid(
                reason = "Cannot pay for an empty cart.",
                condition = { data.items.isEmpty() },
            )
        }
    }

    phase<CheckoutPhase.PaymentInProgress> {
        onEnter {
            command(label = "Pay") {
                CheckoutCommand.Pay(
                    requestId = phase.requestId,
                    items = data.items,
                )
            }
        }
        on<CheckoutEvent.PaymentSucceeded> {
            case(
                label = "matching request",
                condition = { event.requestId == phase.requestId },
            ) {
                transitionTo<CheckoutPhase.Completed> {
                    CheckoutPhase.Completed(orderId = event.orderId)
                }
            }
            ignore(
                reason = "Stale payment success.",
                condition = { event.requestId != phase.requestId },
            )
        }
        on<CheckoutEvent.PaymentFailed> {
            case(
                label = "matching request",
                condition = { event.requestId == phase.requestId },
            ) {
                updateData { data, event -> data.copy(error = event.message) }
                transitionTo<CheckoutPhase.Failed> {
                    CheckoutPhase.Failed(message = event.message)
                }
            }
            ignore(
                reason = "Stale payment failure.",
                condition = { event.requestId != phase.requestId },
            )
        }
    }

    phase<CheckoutPhase.Failed> {
        on<CheckoutEvent.RetryClicked> {
            transitionTo(CheckoutPhase.Loading)
        }
    }

    phase<CheckoutPhase.Completed> {
        onEnter {
            effect(label = "ShowReceipt") {
                CheckoutEffect.ShowReceipt(orderId = phase.orderId)
            }
        }
    }
}

private sealed interface CheckoutPhase {
    data object Idle : CheckoutPhase
    data object Loading : CheckoutPhase
    data object Ready : CheckoutPhase
    data class PaymentInProgress(val requestId: String) : CheckoutPhase
    data class Failed(val message: String) : CheckoutPhase
    data class Completed(val orderId: String) : CheckoutPhase
}

private data class CheckoutData(
    val cartId: String,
    val items: List<String> = emptyList(),
    val error: String? = null,
)

private sealed interface CheckoutEvent {
    data object ScreenEntered : CheckoutEvent
    data class CartLoaded(val items: List<String>) : CheckoutEvent
    data class LoadFailed(val message: String) : CheckoutEvent
    data class PayClicked(val requestId: String) : CheckoutEvent
    data class PaymentSucceeded(
        val requestId: String,
        val orderId: String,
    ) : CheckoutEvent

    data class PaymentFailed(
        val requestId: String,
        val message: String,
    ) : CheckoutEvent

    data object RetryClicked : CheckoutEvent
}

private sealed interface CheckoutCommand {
    data class LoadCart(val cartId: String) : CheckoutCommand
    data class Pay(
        val requestId: String,
        val items: List<String>,
    ) : CheckoutCommand
}

private sealed interface CheckoutEffect {
    data class ShowReceipt(val orderId: String) : CheckoutEffect
}
