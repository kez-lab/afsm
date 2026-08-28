package io.github.afsm.ide

internal data class AfsmStateDiagram(
    val initialStateId: String?,
    val states: List<AfsmDiagramState>,
    val transitions: List<AfsmDiagramTransition>,
)

internal data class AfsmDiagramState(
    val id: String,
    val notes: List<String> = emptyList(),
)

internal data class AfsmDiagramTransition(
    val from: String,
    val to: String,
    val label: String,
)

/** Parses only the stable Mermaid dialect emitted by AfsmMmdWriter. */
internal object AfsmMmdDiagramParser {
    fun parse(source: String): AfsmStateDiagram {
        val lines = source.lineSequence().map(String::trim).toList()
        require(lines.firstOrNull() == HEADER) {
            "This file is not an Afsm-generated state diagram."
        }

        val states = linkedMapOf<String, MutableList<String>>()
        val transitions = mutableListOf<AfsmDiagramTransition>()
        var initialStateId: String? = null
        var notedStateId: String? = null

        lines.drop(1).forEach { line ->
            when {
                line.isBlank() -> Unit
                line == NOTE_END -> notedStateId = null
                notedStateId != null -> {
                    val stateId = requireNotNull(notedStateId)
                    states.getOrPut(stateId) { mutableListOf() }.add(line)
                }
                line.startsWith(INITIAL_PREFIX) -> {
                    val stateId = line.removePrefix(INITIAL_PREFIX).trim()
                    initialStateId = stateId
                    states.getOrPut(stateId) { mutableListOf() }
                }
                line.startsWith(STATE_PREFIX) -> {
                    states.getOrPut(line.removePrefix(STATE_PREFIX).trim()) { mutableListOf() }
                }
                line.startsWith(NOTE_PREFIX) -> {
                    val stateId = line.removePrefix(NOTE_PREFIX).trim()
                    notedStateId = stateId
                    states.getOrPut(stateId) { mutableListOf() }
                }
                TRANSITION.matches(line) -> {
                    val match = requireNotNull(TRANSITION.matchEntire(line))
                    val from = match.groupValues[1].trim()
                    val to = match.groupValues[2].trim()
                    transitions += AfsmDiagramTransition(from, to, match.groupValues[3].trim())
                    states.getOrPut(from) { mutableListOf() }
                    states.getOrPut(to) { mutableListOf() }
                }
            }
        }

        return AfsmStateDiagram(
            initialStateId = initialStateId,
            states = states.map { (id, notes) -> AfsmDiagramState(id, notes) },
            transitions = transitions,
        )
    }

    private const val HEADER = "stateDiagram-v2"
    private const val INITIAL_PREFIX = "[*] -->"
    private const val STATE_PREFIX = "state "
    private const val NOTE_PREFIX = "note right of "
    private const val NOTE_END = "end note"
    private val TRANSITION = Regex("^(.+?)\\s+-->\\s+(.+?):\\s*(.+)$")
}
