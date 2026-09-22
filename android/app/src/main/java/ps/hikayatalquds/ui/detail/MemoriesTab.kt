package ps.hikayatalquds.ui.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import ps.hikayatalquds.R
import ps.hikayatalquds.core.designsystem.HikayatTheme
import ps.hikayatalquds.core.designsystem.component.EmptyNote
import ps.hikayatalquds.core.designsystem.component.HikayatCard
import ps.hikayatalquds.core.designsystem.component.MetaTag
import ps.hikayatalquds.core.designsystem.component.SectionHeader
import ps.hikayatalquds.core.ui.LocalAppLanguage
import ps.hikayatalquds.domain.model.Memory
import ps.hikayatalquds.ui.util.formatIsoDate

/**
 * The memories people have contributed to this place.
 *
 * Only approved memories reach this screen; the repository filters by status,
 * not the UI. Each one is folded shut until tapped - somebody gave this archive
 * a piece of their family's memory, and arriving at it on purpose treats it
 * better than scrolling past it.
 */
@Composable
fun MemoriesTabContent(
    memories: List<Memory>,
    revealedIds: Set<String>,
    onReveal: (String) -> Unit,
    onContribute: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val language = LocalAppLanguage.current
    val colors = HikayatTheme.colors

    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(
            title = stringResource(R.string.memories_title),
            icon = Icons.Filled.RecordVoiceOver,
        )
        Spacer(Modifier.height(12.dp))

        if (memories.isEmpty()) {
            EmptyNote(
                text = stringResource(R.string.memories_empty),
                icon = Icons.Filled.RecordVoiceOver,
            )
        } else {
            memories.forEach { memory ->
                val revealed = memory.id in revealedIds
                HikayatCard(modifier = Modifier.padding(bottom = 12.dp)) {
                    Column(Modifier.padding(15.dp)) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                Icons.Filled.FormatQuote,
                                null,
                                Modifier.size(17.dp),
                                tint = colors.amber,
                            )
                            Spacer(Modifier.width(9.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = memory.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    memory.contributorName?.takeIf { it.isNotBlank() }?.let { name ->
                                        MetaTag(label = name, color = colors.muted)
                                    }
                                    formatIsoDate(memory.submittedAt, language)?.let { date ->
                                        MetaTag(label = date, color = colors.muted)
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        AnimatedVisibility(visible = revealed) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(colors.pageBackground)
                                    .padding(13.dp),
                            ) {
                                Text(
                                    text = memory.content,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontStyle = FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                        if (!revealed) {
                            OutlinedButton(
                                onClick = { onReveal(memory.id) },
                                shape = CircleShape,
                            ) {
                                Icon(Icons.Filled.Visibility, null, Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.memories_reveal),
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        Surface(
            onClick = onContribute,
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.07f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.EditNote,
                    null,
                    Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.contribute_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = stringResource(R.string.contribute_intro),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.muted,
                    )
                }
            }
        }
    }
}
