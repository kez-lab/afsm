package io.github.afsm.ide

internal data class AfsmDiagramEdgeLane(
    val transition: AfsmDiagramTransition,
    val lane: Double,
    val physicalLane: Double,
)

internal object AfsmDiagramEdgeLanes {
    fun assign(transitions: List<AfsmDiagramTransition>): List<AfsmDiagramEdgeLane> {
        val result = arrayOfNulls<AfsmDiagramEdgeLane>(transitions.size)
        transitions.withIndex()
            .groupBy { indexed -> canonicalPair(indexed.value) }
            .values
            .forEach { related ->
                val midpoint = (related.size - 1) / 2.0
                related.forEachIndexed { laneIndex, indexed ->
                    val transition = indexed.value
                    val physicalLane = laneIndex - midpoint
                    val followsCanonicalDirection = transition.from <= transition.to
                    val directionalLane = when {
                        physicalLane == 0.0 -> 0.0
                        followsCanonicalDirection -> physicalLane
                        else -> -physicalLane
                    }
                    result[indexed.index] = AfsmDiagramEdgeLane(
                        transition = transition,
                        lane = directionalLane,
                        physicalLane = physicalLane,
                    )
                }
            }
        return result.filterNotNull()
    }

    private fun canonicalPair(transition: AfsmDiagramTransition): Pair<String, String> =
        if (transition.from <= transition.to) {
            transition.from to transition.to
        } else {
            transition.to to transition.from
        }
}
