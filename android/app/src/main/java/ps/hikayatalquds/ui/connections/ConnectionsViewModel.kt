package ps.hikayatalquds.ui.connections

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import ps.hikayatalquds.data.preferences.UserPreferencesRepository
import ps.hikayatalquds.data.repository.ContentRepository
import ps.hikayatalquds.domain.model.AppLanguage
import ps.hikayatalquds.domain.model.Location
import ps.hikayatalquds.domain.model.Memory
import javax.inject.Inject
import kotlin.math.PI

enum class NodeKind { LOCATION, THEME, MEMORY }

/** One node with its ring angle already decided, so drawing is pure. */
@Immutable
data class ConstellationNode(
    val id: String,
    val kind: NodeKind,
    val label: String,
    val detail: String,
    /** Radians, measured from twelve o'clock. */
    val angle: Double,
    val locationId: String?,
) {
    /** Labels are drawn on a canvas with no wrapping, so they are trimmed here. */
    val shortLabel: String
        get() = if (label.length <= MAX_LABEL) label else label.take(MAX_LABEL - 1) + "…"

    private companion object {
        const val MAX_LABEL = 18
    }
}

@Immutable
data class ConstellationEdge(val fromId: String, val toId: String, val isMemory: Boolean)

@Immutable
data class ConstellationGraph(
    val nodes: List<ConstellationNode> = emptyList(),
    val edges: List<ConstellationEdge> = emptyList(),
) {
    val byId: Map<String, ConstellationNode> = nodes.associateBy { it.id }
    val locationCount: Int get() = nodes.count { it.kind == NodeKind.LOCATION }
    val themeCount: Int get() = nodes.count { it.kind == NodeKind.THEME }
    val memoryCount: Int get() = nodes.count { it.kind == NodeKind.MEMORY }
}

@Immutable
data class ConnectionsUiState(
    val graph: ConstellationGraph = ConstellationGraph(),
    val selectedNodeId: String? = null,
) {
    val selectedNode: ConstellationNode?
        get() = selectedNodeId?.let { graph.byId[it] }
}

@HiltViewModel
class ConnectionsViewModel @Inject constructor(
    content: ContentRepository,
    preferences: UserPreferencesRepository,
) : ViewModel() {

    private val selected = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ConnectionsUiState> = combine(
        content.locations,
        content.allMemories,
        preferences.settings,
        selected,
    ) { locations, memories, settings, selectedId ->
        ConnectionsUiState(
            graph = buildGraph(locations, memories, settings.language),
            selectedNodeId = selectedId,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ConnectionsUiState(),
    )

    fun select(nodeId: String?) {
        selected.value = if (selected.value == nodeId) null else nodeId
    }

    companion object {
        /**
         * Builds the graph from real relationships only.
         *
         * A place is joined to every theme it carries, and a memory to the place
         * it was contributed about. Nothing is joined for visual balance - if a
         * corner of the picture looks empty, that corner of the archive is empty,
         * which is worth being able to see.
         */
        internal fun buildGraph(
            locations: List<Location>,
            memories: List<Memory>,
            language: AppLanguage,
        ): ConstellationGraph {
            if (locations.isEmpty()) return ConstellationGraph()

            val nodes = mutableListOf<ConstellationNode>()
            val edges = mutableListOf<ConstellationEdge>()

            val themes = locations.flatMap { it.categoryLabels }.distinct().sorted()
            themes.forEachIndexed { index, theme ->
                nodes += ConstellationNode(
                    id = "theme:$theme",
                    kind = NodeKind.THEME,
                    label = theme,
                    detail = locations
                        .filter { theme in it.categoryLabels }
                        .joinToString(if (language.isArabic) "، " else ", ") { it.name[language] },
                    angle = ringAngle(index, themes.size),
                    locationId = null,
                )
            }

            locations.forEachIndexed { index, location ->
                val angle = ringAngle(index, locations.size)
                nodes += ConstellationNode(
                    id = "location:${location.id}",
                    kind = NodeKind.LOCATION,
                    label = location.name[language],
                    detail = location.description[language],
                    angle = angle,
                    locationId = location.id,
                )
                location.categoryLabels.forEach { theme ->
                    edges += ConstellationEdge(
                        fromId = "location:${location.id}",
                        toId = "theme:$theme",
                        isMemory = false,
                    )
                }

                // Memories fan outwards from the place they belong to.
                val attached = memories.filter { it.locationId == location.id }
                attached.forEachIndexed { memoryIndex, memory ->
                    val spread = (memoryIndex - (attached.size - 1) / 2.0) * (PI / 9)
                    nodes += ConstellationNode(
                        id = "memory:${memory.id}",
                        kind = NodeKind.MEMORY,
                        label = memory.title,
                        detail = memory.content,
                        angle = angle + spread,
                        locationId = location.id,
                    )
                    edges += ConstellationEdge(
                        fromId = "location:${location.id}",
                        toId = "memory:${memory.id}",
                        isMemory = true,
                    )
                }
            }

            return ConstellationGraph(nodes = nodes, edges = edges)
        }

        private fun ringAngle(index: Int, total: Int): Double =
            (index.toDouble() / maxOf(total, 1)) * 2 * PI - PI / 2
    }
}
