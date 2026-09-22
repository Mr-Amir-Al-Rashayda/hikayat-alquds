package ps.hikayatalquds.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import ps.hikayatalquds.R
import ps.hikayatalquds.core.designsystem.HikayatTheme
import ps.hikayatalquds.core.ui.LocalAppLanguage
import ps.hikayatalquds.data.repository.ArchiveOrigin
import ps.hikayatalquds.domain.model.GenerationOrigin
import ps.hikayatalquds.domain.model.SourceCitation

/**
 * The parts of the interface whose only job is to be honest.
 *
 * In this project these are not garnish. A story that does not say who wrote it,
 * a quiz answer that does not cite its section, or a page that shows bundled
 * data while implying it is live would each break the rule the whole archive is
 * built on. So they get first-class components, and they are never optional
 * where the underlying data has something to declare.
 */

/** Which of the four generators produced a narrative, in plain words. */
@Composable
fun GenerationOriginLine(origin: GenerationOrigin, modifier: Modifier = Modifier) {
    val label = stringResource(
        when (origin) {
            GenerationOrigin.BACKEND_AI -> R.string.origin_backend_ai
            GenerationOrigin.OFFLINE_EXTRACTIVE -> R.string.origin_offline_extractive
            GenerationOrigin.BACKEND_FALLBACK -> R.string.origin_backend_fallback
            GenerationOrigin.ON_DEVICE -> R.string.origin_on_device
        },
    )
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Verified,
            contentDescription = null,
            tint = HikayatTheme.colors.muted,
            modifier = Modifier.size(13.dp),
        )
        Text(
            text = "${stringResource(R.string.origin_label)}: $label",
            style = MaterialTheme.typography.labelSmall,
            color = HikayatTheme.colors.muted,
        )
    }
}

/**
 * Gaps the generator refused to fill.
 *
 * Shown to the reader deliberately, and styled as a note rather than a warning:
 * "the record does not say" is information, not an error.
 */
@Composable
fun UncertaintyNotes(notes: List<String>, modifier: Modifier = Modifier) {
    if (notes.isEmpty()) return
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = HikayatTheme.colors.amber.copy(alpha = 0.07f),
        border = BorderStroke(1.dp, HikayatTheme.colors.amber.copy(alpha = 0.28f)),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = HikayatTheme.colors.amber,
                    modifier = Modifier.size(15.dp),
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    text = stringResource(R.string.uncertainty_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = HikayatTheme.colors.amber,
                )
            }
            notes.forEach { note ->
                Spacer(Modifier.height(7.dp))
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

/** The bibliography behind a place, each entry opening its own source. */
@Composable
fun SourceCitations(citations: List<SourceCitation>, modifier: Modifier = Modifier) {
    if (citations.isEmpty()) return
    val language = LocalAppLanguage.current
    val uriHandler = LocalUriHandler.current

    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(
            title = stringResource(R.string.sources_title),
            icon = Icons.Filled.Description,
        )
        citations.forEach { citation ->
            Spacer(Modifier.height(10.dp))
            val hasLink = citation.url.isNotBlank()
            Surface(
                onClick = { if (hasLink) uriHandler.openUri(citation.url) },
                enabled = hasLink,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = HikayatTheme.colors.pageBackground,
            ) {
                Row(
                    modifier = Modifier.padding(13.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = citation.title[language],
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = citation.publisher[language],
                            style = MaterialTheme.typography.labelSmall,
                            color = HikayatTheme.colors.muted,
                        )
                    }
                    if (hasLink) {
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Filled.OpenInNew,
                            contentDescription = stringResource(R.string.gallery_view_source),
                            tint = HikayatTheme.colors.amber,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}

/** A citation line under a quiz answer or a timeline entry. */
@Composable
fun SourceNote(text: String, modifier: Modifier = Modifier) {
    if (text.isBlank()) return
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Description,
            contentDescription = null,
            tint = HikayatTheme.colors.muted,
            modifier = Modifier.size(12.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = HikayatTheme.colors.muted,
        )
    }
}

/** Flags an entry that rests on oral or religious tradition. */
@Composable
fun TraditionNote(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = HikayatTheme.colors.tradition.copy(alpha = 0.10f),
    ) {
        Column(Modifier.padding(horizontal = 11.dp, vertical = 8.dp)) {
            Text(
                text = stringResource(R.string.tradition_badge),
                style = MaterialTheme.typography.labelMedium,
                color = HikayatTheme.colors.tradition,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = stringResource(R.string.tradition_note),
                style = MaterialTheme.typography.labelSmall,
                color = HikayatTheme.colors.muted,
                fontStyle = FontStyle.Italic,
            )
        }
    }
}

/**
 * Says where the archive on screen came from.
 *
 * The bundled state is not an error and is not styled as one - it is the whole
 * reviewed archive - but the reader is told, because "as shipped" and "live"
 * are different claims.
 */
@Composable
fun ArchiveOriginBanner(
    origin: ArchiveOrigin,
    isOnline: Boolean,
    lastSyncedLabel: String?,
    modifier: Modifier = Modifier,
) {
    val colors = HikayatTheme.colors
    val live = origin == ArchiveOrigin.LIVE
    val accent = if (live) MaterialTheme.colorScheme.primary else colors.amber

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = accent.copy(alpha = 0.07f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.25f)),
    ) {
        Row(
            modifier = Modifier.padding(13.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Icon(
                imageVector = when {
                    live -> Icons.Filled.Verified
                    !isOnline -> Icons.Filled.SignalWifiOff
                    else -> Icons.Filled.Inventory2
                },
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(17.dp),
            )
            Column {
                Text(
                    text = stringResource(
                        if (live) R.string.banner_live_title else R.string.banner_bundled_title,
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = accent,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = when {
                        live && lastSyncedLabel != null ->
                            stringResource(R.string.banner_live_body, lastSyncedLabel)

                        !isOnline -> stringResource(R.string.banner_offline)
                        else -> stringResource(R.string.banner_bundled_body)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

/** The credit and licence that always travel with a photograph. */
@Composable
fun PhotoCredit(
    credit: String?,
    license: String?,
    sourceUrl: String?,
    modifier: Modifier = Modifier,
    bundled: Boolean = false,
) {
    if (credit.isNullOrBlank() && license.isNullOrBlank()) return
    val uriHandler = LocalUriHandler.current
    val label = listOfNotNull(credit?.takeIf { it.isNotBlank() }, license?.takeIf { it.isNotBlank() })
        .joinToString(" · ")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(HikayatTheme.colors.pageBackground)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = HikayatTheme.colors.muted,
            modifier = Modifier.weight(1f),
        )
        if (bundled) {
            MetaTag(
                label = stringResource(R.string.gallery_bundled),
                icon = Icons.Filled.Inventory2,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        if (!sourceUrl.isNullOrBlank()) {
            Surface(
                onClick = { uriHandler.openUri(sourceUrl) },
                shape = MaterialTheme.shapes.extraSmall,
                color = MaterialTheme.colorScheme.surface,
            ) {
                Icon(
                    imageVector = Icons.Filled.OpenInNew,
                    contentDescription = stringResource(R.string.gallery_view_source),
                    tint = HikayatTheme.colors.amber,
                    modifier = Modifier.size(28.dp).padding(7.dp),
                )
            }
        }
    }
}
