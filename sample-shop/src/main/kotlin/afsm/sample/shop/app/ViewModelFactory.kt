package afsm.sample.shop.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

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
