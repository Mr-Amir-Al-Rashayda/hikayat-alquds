package ps.hikayatalquds.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlin.math.roundToInt
import ps.hikayatalquds.R
import ps.hikayatalquds.core.designsystem.HikayatTheme
import ps.hikayatalquds.core.designsystem.component.ArchivePhoto
import ps.hikayatalquds.core.designsystem.component.EmptyNote
import ps.hikayatalquds.core.designsystem.component.HikayatCard
import ps.hikayatalquds.core.designsystem.component.MetaTag
import ps.hikayatalquds.core.designsystem.component.PhotoCredit
import ps.hikayatalquds.core.designsystem.component.SectionHeader
import ps.hikayatalquds.core.ui.LocalAppLanguage
import ps.hikayatalquds.domain.model.BeforeAfterPair
import ps.hikayatalquds.domain.model.MediaEra
import ps.hikayatalquds.domain.model.MediaItem

/**
 * The photographs, each with its photographer and licence.
 *
 * Credit is not a caption that can be trimmed for space - it travels with the
 * image everywhere, including the full-screen view. Where the archive holds a
 * historical and a present photograph of the *same subject*, a comparison is
 * offered; where it does not, the absence is explained rather than hidden.
 */
@Composable
fun GalleryTabContent(
    media: List<MediaItem>,
    beforeAfter: BeforeAfterPair?,
    allowRemote: Boolean,
    modifier: Modifier = Modifier,
) {
    val language = LocalAppLanguage.current
    var viewing by remember { mutableStateOf<MediaItem?>(null) }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ThenAndNow(pair = beforeAfter, allowRemote = allowRemote)

        if (media.isEmpty()) {
            EmptyNote(
                text = stringResource(R.string.empty_gallery),
                icon = Icons.Filled.Collections,
            )
        } else {
            Column {
                SectionHeader(
                    title = stringResource(R.string.tab_gallery),
                    icon = Icons.Filled.Collections,
                )
                Spacer(Modifier.height(12.dp))
                media.forEach { item ->
                    HikayatCard(
                        modifier = Modifier.padding(bottom = 12.dp),
                        onClick = { viewing = item },
                    ) {
                        Column {
                            Box(Modifier.fillMaxWidth().height(200.dp)) {
                                ArchivePhoto(
                                    model = item.displayModel(allowRemote),
                                    contentDescription = item.subject[language].ifBlank { null },
                                    modifier = Modifier.fillMaxSize(),
                                )
                                if (item.era == MediaEra.HISTORICAL) {
                                    MetaTag(
                                        label = item.capturedAt ?: stringResource(R.string.tab_timeline),
                                        icon = Icons.Filled.HistoryEdu,
                                        color = Color.White,
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(8.dp),
                                    )
                                }
                            }
                            if (item.subject[language].isNotBlank() ||
                                item.description[language].isNotBlank()
                            ) {
                                Column(Modifier.padding(14.dp)) {
                                    if (item.subject[language].isNotBlank()) {
                                        Text(
                                            text = item.subject[language],
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                        Spacer(Modifier.height(5.dp))
                                    }
                                    Text(
                                        text = item.description[language],
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                            PhotoCredit(
                                credit = item.credit,
                                license = item.license,
                                sourceUrl = item.sourceUrl,
                                bundled = item.isBundled,
                            )
                        }
                    }
                }
            }
        }
    }

    viewing?.let { item ->
        FullScreenPhoto(
            item = item,
            allowRemote = allowRemote,
            onDismiss = { viewing = null },
        )
    }
}

@Composable
private fun FullScreenPhoto(item: MediaItem, allowRemote: Boolean, onDismiss: () -> Unit) {
    val language = LocalAppLanguage.current
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column {
                Box {
                    ArchivePhoto(
                        model = item.displayModel(allowRemote),
                        contentDescription = item.subject[language].ifBlank { null },
                        modifier = Modifier.fillMaxWidth().height(340.dp),
                        contentScale = ContentScale.Fit,
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            stringResource(R.string.action_close),
                            tint = Color.White,
                        )
                    }
                }
                if (item.description[language].isNotBlank()) {
                    Text(
                        text = item.description[language],
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(14.dp),
                    )
                }
                PhotoCredit(
                    credit = item.credit,
                    license = item.license,
                    sourceUrl = item.sourceUrl,
                    bundled = item.isBundled,
                )
            }
        }
    }
}

/**
 * The then-and-now slider.
 *
 * Only ever built from a `before`/`after` pair the archive marked as the same
 * subject. When there is no pair the panel says why, because "no comparison"
 * and "we didn't bother" look identical if you say nothing.
 */
@Composable
private fun ThenAndNow(pair: BeforeAfterPair?, allowRemote: Boolean) {
    val language = LocalAppLanguage.current
    val colors = HikayatTheme.colors

    if (pair == null) {
        EmptyNote(
            text = stringResource(R.string.gallery_no_pair),
            icon = Icons.Filled.CompareArrows,
        )
        return
    }

    var fraction by remember { mutableFloatStateOf(0.5f) }
    val density = LocalDensity.current
    val handleDescription = stringResource(R.string.cd_comparison_slider)

    HikayatCard {
        Column {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
                SectionHeader(
                    title = stringResource(R.string.gallery_then_and_now),
                    icon = Icons.Filled.CompareArrows,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.gallery_then_and_now_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.muted,
                )
            }
            Spacer(Modifier.height(12.dp))

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .semantics { contentDescription = handleDescription },
            ) {
                // Captured because the inner Box scopes shadow BoxWithConstraints.
                val fullWidth = maxWidth
                val widthPx = with(density) { fullWidth.toPx() }
                ArchivePhoto(
                    model = pair.after.displayModel(allowRemote),
                    contentDescription = pair.after.subject[language].ifBlank { null },
                    modifier = Modifier.fillMaxSize(),
                )
                // The historical photograph is clipped to the dragged fraction,
                // so the two are compared in place rather than side by side.
                Box(
                    Modifier
                        .fillMaxHeight()
                        .width(fullWidth * fraction)
                        .clip(androidx.compose.ui.graphics.RectangleShape),
                ) {
                    ArchivePhoto(
                        model = pair.before.displayModel(allowRemote),
                        contentDescription = pair.before.subject[language].ifBlank { null },
                        modifier = Modifier
                            .width(fullWidth)
                            .fillMaxHeight(),
                    )
                }
                Box(
                    Modifier
                        .align(Alignment.CenterStart)
                        .offset { IntOffset((widthPx * fraction).roundToInt() - 1, 0) }
                        .fillMaxHeight()
                        .width(2.dp)
                        .background(colors.amber),
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .pointerInput(widthPx) {
                            detectHorizontalDragGestures { change, _ ->
                                fraction = (change.position.x / widthPx).coerceIn(0f, 1f)
                            }
                        },
                )
                MetaTag(
                    label = pair.before.capturedAt.orEmpty(),
                    color = Color.White,
                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
                )
                MetaTag(
                    label = pair.after.capturedAt.orEmpty(),
                    color = Color.White,
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                )
            }

            Row(Modifier.fillMaxWidth()) {
                PhotoCredit(
                    credit = pair.before.credit,
                    license = pair.before.license,
                    sourceUrl = pair.before.sourceUrl,
                    bundled = pair.before.isBundled,
                    modifier = Modifier.weight(1f),
                )
            }
            PhotoCredit(
                credit = pair.after.credit,
                license = pair.after.license,
                sourceUrl = pair.after.sourceUrl,
                bundled = pair.after.isBundled,
            )
        }
    }
}
