package afsm.runtime

import afsm.core.Afsm
import afsm.core.AfsmCommandInvocation
import afsm.core.AfsmInvocationKey
import afsm.core.AfsmNoEffect
import afsm.core.AfsmReducer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AfsmPhaseOwnedInvocationTest {
    private val uploadKey = AfsmInvocationKey("draft/image-upload")

    @Test
    fun `phase exit cancels active invocation while ordinary commands stay sequential`() = runTest {
        val uploadStarted = CompletableDeferred<Unit>()
        val uploadCancelled = CompletableDeferred<Unit>()
        val lateResultAttempted = CompletableDeferred<Unit>()
        val ordinaryCommands = mutableListOf<InvocationCommand>()
        val diagnostics = mutableListOf<AfsmDiagnostic>()
        val hostScope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val host = AfsmHost(
            initialState = InvocationState.Editing,
            reducer = invocationReducer(),
            commandHandler = AfsmCommandHandler { command: InvocationCommand, dispatchEvent ->
                when (command) {
                    InvocationCommand.Upload -> {
                        uploadStarted.complete(Unit)
                        try {
                            awaitCancellation()
                        } finally {
                            uploadCancelled.complete(Unit)
                            withContext(NonCancellable) {
                                lateResultAttempted.complete(Unit)
                                dispatchEvent(InvocationEvent.UploadCompleted)
                            }
                        }
                    }

                    InvocationCommand.RecordFirst,
                    InvocationCommand.RecordSecond,
                    -> ordinaryCommands += command
                }
            },
            scope = hostScope,
            config = AfsmConfig(
                logger = AfsmLogger { diagnostic -> diagnostics += diagnostic },
            ),
        )

        host.dispatch(InvocationEvent.StartClicked)
        runCurrent()

        assertTrue(uploadStarted.isCompleted)
        assertEquals(
            listOf(InvocationCommand.RecordFirst, InvocationCommand.RecordSecond),
            ordinaryCommands,
        )
        assertEquals(InvocationState.Uploading, host.state.value)

        host.dispatch(InvocationEvent.EditWhileUploading)
        runCurrent()
        assertEquals(InvocationState.UploadingEdited, host.state.value)

        host.dispatch(InvocationEvent.CancelClicked)
        runCurrent()

        assertTrue(uploadCancelled.isCompleted)
        assertTrue(lateResultAttempted.isCompleted)
        assertEquals(InvocationState.Editing, host.state.value)
        assertEquals(emptyList(), diagnostics)
        hostScope.cancel()
    }

    @Test
    fun `closing host cancels active invocation`() = runTest {
        val uploadStarted = CompletableDeferred<Unit>()
        val uploadCancelled = CompletableDeferred<Unit>()
        val hostScope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val host = AfsmHost(
            initialState = InvocationState.Editing,
            reducer = invocationReducer(),
            commandHandler = AfsmCommandHandler { command: InvocationCommand, _ ->
                if (command == InvocationCommand.Upload) {
                    uploadStarted.complete(Unit)
                    try {
                        awaitCancellation()
                    } finally {
                        uploadCancelled.complete(Unit)
                    }
                }
            },
            scope = hostScope,
        )

        host.dispatch(InvocationEvent.StartClicked)
        runCurrent()
        assertTrue(uploadStarted.isCompleted)

        host.close()
        advanceUntilIdle()

        assertTrue(uploadCancelled.isCompleted)
        hostScope.cancel()
    }

    private fun invocationReducer(): AfsmReducer<
        InvocationState,
        InvocationEvent,
        InvocationCommand,
        AfsmNoEffect,
        > {
        return AfsmReducer { state, event ->
            when (event) {
                InvocationEvent.StartClicked -> Afsm.transitioned(
                    state = InvocationState.Uploading,
                    commands = listOf(
                        InvocationCommand.RecordFirst,
                        InvocationCommand.RecordSecond,
                    ),
                    commandInvocations = listOf(
                        AfsmCommandInvocation.Start(
                            key = uploadKey,
                            command = InvocationCommand.Upload,
                        ),
                    ),
                )

                InvocationEvent.EditWhileUploading -> Afsm.transitioned(
                    state = InvocationState.UploadingEdited,
                )

                InvocationEvent.CancelClicked -> Afsm.transitioned(
                    state = InvocationState.Editing,
                    commandInvocations = listOf(
                        AfsmCommandInvocation.Cancel(uploadKey),
                    ),
                )

                InvocationEvent.UploadCompleted -> Afsm.transitioned(
                    state = InvocationState.Completed,
                )
            }
        }
    }

    private enum class InvocationState {
        Editing,
        Uploading,
        UploadingEdited,
        Completed,
    }

    private enum class InvocationEvent {
        StartClicked,
        EditWhileUploading,
        CancelClicked,
        UploadCompleted,
    }

    private enum class InvocationCommand {
        Upload,
        RecordFirst,
        RecordSecond,
    }
}
