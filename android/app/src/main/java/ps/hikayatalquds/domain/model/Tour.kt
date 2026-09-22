package ps.hikayatalquds.domain.model

import androidx.compose.runtime.Immutable

/** The tour can be narrated and written in a third language the interface does not use. */
enum class TourLanguage(val id: String, val label: String) {
    ARABIC("ar", "العربية"),
    ENGLISH("en", "English"),
    FRENCH("fr", "Français");

    companion object {
        fun fromId(id: String?): TourLanguage = entries.firstOrNull { it.id == id } ?: ARABIC
    }
}

/**
 * How long the visitor has. `stopCount` and `minutes` are the same figures the
 * web planner uses, so an itinerary built on either client comes out the same.
 */
enum class TourDuration(val id: String, val minutes: Int, val stopCount: Int) {
    EXPRESS("express", 110, 3),
    HALF_DAY("half_day", 240, 5),
    FULL_DAY("full_day", 480, 7),
    WEEKEND("weekend", 720, 8);

    companion object {
        fun fromId(id: String?): TourDuration = entries.firstOrNull { it.id == id } ?: HALF_DAY
    }
}

@Immutable
data class TourPreferences(
    val language: TourLanguage = TourLanguage.ARABIC,
    val duration: TourDuration = TourDuration.HALF_DAY,
    val interests: Set<TourInterest> = setOf(TourInterest.HISTORY, TourInterest.ARCHITECTURE),
)

@Immutable
data class ItineraryStop(
    val order: Int,
    /** Two-day routes use this to keep each day's walking coherent. */
    val day: Int,
    val locationId: String,
    val locationName: LocalizedText,
    val latitude: Double,
    val longitude: Double,
    val suggestedMinutes: Int,
    val walkFromPreviousMinutes: Int,
    val distanceFromPreviousMeters: Int,
    val bearingFromPreviousDegrees: Double?,
    val guidance: String,
    val arabicGuidance: String,
    val frenchGuidance: String,
    val matchedInterests: List<TourInterest>,
) {
    fun guidanceFor(language: TourLanguage): String = when (language) {
        TourLanguage.ARABIC -> arabicGuidance
        TourLanguage.FRENCH -> frenchGuidance
        TourLanguage.ENGLISH -> guidance
    }
}

@Immutable
data class Itinerary(
    val id: String,
    val createdAtEpochMillis: Long,
    val title: String,
    val preferences: TourPreferences,
    val totalMinutes: Int,
    val totalWalkingMinutes: Int,
    val stops: List<ItineraryStop>,
    val uncertaintyNotes: List<String>,
) {
    val days: Int get() = stops.maxOfOrNull { it.day } ?: 1
}

/** How close a site must be before offering to walk it. */
const val NEARBY_KILOMETRES = 2.0

@Immutable
data class NearestLocation(val location: Location, val kilometres: Double) {
    val isNearby: Boolean get() = kilometres <= NEARBY_KILOMETRES
}
