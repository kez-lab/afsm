package afsm.sample.shop.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReviewDao {
    @Query("SELECT * FROM reviews WHERE productId = :productId ORDER BY createdAtMillis DESC")
    fun observeReviews(productId: Long): Flow<List<ReviewEntity>>

    @Insert
    suspend fun insert(review: ReviewEntity): Long
}
