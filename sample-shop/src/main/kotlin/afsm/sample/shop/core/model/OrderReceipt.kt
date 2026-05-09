package afsm.sample.shop.core.model

data class OrderReceipt(
    val orderId: Long,
    val productId: Long,
    val totalCents: Long,
)
