package ps.hikayatalquds.ui.map

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.LabelOff
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ps.hikayatalquds.R
import ps.hikayatalquds.core.designsystem.HikayatTheme
import ps.hikayatalquds.core.designsystem.component.ChoiceChip
import ps.hikayatalquds.core.designsystem.component.HikayatCard
import ps.hikayatalquds.core.designsystem.component.MetaTag
import ps.hikayatalquds.core.designsystem.component.OliveHeader
import ps.hikayatalquds.core.designsystem.component.SectionHeader
import ps.hikayatalquds.core.ui.LocalAppLanguage
import ps.hikayatalquds.core.ui.TestTags
import ps.hikayatalquds.domain.model.LocationCategory
import ps.hikayatalquds.ui.HikayatAppState
import ps.hikayatalquds.ui.components.accent
import ps.hikayatalquds.ui.util.formatDistance
import ps.hikayatalquds.ui.util.openWalkingDirections

/**
 * The map screen: the drawing, a legend, category filters, and the optional
 * "am I near one of these places?" check.
 */
@Composable
fun MapScreen(
    appState: HikayatAppState,
    contentPadding: PaddingValues,
    onOpenLocation: (String) -> Unit,
    onOpenPlan: () -> Unit,
    viewModel: MapViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val language = LocalAppLanguage.current
    val colors = HikayatTheme.colors
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) viewModel.findNearest() else viewModel.onLocationPermissionDenied(false)
    }

    val categoryColors = LocationCategory.entries.associateWith { it.accent() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(top = contentPadding.calculateTopPadding()),
    ) {
        OliveHeader(
            title = stringResource(R.string.map_title),
            eyebrow = stringResource(R.string.track_badge),
            subtitle = if (state.itinerary != null) stringResource(R.string.map_route_shown) else null,
            modifier = Modifier.padding(16.dp),
        )

        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(onClick = viewModel::toggleLabels, shape = CircleShape) {
                Icon(
                    imageVector = if (state.showLabels) Icons.Filled.LabelOff else Icons.Filled.Label,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(
                        if (state.showLabels) R.string.map_labels_on else R.string.map_labels_off,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            OutlinedButton(onClick = onOpenPlan, shape = CircleShape) {
                Icon(Icons.Filled.Route, null, Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.nav_plan),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        HikayatCard(modifier = Modifier.padding(horizontal = 16.dp)) {
            JerusalemMap(
                pins = state.pins(),
                framingLocations = state.locations,
                routeIds = state.routeIds,
                language = language,
                showLabels = state.showLabels,
                onSelect = { viewModel.select(it.id) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .testTag(TestTags.MAP_CANVAS),
                categoryColors = categoryColors,
                wallColor = MaterialTheme.colorScheme.primary,
                groundColor = colors.pageBackground,
                labelColor = MaterialTheme.colorScheme.onSurface,
                routeColor = colors.amber,
                gateColor = colors.souq,
            )
        }

        Spacer(Modifier.height(14.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                ChoiceChip(
                    label = stringResource(R.string.map_filter_all),
                    selected = state.categoryFilter == null,
                    onClick = { viewModel.setFilter(null) },
                )
            }
            items(LocationCategory.entries.toList()) { category ->
                ChoiceChip(
                    label = category.label(language),
                    selected = state.categoryFilter == category,
                    onClick = { viewModel.setFilter(category) },
                    leading = {
                        Box(
                            Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(category.accent()),
                        )
                    },
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        val selected = state.selected
        if (selected != null) {
            HikayatCard(
                modifier = Modifier.padding(horizontal = 16.dp),
                accent = colors.amber,
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = selected.name[language],
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = selected.description[language],
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onOpenLocation(selected.id) },
                            shape = CircleShape,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                text = stringResource(R.string.connections_open_place),
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                context.openWalkingDirections(
                                    latitude = selected.latitude,
                                    longitude = selected.longitude,
                                    label = selected.name[language],
                                )
                            },
                            shape = CircleShape,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Filled.DirectionsWalk, null, Modifier.size(15.dp))
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = stringResource(R.string.action_open_in_maps),
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        NearbyPanel(
            state = state.nearby,
            onCheck = { permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION) },
            onOpenLocation = onOpenLocation,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(Modifier.height(16.dp))
        Legend(modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun NearbyPanel(
    state: NearbyState,
    onCheck: () -> Unit,
    onOpenLocation: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val language = LocalAppLanguage.current
    val colors = HikayatTheme.colors

    HikayatCard(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            SectionHeader(
                title = stringResource(R.string.map_nearby_title),
                icon = Icons.Filled.MyLocation,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.map_nearby_body),
                style = MaterialTheme.typography.bodySmall,
                color = colors.muted,
            )
            Spacer(Modifier.height(12.dp))

            when (state) {
                NearbyState.Idle -> OutlinedButton(onClick = onCheck, shape = CircleShape) {
                    Icon(Icons.Filled.MyLocation, null, Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.map_nearby_check),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }

                NearbyState.Locating -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = colors.amber,
                    )
                    Spacer(Modifier.width(9.dp))
                    Text(
                        text = stringResource(R.string.map_nearby_checking),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.muted,
                    )
                }

                is NearbyState.Found -> Column {
                    val nearest = state.nearest
                    if (nearest.isNearby) {
                        Text(
                            text = stringResource(
                                R.string.map_nearby_result,
                                formatDistance((nearest.kilometres * 1000).toInt(), language),
                                nearest.location.name[language],
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = { onOpenLocation(nearest.location.id) },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = colors.amber),
                        ) {
                            Icon(Icons.Filled.DirectionsWalk, null, Modifier.size(15.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.map_start_narration),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    } else {
                        Text(
                            text = stringResource(
                                R.string.map_nearby_far,
                                nearest.location.name[language],
                                nearest.kilometres.toInt(),
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                is NearbyState.Denied -> Text(
                    text = stringResource(R.string.map_nearby_denied),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted,
                )

                NearbyState.Unavailable -> Text(
                    text = stringResource(R.string.map_nearby_unavailable),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted,
                )
            }
        }
    }
}

@Composable
private fun Legend(modifier: Modifier = Modifier) {
    val language = LocalAppLanguage.current
    HikayatCard(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            SectionHeader(title = stringResource(R.string.map_legend))
            Spacer(Modifier.height(10.dp))
            LocationCategory.entries.forEach { category ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(category.accent()),
                    )
                    Spacer(Modifier.width(9.dp))
                    Text(
                        text = category.label(language),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                JERUSALEM_GATES.take(2).forEach { gate ->
                    MetaTag(label = gate.name(language), color = HikayatTheme.colors.souq)
                }
            }
        }
    }
}
