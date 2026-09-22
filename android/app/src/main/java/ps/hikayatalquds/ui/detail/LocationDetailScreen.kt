package ps.hikayatalquds.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ps.hikayatalquds.R
import ps.hikayatalquds.core.designsystem.HikayatTheme
import ps.hikayatalquds.core.designsystem.component.ChoiceChip
import ps.hikayatalquds.core.designsystem.component.CoverPhoto
import ps.hikayatalquds.core.designsystem.component.HikayatCard
import ps.hikayatalquds.core.designsystem.component.MetaTag
import ps.hikayatalquds.core.designsystem.component.SectionHeader
import ps.hikayatalquds.core.designsystem.component.ShimmerBlock
import ps.hikayatalquds.core.ui.LocalAppLanguage
import ps.hikayatalquds.core.ui.TestTags
import ps.hikayatalquds.ui.HikayatAppState
import ps.hikayatalquds.ui.components.accent
import ps.hikayatalquds.ui.util.openWalkingDirections
import ps.hikayatalquds.ui.util.shareText

/**
 * Everything one place has to say, behind seven tabs.
 *
 * The cover, the name and the counts sit above the tabs so the reader always
 * knows where they are; the tab row scrolls because seven labels do not fit an
 * Arabic phone screen and truncating them would be worse than scrolling.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LocationDetailScreen(
    locationId: String,
    appState: HikayatAppState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onOpenLocation: (String) -> Unit,
    onContribute: () -> Unit,
    viewModel: LocationDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val narration by viewModel.narrationState.collectAsStateWithLifecycle()
    val language = LocalAppLanguage.current
    val colors = HikayatTheme.colors
    val context = LocalContext.current
    val detail = state.detail

    Scaffold(
        containerColor = colors.pageBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = detail?.location?.name?.get(language).orEmpty(),
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::toggleBookmark) {
                        Icon(
                            imageVector = if (state.bookmarked) {
                                Icons.Filled.Bookmark
                            } else {
                                Icons.Outlined.BookmarkBorder
                            },
                            contentDescription = stringResource(
                                if (state.bookmarked) R.string.action_unsave else R.string.action_save,
                            ),
                            tint = if (state.bookmarked) colors.amber else colors.muted,
                        )
                    }
                    if (detail != null) {
                        IconButton(
                            onClick = {
                                context.shareText(
                                    text = buildString {
                                        appendLine(detail.location.name[language])
                                        appendLine(detail.location.description[language])
                                        append("hikayat://locations/${detail.location.id}")
                                    },
                                    title = detail.location.name[language],
                                )
                            },
                        ) {
                            Icon(
                                Icons.Filled.Share,
                                stringResource(R.string.action_share),
                                tint = colors.muted,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
            )
        },
    ) { padding ->
        if (detail == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (state.loading) {
                    ShimmerBlock(Modifier.fillMaxWidth().height(180.dp))
                    ShimmerBlock(Modifier.fillMaxWidth().height(120.dp))
                } else {
                    Text(
                        text = stringResource(R.string.error_location_missing),
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.muted,
                    )
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag(TestTags.DETAIL_LIST),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = 32.dp,
            ),
        ) {
            item("cover") {
                Box(Modifier.fillMaxWidth().height(230.dp)) {
                    CoverPhoto(
                        model = detail.location.coverImage?.let { "file:///android_asset/$it" },
                        contentDescription = stringResource(
                            R.string.cd_cover_photo,
                            detail.location.name[language],
                        ),
                        modifier = Modifier.fillMaxSize(),
                    )
                    Column(
                        Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            detail.location.categories.forEach { category ->
                                MetaTag(label = category.label(language), color = Color.White)
                            }
                        }
                        Spacer(Modifier.height(9.dp))
                        Text(
                            text = detail.location.name[language],
                            style = MaterialTheme.typography.displaySmall,
                            color = Color.White,
                        )
                    }
                }
                if (detail.location.coverImageCredit != null) {
                    ps.hikayatalquds.core.designsystem.component.PhotoCredit(
                        credit = detail.location.coverImageCredit,
                        license = detail.location.coverImageLicense,
                        sourceUrl = detail.location.coverImageSourceUrl,
                        bundled = detail.location.coverImage != null,
                    )
                }
            }

            item("summary") {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = detail.location.description[language],
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        ChoiceChip(
                            label = stringResource(R.string.action_open_in_maps),
                            selected = false,
                            onClick = {
                                context.openWalkingDirections(
                                    latitude = detail.location.latitude,
                                    longitude = detail.location.longitude,
                                    label = detail.location.name[language],
                                )
                            },
                            leading = {
                                Icon(
                                    Icons.Filled.DirectionsWalk,
                                    null,
                                    Modifier.size(14.dp),
                                    tint = colors.amber,
                                )
                            },
                        )
                    }
                }
            }

            item("tabs") {
                // The seven sections wrap onto two lines rather than scrolling
                // sideways. All of them are visible at once, which is the point
                // - a reader should see that a place has a quiz and memories,
                // not discover it by swiping - and it sidesteps a horizontal
                // scroller inside a vertical one, which is awkward either way
                // and worse right-to-left.
                FlowRow(
                    modifier = Modifier
                        .testTag(TestTags.DETAIL_TABS)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DetailTab.entries.forEach { tab ->
                        ChoiceChip(
                            label = stringResource(tab.labelRes()),
                            selected = state.tab == tab,
                            onClick = { viewModel.selectTab(tab) },
                            modifier = Modifier.testTag(TestTags.detailTab(tab.name)),
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            item("tab-content") {
                Box(Modifier.padding(horizontal = 16.dp)) {
                    when (state.tab) {
                        DetailTab.STORY -> StoryTabContent(
                            detail = detail,
                            state = state,
                            narration = narration,
                            onAudience = viewModel::setAudience,
                            onTone = viewModel::setTone,
                            onGenerate = { viewModel.generateStory(language) },
                            onRegenerate = { viewModel.generateStory(language, forceRefresh = true) },
                            onNarrate = { viewModel.narrate(language) },
                            onToggleNarration = viewModel::toggleNarration,
                            onStopNarration = viewModel::stopNarration,
                            onSkipSegment = viewModel::skipToSegment,
                            onRate = viewModel::setNarrationRate,
                        )

                        DetailTab.GUIDE -> GuideTabContent(
                            detail = detail,
                            state = state,
                            onAsk = { viewModel.ask(it, language) },
                            onClear = viewModel::clearConversation,
                        )

                        DetailTab.TIMELINE -> TimelineTabContent(events = detail.timeline)

                        DetailTab.GALLERY -> GalleryTabContent(
                            media = detail.media,
                            beforeAfter = detail.beforeAfter,
                            allowRemote = appState.canLoadRemoteImages,
                        )

                        DetailTab.ARTISANS -> ArtisansTabContent(
                            artisans = detail.artisans,
                            allowRemote = appState.canLoadRemoteImages,
                        )

                        DetailTab.QUIZ -> QuizTabContent(
                            questions = detail.quiz,
                            locationName = detail.location.name[language],
                            onCompleted = viewModel::onQuizCompleted,
                        )

                        DetailTab.MEMORIES -> MemoriesTabContent(
                            memories = detail.memories,
                            revealedIds = state.revealedMemoryIds,
                            onReveal = viewModel::revealMemory,
                            onContribute = onContribute,
                        )
                    }
                }
            }

            if (detail.related.isNotEmpty()) {
                item("related") {
                    Column(Modifier.padding(top = 24.dp)) {
                        SectionHeader(
                            title = stringResource(R.string.connections_title),
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                        Spacer(Modifier.height(10.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(detail.related, key = { it.id }) { related ->
                                HikayatCard(
                                    modifier = Modifier.width(160.dp),
                                    onClick = { onOpenLocation(related.id) },
                                ) {
                                    Column {
                                        CoverPhoto(
                                            model = related.coverImage
                                                ?.let { "file:///android_asset/$it" },
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxWidth().height(90.dp),
                                        )
                                        Column(Modifier.padding(11.dp)) {
                                            Text(
                                                text = related.name[language],
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Spacer(Modifier.height(5.dp))
                                            MetaTag(
                                                label = related.primaryCategory.label(language),
                                                color = related.primaryCategory.accent(),
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
    }
}

private fun DetailTab.labelRes(): Int = when (this) {
    DetailTab.STORY -> R.string.tab_story
    DetailTab.GUIDE -> R.string.tab_guide
    DetailTab.TIMELINE -> R.string.tab_timeline
    DetailTab.GALLERY -> R.string.tab_gallery
    DetailTab.ARTISANS -> R.string.tab_artisans
    DetailTab.QUIZ -> R.string.tab_quiz
    DetailTab.MEMORIES -> R.string.tab_memories
}
