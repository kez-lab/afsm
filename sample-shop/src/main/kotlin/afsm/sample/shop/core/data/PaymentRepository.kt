package afsm.sample.shop.core.data

import afsm.sample.shop.core.database.OrderDao
import afsm.sample.shop.core.database.OrderEntity
import afsm.sample.shop.core.model.OrderReceipt
import afsm.sample.shop.core.model.Product
import afsm.sample.shop.core.model.UserSession
import kotlinx.coroutines.delay

class PaymentRepository(
    private val orderDao: OrderDao,
) {
    private val attemptsByProductId = mutableMapOf<Long, Int>()

    suspend fun submitPayment(
        session: UserSession,
        product: Product,
    ): Result<OrderReceipt> {
        delay(400)

        val attempt = nextAttempt(product.id)
        if (attempt == 1 && product.priceCents >= 10_000) {
            return Result.failure(IllegalStateException("Mock payment declined. Try again."))
        }

        val orderId = orderDao.insert(
            OrderEntity(
                userId = session.userId,
                productId = product.id,
                totalCents = product.priceCents,
                status = "paid",
                createdAtMillis = System.currentTimeMillis(),
            ),
        )

        return Result.success(
            OrderReceipt(
                orderId = orderId,
                productId = product.id,
                totalCents = product.priceCents,
            ),
        )
    }

    @Synchronized
    private fun nextAttempt(productId: Long): Int {
        val next = attemptsByProductId.getOrDefault(productId, 0) + 1
        attemptsByProductId[productId] = next
        return next
    }
}
