package ps.hikayatalquds.data.remote

import kotlinx.serialization.Serializable

/**
 * The JSON the Hikayat AlQuds API speaks, as documented in `backend/README.md`.
 *
 * The API is optional: the app ships the whole reviewed archive and works
 * without it. What a reachable API adds is the parts that cannot be bundled -
 * memories other people have contributed since the build, the review status of
 * a submission, and narratives generated against the reviewed sources.
 *
 * Nothing here is required to be present. A field the deployment does not send
 * falls back to what the bundled record already holds rather than blanking it.
 */

@Serializable
data class HealthDto(
    val status: String = "unknown",
    val dataSource: String? = null,
    val aiServiceUrl: String? = null,
    val timestamp: String? = null,
)

@Serializable
data class LocationStatsDto(
    val storyCount: Int = 0,
    val imageCount: Int = 0,
    val timelineEventCount: Int = 0,
    val memoryCount: Int = 0,
    val readingTimeMinutes: Int = 0,
)

@Serializable
data class LocationDto(
    val id: String,
    val name: String,
    val arabicName: String? = null,
    val city: String? = null,
    val country: String = "Palestine",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val description: String? = null,
    val coverImageUrl: String? = null,
    val contentFile: String? = null,
    val aiSummaryFile: String? = null,
    val categories: List<String> = emptyList(),
    val isPublished: Boolean = true,
    val stats: LocationStatsDto? = null,
)

@Serializable
data class StoryDto(
    val id: String,
    val locationId: String,
    val title: String,
    val summary: String? = null,
    val simplifiedStory: String = "",
    val audience: String = "general",
    val language: String = "en",
    val tone: String = "storytelling",
    val source: String? = null,
    val isAiGenerated: Boolean = false,
    val uncertaintyNotes: List<String> = emptyList(),
    val status: String = "published",
)

@Serializable
data class FeaturedStoryDto(
    val story: StoryDto,
    val location: LocationDto? = null,
    val reason: String? = null,
    val forDate: String? = null,
)

@Serializable
data class MediaDto(
    val id: String,
    val locationId: String,
    val type: String = "image",
    val url: String,
    val thumbUrl: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val subject: String? = null,
    val arabicSubject: String? = null,
    val description: String? = null,
    val arabicDescription: String? = null,
    val era: String = "modern",
    val pairRole: String? = null,
    val credit: String? = null,
    val license: String? = null,
    val licenseUrl: String? = null,
    val capturedAt: String? = null,
    val sourceUrl: String? = null,
    val sortOrder: Int = 0,
)

@Serializable
data class BeforeAfterDto(
    val pair: PairDto? = null,
    /** Why there is no pair, when there is not one. */
    val reason: String? = null,
) {
    @Serializable
    data class PairDto(val before: MediaDto, val after: MediaDto)
}

@Serializable
data class TimelineEventDto(
    val id: String,
    val locationId: String,
    val periodLabel: String,
    val description: String,
    val sortYear: Int = 0,
    val sortOrder: Int = 0,
    val isTradition: Boolean = false,
    val sourceFile: String? = null,
)

@Serializable
data class QuizQuestionDto(
    val id: String,
    val locationId: String,
    val question: String,
    val options: List<String> = emptyList(),
    val answerIndex: Int = 0,
    val explanation: String = "",
    val sourceNote: String = "",
    val sortOrder: Int = 0,
)

@Serializable
data class ContributionDto(
    val id: String,
    val referenceCode: String? = null,
    val locationId: String,
    val title: String,
    val content: String = "",
    val contributorName: String? = null,
    val mediaUrl: String? = null,
    val mediaType: String? = null,
    val status: String = "pending_review",
    val submittedAt: String = "",
)

@Serializable
data class NewContributionDto(
    val locationId: String,
    val title: String,
    val content: String,
    val contributorName: String? = null,
    val contributorEmail: String? = null,
)

@Serializable
data class ContributionReceiptDto(
    val status: String = "pending_review",
    val message: String = "",
    val referenceCode: String? = null,
    val contribution: ContributionDto? = null,
)

@Serializable
data class ContributionStatusDto(
    val referenceCode: String,
    val locationId: String = "",
    val title: String = "",
    val status: String = "pending_review",
    val submittedAt: String = "",
    val reviewedAt: String? = null,
    val message: String = "",
    val reviewNotes: String? = null,
)

@Serializable
data class GenerateStoryRequest(
    val locationId: String,
    val targetAudience: String,
    val tone: String,
    val language: String,
    val persist: Boolean = false,
)

@Serializable
data class GeneratedStorySourceDto(
    val locationId: String? = null,
    val locationName: String? = null,
    val contentFile: String? = null,
    val summaryFile: String? = null,
)

@Serializable
data class GeneratedStoryDto(
    val title: String = "",
    val summary: String = "",
    val narrative: String = "",
    val targetAudience: String = "general",
    val language: String = "ar",
    val tone: String = "storytelling",
    val wordCount: Int = 0,
    /** Gaps the generator declined to fill. Shown to the reader. */
    val uncertaintyNotes: List<String> = emptyList(),
    /** Fact-guard findings. For reviewers, never rendered as content. */
    val warnings: List<String> = emptyList(),
    val source: GeneratedStorySourceDto? = null,
    val generatedBy: String? = null,
    val storyId: String? = null,
)

@Serializable
data class ChatTurnDto(val question: String, val answer: String)

@Serializable
data class AskGuideRequest(
    val locationId: String,
    val question: String,
    val history: List<ChatTurnDto> = emptyList(),
    val language: String = "ar",
)

@Serializable
data class GuideAnswerDto(
    val answer: String = "",
    val answeredFromSource: Boolean = false,
    val excerpts: List<String> = emptyList(),
    val uncertaintyNotes: List<String> = emptyList(),
    val generatedBy: String? = null,
)
