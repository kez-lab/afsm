package afsm.viewmodel

import afsm.core.AfsmGraphReducer
import afsm.core.AfsmReducer
import afsm.runtime.AfsmCommandHandler
import afsm.runtime.AfsmConfig
import afsm.runtime.AfsmHost
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

/**
 * Hosts a graphable Afsm machine in this [ViewModel].
 *
 * Use this overload for the standard `afsmMachine { ... }` path where the
 * machine already owns its initial state and reducer behavior. It keeps feature
 * ViewModels focused on command execution instead of repeating
 * `initialState = machine.initialState, reducer = machine`.
 */
public fun <S : Any, E : Any, C : Any, F : Any> ViewModel.afsmHost(
    machine: AfsmGraphReducer<S, E, C, F>,
    commandHandler: AfsmCommandHandler<C, E> = AfsmCommandHandler.none(),
    config: AfsmConfig = AfsmConfig(),
): AfsmHost<S, E, C, F> {
    return afsmHost(
        initialState = machine.initialState,
        reducer = machine,
        commandHandler = commandHandler,
        config = config,
    )
}

/**
 * Hosts an Afsm reducer with an explicit initial state in this [ViewModel].
 *
 * Use this overload when the initial state is dynamic, for example when it is
 * derived from navigation arguments or a [androidx.lifecycle.SavedStateHandle].
 */
public fun <S : Any, E : Any, C : Any, F : Any> ViewModel.afsmHost(
    initialState: S,
    reducer: AfsmReducer<S, E, C, F>,
    commandHandler: AfsmCommandHandler<C, E> = AfsmCommandHandler.none(),
    config: AfsmConfig = AfsmConfig(),
): AfsmHost<S, E, C, F> {
    return AfsmHost(
        initialState = initialState,
        reducer = reducer,
        commandHandler = commandHandler,
        scope = viewModelScope,
        config = config,
    )
}
