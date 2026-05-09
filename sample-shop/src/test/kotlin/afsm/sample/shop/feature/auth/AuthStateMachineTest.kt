package afsm.sample.shop.feature.auth

import afsm.core.AfsmDecision
import afsm.sample.shop.core.model.UserSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AuthStateMachineTest {
    private val machine = AuthStateMachine()

    @Test
    fun `register submit trims inputs enters loading and emits register command`() {
        val state = AuthState.Editing(
            mode = AuthMode.Register,
            form = AuthForm(
                name = "  Mina  ",
                email = "  mina@example.com  ",
                password = "secret1",
            ),
        )

        val result = machine.transition(state, AuthEvent.SubmitClicked)

        assertEquals(AfsmDecision.Transitioned, result.decision)
        assertEquals(
            AuthState.Submitting(
                mode = AuthMode.Register,
                form = AuthForm(
                    name = "Mina",
                    email = "mina@example.com",
                    password = "secret1",
                ),
            ),
            result.state,
        )
        assertEquals(
            listOf(
                AuthCommand.Register(
                    name = "Mina",
                    email = "mina@example.com",
                    password = "secret1",
                ),
            ),
            result.commands,
        )
    }

    @Test
    fun `submit with invalid password stays and does not emit command`() {
        val state = AuthState.Editing(
            mode = AuthMode.Login,
            form = AuthForm(
                email = "mina@example.com",
                password = "123",
            ),
        )

        val result = machine.transition(state, AuthEvent.SubmitClicked)

        assertIs<AfsmDecision.Stayed>(result.decision)
        assertEquals(
            "Password must be at least 6 characters.",
            (result.state as AuthState.Editing).errorMessage,
        )
        assertEquals(emptyList(), result.commands)
    }

    @Test
    fun `auth success moves from submitting to authenticated and emits catalog effect`() {
        val session = UserSession(
            userId = 1,
            name = "Mina",
            email = "mina@example.com",
        )
        val state = AuthState.Submitting(
            mode = AuthMode.Login,
            form = AuthForm(
                email = "mina@example.com",
                password = "secret1",
            ),
        )

        val result = machine.transition(
            state = state,
            event = AuthEvent.AuthSucceeded(session),
        )

        assertEquals(AuthState.Authenticated(session), result.state)
        assertEquals(listOf(AuthEffect.OpenCatalog), result.effects)
    }

    @Test
    fun `auth command result without loading is invalid`() {
        val state = AuthState.Editing()

        val result = machine.transition(
            state = state,
            event = AuthEvent.AuthFailed("late failure"),
        )

        assertIs<AfsmDecision.Invalid>(result.decision)
        assertEquals(state, result.state)
    }

    @Test
    fun `form changes are self transitions inside editing phase`() {
        val state = AuthState.Editing(
            mode = AuthMode.Login,
            form = AuthForm(email = "old@example.com"),
        )

        val result = machine.transition(
            state = state,
            event = AuthEvent.EmailChanged("new@example.com"),
        )

        assertEquals(
            AuthState.Editing(
                mode = AuthMode.Login,
                form = AuthForm(email = "new@example.com"),
            ),
            result.state,
        )
    }
}
