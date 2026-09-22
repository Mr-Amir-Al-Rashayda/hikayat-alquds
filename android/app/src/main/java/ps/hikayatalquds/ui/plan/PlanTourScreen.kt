package ps.hikayatalquds.ui.plan

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Interests
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Map
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
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
import ps.hikayatalquds.core.designsystem.component.UncertaintyNotes
import ps.hikayatalquds.core.ui.LocalAppLanguage
import ps.hikayatalquds.core.ui.TestTags
import ps.hikayatalquds.domain.model.Itinerary
import ps.hikayatalquds.domain.model.ItineraryStop
import ps.hikayatalquds.domain.model.TourDuration
import ps.hikayatalquds.domain.model.TourInterest
import ps.hikayatalquds.domain.model.TourLanguage
import ps.hikayatalquds.domain.tour.bearingLabel
import ps.hikayatalquds.ui.HikayatAppState
import ps.hikayatalquds.ui.util.formatDistance
import ps.hikayatalquds.ui.util.formatMinutes
import ps.hikayatalquds.ui.util.openWalkingDirections

/**
 * Three questions, then a walk.
 *
 * Once a route exists the wizard steps out of the way and the route itself is
 * the screen: numbered stops, the walk between each, a compass direction, and
 * live directions per leg. The estimates say they are estimates, because the
 * Old City is stairs and gates and a straight line is a lie there.
 */
@Composable
fun PlanTourScreen(
    appState: HikayatAppState,
    contentPadding: PaddingValues,
    onOpenLocation: (String) -> Unit,
    onOpenMap: () -> Unit,
    viewModel: PlanTourViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val itinerary = state.itinerary

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(TestTags.PLAN_LIST),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = 30.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item("header") {
            OliveHeader(
                title = stringResource(R.string.plan_title),
                eyebrow = stringResource(R.string.track_badge),
                subtitle = stringResource(R.string.plan_intro),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        if (itinerary == null) {
            item("wizard") {
                Wizard(
                    state = state,
                    onLanguage = viewModel::setLanguage,
                    onDuration = viewModel::setDuration,
                    onInterest = viewModel::toggleInterest,
                    onBack = viewModel::back,
                    onNext = viewModel::next,
                    onBuild = viewModel::build,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        } else {
            item("route-summary") {
                RouteSummary(
                    itinerary = itinerary,
                    onOpenMap = onOpenMap,
                    onStartOver = viewModel::startOver,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            itinerary.stops.forEach { stop ->
                item("stop-${stop.locationId}") {
                    StopCard(
                        stop = stop,
                        tourLanguage = itinerary.preferences.language,
                        onOpen = { onOpenLocation(stop.locationId) },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }

            item("notes") {
                UncertaintyNotes(
                    notes = itinerary.uncertaintyNotes,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun Wizard(
    state: PlanUiState,
    onLanguage: (TourLanguage) -> Unit,
    onDuration: (TourDuration) -> Unit,
    onInterest: (TourInterest) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onBuild: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HikayatTheme.colors

    HikayatCard(modifier = modifier) {
        Column(Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                repeat(PlanUiState.LAST_STEP + 1) { index ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(if (index <= state.step) colors.amber else colors.hairline),
                    )
                }
            }
            Spacer(Modifier.height(20.dp))

            when (state.step) {
                0 -> {
                    SectionHeader(
                        title = stringResource(R.string.plan_step_language),
                        icon = Icons.Filled.Language,
                    )
                    Spacer(Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TourLanguage.entries.forEach { language ->
                            WizardChoice(
                                label = language.label,
                                selected = state.preferences.language == language,
                                onClick = { onLanguage(language) },
                            )
                        }
                    }
                }

                1 -> {
                    SectionHeader(
                        title = stringResource(R.string.plan_step_duration),
                        icon = Icons.Filled.AccessTime,
                    )
                    Spacer(Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TourDuration.entries.forEach { duration ->
                            WizardChoice(
                                label = stringResource(duration.labelRes()),
                                selected = state.preferences.duration == duration,
                                onClick = { onDuration(duration) },
                            )
                        }
                    }
                }

                else -> {
                    SectionHeader(
                        title = stringResource(R.string.plan_step_interests),
                        icon = Icons.Filled.Interests,
                    )
                    Spacer(Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TourInterest.entries.forEach { interest ->
                            WizardChoice(
                                label = stringResource(interest.labelRes()),
                                selected = interest in state.preferences.interests,
                                onClick = { onInterest(interest) },
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(colors.pageBackground)
                    .padding(13.dp),
            ) {
                Text(
                    text = stringResource(
                        R.string.plan_summary,
                        state.preferences.duration.stopCount,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted,
                )
            }

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (state.step > 0) {
                    OutlinedButton(
                        onClick = onBack,
                        shape = CircleShape,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = stringResource(R.string.action_back),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
                if (state.step < PlanUiState.LAST_STEP) {
                    Button(
                        onClick = onNext,
                        shape = CircleShape,
                        modifier = Modifier.weight(1f).testTag(TestTags.PLAN_NEXT),
                    ) {
                        Text(
                            text = stringResource(R.string.action_next),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                } else {
                    Button(
                        onClick = onBuild,
                        enabled = state.canBuild,
                        shape = CircleShape,
                        modifier = Modifier.weight(1f).testTag(TestTags.PLAN_BUILD),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.amber),
                    ) {
                        if (state.building) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = colors.onAmber,
                            )
                        } else {
                            Icon(Icons.Filled.Route, null, Modifier.size(15.dp))
                        }
                        Spacer(Modifier.width(7.dp))
                        Text(
                            text = stringResource(R.string.plan_build),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WizardChoice(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = HikayatTheme.colors
    androidx.compose.material3.Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary else colors.cardBorder,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.weight(1f),
            )
            if (selected) {
                Icon(Icons.Filled.Check, null, Modifier.size(17.dp), tint = colors.amber)
            }
        }
    }
}

@Composable
private fun RouteSummary(
    itinerary: Itinerary,
    onOpenMap: () -> Unit,
    onStartOver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HikayatTheme.colors
    HikayatCard(
        modifier = modifier.testTag(TestTags.PLAN_ROUTE),
        accent = colors.amber.copy(alpha = 0.4f),
    ) {
        Column(Modifier.padding(16.dp)) {
            SectionHeader(
                title = stringResource(R.string.plan_route_title),
                icon = Icons.Filled.Route,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = itinerary.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(
                    R.string.plan_total,
                    itinerary.stops.size,
                    itinerary.totalMinutes,
                    itinerary.totalWalkingMinutes,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = colors.muted,
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Button(onClick = onOpenMap, shape = CircleShape, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Map, null, Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.nav_map),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                    )
                }
                OutlinedButton(
                    onClick = onStartOver,
                    shape = CircleShape,
                    modifier = Modifier.weight(1f).testTag(TestTags.PLAN_DISCARD),
                ) {
                    Text(
                        text = stringResource(R.string.plan_clear),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun StopCard(
    stop: ItineraryStop,
    tourLanguage: TourLanguage,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val language = LocalAppLanguage.current
    val colors = HikayatTheme.colors
    val context = LocalContext.current

    Column(modifier = modifier) {
        if (stop.walkFromPreviousMinutes > 0) {
            Row(
                modifier = Modifier.padding(start = 10.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.DirectionsWalk,
                    null,
                    Modifier.size(14.dp),
                    tint = colors.amber,
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    text = stringResource(
                        R.string.plan_walk_leg,
                        formatMinutes(stop.walkFromPreviousMinutes, language),
                        formatDistance(stop.distanceFromPreviousMeters, language),
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.muted,
                )
                bearingLabel(stop.bearingFromPreviousDegrees, language)?.let { bearing ->
                    Spacer(Modifier.width(7.dp))
                    MetaTag(label = bearing, icon = Icons.Filled.Explore, color = colors.muted)
                }
            }
        }

        HikayatCard(onClick = onOpen) {
            Row(Modifier.padding(15.dp), verticalAlignment = Alignment.Top) {
                Box(
                    Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stop.order.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stop.locationName[language],
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stop.guidanceFor(tourLanguage),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(9.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MetaTag(
                            label = stringResource(R.string.plan_stop_time, stop.suggestedMinutes),
                            icon = Icons.Filled.AccessTime,
                            color = colors.muted,
                        )
                        MetaTag(
                            label = stringResource(R.string.plan_day, stop.day),
                            color = colors.muted,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    ChoiceChip(
                        label = stringResource(R.string.action_open_in_maps),
                        selected = false,
                        onClick = {
                            context.openWalkingDirections(
                                latitude = stop.latitude,
                                longitude = stop.longitude,
                                label = stop.locationName[language],
                            )
                        },
                        leading = {
                            Icon(
                                Icons.AutoMirrored.Filled.DirectionsWalk,
                                null,
                                Modifier.size(13.dp),
                                tint = colors.amber,
                            )
                        },
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

internal fun TourDuration.labelRes(): Int = when (this) {
    TourDuration.EXPRESS -> R.string.plan_duration_express
    TourDuration.HALF_DAY -> R.string.plan_duration_half_day
    TourDuration.FULL_DAY -> R.string.plan_duration_full_day
    TourDuration.WEEKEND -> R.string.plan_duration_weekend
}

internal fun TourInterest.labelRes(): Int = when (this) {
    TourInterest.HISTORY -> R.string.interest_history
    TourInterest.ARCHITECTURE -> R.string.interest_architecture
    TourInterest.RELIGIOUS -> R.string.interest_religious
    TourInterest.FOOD_MARKETS -> R.string.interest_food_markets
    TourInterest.ORAL_HERITAGE -> R.string.interest_oral_heritage
}
