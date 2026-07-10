package afsm.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AfsmPhaseOwnedInvocationTest {
    private val uploadKey = AfsmInvocationKey("draft/image-upload")

    @Test
    fun `entering an invoked phase starts work and leaving cancels it`() {
        val machine = uploadMachine()

        val entered = machine.transition(
            state = machine.initialState,
            event = UploadEvent.StartClicked,
        )

        assertEquals(UploadPhase.Uploading, entered.state.phase)
        assertEquals(emptyList(), entered.commands)
        assertEquals(
            listOf(
                AfsmCommandInvocation.Start(
                    key = uploadKey,
                    command = UploadCommand.StartUpload,
                ),
            ),
            entered.commandInvocations,
        )

        val cancelled = machine.transition(
            state = entered.state,
            event = UploadEvent.CancelClicked,
        )

        assertEquals(UploadPhase.Editing, cancelled.state.phase)
        assertEquals(emptyList(), cancelled.commands)
        assertEquals(
            listOf(AfsmCommandInvocation.Cancel(uploadKey)),
            cancelled.commandInvocations,
        )
    }

    @Test
    fun `topology names invocation start and automatic phase exit cancellation`() {
        val uploading = uploadMachine().topology.states.single { state ->
            state.id == "Uploading"
        }

        assertEquals(listOf("invoke StartUpload"), uploading.entryCommandLabels)
        assertEquals(listOf("cancel draft/image-upload"), uploading.exitCommandLabels)
    }

    @Test
    fun `duplicate invocation key in one phase is rejected`() {
        val failure = assertFailsWith<AfsmDefinitionException> {
            afsmMachine<
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
                        invoke(uploadKey, label = "First") {
                            UploadCommand.StartUpload
                        }
                        invoke(uploadKey, label = "Second") {
                            UploadCommand.StartUpload
                        }
                    }
                }
            }
        }

        assertTrue("Duplicate invocation key" in failure.message.orEmpty())
    }

    @Test
    fun `adjacent phases may reuse a key because cancel precedes next start`() {
        val machine = afsmMachine<
            UploadPhase,
            Unit,
            UploadEvent,
            UploadCommand,
            AfsmNoEffect,
            > {
            initial(UploadPhase.Editing, Unit)

            phase(UploadPhase.Editing)

            phase(UploadPhase.Uploading) {
                onEnter {
                    invoke(uploadKey, label = "InitialUpload") {
                        UploadCommand.StartUpload
                    }
                }
                on<UploadEvent.RetryClicked> {
                    transitionTo(UploadPhase.Retrying)
                }
            }

            phase(UploadPhase.Retrying) {
                onEnter {
                    invoke(uploadKey, label = "RetryUpload") {
                        UploadCommand.StartUpload
                    }
                }
            }
        }

        val result = machine.transition(
            state = AfsmState(UploadPhase.Uploading, Unit),
            event = UploadEvent.RetryClicked,
        )

        assertEquals(
            listOf(
                AfsmCommandInvocation.Cancel(uploadKey),
                AfsmCommandInvocation.Start(uploadKey, UploadCommand.StartUpload),
            ),
            result.commandInvocations,
        )
    }

    @Test
    fun `blank invocation key is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            AfsmInvocationKey("   ")
        }
    }

    private fun uploadMachine(): AfsmDefaultMachine<
        AfsmState<UploadPhase, Unit>,
        UploadEvent,
        UploadCommand,
        AfsmNoEffect,
        > {
        return afsmMachine {
            initial(UploadPhase.Editing, Unit)

            phase(UploadPhase.Editing) {
                on<UploadEvent.StartClicked> {
                    transitionTo(UploadPhase.Uploading)
                }
            }

            phase(UploadPhase.Uploading) {
                onEnter {
                    invoke(uploadKey, label = "StartUpload") {
                        UploadCommand.StartUpload
                    }
                }

                on<UploadEvent.CancelClicked> {
                    transitionTo(UploadPhase.Editing)
                }
            }
        }
    }

    private sealed interface UploadPhase {
        data object Editing : UploadPhase

        data object Uploading : UploadPhase

        data object Retrying : UploadPhase
    }

    private sealed interface UploadEvent {
        data object StartClicked : UploadEvent

        data object CancelClicked : UploadEvent

        data object RetryClicked : UploadEvent
    }

    private enum class UploadCommand {
        StartUpload,
    }
}
