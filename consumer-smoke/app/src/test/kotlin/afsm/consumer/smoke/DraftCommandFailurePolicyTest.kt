package afsm.consumer.smoke

import afsm.runtime.AfsmCommandFailurePolicy
import afsm.runtime.AfsmCommandHandler
import afsm.runtime.AfsmConfig
import afsm.runtime.AfsmDiagnostic
import afsm.runtime.AfsmDiagnosticCode
import afsm.runtime.AfsmHost
import afsm.runtime.AfsmLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DraftCommandFailurePolicyTest {
    @Test
    fun unexpectedCommandHandlerExceptionIsRecordedWithoutCreatingDomainFailureEvent() = runTest {
        val hostScope = newHostScope()
        val diagnostics = mutableListOf<AfsmDiagnostic>()
        val host: AfsmHost<DraftState, DraftEvent, DraftCommand> = AfsmHost(
            initialState = DraftState(
                phase = DraftPhase.Editing,
                data = DraftData(title = "Plan"),
            ),
            reducer = draftStateMachine,
            commandHandler = AfsmCommandHandler { _: DraftCommand, _ ->
                error("writer misconfigured")
            },
            scope = hostScope,
            config = AfsmConfig(
                commandFailurePolicy = AfsmCommandFailurePolicy.Record,
                logger = AfsmLogger { diagnostic ->
                    diagnostics += diagnostic
                },
            ),
        )

        host.dispatch(DraftEvent.SaveClicked)
        advanceUntilIdle()

        assertEquals(DraftPhase.Saving, host.state.value.phase)
        assertEquals(DraftData(title = "Plan"), host.state.value.data)
        assertEquals(AfsmDiagnosticCode.CommandFailure, diagnostics.single().code)
        assertEquals("SaveDraft", diagnostics.single().commandType)
        assertEquals("IllegalStateException", diagnostics.single().failureType)
        assertNull(diagnostics.single().values)
        assertFalse(diagnostics.single().toString().contains("Plan"))
        assertFalse(diagnostics.single().message.contains("writer misconfigured"))
        hostScope.cancel()
    }

    private fun TestScope.newHostScope(): CoroutineScope {
        return CoroutineScope(StandardTestDispatcher(testScheduler))
    }
}
