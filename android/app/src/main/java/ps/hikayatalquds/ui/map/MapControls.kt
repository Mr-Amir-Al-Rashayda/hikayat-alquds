package ps.hikayatalquds.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ps.hikayatalquds.R
import ps.hikayatalquds.core.designsystem.HikayatTheme

/**
 * Zoom and recentre, floating over the drawing.
 *
 * Pinch works, but a one-handed reader on a bus cannot pinch, so the buttons
 * are not decoration.
 */
@Composable
fun BoxScope.MapControls(
    zoom: Float,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onReset: () -> Unit,
) {
    Column(
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MapButton(
            icon = Icons.Filled.Add,
            description = stringResource(R.string.map_zoom_in),
            onClick = onZoomIn,
        )
        MapButton(
            icon = Icons.Filled.Remove,
            description = stringResource(R.string.map_zoom_out),
            onClick = onZoomOut,
        )
        if (zoom > 1f) {
            MapButton(
                icon = Icons.Filled.CenterFocusStrong,
                description = stringResource(R.string.map_reset_view),
                onClick = onReset,
            )
        }
    }
}

@Composable
private fun MapButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, HikayatTheme.colors.cardBorder),
        modifier = Modifier.size(38.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(9.dp),
        )
    }
}
