package afsm.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect

/**
 * Collects one-shot Afsm effects while the current lifecycle is at least
 * [minActiveState].
 *
 * Use this from a route-level composable that owns UI behavior such as
 * navigation, snackbar display, permission launchers, or closing a screen.
 * Effects are not durable state; business information that must survive
 * recreation should be represented in state instead.
 *
 * [minActiveState] must not be [Lifecycle.State.INITIALIZED], matching
 * AndroidX `repeatOnLifecycle` requirements.
 */
@Composable
public fun <F : Any> CollectAfsmEffects(
    effects: Flow<F>,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    onEffect: suspend (F) -> Unit,
) {
    require(minActiveState != Lifecycle.State.INITIALIZED) {
        "minActiveState must not be Lifecycle.State.INITIALIZED."
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnEffect by rememberUpdatedState(onEffect)

    LaunchedEffect(
        effects,
        lifecycleOwner,
        minActiveState,
    ) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(minActiveState) {
            effects.collect { effect ->
                currentOnEffect(effect)
            }
        }
    }
}
