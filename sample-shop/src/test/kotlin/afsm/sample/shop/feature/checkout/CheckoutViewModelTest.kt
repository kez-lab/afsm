package afsm.sample.shop.feature.checkout

import afsm.sample.shop.core.data.PaymentRepository
import afsm.sample.shop.core.data.ProductRepository
import afsm.sample.shop.core.data.SessionRepository
import afsm.sample.shop.core.database.OrderDao
import afsm.sample.shop.core.database.OrderEntity
import afsm.sample.shop.core.database.ProductDao
import afsm.sample.shop.core.database.ProductEntity
import afsm.sample.shop.core.model.UserSession
import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
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
        val savedStateHandle = SavedStateHandle()
        val viewModel = checkoutViewModel(
            productId = productEntity.id,
            productDao = productDao,
            savedStateHandle = savedStateHandle,
        )

        mainDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(productEntity.id), productDao.requestedProductIds)
        assertEquals(CheckoutPhase.ProductReady, viewModel.state.value.phase)
        assertEquals(productEntity.id, viewModel.state.value.data.productId)
        assertEquals(productEntity.id, viewModel.state.value.data.product?.id)
        assertEquals(productEntity.id, savedStateHandle.get<Long>(CheckoutProductIdKey))
    }

    @Test
    fun `payment command reaches durable completion state`() = runTest {
        val orderDao = RecordingOrderDao(nextOrderId = 42)
        val savedStateHandle = SavedStateHandle()
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
            savedStateHandle = savedStateHandle,
        )
        mainDispatcher.scheduler.advanceUntilIdle()

        viewModel.pay()
        mainDispatcher.scheduler.runCurrent()

        assertEquals(1L, savedStateHandle.get<Long>(CheckoutPendingPaymentRequestIdKey))

        mainDispatcher.scheduler.advanceUntilIdle()

        val completed = assertIs<CheckoutPhase.Completed>(viewModel.state.value.phase)
        assertEquals(42, completed.orderId)
        assertEquals(1, orderDao.insertedOrders.size)
        assertEquals(11, orderDao.insertedOrders.single().userId)
        assertEquals(productEntity.id, orderDao.insertedOrders.single().productId)
        assertEquals(42L, savedStateHandle.get<Long>(CheckoutCompletedOrderIdKey))
        assertEquals(null, savedStateHandle.get<Long>(CheckoutPendingPaymentRequestIdKey))
    }

    @Test
    fun `missing session maps payment command to failure without inserting order`() = runTest {
        val orderDao = RecordingOrderDao()
        val savedStateHandle = SavedStateHandle()
        val viewModel = checkoutViewModel(
            productId = productEntity.id,
            productDao = RecordingProductDao(productEntity),
            orderDao = orderDao,
            savedStateHandle = savedStateHandle,
        )

        mainDispatcher.scheduler.advanceUntilIdle()
        viewModel.pay()
        mainDispatcher.scheduler.advanceUntilIdle()

        assertEquals(CheckoutPhase.PaymentFailed, viewModel.state.value.phase)
        assertEquals("Login is required.", viewModel.state.value.data.errorMessage)
        assertEquals(emptyList(), orderDao.insertedOrders)
        assertEquals(null, savedStateHandle.get<Long>(CheckoutPendingPaymentRequestIdKey))
    }

    @Test
    fun `repository payment failure clears pending state and remains retryable`() = runTest {
        val expensiveProduct = productEntity.copy(priceCents = 12_900)
        val orderDao = RecordingOrderDao()
        val savedStateHandle = SavedStateHandle()
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
            productId = expensiveProduct.id,
            productDao = RecordingProductDao(expensiveProduct),
            orderDao = orderDao,
            sessionRepository = sessionRepository,
            savedStateHandle = savedStateHandle,
        )

        mainDispatcher.scheduler.advanceUntilIdle()
        viewModel.pay()
        mainDispatcher.scheduler.runCurrent()

        assertEquals(1L, savedStateHandle.get<Long>(CheckoutPendingPaymentRequestIdKey))

        mainDispatcher.scheduler.advanceUntilIdle()

        assertEquals(CheckoutPhase.PaymentFailed, viewModel.state.value.phase)
        assertEquals(CheckoutPrimaryAction.RetryPayment, viewModel.state.value.toRenderState().primaryAction)
        assertEquals(null, savedStateHandle.get<Long>(CheckoutPendingPaymentRequestIdKey))
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

    @Test
    fun `restored completion starts durable state without commands`() = runTest {
        val productDao = RecordingProductDao(productEntity)
        val orderDao = RecordingOrderDao()
        val savedStateHandle = SavedStateHandle(
            mapOf(
                CheckoutProductIdKey to productEntity.id,
                CheckoutCompletedOrderIdKey to 42L,
                CheckoutPendingPaymentRequestIdKey to 1L,
            ),
        )
        val viewModel = checkoutViewModel(
            productId = productEntity.id,
            productDao = productDao,
            orderDao = orderDao,
            savedStateHandle = savedStateHandle,
        )

        mainDispatcher.scheduler.advanceUntilIdle()

        assertEquals(CheckoutPhase.Completed(orderId = 42), viewModel.state.value.phase)
        assertEquals(emptyList(), productDao.requestedProductIds)
        assertEquals(emptyList(), orderDao.insertedOrders)
        assertEquals(null, savedStateHandle.get<Long>(CheckoutPendingPaymentRequestIdKey))
    }

    @Test
    fun `restored pending payment exposes unknown status without automatic work`() = runTest {
        val productDao = RecordingProductDao(productEntity)
        val orderDao = RecordingOrderDao()
        val savedStateHandle = SavedStateHandle(
            mapOf(
                CheckoutProductIdKey to productEntity.id,
                CheckoutPendingPaymentRequestIdKey to 9L,
            ),
        )
        val viewModel = checkoutViewModel(
            productId = productEntity.id,
            productDao = productDao,
            orderDao = orderDao,
            savedStateHandle = savedStateHandle,
        )

        mainDispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            CheckoutPhase.PaymentStatusUnknown(requestId = 9),
            viewModel.state.value.phase,
        )
        assertEquals(emptyList(), productDao.requestedProductIds)
        assertEquals(emptyList(), orderDao.insertedOrders)
        assertEquals(null, viewModel.state.value.toRenderState().primaryAction)
    }

    private fun checkoutViewModel(
        productId: Long,
        productDao: ProductDao,
        orderDao: OrderDao = RecordingOrderDao(),
        sessionRepository: SessionRepository = SessionRepository(),
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ): CheckoutViewModel {
        return CheckoutViewModel(
            productId = productId,
            productRepository = ProductRepository(productDao),
            paymentRepository = PaymentRepository(orderDao),
            sessionRepository = sessionRepository,
            savedStateHandle = savedStateHandle,
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
