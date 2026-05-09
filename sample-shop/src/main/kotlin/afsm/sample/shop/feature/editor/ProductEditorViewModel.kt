package afsm.sample.shop.feature.editor

import afsm.sample.shop.core.data.ProductRepository
import afsm.sample.shop.core.data.SessionRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProductEditorUiState(
    val title: String = "",
    val description: String = "",
    val priceText: String = "",
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null,
)

class ProductEditorViewModel(
    private val productRepository: ProductRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProductEditorUiState())

    val uiState: StateFlow<ProductEditorUiState> = _uiState.asStateFlow()

    fun updateTitle(value: String) {
        _uiState.update { state -> state.copy(title = value, errorMessage = null) }
    }

    fun updateDescription(value: String) {
        _uiState.update { state -> state.copy(description = value, errorMessage = null) }
    }

    fun updatePrice(value: String) {
        _uiState.update { state -> state.copy(priceText = value, errorMessage = null) }
    }

    fun save() {
        val state = _uiState.value
        if (state.isSaving) {
            return
        }

        val priceCents = parsePriceCents(state.priceText)
        when {
            state.title.isBlank() -> {
                _uiState.update { it.copy(errorMessage = "Title is required.") }
            }

            state.description.isBlank() -> {
                _uiState.update { it.copy(errorMessage = "Description is required.") }
            }

            priceCents == null || priceCents <= 0 -> {
                _uiState.update { it.copy(errorMessage = "Enter a valid price.") }
            }

            else -> {
                viewModelScope.launch {
                    _uiState.update { it.copy(isSaving = true, errorMessage = null) }
                    productRepository.addProduct(
                        title = state.title,
                        description = state.description,
                        priceCents = priceCents,
                        sellerUserId = sessionRepository.currentSession()?.userId,
                    )
                    _uiState.update { it.copy(isSaving = false, isSaved = true) }
                }
            }
        }
    }

    private fun parsePriceCents(priceText: String): Long? {
        val normalized = priceText.trim()
        if (normalized.isBlank()) {
            return null
        }

        val parts = normalized.split(".")
        return when (parts.size) {
            1 -> parts[0].toLongOrNull()?.times(100)
            2 -> {
                val dollars = parts[0].toLongOrNull() ?: return null
                val centsText = parts[1].padEnd(2, '0').take(2)
                val cents = centsText.toLongOrNull() ?: return null
                dollars * 100 + cents
            }

            else -> null
        }
    }
}
