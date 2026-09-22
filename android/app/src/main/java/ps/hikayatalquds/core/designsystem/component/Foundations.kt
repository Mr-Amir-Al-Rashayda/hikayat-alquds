package ps.hikayatalquds.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import android.provider.Settings
import ps.hikayatalquds.core.designsystem.HikayatTheme

/**
 * The building blocks the screens are assembled from.
 *
 * The web app's look comes from a few repeated moves - a hairline-bordered
 * white card on a parchment ground, an olive header band with a faint tatreez
 * dot grid, amber for anything that is a claim about provenance. These put
 * those moves in one place so they stay consistent, and so a change to the
 * design is a change here rather than in nineteen screens.
 */

/** The bordered card used for nearly every block of content. */
@Composable
fun HikayatCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    accent: Color? = null,
    content: @Composable () -> Unit,
) {
    val colors = HikayatTheme.colors
    val border = BorderStroke(1.dp, accent ?: colors.cardBorder)
    if (onClick == null) {
        Card(
            modifier = modifier,
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = border,
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) { content() }
    } else {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = border,
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) { content() }
    }
}

/**
 * Traditional Palestinian embroidery reduced to its grid: a field of dots.
 *
 * Drawn rather than shipped as an image so it scales to any header and tints
 * with the theme. Marked as decorative for screen readers.
 */
@Composable
fun TatreezField(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    alpha: Float = 0.10f,
    spacing: Int = 22,
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .clearAndSetSemantics { },
    ) {
        val step = spacing.dp.toPx()
        val radius = 1.6.dp.toPx()
        var y = step / 2
        while (y < size.height) {
            var x = step / 2
            while (x < size.width) {
                drawCircle(color = color, radius = radius, center = Offset(x, y), alpha = alpha)
                x += step
            }
            y += step
        }
    }
}

/** The olive header band that opens most screens. */
@Composable
fun OliveHeader(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primary,
    ) {
        Box {
            TatreezField(color = MaterialTheme.colorScheme.onPrimary)
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (eyebrow != null) {
                        Text(
                            text = eyebrow,
                            style = MaterialTheme.typography.labelMedium,
                            color = HikayatTheme.colors.amber,
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    if (subtitle != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f),
                        )
                    }
                }
                if (trailing != null) {
                    Spacer(Modifier.width(12.dp))
                    trailing()
                }
            }
        }
    }
}

/** A section title with an amber icon, used down the length of a screen. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = HikayatTheme.colors.amber,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
    }
}

/** A selectable pill: category filters, audience choices, interests. */
@Composable
fun ChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
) {
    val colors = HikayatTheme.colors
    val background by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surface
        },
        label = "chip-background",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        label = "chip-content",
    )

    Surface(
        modifier = modifier.clip(CircleShape).clickable(onClick = onClick),
        shape = CircleShape,
        color = background,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else colors.cardBorder),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            leading?.invoke()
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
            )
        }
    }
}

/** A small non-interactive tag: a licence, a category, "bundled offline". */
@Composable
fun MetaTag(
    label: String,
    modifier: Modifier = Modifier,
    color: Color = HikayatTheme.colors.muted,
    icon: ImageVector? = null,
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .border(1.dp, color.copy(alpha = 0.35f), CircleShape)
            .background(color.copy(alpha = 0.08f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
        }
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

/** A counter with a caption, as used on the location cards and the keepsake. */
@Composable
fun StatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(HikayatTheme.colors.pageBackground)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = HikayatTheme.colors.muted, modifier = Modifier.size(11.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = HikayatTheme.colors.muted,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * A placeholder block that shimmers while real content loads.
 *
 * The pulse respects the system animator setting. That is an accessibility
 * requirement - somebody who has turned animations off should not be given a
 * pulsing rectangle - and it also keeps the app testable: an infinite
 * animation never lets Compose report itself idle, so a UI test that touched a
 * screen while one was running would wait forever.
 */
@Composable
fun ShimmerBlock(modifier: Modifier = Modifier, shape: RoundedCornerShape = RoundedCornerShape(12.dp)) {
    val animated = animationsEnabled()
    val alpha = if (animated) {
        val transition = rememberInfiniteTransition(label = "shimmer")
        transition.animateFloat(
            initialValue = 0.35f,
            targetValue = 0.75f,
            animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
            label = "shimmer-alpha",
        ).value
    } else {
        0.5f
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        HikayatTheme.colors.hairline.copy(alpha = alpha),
                        HikayatTheme.colors.hairline.copy(alpha = alpha * 0.6f),
                    ),
                ),
            ),
    )
}

/**
 * Whether decorative motion should run at all: false in a Compose preview, and
 * false when the reader (or an instrumented test) has turned animations off.
 */
@Composable
fun animationsEnabled(): Boolean {
    if (LocalInspectionMode.current) return false
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) > 0f
    }
}

/** Says plainly that a section is empty, rather than rendering nothing. */
@Composable
fun EmptyNote(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(HikayatTheme.colors.pageBackground)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = HikayatTheme.colors.muted, modifier = Modifier.size(18.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = HikayatTheme.colors.muted,
        )
    }
}

/** A thin olive-to-amber rule; the web app opens every page with it. */
@Composable
fun BrandRule(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary,
                        HikayatTheme.colors.amber,
                        MaterialTheme.colorScheme.primary,
                    ),
                ),
            ),
    )
}

@Composable
fun BoldLabel(text: String, modifier: Modifier = Modifier, color: Color = HikayatTheme.colors.muted) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = modifier,
    )
}
