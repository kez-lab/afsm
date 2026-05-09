package afsm.sample.shop.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reviews")
data class ReviewEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productId: Long,
    val userId: Long,
    val authorName: String,
    val rating: Int,
    val body: String,
    val createdAtMillis: Long,
)
