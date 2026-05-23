package afsm.core

/**
 * Standard Afsm state shape.
 *
 * [phase] is the finite state node that appears in generated state diagrams.
 * [data] is the extended immutable screen data carried across phases.
 */
public data class AfsmState<out P : Any, out D : Any>(
    public val phase: P,
    public val data: D,
)
