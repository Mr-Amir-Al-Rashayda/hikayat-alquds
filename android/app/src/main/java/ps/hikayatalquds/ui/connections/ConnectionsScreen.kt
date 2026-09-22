package ps.hikayatalquds.ui.connections

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ps.hikayatalquds.R
import ps.hikayatalquds.core.designsystem.HikayatTheme
import ps.hikayatalquds.core.designsystem.component.HikayatCard
import ps.hikayatalquds.core.designsystem.component.MetaTag
import ps.hikayatalquds.core.designsystem.component.OliveHeader
import ps.hikayatalquds.core.ui.LocalAppLanguage
import ps.hikayatalquds.core.ui.TestTags
import ps.hikayatalquds.ui.HikayatAppState
import kotlin.math.cos
import kotlin.math.sin

/**
 * The archive drawn as a web rather than a list.
 *
 * Places sit on the outer ring, the themes they share in the middle, and every
 * published memory hangs off the place it belongs to. Each line is a real
 * relationship in the data - a shared category, or a contribution attached to a
 * site - so the shape of the picture says something true: where the archive is
 * dense, and where it is thin.
 *
 * The layout is deterministic, spaced evenly by index, which keeps it stable
 * between frames and avoids a physics simulation for a graph this small.
 */
@Composable
fun ConnectionsScreen(
    appState: HikayatAppState,
    contentPadding: PaddingValues,
    onOpenLocation: (String) -> Unit,
    viewModel: ConnectionsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val language = LocalAppLanguage.current
    val colors = HikayatTheme.colors

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = 30.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item("header") {
            OliveHeader(
                title = stringResource(R.string.connections_title),
                eyebrow = stringResource(R.string.track_badge),
                subtitle = stringResource(R.string.connections_intro),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        item("graph") {
            HikayatCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                ConstellationCanvas(
                    graph = state.graph,
                    selectedId = state.selectedNodeId,
                    onSelect = viewModel::select,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .testTag(TestTags.CONSTELLATION_CANVAS),
                )
            }
        }

        item("legend") {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MetaTag(
                    label = stringResource(R.string.home_all_places),
                    icon = Icons.Filled.Place,
                    color = MaterialTheme.colorScheme.primary,
                )
                MetaTag(
                    label = stringResource(R.string.connections_theme),
                    icon = Icons.Filled.Hub,
                    color = colors.souq,
                )
                MetaTag(
                    label = stringResource(R.string.connections_memory),
                    icon = Icons.Filled.FormatQuote,
                    color = colors.amber,
                )
            }
        }

        val selected = state.selectedNode
        if (selected != null) {
            item("selected") {
                HikayatCard(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    accent = colors.amber,
                ) {
                    Column(Modifier.padding(16.dp)) {
                        MetaTag(
                            label = when (selected.kind) {
                                NodeKind.LOCATION -> stringResource(R.string.search_result_place)
                                NodeKind.THEME -> stringResource(R.string.connections_theme)
                                NodeKind.MEMORY -> stringResource(R.string.connections_memory)
                            },
                            color = colors.muted,
                        )
                        Spacer(Modifier.height(9.dp))
                        Text(
                            text = selected.label,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        if (selected.detail.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = selected.detail,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        selected.locationId?.let { locationId ->
                            Spacer(Modifier.height(14.dp))
                            Button(
                                onClick = { onOpenLocation(locationId) },
                                shape = CircleShape,
                            ) {
                                Text(
                                    text = stringResource(R.string.connections_open_place),
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }
                    }
                }
            }
        }

        item("counts") {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MetaTag(
                    label = "${state.graph.locationCount} ${stringResource(R.string.keepsake_quarters)}",
                    color = colors.muted,
                )
                MetaTag(
                    label = "${state.graph.themeCount} ${stringResource(R.string.connections_theme)}",
                    color = colors.muted,
                )
                MetaTag(
                    label = "${state.graph.memoryCount} ${stringResource(R.string.stat_memories)}",
                    color = colors.muted,
                )
            }
        }
    }
}

@Composable
private fun ConstellationCanvas(
    graph: ConstellationGraph,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HikayatTheme.colors
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val themeColour = colors.souq
    val memoryColour = colors.amber
    val locationColour = MaterialTheme.colorScheme.primary
    val lineColour = colors.hairline
    val labelColour = MaterialTheme.colorScheme.onSurface

    BoxWithConstraints(modifier = modifier.background(colors.pageBackground)) {
        val side = with(density) { minOf(maxWidth, maxHeight).toPx() }
        val hitRadius = with(density) { 22.dp.toPx() }

        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(graph, side) {
                    detectTapGestures { tap ->
                        val nearest = graph.nodes.minByOrNull { node ->
                            (tap - node.position(side)).getDistance()
                        }
                        onSelect(
                            nearest?.takeIf {
                                (tap - it.position(side)).getDistance() <= hitRadius
                            }?.id,
                        )
                    }
                },
        ) {
            Canvas(Modifier.fillMaxSize()) {
                graph.edges.forEach { edge ->
                    val from = graph.byId[edge.fromId] ?: return@forEach
                    val to = graph.byId[edge.toId] ?: return@forEach
                    val touchesSelection =
                        selectedId != null && (edge.fromId == selectedId || edge.toId == selectedId)
                    drawLine(
                        color = if (touchesSelection) memoryColour else lineColour,
                        start = from.position(side),
                        end = to.position(side),
                        strokeWidth = if (touchesSelection) 2.2f else 1.1f,
                    )
                }

                graph.nodes.forEach { node ->
                    val centre = node.position(side)
                    val selected = node.id == selectedId
                    val accent = when (node.kind) {
                        NodeKind.LOCATION -> locationColour
                        NodeKind.THEME -> themeColour
                        NodeKind.MEMORY -> memoryColour
                    }
                    val radius = when (node.kind) {
                        NodeKind.LOCATION -> if (selected) 13f else 10f
                        NodeKind.THEME -> 8f
                        NodeKind.MEMORY -> 6f
                    }
                    if (selected) {
                        drawCircle(accent.copy(alpha = 0.18f), radius * 2.4f, centre)
                    }
                    drawCircle(Color.White, radius + 2f, centre)
                    drawCircle(accent, radius, centre)

                    if (node.kind != NodeKind.MEMORY || selected) {
                        val layout = measurer.measure(
                            text = node.shortLabel,
                            style = TextStyle(
                                fontSize = if (node.kind == NodeKind.LOCATION) 9.sp else 8.sp,
                                color = labelColour,
                            ),
                        )
                        drawText(
                            textLayoutResult = layout,
                            topLeft = centre + Offset(
                                -layout.size.width / 2f,
                                radius + 4f,
                            ),
                        )
                    }
                }
            }
        }
    }
}

/** Polar layout: the ring radius depends only on what kind of node this is. */
private fun ConstellationNode.position(side: Float): Offset {
    val centre = Offset(side / 2f, side / 2f)
    val radius = side * when (kind) {
        NodeKind.THEME -> 0.13f
        NodeKind.LOCATION -> 0.30f
        NodeKind.MEMORY -> 0.42f
    }
    return centre + Offset(cos(angle).toFloat() * radius, sin(angle).toFloat() * radius)
}
