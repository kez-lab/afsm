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
    public val label: String = id,
    public val parentId: String? = null,
)

public enum class AfsmTopologyTransitionKind {
    External,
    Internal
}

public data class AfsmTopologyTransition(
    public val from: String,
    public val event: String,
    public val to: String,
    public val guardLabel: String? = null,
    public val commandLabels: List<String> = emptyList(),
    public val effectLabels: List<String> = emptyList(),
    public val kind: AfsmTopologyTransitionKind = AfsmTopologyTransitionKind.External,
    public val isFallback: Boolean = false,
)

/**
 * Renders the declared topology as Mermaid `.mmd` source.
 */
public fun AfsmTopology.toMmd(): String {
    return buildString {
        appendLine("stateDiagram-v2")

        states.forEach { state ->
            if (state.label == state.id) {
                appendLine("  state ${state.id}")
            } else {
                appendLine("  state ${quoteMmdLabel(state.label)} as ${state.id}")
            }
        }

        transitions.forEach { transition ->
            append("  ")
            append(transition.from)
            append(" --> ")
            append(transition.to)
            append(": ")
            appendLine(transition.mmdLabel())
        }
    }.trimEnd()
}

private fun AfsmTopologyTransition.mmdLabel(): String {
    val parts = mutableListOf(event)
    guardLabel?.let { label ->
        parts += "[$label]"
    }
    if (commandLabels.isNotEmpty()) {
        parts += "/ ${commandLabels.joinToString(", ")}"
    }
    if (effectLabels.isNotEmpty()) {
        parts += "! ${effectLabels.joinToString(", ")}"
    }
    return parts.joinToString(" ")
}

private fun quoteMmdLabel(label: String): String {
    return "\"${label.replace("\"", "\\\"")}\""
}
