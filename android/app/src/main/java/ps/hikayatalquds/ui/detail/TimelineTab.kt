package ps.hikayatalquds.ui.detail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ps.hikayatalquds.R
import ps.hikayatalquds.core.designsystem.HikayatTheme
import ps.hikayatalquds.core.designsystem.component.EmptyNote
import ps.hikayatalquds.core.designsystem.component.HikayatCard
import ps.hikayatalquds.core.designsystem.component.SectionHeader
import ps.hikayatalquds.core.designsystem.component.SourceNote
import ps.hikayatalquds.core.designsystem.component.TraditionNote
import ps.hikayatalquds.core.ui.LocalAppLanguage
import ps.hikayatalquds.domain.model.TimelineEvent

/**
 * The historical timeline.
 *
 * Entries that rest on oral or religious tradition are drawn differently and
 * labelled as tradition. Presenting them in the same visual register as
 * documented history would be the single easiest way for this app to mislead,
 * so the distinction is made in the layout, not just in a footnote.
 */
@Composable
fun TimelineTabContent(
    events: List<TimelineEvent>,
    modifier: Modifier = Modifier,
) {
    val language = LocalAppLanguage.current
    val colors = HikayatTheme.colors

    if (events.isEmpty()) {
        EmptyNote(
            text = stringResource(R.string.empty_timeline),
            icon = Icons.Filled.Timeline,
            modifier = modifier,
        )
        return
    }

    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(
            title = stringResource(R.string.tab_timeline),
            icon = Icons.Filled.Timeline,
        )
        Spacer(Modifier.height(12.dp))

        events.forEachIndexed { index, event ->
            Row(modifier = Modifier.fillMaxWidth()) {
                // The spine: a dot for each entry, hollow where the entry is
                // tradition rather than documented.
                Box(Modifier.width(24.dp).fillMaxHeight()) {
                    Canvas(Modifier.fillMaxHeight().width(24.dp)) {
                        val centre = Offset(size.width / 2, 18f)
                        if (index < events.lastIndex) {
                            drawLine(
                                color = colors.hairline,
                                start = Offset(centre.x, centre.y + 8f),
                                end = Offset(centre.x, size.height),
                                strokeWidth = 2f,
                            )
                        }
                        if (index > 0) {
                            drawLine(
                                color = colors.hairline,
                                start = Offset(centre.x, 0f),
                                end = Offset(centre.x, centre.y - 8f),
                                strokeWidth = 2f,
                            )
                        }
                        if (event.isTradition) {
                            drawCircle(
                                color = colors.tradition,
                                radius = 6f,
                                center = centre,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f),
                            )
                        } else {
                            drawCircle(color = colors.amber, radius = 6f, center = centre)
                        }
                    }
                }
                Spacer(Modifier.width(6.dp))
                Column(Modifier.weight(1f).padding(bottom = 18.dp)) {
                    Text(
                        text = event.periodLabel[language],
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = event.description[language],
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (event.isTradition) {
                        Spacer(Modifier.height(9.dp))
                        TraditionNote()
                    }
                    event.sourceFile?.let { source ->
                        Spacer(Modifier.height(8.dp))
                        SourceNote(source)
                    }
                }
            }
        }
    }
}

/** A compact horizontal timeline for the place header. */
@Composable
fun TimelineStrip(events: List<TimelineEvent>, modifier: Modifier = Modifier) {
    if (events.isEmpty()) return
    val language = LocalAppLanguage.current
    HikayatCard(modifier = modifier) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            events.take(3).forEach { event ->
                Text(
                    text = event.periodLabel[language],
                    style = MaterialTheme.typography.labelSmall,
                    color = HikayatTheme.colors.muted,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
