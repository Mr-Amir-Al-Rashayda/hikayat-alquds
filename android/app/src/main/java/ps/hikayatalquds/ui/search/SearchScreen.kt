package ps.hikayatalquds.ui.search

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ps.hikayatalquds.R
import ps.hikayatalquds.core.designsystem.HikayatTheme
import ps.hikayatalquds.core.designsystem.component.CoverPhoto
import ps.hikayatalquds.core.designsystem.component.EmptyNote
import ps.hikayatalquds.core.designsystem.component.HikayatCard
import ps.hikayatalquds.core.designsystem.component.MetaTag
import ps.hikayatalquds.core.ui.LocalAppLanguage
import ps.hikayatalquds.data.repository.SearchHit
import ps.hikayatalquds.ui.HikayatAppState

/**
 * Search across names, descriptions, landmarks, timelines and stories.
 *
 * Runs entirely against the local database, so it works with the radio off and
 * returns instantly. Arabic is folded for diacritics and alef forms, so a
 * search typed without harakat still finds text written with them.
 */
@Composable
fun SearchScreen(
    appState: HikayatAppState,
    onBack: () -> Unit,
    onOpenLocation: (String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val language = LocalAppLanguage.current
    val colors = HikayatTheme.colors
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Scaffold(
        containerColor = colors.pageBackground,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_search)) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding()),
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .focusRequester(focusRequester),
                placeholder = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = { Icon(Icons.Filled.Search, null, tint = colors.muted) },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setQuery("") }) {
                            Icon(
                                Icons.Filled.Clear,
                                stringResource(R.string.action_close),
                                tint = colors.muted,
                            )
                        }
                    }
                },
                shape = MaterialTheme.shapes.extraLarge,
                singleLine = true,
            )

            Spacer(Modifier.height(14.dp))

            if (state.query.trim().length >= 2 && state.hits.isEmpty()) {
                EmptyNote(
                    text = stringResource(R.string.search_empty),
                    icon = Icons.Filled.Search,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.hits, key = { hit -> hit.key() }) { hit ->
                    HikayatCard(onClick = { onOpenLocation(hit.location.id) }) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(58.dp)) {
                                CoverPhoto(
                                    model = hit.location.coverImage
                                        ?.let { "file:///android_asset/$it" },
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(MaterialTheme.shapes.small),
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                MetaTag(
                                    label = stringResource(
                                        when (hit) {
                                            is SearchHit.LocationHit -> R.string.search_result_place
                                            is SearchHit.StoryHit -> R.string.search_result_story
                                        },
                                    ),
                                    icon = when (hit) {
                                        is SearchHit.LocationHit -> Icons.Filled.Place
                                        is SearchHit.StoryHit -> Icons.AutoMirrored.Filled.MenuBook
                                    },
                                    color = colors.muted,
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = when (hit) {
                                        is SearchHit.LocationHit -> hit.location.name[language]
                                        is SearchHit.StoryHit -> hit.story.title[language]
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = hit.snippet,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.muted,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun SearchHit.key(): String = when (this) {
    is SearchHit.LocationHit -> "location:${location.id}"
    is SearchHit.StoryHit -> "story:${story.id}"
}
