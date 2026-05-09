package afsm.sample.shop.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT COUNT(*) FROM favorites WHERE userId = :userId AND productId = :productId")
    fun observeFavoriteCount(userId: Long, productId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM favorites WHERE userId = :userId AND productId = :productId")
    suspend fun count(userId: Long, productId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE userId = :userId AND productId = :productId")
    suspend fun delete(userId: Long, productId: Long)
}
