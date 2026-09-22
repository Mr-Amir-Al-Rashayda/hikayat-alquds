package ps.hikayatalquds.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import ps.hikayatalquds.R
import ps.hikayatalquds.core.designsystem.HikayatTheme

/**
 * A photograph that fails honestly.
 *
 * Thirty-four of the sixty-four photographs ship inside the APK; the rest are
 * hosted on Wikimedia Commons. When one of those cannot be fetched the frame
 * says so in words - a broken image icon reads as a bug, and a silently hidden
 * image reads as "there is no photograph of this place", which is untrue.
 */
@Composable
fun ArchivePhoto(
    model: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    unavailableMessage: String = stringResource(R.string.gallery_offline_placeholder),
) {
    val colors = HikayatTheme.colors
    Box(modifier = modifier.background(colors.pageBackground)) {
        if (model == null) {
            PhotoUnavailable(unavailableMessage)
            return@Box
        }
        SubcomposeAsyncImage(
            model = model,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = Modifier.fillMaxSize(),
            loading = { ShimmerBlock(Modifier.fillMaxSize()) },
            error = { PhotoUnavailable(unavailableMessage) },
        )
    }
}

@Composable
private fun PhotoUnavailable(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HikayatTheme.colors.pageBackground)
            .padding(18.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Filled.CloudOff,
            contentDescription = null,
            tint = HikayatTheme.colors.muted,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.labelSmall,
            color = HikayatTheme.colors.muted,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * A cover photograph with a legible dark gradient at the foot, so a title can
 * sit on top of any photograph without a per-image judgement call.
 */
@Composable
fun CoverPhoto(
    model: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    overlay: (@Composable () -> Unit)? = null,
) {
    Box(modifier = modifier) {
        ArchivePhoto(
            model = model,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.35f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.66f),
                    ),
                ),
        )
        if (overlay != null) {
            Box(modifier = Modifier.fillMaxSize()) { overlay() }
        }
    }
}

/** Placeholder used where the archive genuinely holds no photograph. */
@Composable
fun NoPhotoPanel(message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(HikayatTheme.colors.pageBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Filled.PhotoCamera,
            contentDescription = null,
            tint = HikayatTheme.colors.muted,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = HikayatTheme.colors.muted,
            textAlign = TextAlign.Center,
        )
    }
}
