package afsm.sample.shop.feature.catalog

import afsm.sample.shop.core.data.ProductRepository
import afsm.sample.shop.core.data.SessionRepository
import afsm.sample.shop.core.model.Product
import afsm.sample.shop.core.model.UserSession
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CatalogUiState(
    val session: UserSession? = null,
    val products: List<Product> = emptyList(),
)

class CatalogViewModel(
    private val productRepository: ProductRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {
    val uiState: StateFlow<CatalogUiState> = combine(
        sessionRepository.session,
        productRepository.observeProducts(),
    ) { session, products ->
        CatalogUiState(
            session = session,
            products = products,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CatalogUiState(
            session = sessionRepository.currentSession(),
        ),
    )

    init {
        viewModelScope.launch {
            productRepository.ensureSeedProducts()
        }
    }

    fun logout() {
        sessionRepository.clearSession()
    }
}
