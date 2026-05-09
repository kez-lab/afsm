package afsm.core

/**
 * Static graph metadata declared by an executable Afsm machine.
 */
public data class AfsmTopology(
    public val states: List<AfsmTopologyState>,
    public val transitions: List<AfsmTopologyTransition>,
)

public data class AfsmTopologyState(
    public val id: String,
)

public data class AfsmTopologyTransition(
    public val from: String,
    public val event: String,
    public val to: String,
)

/**
 * Renders the declared topology as a Mermaid `stateDiagram-v2`.
 */
public fun AfsmTopology.toMermaidStateDiagram(): String {
    return buildString {
        appendLine("stateDiagram-v2")

        states.forEach { state ->
            appendLine("  state ${state.id}")
        }

        transitions.forEach { transition ->
            append("  ")
            append(transition.from)
            append(" --> ")
            append(transition.to)
            append(": ")
            appendLine(transition.event)
        }
    }.trimEnd()
}
