package ps.hikayatalquds.ui.home

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ps.hikayatalquds.R
import ps.hikayatalquds.core.designsystem.HikayatTheme
import ps.hikayatalquds.core.designsystem.component.ArchiveOriginBanner
import ps.hikayatalquds.core.designsystem.component.BrandRule
import ps.hikayatalquds.core.designsystem.component.ChoiceChip
import ps.hikayatalquds.core.designsystem.component.CoverPhoto
import ps.hikayatalquds.core.designsystem.component.HikayatCard
import ps.hikayatalquds.core.designsystem.component.MetaTag
import ps.hikayatalquds.core.designsystem.component.SectionHeader
import ps.hikayatalquds.core.designsystem.component.ShimmerBlock
import ps.hikayatalquds.core.designsystem.component.TatreezField
import ps.hikayatalquds.core.ui.LocalAppLanguage
import ps.hikayatalquds.core.ui.TestTags
import ps.hikayatalquds.domain.model.Badge
import ps.hikayatalquds.domain.model.LocationCategory
import ps.hikayatalquds.ui.HikayatAppState
import ps.hikayatalquds.ui.components.LocationCard
import ps.hikayatalquds.ui.components.accent
import ps.hikayatalquds.ui.util.relativeTimeLabel

/**
 * The front page.
 *
 * Opens with the story of the day rather than a grid, because the point of the
 * archive is the reading, not the browsing. Below it: the reader's own progress,
 * category filters, and the eight places.
 */
@Composable
fun HomeScreen(
    appState: HikayatAppState,
    contentPadding: PaddingValues,
    onOpenLocation: (String) -> Unit,
    onOpenMap: () -> Unit,
    onOpenPlan: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenKeepsake: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val language = LocalAppLanguage.current
    val colors = HikayatTheme.colors

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(TestTags.HOME_LIST),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = 28.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item("brand") {
            BrandRule()
            Spacer(Modifier.height(14.dp))
            BrandBar(
                onOpenSearch = onOpenSearch,
                onOpenSettings = onOpenSettings,
                onOpenAbout = onOpenAbout,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        item("pulse") {
            JerusalemPulse(
                onOpenLocation = onOpenLocation,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        item("intro") {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = stringResource(R.string.home_greeting),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.home_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted,
                )
            }
        }

        item("origin") {
            ArchiveOriginBanner(
                origin = appState.archiveOrigin,
                isOnline = appState.isOnline,
                lastSyncedLabel = relativeTimeLabel(appState.lastSyncedAtEpochMillis, language),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        item("featured") {
            Column(Modifier.padding(horizontal = 16.dp)) {
                SectionHeader(
                    title = stringResource(R.string.home_story_of_the_day),
                    icon = Icons.Filled.AutoStories,
                    trailing = {
                        IconButton(onClick = viewModel::showAnotherStory) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = stringResource(R.string.home_another_story),
                                tint = colors.muted,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    },
                )
                Spacer(Modifier.height(10.dp))
                val featured = state.featured
                if (featured == null) {
                    ShimmerBlock(Modifier.fillMaxWidth().height(210.dp))
                } else {
                    val (story, place) = featured
                    HikayatCard(
                        modifier = Modifier.testTag(TestTags.FEATURED_STORY),
                        onClick = { onOpenLocation(place.id) },
                    ) {
                        Column {
                            Box(Modifier.fillMaxWidth().height(180.dp)) {
                                CoverPhoto(
                                    model = place.coverImage?.let { "file:///android_asset/$it" },
                                    contentDescription = stringResource(
                                        R.string.cd_cover_photo,
                                        place.name[language],
                                    ),
                                    modifier = Modifier.fillMaxWidth().height(180.dp),
                                )
                                Column(
                                    Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(16.dp),
                                ) {
                                    MetaTag(
                                        label = place.name[language],
                                        color = Color.White,
                                        icon = Icons.Filled.Place,
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = story.title[language],
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = Color.White,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            Text(
                                text = story.summary[language],
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(16.dp),
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }

        item("actions") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = onOpenPlan,
                    modifier = Modifier.weight(1f),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    Icon(Icons.Filled.Route, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = stringResource(R.string.home_plan_cta),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                    )
                }
                OutlinedButton(
                    onClick = onOpenMap,
                    modifier = Modifier.weight(1f),
                    shape = CircleShape,
                ) {
                    Icon(Icons.Filled.Place, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = stringResource(R.string.nav_map),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                    )
                }
            }
        }

        item("progress") {
            JourneyPanel(
                state = state,
                onOpenKeepsake = onOpenKeepsake,
                onContinue = { state.continueWith?.let { onOpenLocation(it.location.id) } },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        if (state.bookmarked.isNotEmpty()) {
            item("bookmarks") {
                Column {
                    SectionHeader(
                        title = stringResource(R.string.home_bookmarks),
                        icon = Icons.Filled.Bookmark,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    Spacer(Modifier.height(10.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(state.bookmarked, key = { it.location.id }) { summary ->
                            HikayatCard(
                                modifier = Modifier.width(150.dp),
                                onClick = { onOpenLocation(summary.location.id) },
                            ) {
                                Column {
                                    CoverPhoto(
                                        model = summary.location.coverImage
                                            ?.let { "file:///android_asset/$it" },
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxWidth().height(84.dp),
                                    )
                                    Text(
                                        text = summary.location.name[language],
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(10.dp),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item("filters") {
            Column {
                SectionHeader(
                    title = stringResource(R.string.home_all_places),
                    icon = Icons.Filled.Place,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                Spacer(Modifier.height(10.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        ChoiceChip(
                            label = stringResource(R.string.map_filter_all),
                            selected = state.categoryFilter == null,
                            onClick = { viewModel.setCategoryFilter(null) },
                        )
                    }
                    items(LocationCategory.entries.toList()) { category ->
                        ChoiceChip(
                            label = category.label(language),
                            selected = state.categoryFilter == category,
                            onClick = { viewModel.setCategoryFilter(category) },
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
            }
        }

        if (state.loading) {
            items(3) { index ->
                ShimmerBlock(
                    Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .height(260.dp),
                )
            }
        } else {
            items(state.filtered, key = { it.location.id }) { summary ->
                LocationCard(
                    summary = summary,
                    onClick = { onOpenLocation(summary.location.id) },
                    onToggleBookmark = { viewModel.toggleBookmark(summary.location.id) },
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .testTag(TestTags.placeCard(summary.location.id)),
                )
            }
        }
    }
}

@Composable
private fun BrandBar(
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.app_tagline),
                style = MaterialTheme.typography.labelSmall,
                color = HikayatTheme.colors.muted,
            )
        }
        IconButton(onClick = onOpenSearch) {
            Icon(
                Icons.Filled.Search,
                stringResource(R.string.nav_search),
                Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        IconButton(onClick = onOpenAbout) {
            Icon(
                Icons.Filled.Info,
                stringResource(R.string.nav_about),
                Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        IconButton(onClick = onOpenSettings) {
            Icon(
                Icons.Filled.Settings,
                stringResource(R.string.nav_settings),
                Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun JourneyPanel(
    state: HomeUiState,
    onOpenKeepsake: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val language = LocalAppLanguage.current
    val colors = HikayatTheme.colors
    val total = state.places.size
    val visited = state.progress.visitedLocationIds.size

    HikayatCard(modifier = modifier.fillMaxWidth().testTag(TestTags.PROGRESS_PANEL)) {
        Box {
            TatreezField(color = MaterialTheme.colorScheme.primary, alpha = 0.05f)
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.EmojiEvents,
                        null,
                        Modifier.size(18.dp),
                        tint = colors.amber,
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.home_progress_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = stringResource(R.string.home_progress_subtitle, visited, total),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.muted,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { if (total == 0) 0f else visited.toFloat() / total },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(CircleShape),
                    color = colors.amber,
                    trackColor = colors.hairline,
                    gapSize = 0.dp,
                    drawStopIndicator = {},
                )
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.badges.forEach { badge -> BadgeChip(badge) }
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    if (state.continueWith != null) {
                        OutlinedButton(
                            onClick = onContinue,
                            modifier = Modifier.weight(1f),
                            shape = CircleShape,
                        ) {
                            Text(
                                text = stringResource(R.string.home_continue),
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Button(
                        onClick = onOpenKeepsake,
                        modifier = Modifier.weight(1f),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.amber),
                    ) {
                        Icon(Icons.Filled.AutoAwesome, null, Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.home_keepsake_cta),
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BadgeChip(badge: Badge) {
    val language = LocalAppLanguage.current
    val colors = HikayatTheme.colors
    val label = badge.label[language]
    val description = stringResource(
        if (badge.earned) R.string.cd_earned_badge else R.string.cd_locked_badge,
        label,
    )
    MetaTag(
        label = label,
        icon = Icons.Filled.EmojiEvents,
        color = if (badge.earned) colors.amber else colors.muted,
        modifier = Modifier.semantics { contentDescription = description },
    )
}
