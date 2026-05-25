package afsm.test

import afsm.core.AfsmDecision
import afsm.core.AfsmState
import afsm.core.AfsmTransition
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Asserts that this transition changed phase or state.
 */
public fun <S : Any, C : Any, F : Any> AfsmTransition<S, C, F>.assertTransitioned():
    AfsmTransition<S, C, F> {
    assertEquals(AfsmDecision.Transitioned, decision)
    return this
}

/**
 * Asserts that this transition handled an event without changing phase.
 */
public fun <S : Any, C : Any, F : Any> AfsmTransition<S, C, F>.assertHandled():
    AfsmTransition<S, C, F> {
    assertTrue(
        actual = decision is AfsmDecision.Handled,
        message = "Expected AfsmDecision.Handled but was $decision.",
    )
    return this
}

/**
 * Asserts that this transition handled an event without changing phase and used the expected reason.
 */
public fun <S : Any, C : Any, F : Any> AfsmTransition<S, C, F>.assertHandled(
    reason: String?,
): AfsmTransition<S, C, F> {
    assertHandled()
    assertEquals(reason, (decision as AfsmDecision.Handled).reason)
    return this
}

/**
 * Asserts that this transition intentionally ignored an expected no-op event.
 */
public fun <S : Any, C : Any, F : Any> AfsmTransition<S, C, F>.assertIgnored():
    AfsmTransition<S, C, F> {
    assertTrue(
        actual = decision is AfsmDecision.Ignored,
        message = "Expected AfsmDecision.Ignored but was $decision.",
    )
    return this
}

/**
 * Asserts that this transition intentionally ignored an expected no-op event and used the expected reason.
 */
public fun <S : Any, C : Any, F : Any> AfsmTransition<S, C, F>.assertIgnored(
    reason: String?,
): AfsmTransition<S, C, F> {
    assertIgnored()
    assertEquals(reason, (decision as AfsmDecision.Ignored).reason)
    return this
}

/**
 * Asserts that this event was invalid for the current state.
 */
public fun <S : Any, C : Any, F : Any> AfsmTransition<S, C, F>.assertInvalid():
    AfsmTransition<S, C, F> {
    assertTrue(
        actual = decision is AfsmDecision.Invalid,
        message = "Expected AfsmDecision.Invalid but was $decision.",
    )
    return this
}

/**
 * Asserts that this event was invalid for the current state and used the expected reason.
 */
public fun <S : Any, C : Any, F : Any> AfsmTransition<S, C, F>.assertInvalid(
    reason: String?,
): AfsmTransition<S, C, F> {
    assertInvalid()
    assertEquals(reason, (decision as AfsmDecision.Invalid).reason)
    return this
}

/**
 * Asserts the full state value.
 */
public fun <S : Any, C : Any, F : Any> AfsmTransition<S, C, F>.assertState(
    expected: S,
): AfsmTransition<S, C, F> {
    assertEquals(expected, state)
    return this
}

/**
 * Asserts the phase of the standard `AfsmState<Phase, Data>` state shape.
 */
public fun <P : Any, D : Any, C : Any, F : Any> AfsmTransition<AfsmState<P, D>, C, F>.assertPhase(
    expected: P,
): AfsmTransition<AfsmState<P, D>, C, F> {
    assertEquals(expected, state.phase)
    return this
}

/**
 * Asserts the data of the standard `AfsmState<Phase, Data>` state shape.
 */
public fun <P : Any, D : Any, C : Any, F : Any> AfsmTransition<AfsmState<P, D>, C, F>.assertData(
    expected: D,
): AfsmTransition<AfsmState<P, D>, C, F> {
    assertEquals(expected, state.data)
    return this
}

/**
 * Asserts the ordered commands emitted by this transition.
 */
public fun <S : Any, C : Any, F : Any> AfsmTransition<S, C, F>.assertCommands(
    vararg expected: C,
): AfsmTransition<S, C, F> {
    assertEquals(expected.toList(), commands)
    return this
}

/**
 * Asserts that this transition emitted no commands.
 */
public fun <S : Any, C : Any, F : Any> AfsmTransition<S, C, F>.assertNoCommands():
    AfsmTransition<S, C, F> {
    assertEquals(emptyList(), commands)
    return this
}

/**
 * Asserts the ordered effects emitted by this transition.
 */
public fun <S : Any, C : Any, F : Any> AfsmTransition<S, C, F>.assertEffects(
    vararg expected: F,
): AfsmTransition<S, C, F> {
    assertEquals(expected.toList(), effects)
    return this
}

/**
 * Asserts that this transition emitted no effects.
 */
public fun <S : Any, C : Any, F : Any> AfsmTransition<S, C, F>.assertNoEffects():
    AfsmTransition<S, C, F> {
    assertEquals(emptyList(), effects)
    return this
}

/**
 * Asserts that this transition emitted no commands or effects.
 */
public fun <S : Any, C : Any, F : Any> AfsmTransition<S, C, F>.assertNoOutputs():
    AfsmTransition<S, C, F> {
    assertNoCommands()
    assertNoEffects()
    return this
}
