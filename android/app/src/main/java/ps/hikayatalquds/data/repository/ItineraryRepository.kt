package ps.hikayatalquds.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ps.hikayatalquds.data.preferences.UserPreferencesRepository
import ps.hikayatalquds.domain.model.Itinerary
import ps.hikayatalquds.domain.model.ItineraryStop
import ps.hikayatalquds.domain.model.LocalizedText
import ps.hikayatalquds.domain.model.TourDuration
import ps.hikayatalquds.domain.model.TourInterest
import ps.hikayatalquds.domain.model.TourLanguage
import ps.hikayatalquds.domain.model.TourPreferences
import ps.hikayatalquds.domain.tour.ItineraryPlanner
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The route the visitor built, kept so it survives closing the app mid-walk.
 *
 * Stored as JSON in the same preference store the web app mirrors in
 * `localStorage`, and validated on the way back in: a route restored from a
 * half-written or hand-edited record is discarded rather than drawn with
 * missing coordinates.
 */
@Singleton
class ItineraryRepository @Inject constructor(
    private val preferences: UserPreferencesRepository,
    private val planner: ItineraryPlanner,
    private val content: ContentRepository,
    private val json: Json,
) {
    val itinerary: Flow<Itinerary?> = preferences.itineraryJson.map { stored ->
        stored?.let { raw ->
            runCatching { json.decodeFromString<ItineraryRecord>(raw).toDomain() }
                .getOrNull()
                ?.takeIf { it.isUsable() }
        }
    }

    val lastPreferences: Flow<TourPreferences> = preferences.tourPreferencesJson.map { stored ->
        stored?.let { raw ->
            runCatching { json.decodeFromString<PreferencesRecord>(raw).toDomain() }.getOrNull()
        } ?: TourPreferences()
    }

    suspend fun build(tourPreferences: TourPreferences): Itinerary {
        val locations = content.locations.first()
        val built = planner.build(tourPreferences, locations)
        save(built)
        preferences.saveTourPreferences(
            json.encodeToString(PreferencesRecord.from(tourPreferences)),
        )
        return built
    }

    suspend fun save(itinerary: Itinerary) {
        preferences.saveItinerary(json.encodeToString(ItineraryRecord.from(itinerary)))
        preferences.setPlannedWalkingMinutes(itinerary.totalWalkingMinutes)
    }

    suspend fun clear() {
        preferences.saveItinerary(null)
        preferences.setPlannedWalkingMinutes(0)
    }

    suspend fun current(): Itinerary? = itinerary.first()
}

/** A route is only usable if every stop has real coordinates to walk to. */
internal fun Itinerary.isUsable(): Boolean = stops.isNotEmpty() && stops.all { stop ->
    stop.latitude.isFinite() && stop.longitude.isFinite() &&
        stop.latitude != 0.0 && stop.longitude != 0.0
}

// --- persisted form ----------------------------------------------------------

@Serializable
internal data class PreferencesRecord(
    val language: String,
    val duration: String,
    val interests: List<String>,
) {
    fun toDomain() = TourPreferences(
        language = TourLanguage.fromId(language),
        duration = TourDuration.fromId(duration),
        interests = interests.mapNotNull(TourInterest::fromId).toSet(),
    )

    companion object {
        fun from(preferences: TourPreferences) = PreferencesRecord(
            language = preferences.language.id,
            duration = preferences.duration.id,
            interests = preferences.interests.map { it.id },
        )
    }
}

@Serializable
internal data class StopRecord(
    val order: Int,
    val day: Int = 1,
    val locationId: String,
    val name: String,
    val arabicName: String,
    val latitude: Double,
    val longitude: Double,
    val suggestedMinutes: Int,
    val walkFromPreviousMinutes: Int,
    val distanceFromPreviousMeters: Int,
    val bearingFromPreviousDegrees: Double? = null,
    val guidance: String = "",
    val arabicGuidance: String = "",
    val frenchGuidance: String = "",
    val matchedInterests: List<String> = emptyList(),
) {
    fun toDomain() = ItineraryStop(
        order = order,
        day = day,
        locationId = locationId,
        locationName = LocalizedText(name, arabicName),
        latitude = latitude,
        longitude = longitude,
        suggestedMinutes = suggestedMinutes,
        walkFromPreviousMinutes = walkFromPreviousMinutes,
        distanceFromPreviousMeters = distanceFromPreviousMeters,
        bearingFromPreviousDegrees = bearingFromPreviousDegrees,
        guidance = guidance,
        arabicGuidance = arabicGuidance,
        frenchGuidance = frenchGuidance,
        matchedInterests = matchedInterests.mapNotNull(TourInterest::fromId),
    )

    companion object {
        fun from(stop: ItineraryStop) = StopRecord(
            order = stop.order,
            day = stop.day,
            locationId = stop.locationId,
            name = stop.locationName.en,
            arabicName = stop.locationName.ar,
            latitude = stop.latitude,
            longitude = stop.longitude,
            suggestedMinutes = stop.suggestedMinutes,
            walkFromPreviousMinutes = stop.walkFromPreviousMinutes,
            distanceFromPreviousMeters = stop.distanceFromPreviousMeters,
            bearingFromPreviousDegrees = stop.bearingFromPreviousDegrees,
            guidance = stop.guidance,
            arabicGuidance = stop.arabicGuidance,
            frenchGuidance = stop.frenchGuidance,
            matchedInterests = stop.matchedInterests.map { it.id },
        )
    }
}

@Serializable
internal data class ItineraryRecord(
    val id: String,
    @SerialName("createdAt") val createdAtEpochMillis: Long,
    val title: String,
    val preferences: PreferencesRecord,
    val totalMinutes: Int,
    val totalWalkingMinutes: Int,
    val stops: List<StopRecord>,
    val uncertaintyNotes: List<String> = emptyList(),
) {
    fun toDomain() = Itinerary(
        id = id,
        createdAtEpochMillis = createdAtEpochMillis,
        title = title,
        preferences = preferences.toDomain(),
        totalMinutes = totalMinutes,
        totalWalkingMinutes = totalWalkingMinutes,
        stops = stops.map { it.toDomain() },
        uncertaintyNotes = uncertaintyNotes,
    )

    companion object {
        fun from(itinerary: Itinerary) = ItineraryRecord(
            id = itinerary.id,
            createdAtEpochMillis = itinerary.createdAtEpochMillis,
            title = itinerary.title,
            preferences = PreferencesRecord.from(itinerary.preferences),
            totalMinutes = itinerary.totalMinutes,
            totalWalkingMinutes = itinerary.totalWalkingMinutes,
            stops = itinerary.stops.map(StopRecord::from),
            uncertaintyNotes = itinerary.uncertaintyNotes,
        )
    }
}
