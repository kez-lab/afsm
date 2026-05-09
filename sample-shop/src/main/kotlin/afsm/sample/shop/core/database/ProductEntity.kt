package afsm.sample.shop.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String,
    val priceCents: Long,
    val sellerUserId: Long?,
    val createdAtMillis: Long,
)
