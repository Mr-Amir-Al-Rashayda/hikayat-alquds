package ps.hikayatalquds.data.preferences

import androidx.compose.runtime.Immutable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import ps.hikayatalquds.domain.model.AppLanguage
import ps.hikayatalquds.domain.model.JourneyProgress
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Everything the reader has chosen, plus the private record of where they have been. */
@Immutable
data class UserSettings(
    val language: AppLanguage = AppLanguage.ARABIC,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val apiBaseUrl: String = "",
    val allowRemoteImages: Boolean = true,
    val narrationRate: Float = 0.92f,
    val narrationPitch: Float = 1.0f,
    val narrationVoice: String = "",
    val dailyStoryReminder: Boolean = false,
    val onboardingComplete: Boolean = false,
    val lastSyncedAtEpochMillis: Long = 0L,
    val seededContentVersion: Int = 0,
)

/**
 * The reader's settings and their private journey, kept in DataStore.
 *
 * There are no accounts in Hikayat AlQuds, so this file is the whole of what
 * the product knows about a person, it never leaves the device, and Settings
 * can erase all of it.
 */
@Singleton
class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val THEME = stringPreferencesKey("theme_mode")
        val API_BASE_URL = stringPreferencesKey("api_base_url")
        val ALLOW_REMOTE_IMAGES = booleanPreferencesKey("allow_remote_images")
        val NARRATION_RATE = floatPreferencesKey("narration_rate")
        val NARRATION_PITCH = floatPreferencesKey("narration_pitch")
        val NARRATION_VOICE = stringPreferencesKey("narration_voice")
        val DAILY_REMINDER = booleanPreferencesKey("daily_story_reminder")
        val ONBOARDING = booleanPreferencesKey("onboarding_complete")
        val LAST_SYNCED = longPreferencesKey("last_synced_at")
        val SEEDED_VERSION = intPreferencesKey("seeded_content_version")

        val VISITED = stringSetPreferencesKey("visited_locations")
        val LAST_VISITED = stringPreferencesKey("last_visited_location")
        val BOOKMARKED = stringSetPreferencesKey("bookmarked_locations")
        val QUIZZES = stringSetPreferencesKey("completed_quizzes")
        val LISTENED = stringSetPreferencesKey("listened_locations")
        val MEMORIES = stringSetPreferencesKey("uncovered_memories")
        val WALKING_MINUTES = intPreferencesKey("planned_walking_minutes")

        val ITINERARY = stringPreferencesKey("itinerary_json")
        val TOUR_PREFERENCES = stringPreferencesKey("tour_preferences_json")
    }

    val settings: Flow<UserSettings> = dataStore.data.map { preferences ->
        UserSettings(
            language = AppLanguage.fromTag(preferences[Keys.LANGUAGE]),
            themeMode = runCatching { ThemeMode.valueOf(preferences[Keys.THEME] ?: "SYSTEM") }
                .getOrDefault(ThemeMode.SYSTEM),
            apiBaseUrl = preferences[Keys.API_BASE_URL].orEmpty(),
            allowRemoteImages = preferences[Keys.ALLOW_REMOTE_IMAGES] ?: true,
            narrationRate = preferences[Keys.NARRATION_RATE] ?: 0.92f,
            narrationPitch = preferences[Keys.NARRATION_PITCH] ?: 1.0f,
            narrationVoice = preferences[Keys.NARRATION_VOICE].orEmpty(),
            dailyStoryReminder = preferences[Keys.DAILY_REMINDER] ?: false,
            onboardingComplete = preferences[Keys.ONBOARDING] ?: false,
            lastSyncedAtEpochMillis = preferences[Keys.LAST_SYNCED] ?: 0L,
            seededContentVersion = preferences[Keys.SEEDED_VERSION] ?: 0,
        )
    }

    val progress: Flow<JourneyProgress> = dataStore.data.map { preferences ->
        JourneyProgress(
            visitedLocationIds = preferences[Keys.VISITED].orEmpty(),
            bookmarkedLocationIds = preferences[Keys.BOOKMARKED].orEmpty(),
            completedQuizLocationIds = preferences[Keys.QUIZZES].orEmpty(),
            listenedLocationIds = preferences[Keys.LISTENED].orEmpty(),
            uncoveredMemoryIds = preferences[Keys.MEMORIES].orEmpty(),
            plannedWalkingMinutes = preferences[Keys.WALKING_MINUTES] ?: 0,
            lastVisitedLocationId = preferences[Keys.LAST_VISITED],
        )
    }

    val itineraryJson: Flow<String?> = dataStore.data.map { it[Keys.ITINERARY] }
    val tourPreferencesJson: Flow<String?> = dataStore.data.map { it[Keys.TOUR_PREFERENCES] }

    suspend fun current(): UserSettings = settings.first()

    suspend fun setLanguage(language: AppLanguage) = edit { it[Keys.LANGUAGE] = language.tag }
    suspend fun setThemeMode(mode: ThemeMode) = edit { it[Keys.THEME] = mode.name }
    suspend fun setApiBaseUrl(url: String) = edit { it[Keys.API_BASE_URL] = url.trim() }
    suspend fun setAllowRemoteImages(allow: Boolean) = edit { it[Keys.ALLOW_REMOTE_IMAGES] = allow }
    suspend fun setNarrationRate(rate: Float) = edit { it[Keys.NARRATION_RATE] = rate }
    suspend fun setNarrationPitch(pitch: Float) = edit { it[Keys.NARRATION_PITCH] = pitch }
    suspend fun setNarrationVoice(voice: String) = edit { it[Keys.NARRATION_VOICE] = voice }
    suspend fun setDailyStoryReminder(enabled: Boolean) = edit { it[Keys.DAILY_REMINDER] = enabled }
    suspend fun setOnboardingComplete() = edit { it[Keys.ONBOARDING] = true }
    suspend fun setLastSynced(epochMillis: Long) = edit { it[Keys.LAST_SYNCED] = epochMillis }
    suspend fun setSeededContentVersion(version: Int) = edit { it[Keys.SEEDED_VERSION] = version }

    suspend fun markVisited(locationId: String) = edit { preferences ->
        preferences[Keys.VISITED] = preferences[Keys.VISITED].orEmpty() + locationId
        preferences[Keys.LAST_VISITED] = locationId
    }
    suspend fun markQuizCompleted(locationId: String) = addTo(Keys.QUIZZES, locationId)
    suspend fun markNarrationHeard(locationId: String) = addTo(Keys.LISTENED, locationId)
    suspend fun markMemoryUncovered(memoryId: String) = addTo(Keys.MEMORIES, memoryId)

    suspend fun toggleBookmark(locationId: String) = edit { preferences ->
        val current = preferences[Keys.BOOKMARKED].orEmpty()
        preferences[Keys.BOOKMARKED] =
            if (locationId in current) current - locationId else current + locationId
    }

    suspend fun setPlannedWalkingMinutes(minutes: Int) =
        edit { it[Keys.WALKING_MINUTES] = minutes.coerceAtLeast(0) }

    suspend fun saveItinerary(json: String?) = edit { preferences ->
        if (json == null) preferences.remove(Keys.ITINERARY) else preferences[Keys.ITINERARY] = json
    }

    suspend fun saveTourPreferences(json: String) = edit { it[Keys.TOUR_PREFERENCES] = json }

    /** Forgets the journey but keeps the reader's settings. */
    suspend fun clearJourney() = edit { preferences ->
        listOf(Keys.VISITED, Keys.BOOKMARKED, Keys.QUIZZES, Keys.LISTENED, Keys.MEMORIES)
            .forEach(preferences::remove)
        preferences.remove(Keys.WALKING_MINUTES)
        preferences.remove(Keys.ITINERARY)
        preferences.remove(Keys.LAST_VISITED)
    }

    private suspend fun addTo(key: Preferences.Key<Set<String>>, value: String) = edit { preferences ->
        preferences[key] = preferences[key].orEmpty() + value
    }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        dataStore.edit(block)
    }
}
