package ps.hikayatalquds.domain.model

import androidx.compose.runtime.Immutable

/**
 * The reviewed Jerusalem records the app is built from.
 *
 * Every type here carries where its content came from - a source file, a
 * photographer and licence, the section of the reviewed summary a quiz answer
 * rests on. That is the project's rule expressed in the type system: nothing
 * reaches the screen that cannot say where it came from.
 */

@Immutable
data class SourceCitation(
    val title: LocalizedText,
    val publisher: LocalizedText,
    val url: String,
)

/** The four categories used by the map legend and the constellation. */
enum class LocationCategory(val arabicLabel: String, val englishLabel: String) {
    SACRED("معلم ديني", "Sacred site"),
    SOUQ("سوق تراثي", "Historic souq"),
    QUARTER("حارة تاريخية", "Historic quarter"),
    NEIGHBOURHOOD("حي مقدسي", "Jerusalem neighbourhood");

    fun label(language: AppLanguage) = if (language.isArabic) arabicLabel else englishLabel

    companion object {
        fun fromArabic(value: String): LocationCategory? = entries.firstOrNull { it.arabicLabel == value }
    }
}

enum class TourInterest(val id: String) {
    HISTORY("history"),
    ARCHITECTURE("architecture"),
    RELIGIOUS("religious"),
    FOOD_MARKETS("food_markets"),
    ORAL_HERITAGE("oral_heritage");

    companion object {
        fun fromId(id: String): TourInterest? = entries.firstOrNull { it.id == id }
    }
}

@Immutable
data class Location(
    val id: String,
    val name: LocalizedText,
    val city: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val description: LocalizedText,
    val culturalImportance: LocalizedText,
    val historicalSummary: LocalizedText,
    val landmarks: LocalizedList,
    val storyTitle: LocalizedText,
    val story: LocalizedText,
    /** A step-by-step reading of the place, used by the narration and the tour. */
    val walkthrough: LocalizedText,
    val coverImage: String?,
    val coverImageCredit: String?,
    val coverImageLicense: String?,
    val coverImageSourceUrl: String?,
    val contentFile: String?,
    val categoryLabels: List<String>,
    val interests: List<TourInterest>,
    val citations: List<SourceCitation>,
) {
    val categories: List<LocationCategory>
        get() = categoryLabels.mapNotNull(LocationCategory::fromArabic)

    /** The accent a pin, chip or constellation node takes. */
    val primaryCategory: LocationCategory
        get() = categories.firstOrNull() ?: LocationCategory.QUARTER
}

/** Counts shown on a card so a reader knows what is waiting inside. */
@Immutable
data class LocationStats(
    val storyCount: Int = 0,
    val imageCount: Int = 0,
    val timelineEventCount: Int = 0,
    val memoryCount: Int = 0,
    val readingTimeMinutes: Int = 0,
)

@Immutable
data class LocationSummary(
    val location: Location,
    val stats: LocationStats,
    val visited: Boolean = false,
    val bookmarked: Boolean = false,
)

@Immutable
data class Story(
    val id: String,
    val locationId: String,
    val title: LocalizedText,
    val summary: LocalizedText,
    val body: LocalizedText,
    val audience: String,
    val tone: String,
    val source: String?,
    val isAiGenerated: Boolean,
    val uncertaintyNotes: List<String>,
    val status: String,
)

@Immutable
data class TimelineEvent(
    val id: String,
    val locationId: String,
    val periodLabel: LocalizedText,
    val description: LocalizedText,
    val sortYear: Int,
    val sortOrder: Int,
    /** Rests on religious or oral tradition rather than documented history. */
    val isTradition: Boolean,
    val sourceFile: String?,
)

enum class MediaEra { MODERN, HISTORICAL }

enum class MediaPairRole { COVER, BEFORE, AFTER, NONE }

@Immutable
data class MediaItem(
    val id: String,
    val locationId: String,
    val subject: LocalizedText,
    val description: LocalizedText,
    val era: MediaEra,
    val pairRole: MediaPairRole,
    /** Relative path inside the APK's assets, when this photograph ships offline. */
    val assetPath: String?,
    val remoteUrl: String?,
    val remoteThumbUrl: String?,
    val credit: String?,
    val license: String?,
    val licenseUrl: String?,
    val capturedAt: String?,
    val sourceUrl: String?,
    val width: Int?,
    val height: Int?,
    val sortOrder: Int,
) {
    val isBundled: Boolean get() = assetPath != null

    /** Prefers the bundled copy; a photograph must not need a network to appear. */
    fun displayModel(allowRemote: Boolean): String? = when {
        assetPath != null -> "file:///android_asset/$assetPath"
        allowRemote -> remoteUrl
        else -> null
    }

    fun thumbnailModel(allowRemote: Boolean): String? = when {
        assetPath != null -> "file:///android_asset/$assetPath"
        allowRemote -> remoteThumbUrl ?: remoteUrl
        else -> null
    }
}

/** A historical and a present photograph of the *same* subject, or nothing. */
@Immutable
data class BeforeAfterPair(val before: MediaItem, val after: MediaItem)

@Immutable
data class QuizQuestion(
    val id: String,
    val locationId: String,
    val question: LocalizedText,
    val options: LocalizedList,
    val answerIndex: Int,
    val explanation: LocalizedText,
    /** Where in the reviewed content the answer comes from. */
    val sourceNote: LocalizedText,
    val sortOrder: Int,
)

enum class MemoryStatus(val wire: String) {
    PENDING_REVIEW("pending_review"),
    APPROVED("approved"),
    REJECTED("rejected");

    companion object {
        fun fromWire(value: String?): MemoryStatus =
            entries.firstOrNull { it.wire == value } ?: PENDING_REVIEW
    }
}

/** A memory somebody contributed. Never shown before an editor approves it. */
@Immutable
data class Memory(
    val id: String,
    val referenceCode: String?,
    val locationId: String,
    val title: String,
    val content: String,
    val contributorName: String?,
    val status: MemoryStatus,
    val submittedAt: String,
)

@Immutable
data class NewMemory(
    val locationId: String,
    val title: String,
    val content: String,
    val contributorName: String?,
    val contributorEmail: String?,
)

@Immutable
data class MemoryReceipt(
    val status: MemoryStatus,
    val message: String,
    val referenceCode: String?,
)

@Immutable
data class MemoryStatusReport(
    val referenceCode: String,
    val locationId: String,
    val title: String,
    val status: MemoryStatus,
    val submittedAt: String,
    val reviewedAt: String?,
    val message: String,
    val reviewNotes: String?,
)

enum class ArtisanCategory(val id: String) {
    CRAFT("craft"),
    FOOD("food"),
    MARKET("market");

    companion object {
        fun fromId(id: String?): ArtisanCategory = entries.firstOrNull { it.id == id } ?: CRAFT
    }
}

@Immutable
data class Artisan(
    val id: String,
    val locationId: String,
    val name: LocalizedText,
    val category: ArtisanCategory,
    val description: LocalizedText,
    val supportNote: LocalizedText,
    val locationNote: LocalizedText,
    val imageAlt: LocalizedText,
    val assetPath: String?,
    val remoteImageUrl: String?,
    val imageCredit: String?,
    val imageLicense: String?,
    val imageSourceUrl: String?,
) {
    fun displayModel(allowRemote: Boolean): String? = when {
        assetPath != null -> "file:///android_asset/$assetPath"
        allowRemote -> remoteImageUrl
        else -> null
    }
}

/** Everything one location screen needs, assembled once. */
@Immutable
data class LocationDetail(
    val location: Location,
    val stats: LocationStats,
    val stories: List<Story>,
    val timeline: List<TimelineEvent>,
    val media: List<MediaItem>,
    val beforeAfter: BeforeAfterPair?,
    val quiz: List<QuizQuestion>,
    val memories: List<Memory>,
    val artisans: List<Artisan>,
    val related: List<Location>,
)
