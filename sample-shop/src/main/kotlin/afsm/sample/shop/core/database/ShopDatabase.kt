package afsm.sample.shop.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        FavoriteEntity::class,
        OrderEntity::class,
        ProductEntity::class,
        ReviewEntity::class,
        UserEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class ShopDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao

    abstract fun orderDao(): OrderDao

    abstract fun productDao(): ProductDao

    abstract fun reviewDao(): ReviewDao

    abstract fun userDao(): UserDao
}
