package afsm.core

/**
 * Static graph metadata declared by an executable Afsm machine.
 */
public data class AfsmTopology(
    public val states: List<AfsmTopologyState>,
    public val transitions: List<AfsmTopologyTransition>,
    public val initialStateId: String? = null,
)

public data class AfsmTopologyState(
    public val id: String,
    public val label: String = id,
    public val parentId: String? = null,
    public val entryCommandLabels: List<String> = emptyList(),
    public val entryEffectLabels: List<String> = emptyList(),
    public val exitCommandLabels: List<String> = emptyList(),
    public val exitEffectLabels: List<String> = emptyList(),
)

public enum class AfsmTopologyTransitionKind {
    External,
    Internal
}

public data class AfsmTopologyTransition(
    public val from: String,
    public val event: String,
    public val to: String,
    public val conditionLabel: String? = null,
    public val commandLabels: List<String> = emptyList(),
    public val effectLabels: List<String> = emptyList(),
    public val kind: AfsmTopologyTransitionKind = AfsmTopologyTransitionKind.External,
    public val isFallback: Boolean = false,
)

public data class AfsmMmdOptions(
    public val includeInitialState: Boolean = true,
    public val includeInternalTransitions: Boolean = false,
    public val includeFallbackTransitions: Boolean = true,
    public val includeCommandOnlyInternalTransitions: Boolean = true,
    public val includeEffectOnlyInternalTransitions: Boolean = true,
) {
    public companion object {
        /**
         * Flow-oriented diagram for README/docs/reviews. Text-field self-loops
         * are hidden unless they are named condition, fallback, command, or
         * effect branches.
         */
        public val Flow: AfsmMmdOptions = AfsmMmdOptions()

        /**
         * Complete topology diagram for debugging and audits.
         */
        public val Full: AfsmMmdOptions = AfsmMmdOptions(
            includeInternalTransitions = true,
        )
    }
}

/**
 * Renders the declared topology as Mermaid `.mmd` source.
 */
public fun AfsmTopology.toMmd(
    options: AfsmMmdOptions = AfsmMmdOptions.Flow,
): String {
    return buildString {
        appendLine("stateDiagram-v2")

        if (options.includeInitialState) {
            initialStateId?.let { initial ->
                appendLine("  [*] --> $initial")
            }
        }

        states.forEach { state ->
            if (state.label == state.id) {
                appendLine("  state ${state.id}")
            } else {
                appendLine("  state ${quoteMmdLabel(state.label)} as ${state.id}")
            }
            val notes = state.mmdNotes()
            if (notes.isNotEmpty()) {
                appendLine("  note right of ${state.id}")
                notes.forEach { note ->
                    appendLine("    $note")
                }
                appendLine("  end note")
            }
        }

        transitions.filter { transition -> transition.shouldRender(options) }
            .forEach { transition ->
                append("  ")
                append(transition.from)
                append(" --> ")
                append(transition.to)
                append(": ")
                appendLine(transition.mmdLabel())
            }
    }.trimEnd()
}

private fun AfsmTopologyTransition.shouldRender(options: AfsmMmdOptions): Boolean {
    if (kind == AfsmTopologyTransitionKind.External) {
        return true
    }
    if (isFallback) {
        return options.includeFallbackTransitions
    }
    if (conditionLabel != null) {
        return true
    }
    if (commandLabels.isNotEmpty()) {
        return options.includeCommandOnlyInternalTransitions
    }
    if (effectLabels.isNotEmpty()) {
        return options.includeEffectOnlyInternalTransitions
    }
    return options.includeInternalTransitions
}

private fun AfsmTopologyTransition.mmdLabel(): String {
    val parts = mutableListOf(event)
    conditionLabel?.let { label ->
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

private fun AfsmTopologyState.mmdNotes(): List<String> {
    return buildList {
        if (entryCommandLabels.isNotEmpty()) {
            add("entry / ${entryCommandLabels.joinToString(", ")}")
        }
        if (entryEffectLabels.isNotEmpty()) {
            add("entry ! ${entryEffectLabels.joinToString(", ")}")
        }
        if (exitCommandLabels.isNotEmpty()) {
            add("exit / ${exitCommandLabels.joinToString(", ")}")
        }
        if (exitEffectLabels.isNotEmpty()) {
            add("exit ! ${exitEffectLabels.joinToString(", ")}")
        }
    }
}

private fun quoteMmdLabel(label: String): String {
    return "\"${label.replace("\"", "\\\"")}\""
}
