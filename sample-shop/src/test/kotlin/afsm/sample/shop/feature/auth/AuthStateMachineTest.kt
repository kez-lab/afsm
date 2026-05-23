package afsm.sample.shop.feature.auth

import afsm.core.AfsmDecision
import afsm.sample.shop.core.model.UserSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AuthStateMachineTest {
    private val machine = AuthStateMachine

    @Test
    fun `register submit trims inputs enters loading and emits register command`() {
        val state = authState(
            data = AuthData(
                mode = AuthMode.Register,
                form = AuthForm(
                    name = "  Mina  ",
                    email = "  mina@example.com  ",
                    password = "secret1",
                ),
            ),
        )

        val result = machine.transition(state, AuthEvent.SubmitClicked)

        assertEquals(AfsmDecision.Transitioned, result.decision)
        assertEquals(
            authState(
                phase = AuthPhase.Submitting,
                data = AuthData(
                    mode = AuthMode.Register,
                    form = AuthForm(
                        name = "Mina",
                        email = "mina@example.com",
                        password = "secret1",
                    ),
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
    fun `submit with invalid password handles without phase change and does not emit command`() {
        val state = authState(
            data = AuthData(
                mode = AuthMode.Login,
                form = AuthForm(
                    email = "mina@example.com",
                    password = "123",
                ),
            ),
        )

        val result = machine.transition(state, AuthEvent.SubmitClicked)

        assertIs<AfsmDecision.Handled>(result.decision)
        assertEquals(
            "Password must be at least 6 characters.",
            result.state.data.errorMessage,
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
        val state = authState(
            phase = AuthPhase.Submitting,
            data = AuthData(
                mode = AuthMode.Login,
                form = AuthForm(
                    email = "mina@example.com",
                    password = "secret1",
                ),
            ),
        )

        val result = machine.transition(
            state = state,
            event = AuthEvent.AuthSucceeded(session),
        )

        assertEquals(
            authState(
                phase = AuthPhase.Authenticated(session),
                data = AuthData(),
            ),
            result.state,
        )
        assertEquals(listOf(AuthEffect.OpenCatalog), result.effects)
    }

    @Test
    fun `auth command result without loading is invalid`() {
        val state = authState()

        val result = machine.transition(
            state = state,
            event = AuthEvent.AuthFailed("late failure"),
        )

        assertIs<AfsmDecision.Invalid>(result.decision)
        assertEquals(state, result.state)
    }

    @Test
    fun `form changes update data inside editing phase`() {
        val state = authState(
            data = AuthData(
                mode = AuthMode.Login,
                form = AuthForm(email = "old@example.com"),
            ),
        )

        val result = machine.transition(
            state = state,
            event = AuthEvent.EmailChanged("new@example.com"),
        )

        assertEquals(
            authState(
                data = AuthData(
                    mode = AuthMode.Login,
                    form = AuthForm(email = "new@example.com"),
                ),
            ),
            result.state,
        )
    }
}
