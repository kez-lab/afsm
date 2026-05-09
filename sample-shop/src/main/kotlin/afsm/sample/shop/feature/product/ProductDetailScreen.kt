package afsm.sample.shop.feature.product

import afsm.sample.shop.app.ShopAppContainer
import afsm.sample.shop.app.sampleViewModelFactory
import afsm.sample.shop.core.model.Review
import afsm.sample.shop.core.model.asPriceText
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ProductDetailRoute(
    container: ShopAppContainer,
    productId: Long,
    onBack: () -> Unit,
    onBuy: () -> Unit,
) {
    val factory = remember(container, productId) {
        sampleViewModelFactory {
            ProductDetailViewModel(
                productId = productId,
                productRepository = container.productRepository,
                favoriteRepository = container.favoriteRepository,
                reviewRepository = container.reviewRepository,
                sessionRepository = container.sessionRepository,
            )
        }
    }
    val viewModel: ProductDetailViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ProductDetailScreen(
        state = state,
        onBack = onBack,
        onBuy = onBuy,
        onToggleLike = viewModel::toggleLike,
        onReviewBodyChange = viewModel::updateReviewBody,
        onReviewRatingChange = viewModel::updateReviewRating,
        onSubmitReview = viewModel::submitReview,
    )
}

@Composable
fun ProductDetailScreen(
    state: ProductDetailUiState,
    onBack: () -> Unit,
    onBuy: () -> Unit,
    onToggleLike: () -> Unit,
    onReviewBodyChange: (String) -> Unit,
    onReviewRatingChange: (Int) -> Unit,
    onSubmitReview: () -> Unit,
) {
    Surface {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
        ) {
            item {
                TextButton(onClick = onBack) {
                    Text("Back")
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            val product = state.product
            if (product == null) {
                item {
                    Text(
                        text = "Product not found.",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                return@LazyColumn
            }

            item {
                Text(
                    text = product.title,
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = product.priceCents.asPriceText(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = product.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = onToggleLike,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(if (state.isLiked) "Liked" else "Like")
                    }
                    Button(
                        onClick = onBuy,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Buy")
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Write review",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                RatingControl(
                    rating = state.reviewRating,
                    onRatingChange = onReviewRatingChange,
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.reviewBody,
                    onValueChange = onReviewBodyChange,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    label = { Text("Review") },
                )
                state.message?.let { message ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onSubmitReview,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Submit review")
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Reviews",
                    style = MaterialTheme.typography.titleMedium,
                )
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
            }

            if (state.reviews.isEmpty()) {
                item {
                    Text(
                        text = "No reviews yet.",
                        modifier = Modifier.padding(vertical = 16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(
                    items = state.reviews,
                    key = { review -> review.id },
                ) { review ->
                    ReviewRow(review = review)
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun RatingControl(
    rating: Int,
    onRatingChange: (Int) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        (1..5).forEach { value ->
            OutlinedButton(
                onClick = { onRatingChange(value) },
            ) {
                Text(if (rating == value) "$value*" else value.toString())
            }
        }
    }
}

@Composable
private fun ReviewRow(
    review: Review,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = review.authorName,
                fontWeight = FontWeight.SemiBold,
            )
            Text("${review.rating}/5")
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = review.body,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
