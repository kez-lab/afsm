package io.github.afsm.ide

internal enum class AfsmCanvasMode {
    INSPECT,
    ARRANGE,
}

internal sealed interface AfsmDiagramSelection {
    data class State(val id: String) : AfsmDiagramSelection
    data class Transition(val key: String) : AfsmDiagramSelection
}

internal data class AfsmDiagramSelectionDetails(
    val kind: String,
    val title: String,
    val rows: List<Pair<String, String>>,
)

internal class AfsmGraphInteractionState {
    var mode: AfsmCanvasMode = AfsmCanvasMode.INSPECT
    var selection: AfsmDiagramSelection? = null

    val canMoveElements: Boolean
        get() = mode == AfsmCanvasMode.ARRANGE

    fun showsRouteHandles(transitionKey: String): Boolean =
        mode == AfsmCanvasMode.ARRANGE && selection == AfsmDiagramSelection.Transition(transitionKey)
}

internal object AfsmDiagramKeys {
    fun transition(index: Int, transition: AfsmDiagramTransition): String =
        "$index:${transition.from}→${transition.to}→${transition.label}"
}

internal object AfsmDiagramSearch {
    fun find(diagram: AfsmStateDiagram, query: String): AfsmDiagramSelection? {
        val needle = query.trim()
        if (needle.isEmpty()) return null

        diagram.states.firstOrNull { it.id.contains(needle, ignoreCase = true) }?.let {
            return AfsmDiagramSelection.State(it.id)
        }
        diagram.transitions.withIndex().firstOrNull { (_, transition) ->
            transition.label.contains(needle, ignoreCase = true) ||
                transition.from.contains(needle, ignoreCase = true) ||
                transition.to.contains(needle, ignoreCase = true)
        }?.let { indexed ->
            return AfsmDiagramSelection.Transition(AfsmDiagramKeys.transition(indexed.index, indexed.value))
        }
        return null
    }
}

internal data class AfsmTransitionLabel(
    val event: String,
    val guard: String?,
    val command: String?,
) {
    companion object {
        fun parse(value: String): AfsmTransitionLabel {
            val match = PATTERN.matchEntire(value.trim())
            if (match == null) return AfsmTransitionLabel(value.trim(), null, null)
            return AfsmTransitionLabel(
                event = match.groupValues[1].trim(),
                guard = match.groupValues[2].trim().ifEmpty { null },
                command = match.groupValues[3].trim().ifEmpty { null },
            )
        }

        private val PATTERN = Regex("^(.+?)(?:\\s+\\[(.+)])?(?:\\s+/\\s+(.+))?$")
    }
}
