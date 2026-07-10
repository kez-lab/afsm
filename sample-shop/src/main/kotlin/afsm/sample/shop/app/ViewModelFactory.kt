package afsm.sample.shop.app

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

inline fun <reified VM : ViewModel> sampleViewModelFactory(
    crossinline create: () -> VM,
): ViewModelProvider.Factory {
    return object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(VM::class.java)) {
                "Expected ${VM::class.java.name}, got ${modelClass.name}."
            }
            return create() as T
        }
    }
}

inline fun <reified VM : ViewModel> sampleSavedStateViewModelFactory(
    crossinline create: (SavedStateHandle) -> VM,
): ViewModelProvider.Factory {
    return viewModelFactory {
        initializer {
            create(createSavedStateHandle())
        }
    }
}
