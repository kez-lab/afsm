package io.github.afsm.ide

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project
import java.util.Base64

internal data class AfsmDiagramPoint(val x: Double, val y: Double)

internal data class AfsmDiagramRoute(
    val points: List<AfsmDiagramPoint>,
    val label: AfsmDiagramPoint? = null,
)

internal data class AfsmDiagramLayout(
    val nodes: Map<String, AfsmDiagramPoint> = emptyMap(),
    val controls: Map<String, AfsmDiagramPoint> = emptyMap(),
    val routes: Map<String, AfsmDiagramRoute> = emptyMap(),
)

internal object AfsmDiagramLayoutCodec {
    fun encode(layout: AfsmDiagramLayout): String = listOf(
        encodeMap(layout.nodes),
        encodeMap(layout.controls),
        encodeRoutes(layout.routes),
    ).joinToString("|")

    fun decode(value: String?): AfsmDiagramLayout {
        val sections = value.orEmpty().split('|', limit = 3)
        return AfsmDiagramLayout(
            nodes = decodeMap(sections.getOrNull(0)),
            controls = decodeMap(sections.getOrNull(1)),
            routes = decodeRoutes(sections.getOrNull(2)),
        )
    }

    private fun encodeMap(points: Map<String, AfsmDiagramPoint>): String = points.entries.joinToString(";") { (key, point) ->
        "${Base64.getUrlEncoder().withoutPadding().encodeToString(key.toByteArray())},${point.x},${point.y}"
    }

    private fun decodeMap(section: String?): Map<String, AfsmDiagramPoint> = section.orEmpty()
        .split(';')
        .mapNotNull { entry ->
            val values = entry.split(',', limit = 3)
            if (values.size != 3) return@mapNotNull null
            runCatching {
                Base64.getUrlDecoder().decode(values[0]).decodeToString() to
                    AfsmDiagramPoint(values[1].toDouble(), values[2].toDouble())
            }.getOrNull()
        }.toMap()

    private fun encodeRoutes(routes: Map<String, AfsmDiagramRoute>): String = routes.entries.joinToString(";") { (key, route) ->
        val points = route.points.joinToString("~") { point -> "${point.x}:${point.y}" }
        val label = route.label?.let { "${it.x}:${it.y}" }.orEmpty()
        "${encodeKey(key)},$points,$label"
    }

    private fun decodeRoutes(section: String?): Map<String, AfsmDiagramRoute> = section.orEmpty()
        .split(';')
        .mapNotNull { entry ->
            val values = entry.split(',', limit = 3)
            if (values.size != 3) return@mapNotNull null
            runCatching {
                val points = values[1].split('~').filter(String::isNotEmpty).map(::decodePoint)
                val label = values[2].takeIf(String::isNotEmpty)?.let(::decodePoint)
                decodeKey(values[0]) to AfsmDiagramRoute(points, label)
            }.getOrNull()
        }.toMap()

    private fun decodePoint(value: String): AfsmDiagramPoint {
        val coordinates = value.split(':', limit = 2)
        require(coordinates.size == 2)
        return AfsmDiagramPoint(coordinates[0].toDouble(), coordinates[1].toDouble())
    }

    private fun encodeKey(key: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(key.toByteArray())

    private fun decodeKey(value: String): String = Base64.getUrlDecoder().decode(value).decodeToString()
}

internal class AfsmDiagramLayoutStore(project: Project) {
    private val properties = PropertiesComponent.getInstance(project)

    fun load(graphKey: String): AfsmDiagramLayout =
        AfsmDiagramLayoutCodec.decode(properties.getValue(propertyKey(graphKey)))

    fun save(graphKey: String, layout: AfsmDiagramLayout) {
        properties.setValue(propertyKey(graphKey), AfsmDiagramLayoutCodec.encode(layout))
    }

    fun clear(graphKey: String) {
        properties.unsetValue(propertyKey(graphKey))
    }

    private fun propertyKey(graphKey: String): String = "afsm.graph.preview.layout.${graphKey.hashCode()}"
}
