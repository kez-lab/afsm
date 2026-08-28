package io.github.afsm.ide

import kotlin.math.atan2
import kotlin.math.hypot

internal data class AfsmDirectionMarker(
    val point: AfsmDiagramPoint,
    val angle: Double,
)

internal object AfsmTransitionVisuals {
    fun directionMarkers(points: List<AfsmDiagramPoint>): List<AfsmDirectionMarker> {
        val candidates = points.zipWithNext().mapIndexedNotNull { index, (start, end) ->
            val length = hypot(end.x - start.x, end.y - start.y)
            if (length < MIN_MARKER_SEGMENT_LENGTH) return@mapIndexedNotNull null
            MarkerCandidate(
                index = index,
                length = length,
                marker = AfsmDirectionMarker(
                    point = AfsmDiagramPoint((start.x + end.x) / 2.0, (start.y + end.y) / 2.0),
                    angle = atan2(end.y - start.y, end.x - start.x),
                ),
            )
        }
        return candidates
            .sortedByDescending(MarkerCandidate::length)
            .take(MAX_DIRECTION_MARKERS)
            .sortedBy(MarkerCandidate::index)
            .map(MarkerCandidate::marker)
    }

    fun hoverText(transition: AfsmDiagramTransition): String {
        val label = AfsmTransitionLabel.parse(transition.label)
        return buildList {
            add("${transition.from} → ${transition.to}")
            add("Event: ${label.event}")
            label.guard?.let { add("Guard: $it") }
            label.command?.let { add("Command: $it") }
        }.joinToString("\n")
    }

    private data class MarkerCandidate(
        val index: Int,
        val length: Double,
        val marker: AfsmDirectionMarker,
    )

    private const val MIN_MARKER_SEGMENT_LENGTH = 72.0
    private const val MAX_DIRECTION_MARKERS = 2
}
