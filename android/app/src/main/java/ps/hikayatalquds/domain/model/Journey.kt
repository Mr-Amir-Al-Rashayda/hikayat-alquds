package ps.hikayatalquds.domain.model

import androidx.compose.runtime.Immutable

/**
 * What this phone has done in the archive.
 *
 * There are no accounts. This is a private record kept on the device: which
 * quarters were opened, which quizzes finished, which narrations were heard.
 * None of it is sent anywhere, because none of it is Hikayat AlQuds's business.
 */
@Immutable
data class JourneyProgress(
    val visitedLocationIds: Set<String> = emptySet(),
    val bookmarkedLocationIds: Set<String> = emptySet(),
    val completedQuizLocationIds: Set<String> = emptySet(),
    val listenedLocationIds: Set<String> = emptySet(),
    val uncoveredMemoryIds: Set<String> = emptySet(),
    val plannedWalkingMinutes: Int = 0,
    /** Ordered separately because a preference set has no order. */
    val lastVisitedLocationId: String? = null,
) {
    fun percentComplete(totalLocations: Int): Int =
        if (totalLocations <= 0) 0 else (visitedLocationIds.size * 100) / totalLocations
}

@Immutable
data class Badge(
    val id: String,
    val label: LocalizedText,
    val description: LocalizedText,
    /** How many locations must be opened to earn it. */
    val threshold: Int,
    val earned: Boolean,
)

/**
 * The keepsake: a short reflection assembled only from the counters above.
 * It deliberately makes no historical claim - it is about the visit, not the city.
 */
@Immutable
data class JerusalemKeepsake(
    val generatedAtEpochMillis: Long,
    val exploredLocations: List<Location>,
    val totalLocations: Int,
    val quizzesCompleted: Int,
    val narrationsHeard: Int,
    val memoriesUncovered: Int,
    val plannedWalkingMinutes: Int,
    val heritageSitesDiscovered: Int,
    val badge: LocalizedText,
    val reflection: LocalizedText,
)
