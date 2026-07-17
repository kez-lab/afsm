package afsm.sample.shop.feature.auth

import afsm.sample.shop.core.model.UserSession
import afsm.test.assertCommands
import afsm.test.assertData
import afsm.test.assertHandled
import afsm.test.assertInvalid
import afsm.test.assertNoCommands
import afsm.test.assertPhase
import afsm.test.assertState
import afsm.test.assertTransitioned
import kotlin.test.Test

class AuthStateMachineTest {
    private val machine = authStateMachine

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

        result
            .assertTransitioned()
            .assertState(
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
            )
            .assertCommands(
                AuthCommand.Register(
                    name = "Mina",
                    email = "mina@example.com",
                    password = "secret1",
                ),
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

        result
            .assertHandled()
            .assertPhase(AuthPhase.Editing)
            .assertData(
                AuthData(
                    mode = AuthMode.Login,
                    form = AuthForm(
                        email = "mina@example.com",
                        password = "123",
                    ),
                    errorMessage = "Password must be at least 6 characters.",
                ),
            )
            .assertNoCommands()
    }

    @Test
    fun `auth success moves from submitting to durable authenticated state`() {
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

        result
            .assertTransitioned()
            .assertState(
                authState(
                    phase = AuthPhase.Authenticated(session),
                    data = AuthData(),
                ),
            )
    }

    @Test
    fun `auth command result without loading is invalid`() {
        val state = authState()

        val result = machine.transition(
            state = state,
            event = AuthEvent.AuthFailed("late failure"),
        )

        result
            .assertInvalid()
            .assertState(state)
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

        result
            .assertHandled()
            .assertState(
                authState(
                    data = AuthData(
                        mode = AuthMode.Login,
                        form = AuthForm(email = "new@example.com"),
                    ),
                ),
            )
    }
}
