package afsm.consumer.smoke

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class DraftViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun saveClickedCallsRepositoryAndPublishesSavedState() = runTest {
        val repository = RecordingDraftRepository(Result.success(Unit))
        val viewModel = DraftViewModel(repository)

        viewModel.updateTitle("Plan")
        viewModel.save()
        mainDispatcherRule.advanceUntilIdle()

        assertEquals(listOf("Plan"), repository.savedTitles)
        assertEquals(DraftPhase.Saved, viewModel.state.value.phase)
        assertEquals(DraftData(title = "Plan"), viewModel.state.value.data)
    }

    @Test
    fun saveFailurePublishesEditingStateWithError() = runTest {
        val repository = RecordingDraftRepository(
            Result.failure(IllegalStateException("Network unavailable")),
        )
        val viewModel = DraftViewModel(repository)

        viewModel.updateTitle("Plan")
        viewModel.save()
        mainDispatcherRule.advanceUntilIdle()

        assertEquals(listOf("Plan"), repository.savedTitles)
        assertEquals(DraftPhase.Editing, viewModel.state.value.phase)
        assertEquals(
            DraftData(
                title = "Plan",
                errorMessage = "Network unavailable",
            ),
            viewModel.state.value.data,
        )
    }

    @Test
    fun savedStateHandleTitleSeedsInitialDraftStateWithoutStartingWork() = runTest {
        val repository = RecordingDraftRepository(Result.success(Unit))
        val savedStateHandle = SavedStateHandle(
            mapOf(DraftTitleKey to "Restored plan"),
        )
        val viewModel = DraftViewModel(
            repository = repository,
            initialState = draftStateFromSavedState(savedStateHandle),
        )

        mainDispatcherRule.advanceUntilIdle()

        assertEquals(DraftPhase.Editing, viewModel.state.value.phase)
        assertEquals(
            DraftData(title = "Restored plan"),
            viewModel.state.value.data,
        )
        assertEquals(emptyList<String>(), repository.savedTitles)
    }

    private class RecordingDraftRepository(
        private val result: Result<Unit>,
    ) : DraftRepository {
        val savedTitles = mutableListOf<String>()

        override suspend fun save(title: String): Result<Unit> {
            savedTitles += title
            return result
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }

    fun advanceUntilIdle() {
        dispatcher.scheduler.advanceUntilIdle()
    }
}
