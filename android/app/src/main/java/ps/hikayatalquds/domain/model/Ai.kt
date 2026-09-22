package ps.hikayatalquds.domain.model

import androidx.compose.runtime.Immutable

/** Who the story is being retold for. */
enum class AudienceMode(val id: String) {
    STUDENT("student"),
    TOURIST("tourist"),
    CHILD("child"),
    SHORT("short"),
    HISTORIAN("historian"),
    GENERAL("general");

    companion object {
        fun fromId(id: String?): AudienceMode = entries.firstOrNull { it.id == id } ?: GENERAL
    }
}

enum class StoryTone(val id: String) {
    NEUTRAL("neutral"),
    EDUCATIONAL("educational"),
    EMOTIONAL("emotional"),
    STORYTELLING("storytelling");

    companion object {
        fun fromId(id: String?): StoryTone = entries.firstOrNull { it.id == id } ?: STORYTELLING
    }
}

/**
 * Where a piece of generated text actually came from.
 *
 * Shown to the reader verbatim. A story rewritten on the device from the stored
 * record is a different thing from one a model wrote, and the app says which.
 */
enum class GenerationOrigin(val wire: String) {
    BACKEND_AI("anthropic"),
    OFFLINE_EXTRACTIVE("offline-extractive"),
    BACKEND_FALLBACK("backend-fallback"),
    ON_DEVICE("on-device-extractive");

    companion object {
        fun fromWire(value: String?): GenerationOrigin = when (value) {
            null, "" -> ON_DEVICE
            OFFLINE_EXTRACTIVE.wire -> OFFLINE_EXTRACTIVE
            BACKEND_FALLBACK.wire, "frontend-mock" -> BACKEND_FALLBACK
            ON_DEVICE.wire, "arabic-reviewed-fallback" -> ON_DEVICE
            else -> BACKEND_AI
        }
    }
}

@Immutable
data class GeneratedStory(
    val title: String,
    val summary: String,
    val narrative: String,
    val audience: AudienceMode,
    val tone: StoryTone,
    val language: AppLanguage,
    val wordCount: Int,
    /** Gaps the generator declined to fill. Safe, and important, to show. */
    val uncertaintyNotes: List<String>,
    val origin: GenerationOrigin,
    val sourceFile: String?,
    val locationId: String,
)

@Immutable
data class ChatTurn(val question: String, val answer: String)

@Immutable
data class GuideAnswer(
    val answer: String,
    /** False means the records do not cover the question - not an error. */
    val answeredFromSource: Boolean,
    val excerpts: List<String>,
    val uncertaintyNotes: List<String>,
    val origin: GenerationOrigin,
)
