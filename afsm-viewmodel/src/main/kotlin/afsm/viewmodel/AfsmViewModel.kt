package afsm.viewmodel

import afsm.core.AfsmStateMachine
import afsm.runtime.AfsmCommandHandler
import afsm.runtime.AfsmConfig
import afsm.runtime.AfsmHost
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

public fun <S : Any, E : Any, C : Any, F : Any> ViewModel.afsmHost(
    initialState: S,
    stateMachine: AfsmStateMachine<S, E, C, F>,
    commandHandler: AfsmCommandHandler<C, E> = AfsmCommandHandler.none(),
    config: AfsmConfig = AfsmConfig(),
): AfsmHost<S, E, C, F> {
    return AfsmHost(
        initialState = initialState,
        stateMachine = stateMachine,
        commandHandler = commandHandler,
        scope = viewModelScope,
        config = config,
    )
}
