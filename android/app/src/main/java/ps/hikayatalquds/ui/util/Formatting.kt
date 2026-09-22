package ps.hikayatalquds.ui.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import ps.hikayatalquds.domain.model.AppLanguage
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Small formatting and hand-off helpers.
 *
 * Kept separate from the screens because a date or a distance rendered one way
 * on the home screen and another way on the plan screen reads as two different
 * apps.
 */

/** "just now", "3 hours ago", or a date once it is no longer usefully relative. */
@Composable
fun relativeTimeLabel(epochMillis: Long, language: AppLanguage): String? {
    if (epochMillis <= 0L) return null
    val arabic = language.isArabic
    val elapsed = System.currentTimeMillis() - epochMillis
    val minutes = elapsed / 60_000
    val hours = minutes / 60
    val days = hours / 24

    return when {
        minutes < 2 -> if (arabic) "الآن" else "just now"
        minutes < 60 -> if (arabic) "قبل $minutes دقيقة" else "$minutes min ago"
        hours < 24 -> if (arabic) "قبل $hours ساعة" else "$hours h ago"
        days < 7 -> if (arabic) "قبل $days يوم" else "$days d ago"
        else -> formatDate(epochMillis, language)
    }
}

fun formatDate(epochMillis: Long, language: AppLanguage): String =
    DateTimeFormatter
        .ofPattern("d MMM yyyy", Locale.forLanguageTag(language.tag))
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(epochMillis))

/** Formats an ISO-8601 instant from the API; falls back to the raw text. */
fun formatIsoDate(value: String?, language: AppLanguage): String? {
    if (value.isNullOrBlank()) return null
    return runCatching { formatDate(Instant.parse(value).toEpochMilli(), language) }
        .getOrDefault(value)
}

fun formatDistance(meters: Int, language: AppLanguage): String = when {
    meters >= 1000 -> {
        val km = "%.1f".format(Locale.forLanguageTag(language.tag), meters / 1000.0)
        if (language.isArabic) "$km كم" else "$km km"
    }

    else -> if (language.isArabic) "$meters م" else "$meters m"
}

fun formatMinutes(minutes: Int, language: AppLanguage): String =
    if (language.isArabic) "$minutes دقيقة" else "$minutes min"

/**
 * Hands a walk to whatever maps app the phone has.
 *
 * `geo:` with a query is the documented way to ask for directions without
 * assuming Google Maps is installed. If nothing handles it, the caller is told
 * rather than the tap silently doing nothing.
 */
fun Context.openWalkingDirections(
    latitude: Double,
    longitude: Double,
    label: String,
): Boolean {
    val encoded = Uri.encode(label)
    val attempts = listOf(
        Uri.parse("google.navigation:q=$latitude,$longitude&mode=w"),
        Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude($encoded)"),
        Uri.parse("https://www.openstreetmap.org/directions?route=;$latitude%2C$longitude"),
    )
    attempts.forEach { uri ->
        val intent = Intent(Intent.ACTION_VIEW, uri)
        try {
            startActivity(intent)
            return true
        } catch (_: ActivityNotFoundException) {
            // Try the next form.
        }
    }
    return false
}

/** Opens the system text-to-speech settings so a voice can be installed. */
fun Context.openSpeechSettings(): Boolean = try {
    startActivity(Intent("com.android.settings.TTS_SETTINGS"))
    true
} catch (_: ActivityNotFoundException) {
    false
}

fun Context.shareText(text: String, title: String): Boolean {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        putExtra(Intent.EXTRA_SUBJECT, title)
    }
    return try {
        startActivity(Intent.createChooser(intent, title))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}
