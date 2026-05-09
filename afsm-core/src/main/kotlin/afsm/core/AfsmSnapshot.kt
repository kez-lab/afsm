package afsm.core

/**
 * Runtime value carried by an executable Afsm machine.
 *
 * [phase] is the finite statechart node. [context] is the extended state that
 * stores data shared across phases, such as form input, ids, and retry counts.
 */
public data class AfsmSnapshot<out P : Any, out X : Any>(
    public val phase: P,
    public val context: X,
)
