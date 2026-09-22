package ps.hikayatalquds.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import ps.hikayatalquds.R

/**
 * The Thmanyah family, converted from the web app's .woff2 faces by
 * `android/tools/prepare_media.py`.
 *
 * Three roles, same as the website: a sans for interface chrome, a serif for
 * reading, and a display serif for headings. Arabic and Latin are both covered
 * by the same files, so a language switch never changes the voice of the page.
 */
val ThmanyahSans = FontFamily(
    Font(R.font.thmanyah_sans_regular, FontWeight.Normal),
    Font(R.font.thmanyah_sans_medium, FontWeight.Medium),
    Font(R.font.thmanyah_sans_medium, FontWeight.SemiBold),
    Font(R.font.thmanyah_sans_bold, FontWeight.Bold),
    Font(R.font.thmanyah_sans_bold, FontWeight.Black),
)

val ThmanyahSerif = FontFamily(
    Font(R.font.thmanyah_serif_text_regular, FontWeight.Normal),
    Font(R.font.thmanyah_serif_text_regular, FontWeight.Medium),
    Font(R.font.thmanyah_serif_text_bold, FontWeight.Bold),
    Font(R.font.thmanyah_serif_text_bold, FontWeight.Black),
)

val ThmanyahDisplay = FontFamily(
    Font(R.font.thmanyah_serif_display_regular, FontWeight.Normal),
    Font(R.font.thmanyah_serif_display_bold, FontWeight.Bold),
    Font(R.font.thmanyah_serif_display_black, FontWeight.Black),
)

/**
 * Arabic needs more vertical room than Latin at the same point size; the line
 * heights here are set for Arabic and remain comfortable for English.
 */
private val readable = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

private fun display(size: Int, height: Int, weight: FontWeight = FontWeight.Black) = TextStyle(
    fontFamily = ThmanyahDisplay,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = height.sp,
    lineHeightStyle = readable,
)

private fun serif(size: Int, height: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = ThmanyahSerif,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = height.sp,
    lineHeightStyle = readable,
)

private fun sans(size: Int, height: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = ThmanyahSans,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = height.sp,
    lineHeightStyle = readable,
)

val HikayatTypography = Typography(
    displayLarge = display(40, 50),
    displayMedium = display(33, 42),
    displaySmall = display(27, 36),

    headlineLarge = display(24, 32),
    headlineMedium = display(21, 29),
    headlineSmall = serif(18, 26, FontWeight.Bold),

    titleLarge = serif(18, 26, FontWeight.Bold),
    titleMedium = sans(15, 22, FontWeight.SemiBold),
    titleSmall = sans(13, 19, FontWeight.SemiBold),

    // Body is the serif: this app is mostly for reading.
    bodyLarge = serif(16, 27),
    bodyMedium = serif(14, 24),
    bodySmall = sans(12, 19),

    labelLarge = sans(13, 18, FontWeight.Bold),
    labelMedium = sans(11, 16, FontWeight.Bold),
    labelSmall = sans(10, 15, FontWeight.Medium),
)
