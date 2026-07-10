package afsm.sample.shop.feature.editor

import afsm.compose.CollectAfsmEffects
import afsm.sample.shop.app.ShopAppContainer
import afsm.sample.shop.app.sampleViewModelFactory
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
                imageUploader = MockProductImageUploader(),
            )
        }
    }
    val viewModel: ProductEditorViewModel = viewModel(factory = factory)
    val state by viewModel.state.collectAsStateWithLifecycle()

    CollectAfsmEffects(viewModel.effects) { effect ->
        when (effect) {
            ProductEditorEffect.CloseEditor -> onDone()
        }
    }

    ProductEditorScreen(
        state = state.toRenderState(),
        onEvent = viewModel::onEvent,
    )
}

@Composable
fun ProductEditorScreen(
    state: ProductEditorRenderState,
    onEvent: (ProductEditorEvent) -> Unit,
) {
    Surface {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            Text(
                text = "Register product",
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Status: ${state.statusText}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (state.showDraftFields) {
                ProductDraftFields(
                    form = state.form,
                    enabled = state.fieldsEnabled,
                    onTitleChange = { onEvent(ProductEditorEvent.TitleChanged(it)) },
                    onDescriptionChange = { onEvent(ProductEditorEvent.DescriptionChanged(it)) },
                    onPriceChange = { onEvent(ProductEditorEvent.PriceChanged(it)) },
                )
            }

            state.reviewNote?.let { reviewNote ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Review note: $reviewNote",
                    color = MaterialTheme.colorScheme.error,
                )
            }

            state.publishedTitle?.let { title ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Published product: $title",
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            state.errorMessage?.let { message ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
            ProductEditorActions(
                state = state,
                onEvent = onEvent,
            )
        }
    }
}

@Composable
private fun ProductDraftFields(
    form: ProductDraftForm,
    enabled: Boolean,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = form.title,
        enabled = enabled,
        onValueChange = onTitleChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text("Title") },
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = form.description,
        enabled = enabled,
        onValueChange = onDescriptionChange,
        modifier = Modifier.fillMaxWidth(),
        minLines = 3,
        label = { Text("Description") },
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = form.priceText,
        enabled = enabled,
        onValueChange = onPriceChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text("Price") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    )
}

@Composable
private fun ProductEditorActions(
    state: ProductEditorRenderState,
    onEvent: (ProductEditorEvent) -> Unit,
) {
    val secondaryAction = state.secondaryAction
    if (state.isProcessing) {
        if (secondaryAction == null) {
            Button(
                enabled = false,
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Processing...")
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { onEvent(secondaryAction.toEvent()) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(secondaryAction.label)
                }
                Button(
                    enabled = false,
                    onClick = {},
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Processing...")
                }
            }
        }
        return
    }

    val primaryAction = state.primaryAction ?: return

    if (secondaryAction == null) {
        Button(
            onClick = { onEvent(primaryAction.toEvent()) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(primaryAction.label)
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = { onEvent(secondaryAction.toEvent()) },
                modifier = Modifier.weight(1f),
            ) {
                Text(secondaryAction.label)
            }
            Button(
                onClick = { onEvent(primaryAction.toEvent()) },
                modifier = Modifier.weight(1f),
            ) {
                Text(primaryAction.label)
            }
        }
    }
}

private val ProductEditorPrimaryAction.label: String
    get() = when (this) {
        ProductEditorPrimaryAction.SubmitForReview -> "Submit for review"
        ProductEditorPrimaryAction.ResubmitForReview -> "Resubmit for review"
        ProductEditorPrimaryAction.Publish -> "Publish"
        ProductEditorPrimaryAction.Done -> "Done"
    }

private val ProductEditorSecondaryAction.label: String
    get() = when (this) {
        ProductEditorSecondaryAction.SaveDraft -> "Save draft"
        ProductEditorSecondaryAction.ContinueEditing -> "Continue editing"
        ProductEditorSecondaryAction.CancelUpload -> "Cancel upload"
    }

private fun ProductEditorPrimaryAction.toEvent(): ProductEditorEvent {
    return when (this) {
        ProductEditorPrimaryAction.SubmitForReview -> ProductEditorEvent.SubmitClicked
        ProductEditorPrimaryAction.ResubmitForReview -> ProductEditorEvent.ResubmitClicked
        ProductEditorPrimaryAction.Publish -> ProductEditorEvent.PublishClicked
        ProductEditorPrimaryAction.Done -> ProductEditorEvent.DoneClicked
    }
}

private fun ProductEditorSecondaryAction.toEvent(): ProductEditorEvent {
    return when (this) {
        ProductEditorSecondaryAction.SaveDraft -> ProductEditorEvent.SaveDraftClicked
        ProductEditorSecondaryAction.ContinueEditing -> ProductEditorEvent.ContinueEditingClicked
        ProductEditorSecondaryAction.CancelUpload -> ProductEditorEvent.CancelUploadClicked
    }
}
