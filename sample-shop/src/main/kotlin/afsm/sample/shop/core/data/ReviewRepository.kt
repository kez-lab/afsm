package afsm.sample.shop.core.data

import afsm.sample.shop.core.database.ReviewDao
import afsm.sample.shop.core.database.ReviewEntity
import afsm.sample.shop.core.model.Review
import afsm.sample.shop.core.model.UserSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ReviewRepository(
    private val reviewDao: ReviewDao,
) {
    fun observeReviews(productId: Long): Flow<List<Review>> {
        return reviewDao.observeReviews(productId).map { reviews ->
            reviews.map { review -> review.toModel() }
        }
    }

    suspend fun addReview(
        productId: Long,
        session: UserSession,
        rating: Int,
        body: String,
    ) {
        reviewDao.insert(
            ReviewEntity(
                productId = productId,
                userId = session.userId,
                authorName = session.name,
                rating = rating.coerceIn(1, 5),
                body = body.trim(),
                createdAtMillis = System.currentTimeMillis(),
            ),
        )
    }

    private fun ReviewEntity.toModel(): Review {
        return Review(
            id = id,
            productId = productId,
            authorName = authorName,
            rating = rating,
            body = body,
        )
    }
}
