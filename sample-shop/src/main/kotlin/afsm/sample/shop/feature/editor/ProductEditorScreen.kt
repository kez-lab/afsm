package afsm.sample.shop.feature.editor

import afsm.sample.shop.app.ShopAppContainer
import afsm.sample.shop.app.sampleViewModelFactory
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ProductEditorRoute(
    container: ShopAppContainer,
    onDone: () -> Unit,
) {
    val factory = remember(container) {
        sampleViewModelFactory {
            ProductEditorViewModel(
                productRepository = container.productRepository,
                sessionRepository = container.sessionRepository,
            )
        }
    }
    val viewModel: ProductEditorViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            onDone()
        }
    }

    ProductEditorScreen(
        state = state,
        onTitleChange = viewModel::updateTitle,
        onDescriptionChange = viewModel::updateDescription,
        onPriceChange = viewModel::updatePrice,
        onSaveClick = viewModel::save,
    )
}

@Composable
fun ProductEditorScreen(
    state: ProductEditorUiState,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onSaveClick: () -> Unit,
) {
    Surface {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
        ) {
            Text(
                text = "Register product",
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = state.title,
                enabled = !state.isSaving,
                onValueChange = onTitleChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Title") },
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = state.description,
                enabled = !state.isSaving,
                onValueChange = onDescriptionChange,
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                label = { Text("Description") },
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = state.priceText,
                enabled = !state.isSaving,
                onValueChange = onPriceChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Price") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
                enabled = !state.isSaving,
                onClick = onSaveClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.isSaving) "Saving..." else "Save product")
            }
        }
    }
}
