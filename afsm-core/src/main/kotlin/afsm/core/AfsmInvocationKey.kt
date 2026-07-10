package afsm.core

/**
 * Stable identity for one phase-owned command invocation.
 *
 * Keys should be code-owned identifiers, not user or domain data. One key has
 * one owning phase in a machine definition.
 */
@JvmInline
public value class AfsmInvocationKey(public val value: String) {
    init {
        require(value.isNotBlank()) { "Afsm invocation key must not be blank." }
    }

    override fun toString(): String = value
}
