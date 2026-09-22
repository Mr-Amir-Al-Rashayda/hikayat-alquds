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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ps.hikayatalquds.R
import ps.hikayatalquds.core.designsystem.HikayatTheme
import ps.hikayatalquds.core.designsystem.component.ChoiceChip
import ps.hikayatalquds.core.designsystem.component.HikayatCard
import ps.hikayatalquds.core.designsystem.component.SectionHeader
import ps.hikayatalquds.core.designsystem.component.SourceCitations
import ps.hikayatalquds.core.designsystem.component.GenerationOriginLine
import ps.hikayatalquds.core.designsystem.component.UncertaintyNotes
import ps.hikayatalquds.core.ui.LocalAppLanguage
import ps.hikayatalquds.domain.model.AudienceMode
import ps.hikayatalquds.domain.model.LocationDetail
import ps.hikayatalquds.domain.model.StoryTone
import ps.hikayatalquds.narration.NarrationNotice
import ps.hikayatalquds.narration.NarrationState
import ps.hikayatalquds.ui.util.openSpeechSettings

/**
 * The story tab: the reviewed text, the narration bar, and the retelling flow.
 *
 * The reviewed story comes first and is always present. Generation is offered
 * below it as a *retelling* of the same record for a chosen audience, never as
 * a replacement, and whatever comes back states which generator wrote it and
 * what it refused to claim.
 */
@Composable
fun StoryTabContent(
    detail: LocationDetail,
    state: DetailUiState,
    narration: NarrationState,
    onAudience: (AudienceMode) -> Unit,
    onTone: (StoryTone) -> Unit,
    onGenerate: () -> Unit,
    onRegenerate: () -> Unit,
    onNarrate: () -> Unit,
    onToggleNarration: () -> Unit,
    onStopNarration: () -> Unit,
    onSkipSegment: (Int) -> Unit,
    onRate: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val language = LocalAppLanguage.current
    val colors = HikayatTheme.colors
    val location = detail.location
    val reviewed = detail.stories.firstOrNull()

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        NarrationBar(
            state = narration,
            belongsHere = narration.locationId == location.id,
            onNarrate = onNarrate,
            onToggle = onToggleNarration,
            onStop = onStopNarration,
            onSkip = onSkipSegment,
            onRate = onRate,
        )

        HikayatCard {
            Column(Modifier.padding(16.dp)) {
                SectionHeader(title = stringResource(R.string.story_reviewed))
                Spacer(Modifier.height(10.dp))
                Text(
                    text = reviewed?.title?.get(language) ?: location.storyTitle[language],
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = reviewed?.body?.get(language) ?: location.story[language],
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = location.walkthrough[language],
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (location.culturalImportance[language].isNotBlank()) {
                    Spacer(Modifier.height(14.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .background(colors.pageBackground)
                            .padding(14.dp),
                    ) {
                        Text(
                            text = location.culturalImportance[language],
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }

        RetellPanel(
            state = state,
            onAudience = onAudience,
            onTone = onTone,
            onGenerate = onGenerate,
            onRegenerate = onRegenerate,
        )

        if (location.citations.isNotEmpty()) {
            HikayatCard {
                SourceCitations(
                    citations = location.citations,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}

@Composable
private fun RetellPanel(
    state: DetailUiState,
    onAudience: (AudienceMode) -> Unit,
    onTone: (StoryTone) -> Unit,
    onGenerate: () -> Unit,
    onRegenerate: () -> Unit,
) {
    val colors = HikayatTheme.colors
    HikayatCard(accent = colors.amber.copy(alpha = 0.4f)) {
        Column(Modifier.padding(16.dp)) {
            SectionHeader(
                title = stringResource(R.string.story_generate_title),
                icon = Icons.Filled.AutoAwesome,
            )
            Spacer(Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.story_generate_step_audience),
                style = MaterialTheme.typography.labelLarge,
                color = colors.muted,
            )
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(AudienceMode.entries.toList()) { mode ->
                    ChoiceChip(
                        label = stringResource(mode.labelRes()),
                        selected = state.audience == mode,
                        onClick = { onAudience(mode) },
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.story_generate_step_tone),
                style = MaterialTheme.typography.labelLarge,
                color = colors.muted,
            )
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(StoryTone.entries.toList()) { tone ->
                    ChoiceChip(
                        label = stringResource(tone.labelRes()),
                        selected = state.tone == tone,
                        onClick = { onTone(tone) },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = if (state.generated == null) onGenerate else onRegenerate,
                    enabled = !state.generating,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.amber),
                ) {
                    if (state.generating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = colors.onAmber,
                        )
                    } else {
                        Icon(Icons.Filled.AutoAwesome, null, Modifier.size(15.dp))
                    }
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = stringResource(
                            if (state.generated == null) {
                                R.string.story_generate_action
                            } else {
                                R.string.story_regenerate
                            },
                        ),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                if (state.generated != null) {
                    Text(
                        text = stringResource(R.string.story_word_count, state.generated.wordCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.muted,
                    )
                }
            }

            if (state.generationError != null) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        Icons.Filled.WarningAmber,
                        null,
                        Modifier.size(15.dp),
                        tint = colors.wrong,
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = state.generationError,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.wrong,
                    )
                }
            }

            AnimatedVisibility(visible = state.generated != null) {
                val story = state.generated
                if (story != null) {
                    Column {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = story.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = story.narrative,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(12.dp))
                        GenerationOriginLine(story.origin)
                        Spacer(Modifier.height(10.dp))
                        UncertaintyNotes(story.uncertaintyNotes)
                    }
                }
            }
        }
    }
}

/**
 * Play, pause, sentence position and speed.
 *
 * The sentence counter is not decoration: Android's engine cannot pause, so the
 * app tracks sentences to fake it, and showing the position is what makes that
 * legible rather than mysterious.
 */
@Composable
private fun NarrationBar(
    state: NarrationState,
    belongsHere: Boolean,
    onNarrate: () -> Unit,
    onToggle: () -> Unit,
    onStop: () -> Unit,
    onSkip: (Int) -> Unit,
    onRate: (Float) -> Unit,
) {
    val colors = HikayatTheme.colors
    val context = LocalContext.current
    val language = LocalAppLanguage.current
    val active = belongsHere && state.isActive

    HikayatCard {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!active) {
                    Button(
                        onClick = onNarrate,
                        enabled = state.available || state.preparing,
                        shape = CircleShape,
                    ) {
                        Icon(Icons.Filled.PlayArrow, null, Modifier.size(17.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.narration_listen),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                } else {
                    IconButton(onClick = onToggle) {
                        Icon(
                            imageVector = if (state.speaking) {
                                Icons.Filled.Pause
                            } else {
                                Icons.Filled.PlayArrow
                            },
                            contentDescription = stringResource(
                                if (state.speaking) R.string.narration_pause else R.string.narration_resume,
                            ),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    IconButton(onClick = onStop) {
                        Icon(
                            Icons.Filled.Stop,
                            stringResource(R.string.narration_stop),
                            tint = colors.muted,
                        )
                    }
                }

                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.GraphicEq,
                            null,
                            Modifier.size(13.dp),
                            tint = colors.amber,
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = if (active && state.segments.isNotEmpty()) {
                                stringResource(
                                    R.string.narration_progress,
                                    state.currentSegment + 1,
                                    state.segments.size,
                                )
                            } else {
                                stringResource(R.string.narration_voice_device)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.muted,
                        )
                    }
                    if (active) {
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { state.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(CircleShape),
                            color = colors.amber,
                            trackColor = colors.hairline,
                            gapSize = 0.dp,
                            drawStopIndicator = {},
                        )
                    }
                }
            }

            if (active) {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Speed, null, Modifier.size(13.dp), tint = colors.muted)
                    Spacer(Modifier.width(8.dp))
                    NARRATION_SPEEDS.forEach { speed ->
                        ChoiceChip(
                            label = "${speed}x",
                            selected = kotlin.math.abs(state.rate - speed) < 0.01f,
                            onClick = { onRate(speed) },
                            modifier = Modifier.padding(end = 6.dp),
                        )
                    }
                }
                if (state.currentSegment >= 0) {
                    Spacer(Modifier.height(10.dp))
                    // The sentence being read, highlighted so a long Arabic
                    // paragraph can be followed by eye as well as by ear.
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .background(colors.amber.copy(alpha = 0.09f))
                            .padding(12.dp),
                    ) {
                        Text(
                            text = state.segments.getOrNull(state.currentSegment)?.text.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            val notice = state.notice
            if (notice != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = when (notice) {
                        NarrationNotice.NO_VOICE_FOR_LANGUAGE -> stringResource(
                            R.string.narration_no_voice,
                            stringResource(
                                if (language.isArabic) {
                                    R.string.settings_language_arabic
                                } else {
                                    R.string.settings_language_english
                                },
                            ),
                        )

                        NarrationNotice.LANGUAGE_DATA_MISSING ->
                            stringResource(R.string.narration_language_data)

                        NarrationNotice.NO_ENGINE -> stringResource(R.string.narration_no_engine)
                        NarrationNotice.FAILED -> stringResource(R.string.narration_failed)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted,
                )
                if (notice != NarrationNotice.FAILED) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { context.openSpeechSettings() },
                        shape = CircleShape,
                    ) {
                        Text(
                            text = stringResource(R.string.narration_open_settings),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }
    }
}

/** Slow enough for Arabic prose, fast enough to skim. */
private val NARRATION_SPEEDS = listOf(0.78f, 0.92f, 1.05f, 1.25f)

internal fun AudienceMode.labelRes(): Int = when (this) {
    AudienceMode.STUDENT -> R.string.audience_student
    AudienceMode.TOURIST -> R.string.audience_tourist
    AudienceMode.CHILD -> R.string.audience_child
    AudienceMode.SHORT -> R.string.audience_short
    AudienceMode.HISTORIAN -> R.string.audience_historian
    AudienceMode.GENERAL -> R.string.audience_general
}

internal fun StoryTone.labelRes(): Int = when (this) {
    StoryTone.NEUTRAL -> R.string.tone_neutral
    StoryTone.EDUCATIONAL -> R.string.tone_educational
    StoryTone.EMOTIONAL -> R.string.tone_emotional
    StoryTone.STORYTELLING -> R.string.tone_storytelling
}
