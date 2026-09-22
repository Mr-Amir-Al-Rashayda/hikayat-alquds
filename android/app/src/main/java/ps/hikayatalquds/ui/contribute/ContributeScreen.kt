package ps.hikayatalquds.ui.contribute

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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
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
import ps.hikayatalquds.domain.model.MemoryStatus
import ps.hikayatalquds.ui.HikayatAppState
import ps.hikayatalquds.ui.util.formatIsoDate

/**
 * Where somebody adds a memory to the archive.
 *
 * Three promises are kept on this screen. Nothing is published without an
 * editor. If the submission cannot be sent it is stored on the device and the
 * contributor is told exactly that, never given a fake receipt. And the
 * reference code is theirs to keep, so an anonymous contributor can find their
 * own memory again.
 */
@Composable
fun ContributeScreen(
    appState: HikayatAppState,
    contentPadding: PaddingValues,
    initialLocationId: String?,
    snackbarHostState: SnackbarHostState,
    viewModel: ContributeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val language = LocalAppLanguage.current
    val colors = HikayatTheme.colors
    val clipboard = LocalClipboardManager.current

    LaunchedEffect(initialLocationId) { viewModel.preselect(initialLocationId) }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = 30.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item("header") {
            OliveHeader(
                title = stringResource(R.string.contribute_title),
                eyebrow = stringResource(R.string.track_badge),
                subtitle = stringResource(R.string.contribute_intro),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        if (state.queued.isNotEmpty()) {
            item("queued") {
                HikayatCard(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    accent = colors.amber,
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.CloudQueue,
                            null,
                            Modifier.size(18.dp),
                            tint = colors.amber,
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            // Arabic has six plural forms; the framework picks
                            // the right one, which two hand-written strings
                            // could not.
                            text = pluralStringResource(
                                R.plurals.contribute_queue_pending,
                                state.queued.size,
                                state.queued.size,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }

        state.outcome?.let { outcome ->
            item("outcome") {
                OutcomePanel(
                    outcome = outcome,
                    onCopy = { code ->
                        clipboard.setText(AnnotatedString(code))
                    },
                    onDismiss = viewModel::dismissOutcome,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }

        item("form") {
            HikayatCard(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .testTag(TestTags.CONTRIBUTE_FORM),
            ) {
                Column(Modifier.padding(16.dp)) {
                    SectionHeader(
                        title = stringResource(R.string.contribute_place),
                        icon = Icons.Filled.EditNote,
                    )
                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.locations, key = { it.id }) { location ->
                            ChoiceChip(
                                label = location.name[language],
                                selected = state.selectedLocationId == location.id,
                                onClick = { viewModel.selectLocation(location.id) },
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))
                    OutlinedTextField(
                        value = state.title,
                        onValueChange = viewModel::setTitle,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.contribute_headline)) },
                        shape = MaterialTheme.shapes.medium,
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                        ),
                    )

                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = state.body,
                        onValueChange = viewModel::setBody,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        label = { Text(stringResource(R.string.contribute_body)) },
                        placeholder = { Text(stringResource(R.string.contribute_body_hint)) },
                        shape = MaterialTheme.shapes.medium,
                        supportingText = {
                            if (state.body.isNotEmpty() && !state.bodyLongEnough) {
                                Text(
                                    text = stringResource(R.string.contribute_min_length),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.wrong,
                                )
                            }
                        },
                        isError = state.body.isNotEmpty() && !state.bodyLongEnough,
                    )

                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = state.contributorName,
                        onValueChange = viewModel::setName,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.contribute_name)) },
                        shape = MaterialTheme.shapes.medium,
                        singleLine = true,
                    )

                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = state.contributorEmail,
                        onValueChange = viewModel::setEmail,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.contribute_email)) },
                        shape = MaterialTheme.shapes.medium,
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Email,
                        ),
                    )

                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = viewModel::submit,
                        enabled = state.canSubmit,
                        modifier = Modifier.fillMaxWidth(),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.amber),
                    ) {
                        if (state.submitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(15.dp),
                                strokeWidth = 2.dp,
                                color = colors.onAmber,
                            )
                        } else {
                            Icon(Icons.Filled.Send, null, Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.contribute_submit),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }

        item("lookup") {
            HikayatCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    SectionHeader(
                        title = stringResource(R.string.contribute_status_title),
                        icon = Icons.Filled.Search,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = state.lookupCode,
                        onValueChange = viewModel::setLookupCode,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.contribute_status_hint)) },
                        shape = MaterialTheme.shapes.medium,
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                        ),
                        trailingIcon = {
                            IconButton(
                                onClick = viewModel::lookUp,
                                enabled = state.lookupCode.isNotBlank() && !state.lookupBusy,
                            ) {
                                if (state.lookupBusy) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(15.dp),
                                        strokeWidth = 2.dp,
                                        color = colors.amber,
                                    )
                                } else {
                                    Icon(
                                        Icons.Filled.Search,
                                        stringResource(R.string.contribute_status_lookup),
                                        tint = colors.amber,
                                    )
                                }
                            }
                        },
                    )

                    if (state.lookupCodeUnknown) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.contribute_status_unknown),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.muted,
                        )
                    } else {
                        state.lookupError?.let { error ->
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.muted,
                            )
                        }
                    }

                    state.lookupResult?.let { report ->
                        Spacer(Modifier.height(14.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .background(colors.pageBackground)
                                .padding(14.dp),
                        ) {
                            Column {
                                MetaTag(
                                    label = report.status.label(language.isArabic),
                                    color = when (report.status) {
                                        MemoryStatus.APPROVED -> colors.correct
                                        MemoryStatus.REJECTED -> colors.wrong
                                        MemoryStatus.PENDING_REVIEW -> colors.amber
                                    },
                                )
                                Spacer(Modifier.height(9.dp))
                                Text(
                                    text = report.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = report.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                formatIsoDate(report.submittedAt, language)?.let { date ->
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = date,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.muted,
                                    )
                                }
                                report.reviewNotes?.takeIf { it.isNotBlank() }?.let { notes ->
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = notes,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.muted,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OutcomePanel(
    outcome: ContributeOutcome,
    onCopy: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HikayatTheme.colors
    val accent = when (outcome) {
        is ContributeOutcome.Accepted -> colors.correct
        ContributeOutcome.Queued -> colors.amber
        is ContributeOutcome.Rejected -> colors.wrong
    }

    HikayatCard(modifier = modifier, accent = accent) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when (outcome) {
                        is ContributeOutcome.Accepted -> Icons.Filled.TaskAlt
                        ContributeOutcome.Queued -> Icons.Filled.CloudQueue
                        is ContributeOutcome.Rejected -> Icons.Filled.WarningAmber
                    },
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(19.dp),
                )
                Spacer(Modifier.width(9.dp))
                Text(
                    text = when (outcome) {
                        is ContributeOutcome.Accepted -> stringResource(R.string.contribute_accepted)
                        ContributeOutcome.Queued -> stringResource(R.string.contribute_queued_title)
                        is ContributeOutcome.Rejected ->
                            stringResource(R.string.contribute_rejected, outcome.message)
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = accent,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Filled.Close,
                        stringResource(R.string.action_close),
                        Modifier.size(17.dp),
                        tint = colors.muted,
                    )
                }
            }

            if (outcome is ContributeOutcome.Queued) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.contribute_queued_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            if (outcome is ContributeOutcome.Accepted && outcome.referenceCode != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.contribute_reference),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.muted,
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = outcome.referenceCode,
                        style = MaterialTheme.typography.headlineSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(10.dp))
                    IconButton(onClick = { onCopy(outcome.referenceCode) }) {
                        Icon(
                            Icons.Filled.ContentCopy,
                            stringResource(R.string.action_share),
                            Modifier.size(17.dp),
                            tint = colors.amber,
                        )
                    }
                }
            }
        }
    }
}

private fun MemoryStatus.label(arabic: Boolean): String = when (this) {
    MemoryStatus.PENDING_REVIEW -> if (arabic) "قيد المراجعة" else "In review"
    MemoryStatus.APPROVED -> if (arabic) "مُعتمدة" else "Approved"
    MemoryStatus.REJECTED -> if (arabic) "غير مقبولة" else "Not accepted"
}
