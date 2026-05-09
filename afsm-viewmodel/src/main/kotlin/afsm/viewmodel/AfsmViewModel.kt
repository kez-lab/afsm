package afsm.viewmodel

import afsm.core.AfsmReducer
import afsm.runtime.AfsmCommandHandler
import afsm.runtime.AfsmConfig
import afsm.runtime.AfsmHost
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

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
