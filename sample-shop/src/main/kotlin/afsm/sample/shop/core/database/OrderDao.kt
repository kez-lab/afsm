package afsm.sample.shop.core.database

import androidx.room.Dao
import androidx.room.Insert

@Dao
interface OrderDao {
    @Insert
    suspend fun insert(order: OrderEntity): Long
}
