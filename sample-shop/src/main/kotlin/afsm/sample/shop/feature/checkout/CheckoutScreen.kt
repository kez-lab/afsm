package afsm.sample.shop.feature.checkout

import afsm.sample.shop.app.ShopAppContainer
import afsm.sample.shop.app.sampleViewModelFactory
import afsm.sample.shop.core.model.asPriceText
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun CheckoutRoute(
    container: ShopAppContainer,
    productId: Long,
    onBack: () -> Unit,
    onPaymentComplete: () -> Unit,
) {
    val factory = remember(container, productId) {
        sampleViewModelFactory {
            CheckoutViewModel(
                productId = productId,
                productRepository = container.productRepository,
                paymentRepository = container.paymentRepository,
                sessionRepository = container.sessionRepository,
            )
        }
    }
    val viewModel: CheckoutViewModel = viewModel(factory = factory)
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is CheckoutEffect.PaymentCompleted -> onPaymentComplete()
            }
        }
    }

    CheckoutScreen(
        state = state,
        onBack = onBack,
        onPayClick = { viewModel.onEvent(CheckoutEvent.PayClicked) },
        onRetryClick = { viewModel.onEvent(CheckoutEvent.RetryClicked) },
    )
}

@Composable
fun CheckoutScreen(
    state: CheckoutState,
    onBack: () -> Unit,
    onPayClick: () -> Unit,
    onRetryClick: () -> Unit,
) {
    Surface {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
        ) {
            TextButton(onClick = onBack) {
                Text("Back")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Checkout",
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (state.isLoadingProduct) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(12.dp))
                Text("Loading product...")
            } else {
                val product = state.product
                if (product == null) {
                    Text(
                        text = state.errorMessage ?: "Product not found.",
                        color = MaterialTheme.colorScheme.error,
                    )
                } else {
                    Text(
                        text = product.title,
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = product.priceCents.asPriceText(),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "Mock payment intentionally fails on the first attempt for higher-priced products.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    state.errorMessage?.let { message ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    Button(
                        enabled = !state.isPaying,
                        onClick = if (state.errorMessage == null) onPayClick else onRetryClick,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            when {
                                state.isPaying -> "Processing..."
                                state.errorMessage == null -> "Pay"
                                else -> "Retry payment"
                            },
                        )
                    }
                }
            }
        }
    }
}
