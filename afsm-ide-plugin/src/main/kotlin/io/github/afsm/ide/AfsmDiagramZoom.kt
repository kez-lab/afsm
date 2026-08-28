package io.github.afsm.ide

import kotlin.math.min

internal object AfsmDiagramZoom {
    const val MIN = 0.35
    const val MAX = 2.5
    private const val FIT_PADDING = 0.9

    fun clamp(value: Double): Double = value.coerceIn(MIN, MAX)

    fun fit(viewportWidth: Int, viewportHeight: Int, contentWidth: Int, contentHeight: Int): Double {
        if (viewportWidth <= 0 || viewportHeight <= 0 || contentWidth <= 0 || contentHeight <= 0) return 1.0
        return clamp(
            min(
                1.0,
                min(
                    viewportWidth.toDouble() / contentWidth,
                    viewportHeight.toDouble() / contentHeight,
                ) * FIT_PADDING,
            ),
        )
    }
}
