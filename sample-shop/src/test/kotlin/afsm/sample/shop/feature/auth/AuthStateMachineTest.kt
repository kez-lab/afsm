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
        val state = AuthState(
            mode = AuthMode.Register,
            name = "  Mina  ",
            email = "  mina@example.com  ",
            password = "secret1",
        )

        val result = machine.transition(state, AuthEvent.SubmitClicked)

        assertEquals(AfsmDecision.Transitioned, result.decision)
        assertEquals(
            state.copy(
                name = "Mina",
                email = "mina@example.com",
                isLoading = true,
                errorMessage = null,
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
        val state = AuthState(
            mode = AuthMode.Login,
            email = "mina@example.com",
            password = "123",
        )

        val result = machine.transition(state, AuthEvent.SubmitClicked)

        assertIs<AfsmDecision.Stayed>(result.decision)
        assertEquals("Password must be at least 6 characters.", result.state.errorMessage)
        assertEquals(emptyList(), result.commands)
    }

    @Test
    fun `auth success stops loading and emits catalog effect`() {
        val state = AuthState(
            email = "mina@example.com",
            password = "secret1",
            isLoading = true,
        )

        val result = machine.transition(
            state = state,
            event = AuthEvent.AuthSucceeded(
                UserSession(
                    userId = 1,
                    name = "Mina",
                    email = "mina@example.com",
                ),
            ),
        )

        assertEquals(false, result.state.isLoading)
        assertEquals(listOf(AuthEffect.OpenCatalog), result.effects)
    }

    @Test
    fun `auth command result without loading is invalid`() {
        val result = machine.transition(
            state = AuthState(),
            event = AuthEvent.AuthFailed("late failure"),
        )

        assertIs<AfsmDecision.Invalid>(result.decision)
        assertEquals(AuthState(), result.state)
    }
}
