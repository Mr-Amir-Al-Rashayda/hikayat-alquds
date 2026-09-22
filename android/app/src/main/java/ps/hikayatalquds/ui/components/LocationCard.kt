package ps.hikayatalquds.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ps.hikayatalquds.R
import ps.hikayatalquds.core.designsystem.HikayatTheme
import ps.hikayatalquds.core.designsystem.component.CoverPhoto
import ps.hikayatalquds.core.designsystem.component.HikayatCard
import ps.hikayatalquds.core.designsystem.component.MetaTag
import ps.hikayatalquds.core.ui.LocalAppLanguage
import ps.hikayatalquds.domain.model.LocationCategory
import ps.hikayatalquds.domain.model.LocationSummary

/**
 * One place in a list.
 *
 * Carries its counts, because a card that only shows a name asks the reader to
 * tap to find out whether there is anything inside. The photograph is the
 * bundled cover, so a list scrolls with no network.
 */
@Composable
fun LocationCard(
    summary: LocationSummary,
    onClick: () -> Unit,
    onToggleBookmark: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val language = LocalAppLanguage.current
    val colors = HikayatTheme.colors
    val location = summary.location
    val name = location.name[language]

    HikayatCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        accent = if (summary.visited) colors.amber.copy(alpha = 0.55f) else null,
    ) {
        Column {
            Box(Modifier.fillMaxWidth().height(164.dp)) {
                CoverPhoto(
                    model = location.coverImage?.let { "file:///android_asset/$it" },
                    contentDescription = stringResource(R.string.cd_cover_photo, name),
                    modifier = Modifier.fillMaxWidth().height(164.dp),
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (summary.visited) {
                        MetaTag(
                            label = stringResource(R.string.label_visited),
                            icon = Icons.Filled.CheckCircle,
                            color = colors.amber,
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    IconButton(
                        onClick = onToggleBookmark,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape),
                    ) {
                        Icon(
                            imageVector = if (summary.bookmarked) {
                                Icons.Filled.Bookmark
                            } else {
                                Icons.Outlined.BookmarkBorder
                            },
                            contentDescription = stringResource(
                                if (summary.bookmarked) R.string.action_unsave else R.string.action_save,
                            ),
                            tint = Color.White,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(14.dp),
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Column(Modifier.padding(14.dp)) {
                Text(
                    text = location.description[language],
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(11.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    location.categories.take(2).forEach { category ->
                        MetaTag(
                            label = category.label(language),
                            color = category.accent(),
                        )
                    }
                }
                Spacer(Modifier.height(11.dp))
                CountRow(summary)
            }
        }
    }
}

@Composable
private fun CountRow(summary: LocationSummary) {
    val stats = summary.stats
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Count(Icons.Outlined.MenuBook, stats.storyCount, stringResource(R.string.stat_stories))
        Count(Icons.Outlined.Image, stats.imageCount, stringResource(R.string.stat_photographs))
        Count(Icons.Outlined.Timeline, stats.timelineEventCount, stringResource(R.string.stat_timeline))
        if (stats.memoryCount > 0) {
            Count(
                Icons.Outlined.RecordVoiceOver,
                stats.memoryCount,
                stringResource(R.string.stat_memories),
            )
        }
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.Schedule,
                contentDescription = null,
                tint = HikayatTheme.colors.muted,
                modifier = Modifier.size(12.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.stat_reading_time, stats.readingTimeMinutes),
                style = MaterialTheme.typography.labelSmall,
                color = HikayatTheme.colors.muted,
            )
        }
    }
}

@Composable
private fun Count(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: Int,
    label: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.semantics { contentDescription = "$value $label" },
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = HikayatTheme.colors.muted,
            modifier = Modifier.size(12.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = HikayatTheme.colors.muted,
        )
    }
}

/** The colour a category takes on a card, a pin and a constellation node. */
@Composable
fun LocationCategory.accent(): Color = when (this) {
    LocationCategory.SACRED -> HikayatTheme.colors.sacred
    LocationCategory.SOUQ -> HikayatTheme.colors.souq
    LocationCategory.QUARTER -> HikayatTheme.colors.quarter
    LocationCategory.NEIGHBOURHOOD -> HikayatTheme.colors.neighbourhood
}
