package afsm.runtime

import afsm.core.Afsm
import afsm.core.AfsmNoEffect
import afsm.core.AfsmReducer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame

@OptIn(ExperimentalCoroutinesApi::class)
class AfsmDiagnosticPrivacyTest {
    @Test
    fun `default invalid diagnostic exposes types but no credential values`() = runTest {
        val diagnostics = mutableListOf<AfsmDiagnostic>()
        val hostScope = newHostScope()
        val state = SensitiveState(password = "secret-password")
        val event = SensitiveEvent.Submit(email = "ada@example.com")
        val host: AfsmHost<
            SensitiveState,
            SensitiveEvent,
            SensitiveCommand,
            AfsmNoEffect,
            > = AfsmHost(
            initialState = state,
            reducer = AfsmReducer { currentState: SensitiveState, _: SensitiveEvent ->
                Afsm.invalid(
                    state = currentState,
                    reason = "password=secret-password email=ada@example.com",
                )
            },
            commandHandler = AfsmCommandHandler.none(),
            scope = hostScope,
            config = AfsmConfig(
                invalidTransitionPolicy = AfsmInvalidTransitionPolicy.Record,
                logger = AfsmLogger { diagnostic -> diagnostics += diagnostic },
            ),
        )

        host.dispatch(event)
        advanceUntilIdle()

        val diagnostic = diagnostics.single()
        assertEquals(AfsmDiagnosticCode.InvalidTransition, diagnostic.code)
        assertEquals(AfsmDiagnosticDecision.Invalid, diagnostic.decision)
        assertEquals("Invalid Afsm transition.", diagnostic.message)
        assertEquals("SensitiveState", diagnostic.stateType)
        assertEquals("Submit", diagnostic.eventType)
        assertNull(diagnostic.commandType)
        assertNull(diagnostic.failureType)
        assertEquals(emptyMap(), diagnostic.metadata)
        assertNull(diagnostic.values)
        assertContainsNoSecrets(diagnostic)
        hostScope.cancel()
    }

    @Test
    fun `default command failure exposes exception type but no command or throwable values`() = runTest {
        val diagnostics = mutableListOf<AfsmDiagnostic>()
        val hostScope = newHostScope()
        val host: AfsmHost<
            SensitiveState,
            SensitiveEvent,
            SensitiveCommand,
            AfsmNoEffect,
            > = AfsmHost(
            initialState = SensitiveState(password = "secret-password"),
            reducer = AfsmReducer { currentState: SensitiveState, _: SensitiveEvent ->
                Afsm.transitioned(
                    state = currentState,
                    commands = listOf(SensitiveCommand.Login(password = "secret-password")),
                )
            },
            commandHandler = AfsmCommandHandler { _: SensitiveCommand, _ ->
                error("token=private-token")
            },
            scope = hostScope,
            config = AfsmConfig(
                commandFailurePolicy = AfsmCommandFailurePolicy.Record,
                logger = AfsmLogger { diagnostic -> diagnostics += diagnostic },
            ),
        )

        host.dispatch(SensitiveEvent.Submit(email = "ada@example.com"))
        advanceUntilIdle()

        val diagnostic = diagnostics.single()
        assertEquals(AfsmDiagnosticCode.CommandFailure, diagnostic.code)
        assertEquals(AfsmDiagnosticDecision.Transitioned, diagnostic.decision)
        assertEquals("Afsm command failed.", diagnostic.message)
        assertEquals("SensitiveState", diagnostic.stateType)
        assertEquals("Submit", diagnostic.eventType)
        assertEquals("Login", diagnostic.commandType)
        assertEquals("IllegalStateException", diagnostic.failureType)
        assertNull(diagnostic.values)
        assertContainsNoSecrets(diagnostic)
        hostScope.cancel()
    }

    @Test
    fun `raw values require explicit IncludeValues policy and diagnostic string stays safe`() = runTest {
        val diagnostics = mutableListOf<AfsmDiagnostic>()
        val hostScope = newHostScope()
        val state = SensitiveState(password = "secret-password")
        val event = SensitiveEvent.Submit(email = "ada@example.com")
        val command = SensitiveCommand.Login(password = "secret-password")
        val failure = IllegalStateException("token=private-token")
        val host: AfsmHost<
            SensitiveState,
            SensitiveEvent,
            SensitiveCommand,
            AfsmNoEffect,
            > = AfsmHost(
            initialState = state,
            reducer = AfsmReducer { currentState: SensitiveState, _: SensitiveEvent ->
                Afsm.transitioned(
                    state = currentState,
                    commands = listOf(command),
                )
            },
            commandHandler = AfsmCommandHandler { _: SensitiveCommand, _ -> throw failure },
            scope = hostScope,
            config = AfsmConfig(
                commandFailurePolicy = AfsmCommandFailurePolicy.Record,
                diagnosticDataPolicy = AfsmDiagnosticDataPolicy.IncludeValues,
                logger = AfsmLogger { diagnostic -> diagnostics += diagnostic },
            ),
        )

        host.dispatch(event)
        advanceUntilIdle()

        val diagnostic = diagnostics.single()
        val values = requireNotNull(diagnostic.values)
        assertSame(state, values.state)
        assertSame(event, values.event)
        assertSame(command, values.command)
        assertEquals("token=private-token", values.reason)
        assertSame(failure, values.throwable)
        assertContainsNoSecrets(diagnostic)
        hostScope.cancel()
    }

    private fun assertContainsNoSecrets(diagnostic: AfsmDiagnostic) {
        val safeText = buildString {
            append(diagnostic.toString())
            append(diagnostic.message)
            append(diagnostic.stateType)
            append(diagnostic.eventType)
            append(diagnostic.commandType)
            append(diagnostic.failureType)
            append(diagnostic.metadata)
        }
        assertFalse("secret-password" in safeText)
        assertFalse("ada@example.com" in safeText)
        assertFalse("private-token" in safeText)
    }

    private fun TestScope.newHostScope(): CoroutineScope {
        return CoroutineScope(StandardTestDispatcher(testScheduler))
    }

    private data class SensitiveState(
        val password: String,
    )

    private sealed interface SensitiveEvent {
        data class Submit(
            val email: String,
        ) : SensitiveEvent
    }

    private sealed interface SensitiveCommand {
        data class Login(
            val password: String,
        ) : SensitiveCommand
    }
}
