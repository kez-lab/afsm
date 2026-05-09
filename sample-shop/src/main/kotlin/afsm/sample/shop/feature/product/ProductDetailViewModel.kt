package afsm.sample.shop.feature.product

import afsm.sample.shop.core.data.FavoriteRepository
import afsm.sample.shop.core.data.ProductRepository
import afsm.sample.shop.core.data.ReviewRepository
import afsm.sample.shop.core.data.SessionRepository
import afsm.sample.shop.core.model.Product
import afsm.sample.shop.core.model.Review
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProductDetailUiState(
    val product: Product? = null,
    val isLiked: Boolean = false,
    val reviews: List<Review> = emptyList(),
    val reviewBody: String = "",
    val reviewRating: Int = 5,
    val message: String? = null,
)

class ProductDetailViewModel(
    private val productId: Long,
    private val productRepository: ProductRepository,
    private val favoriteRepository: FavoriteRepository,
    private val reviewRepository: ReviewRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {
    private val formState = MutableStateFlow(
        ProductDetailUiState(
            reviewRating = 5,
        ),
    )

    val uiState: StateFlow<ProductDetailUiState> = combine(
        productRepository.observeProduct(productId),
        favoriteFlow(),
        reviewRepository.observeReviews(productId),
        formState,
    ) { product, isLiked, reviews, form ->
        form.copy(
            product = product,
            isLiked = isLiked,
            reviews = reviews,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProductDetailUiState(),
    )

    fun toggleLike() {
        val session = sessionRepository.currentSession() ?: return
        viewModelScope.launch {
            favoriteRepository.toggle(
                userId = session.userId,
                productId = productId,
            )
        }
    }

    fun updateReviewBody(value: String) {
        formState.update { state ->
            state.copy(
                reviewBody = value,
                message = null,
            )
        }
    }

    fun updateReviewRating(value: Int) {
        formState.update { state ->
            state.copy(
                reviewRating = value.coerceIn(1, 5),
                message = null,
            )
        }
    }

    fun submitReview() {
        val session = sessionRepository.currentSession()
        if (session == null) {
            formState.update { it.copy(message = "Login is required.") }
            return
        }

        val current = formState.value
        if (current.reviewBody.isBlank()) {
            formState.update { it.copy(message = "Review body is required.") }
            return
        }

        viewModelScope.launch {
            reviewRepository.addReview(
                productId = productId,
                session = session,
                rating = current.reviewRating,
                body = current.reviewBody,
            )
            formState.update {
                it.copy(
                    reviewBody = "",
                    reviewRating = 5,
                    message = "Review saved.",
                )
            }
        }
    }

    private fun favoriteFlow(): Flow<Boolean> {
        val session = sessionRepository.currentSession()
        return if (session == null) {
            flowOf(false)
        } else {
            favoriteRepository.observeLiked(
                userId = session.userId,
                productId = productId,
            )
        }
    }
}
