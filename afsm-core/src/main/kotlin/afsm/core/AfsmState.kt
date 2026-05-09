package afsm.core

/**
 * Standard Afsm state shape.
 *
 * [phase] is the finite state node that appears in generated state diagrams.
 * [context] is the extended immutable data carried across phases.
 */
public data class AfsmState<out P : Any, out X : Any>(
    public val phase: P,
    public val context: X,
)
