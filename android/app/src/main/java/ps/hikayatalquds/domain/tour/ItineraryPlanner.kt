package ps.hikayatalquds.domain.tour

import ps.hikayatalquds.domain.model.AppLanguage
import ps.hikayatalquds.domain.model.Itinerary
import ps.hikayatalquds.domain.model.ItineraryStop
import ps.hikayatalquds.domain.model.Location
import ps.hikayatalquds.domain.model.LocalizedText
import ps.hikayatalquds.domain.model.NearestLocation
import ps.hikayatalquds.domain.model.TourDuration
import ps.hikayatalquds.domain.model.TourInterest
import ps.hikayatalquds.domain.model.TourLanguage
import ps.hikayatalquds.domain.model.TourPreferences
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Builds a walking route through Jerusalem from what the visitor tells it.
 *
 * Deliberately the same algorithm as the web planner, figure for figure, so a
 * route planned on a laptop and one planned on a phone are the same route:
 * score each place against the chosen interests, take as many as the available
 * time allows, then order them greedily by walking distance to cut backtracking.
 *
 * The times it prints are estimates of *walking*, at 4.5 km/h in a straight
 * line. The Old City is stairs, gates and closures, so the route says so in its
 * own uncertainty notes rather than implying a precision it does not have.
 */
@Singleton
class ItineraryPlanner @Inject constructor() {

    fun build(preferences: TourPreferences, locations: List<Location>): Itinerary {
        val duration = preferences.duration
        val ranked = locations.sortedWith(
            compareByDescending<Location> { score(it, preferences.interests) }.thenBy { it.id },
        )

        val remaining = ranked.take(duration.stopCount).toMutableList()
        val ordered = mutableListOf<Location>()
        if (remaining.isNotEmpty()) ordered += remaining.removeAt(0)
        while (remaining.isNotEmpty()) {
            val last = ordered.last()
            remaining.sortWith(
                compareBy<Location> { leg(last, it).minutes }.thenBy { it.id },
            )
            ordered += remaining.removeAt(0)
        }

        // Roughly 72% of the day is time on your feet at the stops; the rest is
        // walking, waiting and the things a route cannot plan.
        val stopMinutes = maxOf(25, (duration.minutes * 0.72 / maxOf(ordered.size, 1)).toInt())
        var walking = 0

        val stops = ordered.mapIndexed { index, location ->
            val previous = ordered.getOrNull(index - 1)
            val leg = previous?.let { leg(it, location) } ?: Leg(0, 0, null)
            walking += leg.minutes
            val matched = location.interests.filter { it in preferences.interests }
            ItineraryStop(
                order = index + 1,
                day = if (duration == TourDuration.WEEKEND &&
                    index >= ceil(ordered.size / 2.0).toInt()
                ) 2 else 1,
                locationId = location.id,
                locationName = location.name,
                latitude = location.latitude,
                longitude = location.longitude,
                suggestedMinutes = stopMinutes,
                walkFromPreviousMinutes = leg.minutes,
                distanceFromPreviousMeters = leg.meters,
                bearingFromPreviousDegrees = leg.bearing,
                guidance = location.landmarks.en.take(2).joinToString(" · ")
                    .ifBlank { "Follow the reviewed location story." },
                arabicGuidance = location.landmarks.ar.take(2).joinToString(" · ")
                    .ifBlank { "اتبع حكاية المكان المراجعة." },
                frenchGuidance = location.landmarks.en.take(2).joinToString(" · ")
                    .ifBlank { "Suivez le récit documenté de ce lieu." },
                matchedInterests = matched,
            )
        }

        return Itinerary(
            id = "quds-${System.currentTimeMillis()}",
            createdAtEpochMillis = System.currentTimeMillis(),
            title = when (preferences.language) {
                TourLanguage.ARABIC -> "مساري الذكي في القدس"
                TourLanguage.FRENCH -> "Mon itinéraire intelligent à Jérusalem"
                TourLanguage.ENGLISH -> "My smart Jerusalem route"
            },
            preferences = preferences,
            totalMinutes = stops.sumOf { it.suggestedMinutes + it.walkFromPreviousMinutes },
            totalWalkingMinutes = walking,
            stops = stops,
            uncertaintyNotes = uncertaintyNotes(preferences.language),
        )
    }

    /**
     * How well a place matches the chosen interests. Direct matches dominate;
     * the ordered priority lists break ties between places that all match, so
     * "history" opens on Maghariba rather than whichever id sorts first.
     */
    internal fun score(location: Location, interests: Set<TourInterest>): Int {
        val matches = location.interests.count { it in interests }
        val priority = interests.sumOf { interest ->
            val order = INTEREST_PRIORITY[interest].orEmpty()
            val index = order.indexOf(location.id)
            if (index < 0) 0 else (order.size - index) * 10
        }
        return matches * 100 + priority
    }

    private data class Leg(val minutes: Int, val meters: Int, val bearing: Double?)

    /**
     * Distance on the local flat approximation: at Jerusalem's latitude a
     * degree of latitude is ~111 km and a degree of longitude ~94 km. Over a
     * few hundred metres this is indistinguishable from the great circle and
     * costs no trigonometry.
     */
    private fun leg(from: Location, to: Location): Leg {
        val latKm = (from.latitude - to.latitude) * 111
        val lngKm = (from.longitude - to.longitude) * 94
        val distanceKm = hypot(latKm, lngKm)

        val lat1 = Math.toRadians(from.latitude)
        val lat2 = Math.toRadians(to.latitude)
        val deltaLng = Math.toRadians(to.longitude - from.longitude)
        val y = sin(deltaLng) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(deltaLng)

        return Leg(
            minutes = maxOf(3, (distanceKm / WALKING_KM_PER_HOUR * 60).roundToInt()),
            meters = (distanceKm * 1000 / 10).roundToInt() * 10,
            bearing = (Math.toDegrees(atan2(y, x)) + 360) % 360,
        )
    }

    private fun uncertaintyNotes(language: TourLanguage): List<String> = when (language) {
        TourLanguage.ARABIC -> listOf(
            "أوقات المشي تقديرية؛ افتح الاتجاهات الحية لكل مقطع وراعِ مداخل الأبواب وساعات الفتح والتضاريس والظروف المحلية.",
            "تعتمد التوصيات على سجلات حكاية القدس المراجعة ولا تضيف ادعاءات تاريخية غير موثقة.",
        )

        TourLanguage.FRENCH -> listOf(
            "Les temps de marche sont approximatifs : ouvrez l'itinéraire en direct pour chaque étape et vérifiez les accès, les horaires, le relief et les conditions locales.",
            "Les recommandations reposent uniquement sur les dossiers vérifiés de Hikayat AlQuds.",
        )

        TourLanguage.ENGLISH -> listOf(
            "Walking times are approximate; open live directions for each leg and account for gate access, opening hours, terrain and current local conditions.",
            "Recommendations use reviewed Hikayat AlQuds records; the route does not invent historical claims.",
        )
    }

    /**
     * Finds the closest place to a coordinate, using the great-circle distance
     * because this one can be tens of kilometres rather than a few hundred metres.
     */
    fun nearest(latitude: Double, longitude: Double, locations: List<Location>): NearestLocation? =
        locations.minByOrNull { greatCircleKm(latitude, longitude, it.latitude, it.longitude) }
            ?.let { NearestLocation(it, greatCircleKm(latitude, longitude, it.latitude, it.longitude)) }

    internal fun greatCircleKm(aLat: Double, aLng: Double, bLat: Double, bLng: Double): Double {
        val dLat = Math.toRadians(bLat - aLat)
        val dLng = Math.toRadians(bLng - aLng)
        val h = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(aLat)) * cos(Math.toRadians(bLat)) * sin(dLng / 2) * sin(dLng / 2)
        return 2 * EARTH_RADIUS_KM * kotlin.math.asin(minOf(1.0, kotlin.math.sqrt(h)))
    }

    companion object {
        const val WALKING_KM_PER_HOUR = 4.5
        const val EARTH_RADIUS_KM = 6371.0

        /**
         * Which places serve which interest best, in order. Taken from the web
         * planner so both clients recommend the same walk.
         */
        val INTEREST_PRIORITY: Map<TourInterest, List<String>> = mapOf(
            TourInterest.HISTORY to listOf(
                "maghariba-quarter", "bab-al-amud", "muslim-quarter", "christian-quarter",
                "armenian-quarter", "sheikh-jarrah", "silwan", "at-tur",
            ),
            TourInterest.ARCHITECTURE to listOf(
                "muslim-quarter", "christian-quarter", "armenian-quarter", "bab-al-amud",
                "sheikh-jarrah", "maghariba-quarter", "silwan", "at-tur",
            ),
            TourInterest.RELIGIOUS to listOf(
                "christian-quarter", "muslim-quarter", "at-tur", "armenian-quarter",
                "maghariba-quarter", "bab-al-amud",
            ),
            TourInterest.FOOD_MARKETS to listOf(
                "bab-al-amud", "muslim-quarter", "christian-quarter",
            ),
            TourInterest.ORAL_HERITAGE to listOf(
                "silwan", "sheikh-jarrah", "maghariba-quarter", "bab-al-amud",
                "armenian-quarter", "muslim-quarter",
            ),
        )
    }
}

/** A compass word for a bearing, so a direction can be read aloud. */
fun bearingLabel(degrees: Double?, language: AppLanguage): String? {
    if (degrees == null) return null
    val names = if (language.isArabic) {
        listOf("شمالاً", "شمال شرق", "شرقاً", "جنوب شرق", "جنوباً", "جنوب غرب", "غرباً", "شمال غرب")
    } else {
        listOf("north", "north-east", "east", "south-east", "south", "south-west", "west", "north-west")
    }
    val index = (((degrees % 360) + 360) % 360 / 45).roundToInt() % 8
    return names[index]
}

/** Short label for a walking leg, e.g. "6 min · 420 m". */
fun legLabel(minutes: Int, meters: Int, language: AppLanguage): String {
    val distance = if (meters >= 1000) {
        val km = meters / 1000.0
        if (language.isArabic) "${"%.1f".format(km)} كم" else "${"%.1f".format(km)} km"
    } else {
        if (language.isArabic) "$meters م" else "$meters m"
    }
    val time = if (language.isArabic) "$minutes دقيقة" else "$minutes min"
    return "$time · $distance"
}

/** Human title for the localized name, honouring the interface language. */
fun LocalizedText.orName(language: AppLanguage, fallback: String): String =
    this[language].ifBlank { fallback }
