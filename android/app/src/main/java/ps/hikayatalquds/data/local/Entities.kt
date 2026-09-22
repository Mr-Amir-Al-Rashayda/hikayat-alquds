package ps.hikayatalquds.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room is the app's single source of truth.
 *
 * Both inputs - the JSON bundled in the APK and, when one is configured, the
 * Hikayat AlQuds API - are written here, and every screen reads from here. That
 * is what makes the app work identically on a plane and on a good connection,
 * and it is why a screen never has to ask "am I online?" to know what to draw.
 *
 * Bilingual columns are stored side by side rather than as two rows, because
 * the Arabic and the English are one reviewed record, not two translations.
 */

@Entity(tableName = "locations")
data class LocationEntity(
    @PrimaryKey val id: String,
    val name: String,
    val arabicName: String,
    val city: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val description: String,
    val arabicDescription: String,
    val culturalImportance: String,
    val arabicCulturalImportance: String,
    val historicalSummary: String,
    val arabicHistoricalSummary: String,
    val landmarks: List<String>,
    val arabicLandmarks: List<String>,
    val storyTitle: String,
    val arabicStoryTitle: String,
    val story: String,
    val arabicStory: String,
    val walkthrough: String,
    val arabicWalkthrough: String,
    val coverImage: String?,
    val coverImageCredit: String?,
    val coverImageLicense: String?,
    val coverImageSourceUrl: String?,
    val contentFile: String?,
    val categories: List<String>,
    val interests: List<String>,
    val isPublished: Boolean,
    val citations: List<String>,
    val sortOrder: Int,
)

@Entity(tableName = "stories", indices = [Index("locationId")])
data class StoryEntity(
    @PrimaryKey val id: String,
    val locationId: String,
    val title: String,
    val arabicTitle: String,
    val summary: String,
    val arabicSummary: String,
    val body: String,
    val arabicBody: String,
    val audience: String,
    val tone: String,
    val source: String?,
    val isAiGenerated: Boolean,
    val uncertaintyNotes: List<String>,
    val status: String,
    val createdAt: Long,
)

@Entity(tableName = "timeline_events", indices = [Index("locationId")])
data class TimelineEventEntity(
    @PrimaryKey val id: String,
    val locationId: String,
    val periodLabel: String,
    val arabicPeriodLabel: String,
    val description: String,
    val arabicDescription: String,
    val sortYear: Int,
    val sortOrder: Int,
    val isTradition: Boolean,
    val sourceFile: String?,
)

@Entity(tableName = "media", indices = [Index("locationId")])
data class MediaEntity(
    @PrimaryKey val id: String,
    val locationId: String,
    val subject: String,
    val arabicSubject: String,
    val description: String,
    val arabicDescription: String,
    val era: String,
    val pairRole: String?,
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
)

@Entity(tableName = "quiz_questions", indices = [Index("locationId")])
data class QuizQuestionEntity(
    @PrimaryKey val id: String,
    val locationId: String,
    val question: String,
    val arabicQuestion: String,
    val options: List<String>,
    val arabicOptions: List<String>,
    val answerIndex: Int,
    val explanation: String,
    val arabicExplanation: String,
    val sourceNote: String,
    val arabicSourceNote: String,
    val sortOrder: Int,
)

@Entity(tableName = "memories", indices = [Index("locationId")])
data class MemoryEntity(
    @PrimaryKey val id: String,
    val referenceCode: String?,
    val locationId: String,
    val title: String,
    val content: String,
    val contributorName: String?,
    val status: String,
    val submittedAt: String,
    /** True while a submission made offline is still waiting to be sent. */
    val pendingUpload: Boolean = false,
    val contributorEmail: String? = null,
)

@Entity(tableName = "artisans", indices = [Index("locationId")])
data class ArtisanEntity(
    @PrimaryKey val id: String,
    val locationId: String,
    val name: String,
    val arabicName: String,
    val category: String,
    val description: String,
    val arabicDescription: String,
    val supportNote: String,
    val arabicSupportNote: String,
    val locationNote: String,
    val arabicLocationNote: String,
    val assetPath: String?,
    val remoteImageUrl: String?,
    val imageAlt: String,
    val arabicImageAlt: String,
    val imageCredit: String?,
    val imageLicense: String?,
    val imageSourceUrl: String?,
)

/** A narrative generated for a location, kept so it can be reread offline. */
@Entity(tableName = "generated_stories", indices = [Index("locationId")])
data class GeneratedStoryEntity(
    @PrimaryKey val id: String,
    val locationId: String,
    val title: String,
    val summary: String,
    val narrative: String,
    val audience: String,
    val tone: String,
    val language: String,
    val wordCount: Int,
    val uncertaintyNotes: List<String>,
    val origin: String,
    val sourceFile: String?,
    val createdAt: Long,
)
