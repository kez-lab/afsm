package afsm.sample.shop.feature.editor

import afsm.sample.shop.core.data.ProductRepository
import afsm.sample.shop.core.data.SessionRepository
import afsm.sample.shop.core.database.ProductDao
import afsm.sample.shop.core.database.ProductEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

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
    fun `cancel upload returns to editing and cooperative upload cannot complete later`() = runTest {
        val viewModel = ProductEditorViewModel(
            productRepository = ProductRepository(NoOpProductDao),
            sessionRepository = SessionRepository(),
        )

        viewModel.onEvent(ProductEditorEvent.TitleChanged("Travel Mug"))
        viewModel.onEvent(
            ProductEditorEvent.DescriptionChanged("Leakproof mug for commuting."),
        )
        viewModel.onEvent(ProductEditorEvent.PriceChanged("24.50"))
        viewModel.onEvent(ProductEditorEvent.SubmitClicked)
        mainDispatcher.scheduler.runCurrent()

        assertEquals(
            ProductEditorPhase.ImageUploadInProgress,
            viewModel.state.value.phase,
        )
        assertEquals(
            ProductEditorSecondaryAction.CancelUpload,
            viewModel.state.value.toRenderState().secondaryAction,
        )

        viewModel.onEvent(ProductEditorEvent.CancelUploadClicked)
        mainDispatcher.scheduler.runCurrent()

        assertEquals(ProductEditorPhase.EditingDraft, viewModel.state.value.phase)

        mainDispatcher.scheduler.advanceTimeBy(300)
        mainDispatcher.scheduler.runCurrent()

        assertEquals(ProductEditorPhase.EditingDraft, viewModel.state.value.phase)
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
