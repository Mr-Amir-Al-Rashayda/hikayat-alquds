package ps.hikayatalquds.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import ps.hikayatalquds.data.preferences.UserPreferencesRepository
import ps.hikayatalquds.domain.model.AppLanguage
import ps.hikayatalquds.domain.model.Badge
import ps.hikayatalquds.domain.model.JerusalemKeepsake
import ps.hikayatalquds.domain.model.JourneyProgress
import ps.hikayatalquds.domain.model.LocalizedText
import ps.hikayatalquds.domain.model.Location
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The reader's own record: where they have been, what they finished, and the
 * keepsake assembled from it.
 *
 * "Progress" here means progress through the places in this guide, never a
 * claim about how much of Jerusalem someone has seen. The reflection in the
 * keepsake is written from these counters alone and makes no historical
 * statement - it is about the visit, not about the city.
 */
@Singleton
class JourneyRepository @Inject constructor(
    private val preferences: UserPreferencesRepository,
    private val content: ContentRepository,
) {
    val progress: Flow<JourneyProgress> = preferences.progress

    val badges: Flow<List<Badge>> = combine(
        preferences.progress,
        content.locations,
    ) { journey, locations -> buildBadges(journey.visitedLocationIds.size, locations.size) }

    suspend fun markVisited(locationId: String) = preferences.markVisited(locationId)
    suspend fun markQuizCompleted(locationId: String) = preferences.markQuizCompleted(locationId)
    suspend fun markNarrationHeard(locationId: String) = preferences.markNarrationHeard(locationId)
    suspend fun markMemoryUncovered(memoryId: String) = preferences.markMemoryUncovered(memoryId)
    suspend fun toggleBookmark(locationId: String) = preferences.toggleBookmark(locationId)
    suspend fun reset() = preferences.clearJourney()

    val keepsake: Flow<JerusalemKeepsake> = combine(
        preferences.progress,
        content.locations,
    ) { journey, locations -> keepsakeOf(journey, locations) }

    suspend fun keepsakeNow(): JerusalemKeepsake =
        keepsakeOf(preferences.progress.first(), content.locations.first())

    private fun keepsakeOf(journey: JourneyProgress, locations: List<Location>): JerusalemKeepsake {
        val explored = locations.filter { it.id in journey.visitedLocationIds }
        val discovered = (
            journey.visitedLocationIds +
                journey.completedQuizLocationIds +
                journey.listenedLocationIds
            ).size
        val count = explored.size
        val total = locations.size

        return JerusalemKeepsake(
            generatedAtEpochMillis = System.currentTimeMillis(),
            exploredLocations = explored,
            totalLocations = total,
            quizzesCompleted = journey.completedQuizLocationIds.size,
            narrationsHeard = journey.listenedLocationIds.size,
            memoriesUncovered = journey.uncoveredMemoryIds.size,
            plannedWalkingMinutes = journey.plannedWalkingMinutes,
            heritageSitesDiscovered = discovered,
            badge = when {
                total > 0 && count >= total && journey.completedQuizLocationIds.size >= 4 ->
                    LocalizedText("Guardian of Jerusalem's Story", "حارس حكاية القدس")

                count >= 3 -> LocalizedText("Jerusalem Storyteller", "راوي القدس")
                else -> LocalizedText("A Step into Jerusalem", "خطوة في القدس")
            },
            reflection = when {
                count == 0 -> LocalizedText(
                    "Every story begins at a gate; choose your first neighbourhood to begin.",
                    "كل حكاية تبدأ من باب؛ اختر أول حي لتبدأ حكايتك.",
                )

                total > 0 && count >= total -> LocalizedText(
                    "You crossed Jerusalem's gates and quarters; the map has become a memory you carry.",
                    "عبرت أبواب القدس وحاراتها؛ صارت الخريطة ذاكرةً تمشي معك.",
                )

                else -> LocalizedText(
                    "You explored $count Jerusalem places; every site remembered opens another path into the story.",
                    "استكشفت $count من حارات القدس؛ وكل موقع حفظته يفتح طريقاً جديداً للحكاية.",
                )
            },
        )
    }

    /** Plain text for the share sheet and the saved keepsake file. */
    fun shareText(keepsake: JerusalemKeepsake, language: AppLanguage): String {
        val title = if (language.isArabic) "حكايتي في القدس" else "My Jerusalem Story"
        val places = keepsake.exploredLocations.joinToString(if (language.isArabic) "، " else ", ") {
            it.name[language]
        }
        val stats = if (language.isArabic) {
            "مواقع مكتشفة: ${keepsake.heritageSitesDiscovered} · مشي مخطط: ${keepsake.plannedWalkingMinutes} دقيقة · " +
                "اختبارات: ${keepsake.quizzesCompleted} · روايات مسموعة: ${keepsake.narrationsHeard} · " +
                "ذكريات: ${keepsake.memoriesUncovered}"
        } else {
            "Heritage sites discovered: ${keepsake.heritageSitesDiscovered} · Planned walking: " +
                "${keepsake.plannedWalkingMinutes} min · Quizzes: ${keepsake.quizzesCompleted} · " +
                "Narrations: ${keepsake.narrationsHeard} · Memories: ${keepsake.memoriesUncovered}"
        }
        return buildString {
            appendLine("$title — ${keepsake.badge[language]}")
            appendLine(keepsake.reflection[language])
            if (places.isNotBlank()) appendLine(places)
            appendLine(stats)
            append("HIKAYAT ALQUDS · حكاية القدس")
        }
    }

    companion object {
        /**
         * Thresholds are deduplicated so a small archive never awards two
         * differently named badges for the same single action.
         */
        internal fun buildBadges(visitedCount: Int, totalLocations: Int): List<Badge> {
            val definitions = listOf(
                Triple(
                    "first-step",
                    LocalizedText("First step", "خطوة أولى") to
                        LocalizedText(
                            "Opened your first Jerusalem quarter.",
                            "فتحت أول حارة مقدسية.",
                        ),
                    1,
                ),
                Triple(
                    "storyteller",
                    LocalizedText("Jerusalem storyteller", "راوي القدس") to
                        LocalizedText("Opened three locations.", "فتحت ثلاثة أماكن."),
                    3,
                ),
                Triple(
                    "archivist",
                    LocalizedText("Guardian of Jerusalem's Story", "حارس حكاية القدس") to
                        LocalizedText(
                            "Opened every Jerusalem location in the guide.",
                            "فتحت كل أماكن القدس في الدليل.",
                        ),
                    maxOf(totalLocations, 1),
                ),
            )

            val seen = mutableSetOf<Int>()
            return definitions.mapNotNull { (id, text, threshold) ->
                if (!seen.add(threshold)) return@mapNotNull null
                Badge(
                    id = id,
                    label = text.first,
                    description = text.second,
                    threshold = threshold,
                    earned = visitedCount >= threshold,
                )
            }
        }
    }
}
