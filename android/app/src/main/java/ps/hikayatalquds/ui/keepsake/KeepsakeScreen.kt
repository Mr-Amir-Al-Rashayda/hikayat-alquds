package ps.hikayatalquds.ui.keepsake

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import ps.hikayatalquds.R
import ps.hikayatalquds.core.designsystem.HikayatTheme
import ps.hikayatalquds.core.designsystem.component.ChoiceChip
import ps.hikayatalquds.core.designsystem.component.HikayatCard
import ps.hikayatalquds.core.designsystem.component.OliveHeader
import ps.hikayatalquds.core.designsystem.component.StatTile
import ps.hikayatalquds.core.designsystem.component.TatreezField
import ps.hikayatalquds.core.ui.LocalAppLanguage
import ps.hikayatalquds.ui.HikayatAppState
import ps.hikayatalquds.ui.util.shareText

/**
 * "My Jerusalem Story": a keepsake built from the reader's own counters.
 *
 * The reflection is assembled from what this device did - places opened,
 * quizzes finished, narrations heard - and makes no historical claim. That
 * disclaimer is on the screen, not in a help page, because a generated
 * sentence about Jerusalem deserves to say what it is.
 */
@Composable
fun KeepsakeScreen(
    appState: HikayatAppState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onOpenLocation: (String) -> Unit,
    viewModel: KeepsakeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val language = LocalAppLanguage.current
    val colors = HikayatTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keepsake = state.keepsake

    // Resolved here rather than inside the callbacks: a lambda has no
    // composition to read the reader's chosen language from.
    val keepsakeTitle = stringResource(R.string.keepsake_title)
    val savedMessage = stringResource(R.string.keepsake_saved, viewModel.suggestedFileName)
    val failedMessage = stringResource(R.string.error_generic)

    // The system picker puts the keepsake somewhere the reader can find it,
    // with no storage permission and no guessing at a directory.
    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain"),
    ) { target ->
        if (target == null) return@rememberLauncherForActivityResult
        scope.launch {
            val written = viewModel.writeKeepsake(target, language)
            snackbarHostState.showSnackbar(if (written) savedMessage else failedMessage)
        }
    }

    Scaffold(
        containerColor = colors.pageBackground,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.keepsake_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            stringResource(R.string.action_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
            )
        },
    ) { padding ->
        if (keepsake == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.loading),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted,
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + 8.dp,
                bottom = 30.dp,
                start = 16.dp,
                end = 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item("badge") {
                OliveHeader(
                    title = keepsake.badge[language],
                    eyebrow = stringResource(R.string.keepsake_subtitle),
                    subtitle = keepsake.reflection[language],
                )
            }

            item("stats") {
                // Built in composable scope: a LazyGrid item lambda is not
                // composable, so stringResource cannot be called inside it.
                val tiles = listOf(
                    KeepsakeStat(
                        keepsake.heritageSitesDiscovered.toString(),
                        stringResource(R.string.keepsake_sites),
                        Icons.Filled.Place,
                    ),
                    KeepsakeStat(
                        keepsake.plannedWalkingMinutes.toString(),
                        stringResource(R.string.keepsake_walking),
                        Icons.AutoMirrored.Filled.DirectionsWalk,
                    ),
                    KeepsakeStat(
                        keepsake.quizzesCompleted.toString(),
                        stringResource(R.string.keepsake_quizzes),
                        Icons.Filled.EmojiEvents,
                    ),
                    KeepsakeStat(
                        keepsake.narrationsHeard.toString(),
                        stringResource(R.string.keepsake_narrations),
                        Icons.Filled.RecordVoiceOver,
                    ),
                    KeepsakeStat(
                        keepsake.memoriesUncovered.toString(),
                        stringResource(R.string.keepsake_memories),
                        Icons.Filled.AutoAwesome,
                    ),
                    KeepsakeStat(
                        "${keepsake.exploredLocations.size}/${keepsake.totalLocations}",
                        stringResource(R.string.keepsake_quarters),
                        Icons.Filled.Place,
                    ),
                )
                HikayatCard {
                    Box {
                        TatreezField(color = MaterialTheme.colorScheme.primary, alpha = 0.04f)
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(190.dp)
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            userScrollEnabled = false,
                        ) {
                            items(tiles) { tile ->
                                StatTile(
                                    value = tile.value,
                                    label = tile.label,
                                    icon = tile.icon,
                                )
                            }
                        }
                    }
                }
            }

            item("places") {
                HikayatCard {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.keepsake_places),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(10.dp))
                        if (keepsake.exploredLocations.isEmpty()) {
                            Text(
                                text = stringResource(R.string.keepsake_places_empty),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.muted,
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                keepsake.exploredLocations.chunked(2).forEach { pair ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        pair.forEach { location ->
                                            ChoiceChip(
                                                label = location.name[language],
                                                selected = false,
                                                onClick = { onOpenLocation(location.id) },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item("actions") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            scope.launch {
                                val text = viewModel.shareText(language)
                                context.shareText(text = text, title = keepsakeTitle)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.amber),
                    ) {
                        Icon(Icons.Filled.Share, null, Modifier.size(15.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(
                            text = stringResource(R.string.keepsake_share),
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                        )
                    }
                    OutlinedButton(
                        onClick = { saveLauncher.launch(viewModel.suggestedFileName) },
                        modifier = Modifier.weight(1f),
                        shape = CircleShape,
                    ) {
                        Icon(Icons.Filled.Save, null, Modifier.size(15.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(
                            text = stringResource(R.string.keepsake_save),
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                        )
                    }
                }
            }

            item("disclaimer") {
                Text(
                    text = stringResource(R.string.keepsake_disclaimer),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.muted,
                    textAlign = TextAlign.Start,
                )
            }
        }
    }
}

/** One counter on the keepsake grid. */
private data class KeepsakeStat(
    val value: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)
