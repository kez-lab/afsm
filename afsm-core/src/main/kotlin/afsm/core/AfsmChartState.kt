package afsm.core

/**
 * Runtime state carried by an executable Afsm statechart.
 *
 * [phase] is the finite statechart node. [context] is the extended state that
 * stores data shared across phases, such as form input, ids, and retry counts.
 */
public data class AfsmChartState<out P : Any, out X : Any>(
    public val phase: P,
    public val context: X,
)
