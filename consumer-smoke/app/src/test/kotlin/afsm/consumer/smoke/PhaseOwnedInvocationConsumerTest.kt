package afsm.consumer.smoke

import afsm.core.AfsmCommandInvocation
import afsm.core.AfsmInvocationKey
import afsm.core.AfsmNoEffect
import afsm.core.AfsmState
import afsm.core.afsmMachine
import afsm.runtime.AfsmCommandHandler
import afsm.runtime.AfsmHost
import afsm.test.assertCommandInvocations
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PhaseOwnedInvocationConsumerTest {
    private val uploadKey = AfsmInvocationKey("consumer/image-upload")
    private val machine = afsmMachine<
        UploadPhase,
        Unit,
        UploadEvent,
        UploadCommand,
        AfsmNoEffect,
        > {
        initial(UploadPhase.Editing, Unit)

        phase(UploadPhase.Editing) {
            on<UploadEvent.StartClicked> {
                transitionTo(UploadPhase.Uploading)
            }
        }

        phase(UploadPhase.Uploading) {
            onEnter {
                invoke(uploadKey, label = "StartUpload") {
                    UploadCommand.Start
                }
            }

            on<UploadEvent.CancelClicked> {
                transitionTo(UploadPhase.Editing)
            }
        }
    }

    @Test
    fun publishedDslRuntimeAndTestHelperCancelPhaseOwnedWork() = runTest {
        val start = machine.transition(machine.initialState, UploadEvent.StartClicked)
        start.assertCommandInvocations(
            AfsmCommandInvocation.Start(uploadKey, UploadCommand.Start),
        )

        val started = CompletableDeferred<Unit>()
        val cancelled = CompletableDeferred<Unit>()
        val hostScope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val host = AfsmHost(
            initialState = machine.initialState,
            reducer = machine,
            commandHandler = AfsmCommandHandler { _: UploadCommand, _ ->
                started.complete(Unit)
                try {
                    awaitCancellation()
                } finally {
                    cancelled.complete(Unit)
                }
            },
            scope = hostScope,
        )

        host.dispatch(UploadEvent.StartClicked)
        runCurrent()
        assertTrue(started.isCompleted)

        host.dispatch(UploadEvent.CancelClicked)
        runCurrent()

        assertTrue(cancelled.isCompleted)
        assertEquals(UploadPhase.Editing, host.state.value.phase)
        hostScope.cancel()
    }

    private sealed interface UploadPhase {
        data object Editing : UploadPhase

        data object Uploading : UploadPhase
    }

    private sealed interface UploadEvent {
        data object StartClicked : UploadEvent

        data object CancelClicked : UploadEvent
    }

    private sealed interface UploadCommand {
        data object Start : UploadCommand
    }
}
