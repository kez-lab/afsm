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
        state = state,
        onEvent = viewModel::onEvent,
    )
}

@Composable
fun ProductEditorScreen(
    state: ProductEditorState,
    onEvent: (ProductEditorEvent) -> Unit,
) {
    val draft = state.draftOrNull()
    val form = draft?.form ?: ProductDraftForm()
    val fieldsEnabled = state.phase == ProductEditorPhase.EditingDraft ||
        state.phase is ProductEditorPhase.Rejected
    val errorMessage = when (state.phase) {
        ProductEditorPhase.EditingDraft,
        ProductEditorPhase.DraftSaved,
        is ProductEditorPhase.Rejected -> state.context.errorMessage

        else -> null
    }

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
                text = "Status: ${state.statusText()}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(16.dp))

            ProductDraftFields(
                form = form,
                enabled = fieldsEnabled,
                onTitleChange = { onEvent(ProductEditorEvent.TitleChanged(it)) },
                onDescriptionChange = { onEvent(ProductEditorEvent.DescriptionChanged(it)) },
                onPriceChange = { onEvent(ProductEditorEvent.PriceChanged(it)) },
            )

            val phase = state.phase
            if (phase is ProductEditorPhase.Rejected) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Review note: ${phase.reason}",
                    color = MaterialTheme.colorScheme.error,
                )
            }

            if (phase is ProductEditorPhase.Published) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Published product: ${phase.title}",
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            errorMessage?.let { message ->
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
    state: ProductEditorState,
    onEvent: (ProductEditorEvent) -> Unit,
) {
    when (state.phase) {
        ProductEditorPhase.EditingDraft -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { onEvent(ProductEditorEvent.SaveDraftClicked) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Save draft")
                }
                Button(
                    onClick = { onEvent(ProductEditorEvent.SubmitClicked) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Submit for review")
                }
            }
        }

        ProductEditorPhase.DraftSaved -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { onEvent(ProductEditorEvent.ContinueEditingClicked) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Continue editing")
                }
                Button(
                    onClick = { onEvent(ProductEditorEvent.SubmitClicked) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Submit for review")
                }
            }
        }

        is ProductEditorPhase.Rejected -> {
            Button(
                onClick = { onEvent(ProductEditorEvent.ResubmitClicked) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Resubmit for review")
            }
        }

        ProductEditorPhase.Approved -> {
            Button(
                onClick = { onEvent(ProductEditorEvent.PublishClicked) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Publish")
            }
        }

        is ProductEditorPhase.Published -> {
            Button(
                onClick = { onEvent(ProductEditorEvent.DoneClicked) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Done")
            }
        }

        ProductEditorPhase.SavingDraft,
        ProductEditorPhase.ImageUploadInProgress,
        is ProductEditorPhase.ReviewSubmissionInProgress,
        ProductEditorPhase.PublishInProgress -> {
            Button(
                enabled = false,
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Processing...")
            }
        }
    }
}

private fun ProductEditorState.statusText(): String {
    return when (phase) {
        ProductEditorPhase.EditingDraft -> "Editing draft"
        ProductEditorPhase.SavingDraft -> "Saving draft"
        ProductEditorPhase.DraftSaved -> "Draft saved"
        ProductEditorPhase.ImageUploadInProgress -> "Uploading mock images"
        is ProductEditorPhase.ReviewSubmissionInProgress -> "Submitting for review"
        is ProductEditorPhase.Rejected -> "Review rejected"
        ProductEditorPhase.Approved -> "Review approved"
        ProductEditorPhase.PublishInProgress -> "Publishing product"
        is ProductEditorPhase.Published -> "Product published"
    }
}
