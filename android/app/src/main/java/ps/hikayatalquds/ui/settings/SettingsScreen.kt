package ps.hikayatalquds.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ps.hikayatalquds.BuildConfig
import ps.hikayatalquds.R
import ps.hikayatalquds.core.designsystem.HikayatTheme
import ps.hikayatalquds.core.designsystem.component.ChoiceChip
import ps.hikayatalquds.core.designsystem.component.HikayatCard
import ps.hikayatalquds.core.designsystem.component.SectionHeader
import ps.hikayatalquds.data.preferences.ThemeMode
import ps.hikayatalquds.domain.model.AppLanguage
import ps.hikayatalquds.ui.HikayatAppState

/**
 * Settings, including the one that matters most: the app works with no API at
 * all, and this screen says so rather than presenting an empty field as broken.
 */
@Composable
fun SettingsScreen(
    appState: HikayatAppState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onOpenAbout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = HikayatTheme.colors
    var confirmReset by remember { mutableStateOf(false) }

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> viewModel.setDailyStoryReminder(granted) }

    Scaffold(
        containerColor = colors.pageBackground,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenAbout) {
                        Icon(Icons.Filled.Info, stringResource(R.string.nav_about))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + 8.dp,
                bottom = 30.dp,
                start = 16.dp,
                end = 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item("language") {
                SettingsCard(
                    title = stringResource(R.string.settings_language),
                    icon = Icons.Filled.Language,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppLanguage.entries.forEach { language ->
                            ChoiceChip(
                                label = stringResource(
                                    if (language.isArabic) {
                                        R.string.settings_language_arabic
                                    } else {
                                        R.string.settings_language_english
                                    },
                                ),
                                selected = state.settings.language == language,
                                onClick = { viewModel.setLanguage(language) },
                            )
                        }
                    }
                }
            }

            item("appearance") {
                SettingsCard(
                    title = stringResource(R.string.settings_appearance),
                    icon = Icons.Filled.DarkMode,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            ChoiceChip(
                                label = stringResource(mode.labelRes()),
                                selected = state.settings.themeMode == mode,
                                onClick = { viewModel.setTheme(mode) },
                            )
                        }
                    }
                }
            }

            item("data") {
                SettingsCard(
                    title = stringResource(R.string.settings_data),
                    icon = Icons.Filled.Cloud,
                ) {
                    OutlinedTextField(
                        value = state.apiDraft,
                        onValueChange = viewModel::setApiDraft,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.settings_api_url)) },
                        placeholder = { Text(stringResource(R.string.settings_api_url_hint)) },
                        shape = MaterialTheme.shapes.medium,
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                        ),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.settings_api_url_help),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.muted,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        Button(
                            onClick = viewModel::saveApiAddress,
                            shape = CircleShape,
                        ) {
                            Text(
                                text = stringResource(R.string.settings_api_check),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                        OutlinedButton(onClick = viewModel::syncNow, shape = CircleShape) {
                            Icon(Icons.Filled.Refresh, null, Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.settings_sync_now),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }

                    when (val connection = state.connection) {
                        ConnectionCheck.Busy -> {
                            Spacer(Modifier.height(10.dp))
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = colors.amber,
                            )
                        }

                        is ConnectionCheck.Reachable -> SettingsNote(
                            stringResource(R.string.settings_api_ok, connection.dataSource),
                            colors.correct,
                        )

                        is ConnectionCheck.Failed -> SettingsNote(
                            stringResource(R.string.settings_api_failed, connection.reason),
                            colors.muted,
                        )

                        ConnectionCheck.InvalidAddress -> SettingsNote(
                            stringResource(R.string.settings_api_url_invalid),
                            colors.wrong,
                        )

                        ConnectionCheck.Idle -> Unit
                    }

                    Spacer(Modifier.height(16.dp))
                    SettingsToggle(
                        label = stringResource(R.string.settings_remote_images),
                        help = stringResource(R.string.settings_remote_images_help),
                        checked = state.settings.allowRemoteImages,
                        onCheckedChange = viewModel::setAllowRemoteImages,
                    )
                }
            }

            item("narration") {
                SettingsCard(
                    title = stringResource(R.string.settings_narration),
                    icon = Icons.Filled.RecordVoiceOver,
                ) {
                    Text(
                        text = stringResource(R.string.narration_speed),
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.muted,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0.78f, 0.92f, 1.05f, 1.25f).forEach { speed ->
                            ChoiceChip(
                                label = "${speed}x",
                                selected = kotlin.math.abs(
                                    state.settings.narrationRate - speed,
                                ) < 0.01f,
                                onClick = { viewModel.setNarrationRate(speed) },
                            )
                        }
                    }

                    if (state.voices.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.narration_voice),
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.muted,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.narration_voice_device),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.muted,
                        )
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(state.voices, key = { it.id }) { voice ->
                                ChoiceChip(
                                    label = voice.label,
                                    selected = state.selectedVoice == voice.id,
                                    onClick = { viewModel.setNarrationVoice(voice.id) },
                                )
                            }
                        }
                    }
                }
            }

            item("notifications") {
                SettingsCard(
                    title = stringResource(R.string.settings_notifications),
                    icon = Icons.Filled.Notifications,
                ) {
                    SettingsToggle(
                        label = stringResource(R.string.settings_daily_story),
                        help = stringResource(R.string.settings_daily_story_help),
                        checked = state.settings.dailyStoryReminder,
                        onCheckedChange = { enabled ->
                            if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                viewModel.setDailyStoryReminder(enabled)
                            }
                        },
                    )
                }
            }

            item("privacy") {
                SettingsCard(
                    title = stringResource(R.string.settings_privacy),
                    icon = Icons.Filled.Shield,
                ) {
                    Text(
                        text = stringResource(R.string.settings_privacy_help),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.muted,
                    )
                    Spacer(Modifier.height(14.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { confirmReset = true },
                            shape = CircleShape,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Filled.DeleteForever, null, Modifier.size(15.dp))
                            Spacer(Modifier.width(7.dp))
                            Text(
                                text = stringResource(R.string.settings_reset_journey),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                        OutlinedButton(
                            onClick = viewModel::clearGeneratedStories,
                            shape = CircleShape,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = stringResource(R.string.settings_clear_generated),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                        OutlinedButton(
                            onClick = viewModel::reinstallArchive,
                            enabled = !state.reseeding,
                            shape = CircleShape,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            if (state.reseeding) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Icon(Icons.Filled.Restore, null, Modifier.size(15.dp))
                            }
                            Spacer(Modifier.width(7.dp))
                            Text(
                                text = stringResource(R.string.settings_reseed),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }
            }

            item("version") {
                Text(
                    text = stringResource(
                        R.string.settings_about_app,
                        BuildConfig.VERSION_NAME,
                        BuildConfig.VERSION_CODE,
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.muted,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(stringResource(R.string.settings_reset_journey)) },
            text = { Text(stringResource(R.string.settings_reset_journey_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetJourney()
                        confirmReset = false
                    },
                ) {
                    Text(
                        text = stringResource(R.string.settings_reset_journey),
                        color = colors.wrong,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) {
                    Text(stringResource(R.string.action_close))
                }
            },
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit,
) {
    HikayatCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            SectionHeader(title = title, icon = icon)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

private typealias ColumnScope = androidx.compose.foundation.layout.ColumnScope

@Composable
private fun SettingsToggle(
    label: String,
    help: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = help,
                style = MaterialTheme.typography.labelSmall,
                color = HikayatTheme.colors.muted,
            )
        }
        Spacer(Modifier.width(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = androidx.compose.material3.SwitchDefaults.colors(
                checkedTrackColor = HikayatTheme.colors.amber,
            ),
        )
    }
}

@Composable
private fun SettingsNote(text: String, color: androidx.compose.ui.graphics.Color) {
    Spacer(Modifier.height(10.dp))
    Text(text = text, style = MaterialTheme.typography.bodySmall, color = color)
}

private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.settings_theme_system
    ThemeMode.LIGHT -> R.string.settings_theme_light
    ThemeMode.DARK -> R.string.settings_theme_dark
}
