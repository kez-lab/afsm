package afsm.consumer.smoke

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DraftViewModelTest {
    @Test
    fun saveClickedCallsRepositoryAndPublishesSavedState() = runTest {
        val mainDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(mainDispatcher)
        try {
            val repository = RecordingDraftRepository(Result.success(Unit))
            val viewModel = DraftViewModel(repository)

            viewModel.onEvent(DraftEvent.TitleChanged("Plan"))
            viewModel.onEvent(DraftEvent.SaveClicked)
            mainDispatcher.scheduler.advanceUntilIdle()

            assertEquals(listOf("Plan"), repository.savedTitles)
            assertEquals(DraftPhase.Saved, viewModel.state.value.phase)
            assertEquals(DraftData(title = "Plan"), viewModel.state.value.data)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun saveFailurePublishesEditingStateWithError() = runTest {
        val mainDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(mainDispatcher)
        try {
            val repository = RecordingDraftRepository(
                Result.failure(IllegalStateException("Network unavailable")),
            )
            val viewModel = DraftViewModel(repository)

            viewModel.onEvent(DraftEvent.TitleChanged("Plan"))
            viewModel.onEvent(DraftEvent.SaveClicked)
            mainDispatcher.scheduler.advanceUntilIdle()

            assertEquals(listOf("Plan"), repository.savedTitles)
            assertEquals(DraftPhase.Editing, viewModel.state.value.phase)
            assertEquals(
                DraftData(
                    title = "Plan",
                    errorMessage = "Network unavailable",
                ),
                viewModel.state.value.data,
            )
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun savedStateHandleTitleSeedsInitialDraftStateWithoutStartingWork() = runTest {
        val mainDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(mainDispatcher)
        try {
            val repository = RecordingDraftRepository(Result.success(Unit))
            val savedStateHandle = SavedStateHandle(
                mapOf(DraftTitleKey to "Restored plan"),
            )
            val viewModel = DraftViewModel(
                repository = repository,
                initialState = draftStateFromSavedState(savedStateHandle),
            )

            mainDispatcher.scheduler.advanceUntilIdle()

            assertEquals(DraftPhase.Editing, viewModel.state.value.phase)
            assertEquals(
                DraftData(title = "Restored plan"),
                viewModel.state.value.data,
            )
            assertEquals(emptyList<String>(), repository.savedTitles)
        } finally {
            Dispatchers.resetMain()
        }
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
