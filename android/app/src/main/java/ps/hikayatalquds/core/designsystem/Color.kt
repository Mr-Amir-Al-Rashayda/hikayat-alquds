package ps.hikayatalquds.core.designsystem

import androidx.compose.ui.graphics.Color

/**
 * The Hikayat AlQuds palette, taken from the web app's `@theme` block so the
 * two clients are recognisably the same product.
 *
 * Light is the canonical look: warm Jerusalem stone, olive and amber. Dark is
 * not an inversion - a stone city does not read well as pure black - but the
 * same hues re-weighted against a deep olive-brown ground, which keeps the
 * photographs looking like photographs at night.
 */
object BrandColors {
    // Light - mirrors --color-brand-* in frontend/src/index.css
    val Olive = Color(0xFF5A5A40)
    val OliveDeep = Color(0xFF4A4A35)
    val Amber = Color(0xFFD97706)
    val AmberDeep = Color(0xFFB45F05)
    val Background = Color(0xFFF5F5F0)
    val Surface = Color(0xFFFFFFFF)
    val TextPrimary = Color(0xFF2D2D2A)
    val Border = Color(0xFFD1D1CA)
    val BorderLight = Color(0xFFE2E2DA)
    val Muted = Color(0xFF8A8A80)

    // Category accents, shared with the map legend and the constellation.
    val Sacred = Color(0xFFD97706)
    val Souq = Color(0xFF98723A)
    val Quarter = Color(0xFF5A5A40)
    val Neighbourhood = Color(0xFF688158)

    // Dark
    val DarkBackground = Color(0xFF16160F)
    val DarkSurface = Color(0xFF20201A)
    val DarkSurfaceHigh = Color(0xFF2A2A22)
    val DarkText = Color(0xFFEDEDE3)
    val DarkMuted = Color(0xFFA3A398)
    val DarkBorder = Color(0xFF3A3A30)
    val OliveLight = Color(0xFFB9BC92)
    val AmberLight = Color(0xFFF0A63C)

    // Shared semantics
    val Correct = Color(0xFF3F7A44)
    val Wrong = Color(0xFFB3261E)
    val TraditionTint = Color(0xFF98723A)
}
