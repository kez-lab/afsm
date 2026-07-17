package afsm.test

import afsm.core.AfsmDecision
import afsm.core.AfsmCommandInvocation
import afsm.core.AfsmState
import afsm.core.AfsmTransition
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Asserts that this transition changed phase or state.
 */
public fun <S : Any, C : Any> AfsmTransition<S, C>.assertTransitioned():
    AfsmTransition<S, C> {
    assertEquals(AfsmDecision.Transitioned, decision)
    return this
}

/**
 * Asserts that this transition handled an event without changing phase.
 */
public fun <S : Any, C : Any> AfsmTransition<S, C>.assertHandled():
    AfsmTransition<S, C> {
    assertTrue(
        actual = decision is AfsmDecision.Handled,
        message = "Expected AfsmDecision.Handled but was $decision.",
    )
    return this
}

/**
 * Asserts that this transition handled an event without changing phase and used
 * the expected reason.
 */
public fun <S : Any, C : Any> AfsmTransition<S, C>.assertHandled(
    reason: String?,
): AfsmTransition<S, C> {
    assertHandled()
    assertEquals(reason, (decision as AfsmDecision.Handled).reason)
    return this
}

/**
 * Asserts that this transition intentionally ignored an expected no-op event.
 */
public fun <S : Any, C : Any> AfsmTransition<S, C>.assertIgnored():
    AfsmTransition<S, C> {
    assertTrue(
        actual = decision is AfsmDecision.Ignored,
        message = "Expected AfsmDecision.Ignored but was $decision.",
    )
    return this
}

/**
 * Asserts that this transition intentionally ignored an expected no-op event
 * and used the expected reason.
 */
public fun <S : Any, C : Any> AfsmTransition<S, C>.assertIgnored(
    reason: String?,
): AfsmTransition<S, C> {
    assertIgnored()
    assertEquals(reason, (decision as AfsmDecision.Ignored).reason)
    return this
}

/**
 * Asserts that this event was invalid for the current state.
 */
public fun <S : Any, C : Any> AfsmTransition<S, C>.assertInvalid():
    AfsmTransition<S, C> {
    assertTrue(
        actual = decision is AfsmDecision.Invalid,
        message = "Expected AfsmDecision.Invalid but was $decision.",
    )
    return this
}

/**
 * Asserts that this event was invalid for the current state and used the
 * expected reason.
 */
public fun <S : Any, C : Any> AfsmTransition<S, C>.assertInvalid(
    reason: String?,
): AfsmTransition<S, C> {
    assertInvalid()
    assertEquals(reason, (decision as AfsmDecision.Invalid).reason)
    return this
}

/**
 * Asserts the full state value.
 */
public fun <S : Any, C : Any> AfsmTransition<S, C>.assertState(
    expected: S,
): AfsmTransition<S, C> {
    assertEquals(expected, state)
    return this
}

/**
 * Asserts the phase of the standard `AfsmState<Phase, Data>` state shape.
 */
public fun <P : Any, D : Any, C : Any> AfsmTransition<AfsmState<P, D>, C>.assertPhase(
    expected: P,
): AfsmTransition<AfsmState<P, D>, C> {
    assertEquals(expected, state.phase)
    return this
}

/**
 * Asserts the data of the standard `AfsmState<Phase, Data>` state shape.
 */
public fun <P : Any, D : Any, C : Any> AfsmTransition<AfsmState<P, D>, C>.assertData(
    expected: D,
): AfsmTransition<AfsmState<P, D>, C> {
    assertEquals(expected, state.data)
    return this
}

/**
 * Asserts the ordered commands emitted by this transition.
 */
public fun <S : Any, C : Any> AfsmTransition<S, C>.assertCommands(
    vararg expected: C,
): AfsmTransition<S, C> {
    assertEquals(expected.toList(), commands)
    return this
}

/**
 * Asserts that this transition emitted no commands.
 */
public fun <S : Any, C : Any> AfsmTransition<S, C>.assertNoCommands():
    AfsmTransition<S, C> {
    assertEquals(emptyList(), commands)
    return this
}

/**
 * Asserts the ordered phase-owned command invocation operations.
 */
public fun <S : Any, C : Any> AfsmTransition<S, C>.assertCommandInvocations(
    vararg expected: AfsmCommandInvocation<C>,
): AfsmTransition<S, C> {
    assertEquals(expected.toList(), commandInvocations)
    return this
}

/**
 * Asserts that this transition emitted no phase-owned command invocation work.
 */
public fun <S : Any, C : Any> AfsmTransition<S, C>.assertNoCommandInvocations():
    AfsmTransition<S, C> {
    assertEquals(emptyList(), commandInvocations)
    return this
}

/**
 * Asserts that this transition emitted no command work.
 */
public fun <S : Any, C : Any> AfsmTransition<S, C>.assertNoOutputs():
    AfsmTransition<S, C> {
    assertNoCommands()
    assertNoCommandInvocations()
    return this
}
