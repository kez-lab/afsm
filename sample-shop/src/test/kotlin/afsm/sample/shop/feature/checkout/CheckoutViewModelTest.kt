package afsm.sample.shop.feature.checkout

import afsm.sample.shop.core.data.PaymentRepository
import afsm.sample.shop.core.data.ProductRepository
import afsm.sample.shop.core.data.SessionRepository
import afsm.sample.shop.core.database.OrderDao
import afsm.sample.shop.core.database.OrderEntity
import afsm.sample.shop.core.database.ProductDao
import afsm.sample.shop.core.database.ProductEntity
import afsm.sample.shop.core.model.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class CheckoutViewModelTest {
    private val mainDispatcher = StandardTestDispatcher()

    private val productEntity = ProductEntity(
        id = 7,
        title = "Everyday Backpack",
        description = "A compact city bag.",
        priceCents = 8_900,
        sellerUserId = null,
        createdAtMillis = 1,
    )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `navigation product id drives load command and ready state`() = runTest {
        val productDao = RecordingProductDao(productEntity)
        val viewModel = checkoutViewModel(
            productId = productEntity.id,
            productDao = productDao,
        )

        mainDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(productEntity.id), productDao.requestedProductIds)
        assertEquals(CheckoutPhase.ProductReady, viewModel.state.value.phase)
        assertEquals(productEntity.id, viewModel.state.value.data.productId)
        assertEquals(productEntity.id, viewModel.state.value.data.product?.id)
    }

    @Test
    fun `payment command reaches durable completion and active effect collector`() = runTest {
        val orderDao = RecordingOrderDao(nextOrderId = 42)
        val sessionRepository = SessionRepository().apply {
            setSession(
                UserSession(
                    userId = 11,
                    name = "Ada",
                    email = "ada@example.com",
                ),
            )
        }
        val viewModel = checkoutViewModel(
            productId = productEntity.id,
            productDao = RecordingProductDao(productEntity),
            orderDao = orderDao,
            sessionRepository = sessionRepository,
        )
        val effects = mutableListOf<CheckoutEffect>()

        mainDispatcher.scheduler.advanceUntilIdle()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effect -> effects += effect }
        }

        viewModel.onEvent(CheckoutEvent.PayClicked)
        mainDispatcher.scheduler.advanceUntilIdle()

        val completed = assertIs<CheckoutPhase.Completed>(viewModel.state.value.phase)
        assertEquals(42, completed.orderId)
        assertEquals(
            listOf<CheckoutEffect>(CheckoutEffect.PaymentCompleted(orderId = 42)),
            effects,
        )
        assertEquals(1, orderDao.insertedOrders.size)
        assertEquals(11, orderDao.insertedOrders.single().userId)
        assertEquals(productEntity.id, orderDao.insertedOrders.single().productId)
    }

    @Test
    fun `missing session maps payment command to failure without inserting order`() = runTest {
        val orderDao = RecordingOrderDao()
        val viewModel = checkoutViewModel(
            productId = productEntity.id,
            productDao = RecordingProductDao(productEntity),
            orderDao = orderDao,
        )

        mainDispatcher.scheduler.advanceUntilIdle()
        viewModel.onEvent(CheckoutEvent.PayClicked)
        mainDispatcher.scheduler.advanceUntilIdle()

        assertEquals(CheckoutPhase.PaymentFailed, viewModel.state.value.phase)
        assertEquals("Login is required.", viewModel.state.value.data.errorMessage)
        assertEquals(emptyList(), orderDao.insertedOrders)
    }

    @Test
    fun `missing product maps load command to unavailable state`() = runTest {
        val productDao = RecordingProductDao(product = null)
        val viewModel = checkoutViewModel(
            productId = 404,
            productDao = productDao,
        )

        mainDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(404L), productDao.requestedProductIds)
        assertEquals(CheckoutPhase.ProductUnavailable, viewModel.state.value.phase)
        assertEquals("Product is no longer available.", viewModel.state.value.data.errorMessage)
    }

    private fun checkoutViewModel(
        productId: Long,
        productDao: ProductDao,
        orderDao: OrderDao = RecordingOrderDao(),
        sessionRepository: SessionRepository = SessionRepository(),
    ): CheckoutViewModel {
        return CheckoutViewModel(
            productId = productId,
            productRepository = ProductRepository(productDao),
            paymentRepository = PaymentRepository(orderDao),
            sessionRepository = sessionRepository,
        )
    }

    private class RecordingProductDao(
        private val product: ProductEntity?,
    ) : ProductDao {
        val requestedProductIds = mutableListOf<Long>()

        override suspend fun count(): Int = if (product == null) 0 else 1

        override fun observeProducts(): Flow<List<ProductEntity>> =
            flowOf(listOfNotNull(product))

        override fun observeProduct(id: Long): Flow<ProductEntity?> =
            flowOf(product?.takeIf { it.id == id })

        override suspend fun findById(id: Long): ProductEntity? {
            requestedProductIds += id
            return product?.takeIf { it.id == id }
        }

        override suspend fun insert(product: ProductEntity): Long {
            error("Checkout must not insert products.")
        }

        override suspend fun insertAll(products: List<ProductEntity>) {
            error("Checkout must not seed products.")
        }
    }

    private class RecordingOrderDao(
        private val nextOrderId: Long = 1,
    ) : OrderDao {
        val insertedOrders = mutableListOf<OrderEntity>()

        override suspend fun insert(order: OrderEntity): Long {
            insertedOrders += order
            return nextOrderId
        }
    }
}
