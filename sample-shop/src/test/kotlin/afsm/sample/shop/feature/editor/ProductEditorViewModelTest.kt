package afsm.sample.shop.feature.editor

import afsm.sample.shop.core.data.ProductRepository
import afsm.sample.shop.core.data.SessionRepository
import afsm.sample.shop.core.database.ProductDao
import afsm.sample.shop.core.database.ProductEntity
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.coroutines.cancellation.CancellationException

@OptIn(ExperimentalCoroutinesApi::class)
class ProductEditorViewModelTest {
    private val mainDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `cancel upload cancels injected uploader and cannot complete later`() = runTest {
        val uploader = ControllableProductImageUploader()
        val viewModel = productEditorViewModel(uploader)

        enterValidUpload(viewModel)
        mainDispatcher.scheduler.runCurrent()

        assertTrue(uploader.started.isCompleted)
        assertEquals(
            ProductEditorPhase.ImageUploadInProgress,
            viewModel.state.value.phase,
        )
        assertEquals(
            ProductEditorSecondaryAction.CancelUpload,
            viewModel.state.value.toRenderState().secondaryAction,
        )

        viewModel.cancelUpload()
        mainDispatcher.scheduler.runCurrent()

        assertTrue(uploader.cancelled.isCompleted)
        assertEquals(ProductEditorPhase.EditingDraft, viewModel.state.value.phase)
    }

    @Test
    fun `uploader failure becomes safe typed failure event`() = runTest {
        val viewModel = productEditorViewModel(
            ProductImageUploader {
                error("private backend detail")
            },
        )

        enterValidUpload(viewModel)
        mainDispatcher.scheduler.runCurrent()

        assertEquals(ProductEditorPhase.EditingDraft, viewModel.state.value.phase)
        assertEquals(
            "Image upload failed.",
            viewModel.state.value.data.errorMessage,
        )
    }

    @Test
    fun `uploader cancellation is not converted to domain failure`() = runTest {
        val viewModel = productEditorViewModel(
            ProductImageUploader {
                throw CancellationException("upload cancelled")
            },
        )

        enterValidUpload(viewModel)
        mainDispatcher.scheduler.runCurrent()

        assertEquals(
            ProductEditorPhase.ImageUploadInProgress,
            viewModel.state.value.phase,
        )
        assertEquals(null, viewModel.state.value.data.errorMessage)
    }

    private fun productEditorViewModel(
        imageUploader: ProductImageUploader,
    ): ProductEditorViewModel {
        return ProductEditorViewModel(
            productRepository = ProductRepository(NoOpProductDao),
            sessionRepository = SessionRepository(),
            imageUploader = imageUploader,
        )
    }

    private fun enterValidUpload(viewModel: ProductEditorViewModel) {
        viewModel.updateTitle("Travel Mug")
        viewModel.updateDescription("Leakproof mug for commuting.")
        viewModel.updatePrice("24.50")
        viewModel.submitForReview()
    }

    private class ControllableProductImageUploader : ProductImageUploader {
        val started = CompletableDeferred<Unit>()
        val cancelled = CompletableDeferred<Unit>()

        override suspend fun upload(draft: ProductDraft): String {
            started.complete(Unit)
            try {
                awaitCancellation()
            } finally {
                cancelled.complete(Unit)
            }
        }
    }

    private data object NoOpProductDao : ProductDao {
        override suspend fun count(): Int = 0

        override fun observeProducts(): Flow<List<ProductEntity>> = flowOf(emptyList())

        override fun observeProduct(id: Long): Flow<ProductEntity?> = flowOf(null)

        override suspend fun findById(id: Long): ProductEntity? = null

        override suspend fun insert(product: ProductEntity): Long = 1

        override suspend fun insertAll(products: List<ProductEntity>) = Unit
    }
}
