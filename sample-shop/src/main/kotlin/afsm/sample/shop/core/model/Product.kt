package afsm.sample.shop.core.model

data class Product(
    val id: Long,
    val title: String,
    val description: String,
    val priceCents: Long,
    val sellerUserId: Long?,
)
