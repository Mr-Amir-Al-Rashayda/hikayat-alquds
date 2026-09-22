package ps.hikayatalquds.ui.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ps.hikayatalquds.R
import ps.hikayatalquds.core.designsystem.HikayatTheme
import ps.hikayatalquds.core.designsystem.component.EmptyNote
import ps.hikayatalquds.core.designsystem.component.HikayatCard
import ps.hikayatalquds.core.designsystem.component.SectionHeader
import ps.hikayatalquds.core.designsystem.component.SourceNote
import ps.hikayatalquds.core.ui.LocalAppLanguage
import ps.hikayatalquds.domain.model.QuizQuestion

/**
 * A short quiz after the story.
 *
 * Once answered, every question shows both the explanation and the place in the
 * reviewed content the answer comes from. That is deliberate: a quiz that only
 * says "correct" is asking to be trusted, and this archive does not ask anyone
 * to take its word for anything.
 *
 * Colour appears only after an answer, so the layout cannot leak which option
 * is right.
 */
@Composable
fun QuizTabContent(
    questions: List<QuizQuestion>,
    locationName: String,
    onCompleted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val language = LocalAppLanguage.current
    val colors = HikayatTheme.colors

    if (questions.isEmpty()) {
        EmptyNote(
            text = stringResource(R.string.quiz_empty),
            icon = Icons.Filled.Quiz,
            modifier = modifier,
        )
        return
    }

    val answers = remember(questions) { mutableStateMapOf<String, Int>() }
    val answered = answers.size
    val correct = questions.count { answers[it.id] == it.answerIndex }
    val finished = answered == questions.size

    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(
            title = stringResource(R.string.tab_quiz),
            icon = Icons.Filled.Quiz,
            trailing = {
                if (answered > 0) {
                    Surface(
                        onClick = { answers.clear() },
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surface,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Filled.RestartAlt,
                                null,
                                Modifier.size(13.dp),
                                tint = colors.muted,
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = stringResource(R.string.quiz_restart),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.muted,
                            )
                        }
                    }
                }
            },
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = buildString {
                append(stringResource(R.string.quiz_progress, answered, questions.size))
                if (answered > 0) {
                    append(" · ")
                    append(stringResource(R.string.quiz_correct_count, correct))
                }
            },
            style = MaterialTheme.typography.labelSmall,
            color = colors.muted,
        )
        Spacer(Modifier.height(14.dp))

        questions.forEachIndexed { index, question ->
            val chosen = answers[question.id]
            HikayatCard(modifier = Modifier.padding(bottom = 12.dp)) {
                Column(Modifier.padding(15.dp)) {
                    Text(
                        text = "${index + 1}. ${question.question[language]}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(11.dp))

                    question.options[language].forEachIndexed { optionIndex, option ->
                        val isChosen = chosen == optionIndex
                        val isAnswer = optionIndex == question.answerIndex
                        val border by animateColorAsState(
                            targetValue = when {
                                chosen == null -> colors.cardBorder
                                isAnswer -> colors.correct
                                isChosen -> colors.wrong
                                else -> colors.hairline
                            },
                            label = "quiz-border",
                        )
                        Surface(
                            onClick = {
                                if (chosen == null) {
                                    answers[question.id] = optionIndex
                                    if (answers.size == questions.size) onCompleted()
                                }
                            },
                            enabled = chosen == null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 7.dp),
                            shape = MaterialTheme.shapes.small,
                            color = when {
                                chosen == null -> MaterialTheme.colorScheme.surface
                                isAnswer -> colors.correct.copy(alpha = 0.10f)
                                isChosen -> colors.wrong.copy(alpha = 0.10f)
                                else -> MaterialTheme.colorScheme.surface
                            },
                            border = BorderStroke(1.dp, border),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (chosen != null && (isAnswer || isChosen)) {
                                    Icon(
                                        imageVector = if (isAnswer) {
                                            Icons.Filled.Check
                                        } else {
                                            Icons.Filled.Close
                                        },
                                        contentDescription = null,
                                        tint = if (isAnswer) colors.correct else colors.wrong,
                                        modifier = Modifier.size(15.dp),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                }
                                Text(
                                    text = option,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = when {
                                        chosen == null -> MaterialTheme.colorScheme.onSurface
                                        isAnswer || isChosen -> MaterialTheme.colorScheme.onSurface
                                        else -> colors.muted
                                    },
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = chosen != null) {
                        Column {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = question.explanation[language],
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(Modifier.height(8.dp))
                            SourceNote(
                                text = "${stringResource(R.string.quiz_source)}: " +
                                    question.sourceNote[language],
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(visible = finished) {
            HikayatCard(accent = colors.amber.copy(alpha = 0.4f)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        Icons.Filled.EmojiEvents,
                        null,
                        Modifier.size(24.dp),
                        tint = colors.amber,
                    )
                    Text(
                        text = stringResource(R.string.quiz_score, correct, questions.size),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = if (correct == questions.size) {
                            stringResource(R.string.quiz_perfect, locationName)
                        } else {
                            stringResource(R.string.quiz_keep_reading)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.muted,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
