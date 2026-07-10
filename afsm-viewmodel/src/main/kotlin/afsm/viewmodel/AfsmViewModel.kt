package afsm.viewmodel

import afsm.core.AfsmDefaultMachine
import afsm.core.AfsmMachine
import afsm.core.AfsmReducer
import afsm.runtime.AfsmCommandHandler
import afsm.runtime.AfsmConfig
import afsm.runtime.AfsmHost
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

/**
 * Hosts a graphable Afsm machine in this [ViewModel].
 *
 * Use this overload only when the machine owns a genuine default state. It
 * keeps static feature ViewModels focused on command execution instead of
 * repeating `initialState = machine.initialState, reducer = machine`.
 *
 * If the machine emits commands, pass [commandHandler]. The default command
 * handler is intended only for machines that never emit commands.
 */
public fun <S : Any, E : Any, C : Any, F : Any> ViewModel.afsmHost(
    machine: AfsmDefaultMachine<S, E, C, F>,
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
 * Hosts an Afsm machine with an explicit initial state in this [ViewModel].
 *
 * Use this overload when a graphable machine owns transition behavior and
 * topology, but the starting state comes from Android runtime inputs such as
 * navigation arguments, a deep link, repository restoration, or a
 * [androidx.lifecycle.SavedStateHandle].
 *
 * The supplied [initialState] is the runtime start state. It is mandatory when
 * [machine] has no default and may explicitly override a default machine.
 *
 * If the machine emits commands, pass [commandHandler]. The default command
 * handler is intended only for machines that never emit commands.
 */
public fun <S : Any, E : Any, C : Any, F : Any> ViewModel.afsmHost(
    machine: AfsmMachine<S, E, C, F>,
    initialState: S,
    commandHandler: AfsmCommandHandler<C, E> = AfsmCommandHandler.none(),
    config: AfsmConfig = AfsmConfig(),
): AfsmHost<S, E, C, F> {
    return afsmHost(
        initialState = initialState,
        reducer = machine,
        commandHandler = commandHandler,
        config = config,
    )
}

/**
 * Hosts an Afsm reducer with an explicit initial state in this [ViewModel].
 *
 * Use this overload when the initial state is dynamic, for example when it is
 * derived from navigation arguments or a [androidx.lifecycle.SavedStateHandle],
 * and the reducer intentionally does not expose graph topology.
 *
 * If the reducer emits commands, pass [commandHandler]. The default command
 * handler is intended only for reducers that never emit commands.
 */
public fun <S : Any, E : Any, C : Any, F : Any> ViewModel.afsmHost(
    reducer: AfsmReducer<S, E, C, F>,
    initialState: S,
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
