package afsm.sample.shop.core.model

data class Review(
    val id: Long,
    val productId: Long,
    val authorName: String,
    val rating: Int,
    val body: String,
)
