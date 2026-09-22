package ps.hikayatalquds.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

/**
 * Colours the Material scheme has no slot for but the design uses everywhere:
 * the parchment page ground, hairline borders, the muted caption grey and the
 * four category accents shared with the map legend.
 */
@Immutable
data class HikayatExtendedColors(
    val pageBackground: Color,
    val cardBorder: Color,
    val hairline: Color,
    val muted: Color,
    val amber: Color,
    val onAmber: Color,
    val correct: Color,
    val wrong: Color,
    val tradition: Color,
    val sacred: Color,
    val souq: Color,
    val quarter: Color,
    val neighbourhood: Color,
    val isDark: Boolean,
)

val LocalHikayatColors = staticCompositionLocalOf {
    HikayatExtendedColors(
        pageBackground = BrandColors.Background,
        cardBorder = BrandColors.Border,
        hairline = BrandColors.BorderLight,
        muted = BrandColors.Muted,
        amber = BrandColors.Amber,
        onAmber = Color.White,
        correct = BrandColors.Correct,
        wrong = BrandColors.Wrong,
        tradition = BrandColors.TraditionTint,
        sacred = BrandColors.Sacred,
        souq = BrandColors.Souq,
        quarter = BrandColors.Quarter,
        neighbourhood = BrandColors.Neighbourhood,
        isDark = false,
    )
}

private val LightScheme = lightColorScheme(
    primary = BrandColors.Olive,
    onPrimary = BrandColors.Background,
    primaryContainer = Color(0xFFE6E7D6),
    onPrimaryContainer = BrandColors.OliveDeep,
    secondary = BrandColors.Amber,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFBEBD5),
    onSecondaryContainer = Color(0xFF6B3A02),
    tertiary = BrandColors.Souq,
    onTertiary = Color.White,
    background = BrandColors.Background,
    onBackground = BrandColors.TextPrimary,
    surface = BrandColors.Surface,
    onSurface = BrandColors.TextPrimary,
    surfaceVariant = Color(0xFFEDEDE4),
    onSurfaceVariant = Color(0xFF5B5B52),
    outline = BrandColors.Border,
    outlineVariant = BrandColors.BorderLight,
    error = BrandColors.Wrong,
    onError = Color.White,
)

private val DarkScheme = darkColorScheme(
    primary = BrandColors.OliveLight,
    onPrimary = Color(0xFF23230F),
    primaryContainer = Color(0xFF3B3B2A),
    onPrimaryContainer = Color(0xFFDCDEBB),
    secondary = BrandColors.AmberLight,
    onSecondary = Color(0xFF3A1F00),
    secondaryContainer = Color(0xFF53330A),
    onSecondaryContainer = Color(0xFFFBD9A8),
    tertiary = Color(0xFFD5B182),
    onTertiary = Color(0xFF3A2A12),
    background = BrandColors.DarkBackground,
    onBackground = BrandColors.DarkText,
    surface = BrandColors.DarkSurface,
    onSurface = BrandColors.DarkText,
    surfaceVariant = BrandColors.DarkSurfaceHigh,
    onSurfaceVariant = BrandColors.DarkMuted,
    outline = BrandColors.DarkBorder,
    outlineVariant = Color(0xFF2E2E26),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
)

/** Generous radii; the web app leans on rounded 2xl/3xl cards throughout. */
val HikayatShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

@Composable
fun HikayatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val scheme = if (darkTheme) DarkScheme else LightScheme
    val extended = if (darkTheme) {
        HikayatExtendedColors(
            pageBackground = BrandColors.DarkBackground,
            cardBorder = BrandColors.DarkBorder,
            hairline = Color(0xFF2E2E26),
            muted = BrandColors.DarkMuted,
            amber = BrandColors.AmberLight,
            onAmber = Color(0xFF2A1700),
            correct = Color(0xFF8FD69A),
            wrong = Color(0xFFF2B8B5),
            tradition = Color(0xFFD5B182),
            sacred = BrandColors.AmberLight,
            souq = Color(0xFFC79A62),
            quarter = BrandColors.OliveLight,
            neighbourhood = Color(0xFF9EBE8B),
            isDark = true,
        )
    } else {
        HikayatExtendedColors(
            pageBackground = BrandColors.Background,
            cardBorder = BrandColors.Border,
            hairline = BrandColors.BorderLight,
            muted = BrandColors.Muted,
            amber = BrandColors.Amber,
            onAmber = Color.White,
            correct = BrandColors.Correct,
            wrong = BrandColors.Wrong,
            tradition = BrandColors.TraditionTint,
            sacred = BrandColors.Sacred,
            souq = BrandColors.Souq,
            quarter = BrandColors.Quarter,
            neighbourhood = BrandColors.Neighbourhood,
            isDark = false,
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalHikayatColors provides extended) {
        MaterialTheme(
            colorScheme = scheme,
            typography = HikayatTypography,
            shapes = HikayatShapes,
            content = content,
        )
    }
}

/** Shorthand for the extended palette: `HikayatTheme.colors.muted`. */
object HikayatTheme {
    val colors: HikayatExtendedColors
        @Composable @ReadOnlyComposable get() = LocalHikayatColors.current
}
