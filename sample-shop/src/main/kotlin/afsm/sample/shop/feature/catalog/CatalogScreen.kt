package afsm.sample.shop.feature.catalog

import afsm.sample.shop.app.ShopAppContainer
import afsm.sample.shop.app.sampleViewModelFactory
import afsm.sample.shop.core.model.Product
import afsm.sample.shop.core.model.asPriceText
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun CatalogRoute(
    container: ShopAppContainer,
    onProductClick: (Long) -> Unit,
    onAddProductClick: () -> Unit,
    onLoggedOut: () -> Unit,
) {
    val factory = remember(container) {
        sampleViewModelFactory {
            CatalogViewModel(
                productRepository = container.productRepository,
                sessionRepository = container.sessionRepository,
            )
        }
    }
    val viewModel: CatalogViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.session) {
        if (state.session == null) {
            onLoggedOut()
        }
    }

    CatalogScreen(
        state = state,
        onProductClick = onProductClick,
        onAddProductClick = onAddProductClick,
        onLogoutClick = viewModel::logout,
    )
}

@Composable
fun CatalogScreen(
    state: CatalogUiState,
    onProductClick: (Long) -> Unit,
    onAddProductClick: () -> Unit,
    onLogoutClick: () -> Unit,
) {
    Surface {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Products",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        text = state.session?.name ?: "No session",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onLogoutClick) {
                    Text("Logout")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onAddProductClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Register product")
            }
            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
            ) {
                items(
                    items = state.products,
                    key = { product -> product.id },
                ) { product ->
                    ProductRow(
                        product = product,
                        onClick = { onProductClick(product.id) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun ProductRow(
    product: Product,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = product.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = product.priceCents.asPriceText(),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = product.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
