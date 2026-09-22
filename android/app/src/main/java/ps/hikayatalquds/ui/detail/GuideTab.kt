package ps.hikayatalquds.ui.detail

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
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ps.hikayatalquds.R
import ps.hikayatalquds.core.designsystem.HikayatTheme
import ps.hikayatalquds.core.designsystem.component.ChoiceChip
import ps.hikayatalquds.core.designsystem.component.HikayatCard
import ps.hikayatalquds.core.designsystem.component.GenerationOriginLine
import ps.hikayatalquds.core.designsystem.component.MetaTag
import ps.hikayatalquds.core.designsystem.component.SectionHeader
import ps.hikayatalquds.core.designsystem.component.SourceNote
import ps.hikayatalquds.core.ui.LocalAppLanguage
import ps.hikayatalquds.core.ui.TestTags
import ps.hikayatalquds.domain.model.LocationDetail

/**
 * The heritage guide.
 *
 * Answers come from the reviewed record and nothing else. When the record does
 * not cover a question the guide says so, and the interface marks that answer
 * as *not in the record* rather than dressing a refusal up as an answer - the
 * refusal is the honest result and it is labelled as a result, not an error.
 */
@Composable
fun GuideTabContent(
    detail: LocationDetail,
    state: DetailUiState,
    onAsk: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val language = LocalAppLanguage.current
    val colors = HikayatTheme.colors
    var draft by remember { mutableStateOf("") }

    val suggestions = remember(detail, language) { detail.suggestedQuestions(language.isArabic) }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        HikayatCard {
            Column(Modifier.padding(16.dp)) {
                SectionHeader(
                    title = stringResource(R.string.guide_title),
                    icon = Icons.Filled.Chat,
                    trailing = {
                        if (state.conversation.isNotEmpty()) {
                            IconButton(onClick = onClear) {
                                Icon(
                                    Icons.Filled.DeleteSweep,
                                    stringResource(R.string.guide_clear),
                                    Modifier.size(18.dp),
                                    tint = colors.muted,
                                )
                            }
                        }
                    },
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.guide_intro),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted,
                )

                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TestTags.GUIDE_INPUT),
                    placeholder = {
                        Text(
                            text = stringResource(R.string.guide_hint),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    shape = MaterialTheme.shapes.medium,
                    maxLines = 3,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        imeAction = ImeAction.Send,
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onSend = {
                            if (draft.isNotBlank()) {
                                onAsk(draft)
                                draft = ""
                            }
                        },
                    ),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (draft.isNotBlank()) {
                                    onAsk(draft)
                                    draft = ""
                                }
                            },
                            enabled = draft.isNotBlank() && !state.guidePending,
                        ) {
                            Icon(
                                Icons.Filled.Send,
                                stringResource(R.string.guide_send),
                                tint = if (draft.isNotBlank()) {
                                    colors.amber
                                } else {
                                    colors.muted
                                },
                            )
                        }
                    },
                )

                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(suggestions) { suggestion ->
                        ChoiceChip(
                            label = suggestion,
                            selected = false,
                            onClick = { onAsk(suggestion) },
                            leading = {
                                Icon(
                                    Icons.Filled.HelpOutline,
                                    null,
                                    Modifier.size(12.dp),
                                    tint = colors.muted,
                                )
                            },
                        )
                    }
                }
            }
        }

        state.conversation.asReversed().forEach { exchange ->
            HikayatCard {
                Column(Modifier.padding(16.dp)) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                            .padding(12.dp),
                    ) {
                        Text(
                            text = exchange.question,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(Modifier.height(12.dp))

                    val answer = exchange.answer
                    when {
                        exchange.pending || answer == null -> Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(15.dp),
                                strokeWidth = 2.dp,
                                color = colors.amber,
                            )
                            Spacer(Modifier.width(9.dp))
                            Text(
                                text = stringResource(R.string.loading),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.muted,
                            )
                        }

                        else -> Column {
                            if (!answer.answeredFromSource) {
                                MetaTag(
                                    label = stringResource(R.string.guide_not_in_record),
                                    icon = Icons.Filled.HelpOutline,
                                    color = colors.amber,
                                )
                                Spacer(Modifier.height(9.dp))
                            }
                            Text(
                                text = answer.answer,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            if (answer.excerpts.isNotEmpty()) {
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = stringResource(R.string.guide_excerpts),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.muted,
                                )
                                answer.excerpts.take(3).forEach { excerpt ->
                                    Spacer(Modifier.height(6.dp))
                                    SourceNote(excerpt)
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            GenerationOriginLine(answer.origin)
                        }
                    }
                }
            }
        }

        if (state.conversation.isEmpty()) {
            Text(
                text = detail.location.historicalSummary[language],
                style = MaterialTheme.typography.bodySmall,
                color = colors.muted,
                textAlign = TextAlign.Start,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

/**
 * Starter questions built from this place's own record, so the first tap is
 * guaranteed to be answerable rather than a demonstration of a refusal.
 */
private fun LocationDetail.suggestedQuestions(arabic: Boolean): List<String> {
    val name = if (arabic) location.name.ar else location.name.en
    return if (arabic) {
        listOfNotNull(
            "ما تاريخ $name؟",
            location.landmarks.ar.firstOrNull()?.let { "ما أهمية $it؟" },
            "ما الذي يمكن أن أراه هنا؟",
            "ما أهمية هذا المكان ثقافياً؟",
        )
    } else {
        listOfNotNull(
            "What is the history of $name?",
            location.landmarks.en.firstOrNull()?.let { "Why does $it matter?" },
            "What can I see here?",
            "Why is this place culturally important?",
        )
    }
}
