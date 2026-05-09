package afsm.sample.shop.core.data

import afsm.sample.shop.core.database.FavoriteDao
import afsm.sample.shop.core.database.FavoriteEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FavoriteRepository(
    private val favoriteDao: FavoriteDao,
) {
    fun observeLiked(
        userId: Long,
        productId: Long,
    ): Flow<Boolean> {
        return favoriteDao.observeFavoriteCount(
            userId = userId,
            productId = productId,
        ).map { count -> count > 0 }
    }

    suspend fun toggle(
        userId: Long,
        productId: Long,
    ) {
        val isLiked = favoriteDao.count(
            userId = userId,
            productId = productId,
        ) > 0

        if (isLiked) {
            favoriteDao.delete(
                userId = userId,
                productId = productId,
            )
        } else {
            favoriteDao.insert(
                FavoriteEntity(
                    userId = userId,
                    productId = productId,
                    createdAtMillis = System.currentTimeMillis(),
                ),
            )
        }
    }
}
