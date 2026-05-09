package afsm.sample.shop.core.database

import androidx.room.Entity

@Entity(
    tableName = "favorites",
    primaryKeys = ["userId", "productId"],
)
data class FavoriteEntity(
    val userId: Long,
    val productId: Long,
    val createdAtMillis: Long,
)
