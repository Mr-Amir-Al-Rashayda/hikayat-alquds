package ps.hikayatalquds.data.mapper

import ps.hikayatalquds.data.asset.BundledArtisan
import ps.hikayatalquds.data.asset.BundledLocation
import ps.hikayatalquds.data.asset.BundledMedia
import ps.hikayatalquds.data.asset.BundledMemory
import ps.hikayatalquds.data.asset.BundledQuizQuestion
import ps.hikayatalquds.data.asset.BundledStory
import ps.hikayatalquds.data.asset.BundledTimelineEvent
import ps.hikayatalquds.data.local.ArtisanEntity
import ps.hikayatalquds.data.local.GeneratedStoryEntity
import ps.hikayatalquds.data.local.LocationEntity
import ps.hikayatalquds.data.local.MediaEntity
import ps.hikayatalquds.data.local.MemoryEntity
import ps.hikayatalquds.data.local.QuizQuestionEntity
import ps.hikayatalquds.data.local.StoryEntity
import ps.hikayatalquds.data.local.TimelineEventEntity
import ps.hikayatalquds.domain.model.AppLanguage
import ps.hikayatalquds.domain.model.Artisan
import ps.hikayatalquds.domain.model.ArtisanCategory
import ps.hikayatalquds.domain.model.AudienceMode
import ps.hikayatalquds.domain.model.GeneratedStory
import ps.hikayatalquds.domain.model.GenerationOrigin
import ps.hikayatalquds.domain.model.Location
import ps.hikayatalquds.domain.model.LocalizedList
import ps.hikayatalquds.domain.model.LocalizedText
import ps.hikayatalquds.domain.model.MediaEra
import ps.hikayatalquds.domain.model.MediaItem
import ps.hikayatalquds.domain.model.MediaPairRole
import ps.hikayatalquds.domain.model.Memory
import ps.hikayatalquds.domain.model.MemoryStatus
import ps.hikayatalquds.domain.model.QuizQuestion
import ps.hikayatalquds.domain.model.SourceCitation
import ps.hikayatalquds.domain.model.Story
import ps.hikayatalquds.domain.model.StoryTone
import ps.hikayatalquds.domain.model.TimelineEvent
import ps.hikayatalquds.domain.model.TourInterest

/**
 * Translations between the three shapes the same record takes: the JSON that
 * ships in the APK, the Room row, and the immutable model the UI reads.
 *
 * Kept in one file on purpose - when a field is added to the archive, every
 * place that has to learn about it is on this screen.
 */

/** Citations are one row each, tab-separated. No citation text contains a tab. */
private const val FIELD = "\t"

private fun SourceCitation.encode(): String =
    listOf(title.en, title.ar, publisher.en, publisher.ar, url).joinToString(FIELD)

private fun String.decodeCitation(): SourceCitation? {
    val parts = split(FIELD)
    if (parts.size < 5) return null
    return SourceCitation(
        title = LocalizedText(parts[0], parts[1]),
        publisher = LocalizedText(parts[2], parts[3]),
        url = parts[4],
    )
}

// --- bundled JSON -> Room ----------------------------------------------------

fun BundledLocation.toEntity(sortOrder: Int) = LocationEntity(
    id = id,
    name = name,
    arabicName = arabicName,
    city = city,
    country = country,
    latitude = latitude,
    longitude = longitude,
    description = description,
    arabicDescription = arabicDescription,
    culturalImportance = culturalImportance,
    arabicCulturalImportance = arabicCulturalImportance,
    historicalSummary = historicalSummary,
    arabicHistoricalSummary = arabicHistoricalSummary,
    landmarks = landmarks,
    arabicLandmarks = arabicLandmarks,
    storyTitle = storyTitle,
    arabicStoryTitle = arabicStoryTitle,
    story = story,
    arabicStory = arabicStory,
    walkthrough = walkthrough,
    arabicWalkthrough = arabicWalkthrough,
    coverImage = coverImage,
    coverImageCredit = coverImageCredit,
    coverImageLicense = coverImageLicense,
    coverImageSourceUrl = coverImageSourceUrl,
    contentFile = contentFile,
    categories = categories,
    interests = interests,
    isPublished = isPublished,
    citations = citations.map {
        SourceCitation(
            title = LocalizedText(it.title, it.arabicTitle),
            publisher = LocalizedText(it.publisher, it.arabicPublisher),
            url = it.url,
        ).encode()
    },
    sortOrder = sortOrder,
)

fun BundledStory.toEntity(createdAt: Long) = StoryEntity(
    id = id,
    locationId = locationId,
    title = title,
    arabicTitle = arabicTitle,
    summary = summary,
    arabicSummary = arabicSummary,
    body = simplifiedStory,
    arabicBody = arabicSimplifiedStory,
    audience = audience,
    tone = tone,
    source = source,
    isAiGenerated = isAiGenerated,
    uncertaintyNotes = uncertaintyNotes,
    status = status,
    createdAt = createdAt,
)

fun BundledTimelineEvent.toEntity() = TimelineEventEntity(
    id = id,
    locationId = locationId,
    periodLabel = periodLabel,
    arabicPeriodLabel = arabicPeriodLabel,
    description = description,
    arabicDescription = arabicDescription,
    sortYear = sortYear,
    sortOrder = sortOrder,
    isTradition = isTradition,
    sourceFile = sourceFile,
)

fun BundledMedia.toEntity() = MediaEntity(
    id = id,
    locationId = locationId,
    subject = subject,
    arabicSubject = arabicSubject,
    description = description,
    arabicDescription = arabicDescription,
    era = era,
    pairRole = pairRole,
    assetPath = assetPath,
    remoteUrl = remoteUrl,
    remoteThumbUrl = remoteThumbUrl,
    credit = credit,
    license = license,
    licenseUrl = licenseUrl,
    capturedAt = capturedAt,
    sourceUrl = sourceUrl,
    width = width,
    height = height,
    sortOrder = sortOrder,
)

fun BundledQuizQuestion.toEntity() = QuizQuestionEntity(
    id = id,
    locationId = locationId,
    question = question,
    arabicQuestion = arabicQuestion,
    options = options,
    arabicOptions = arabicOptions,
    answerIndex = answerIndex,
    explanation = explanation,
    arabicExplanation = arabicExplanation,
    sourceNote = sourceNote,
    arabicSourceNote = arabicSourceNote,
    sortOrder = sortOrder,
)

fun BundledMemory.toEntity() = MemoryEntity(
    id = id,
    referenceCode = referenceCode,
    locationId = locationId,
    title = title,
    content = content,
    contributorName = contributorName,
    status = status,
    submittedAt = submittedAt,
    pendingUpload = false,
)

fun BundledArtisan.toEntity() = ArtisanEntity(
    id = id,
    locationId = locationId,
    name = name,
    arabicName = arabicName,
    category = category,
    description = description,
    arabicDescription = arabicDescription,
    supportNote = supportNote,
    arabicSupportNote = arabicSupportNote,
    locationNote = locationNote,
    arabicLocationNote = arabicLocationNote,
    assetPath = fallbackImageUrl ?: assetPath?.takeUnless { it.startsWith("http") },
    remoteImageUrl = remoteImageUrl,
    imageAlt = imageAlt,
    arabicImageAlt = arabicImageAlt,
    imageCredit = imageCredit,
    imageLicense = imageLicense,
    imageSourceUrl = imageSourceUrl,
)

// --- Room -> domain ----------------------------------------------------------

fun LocationEntity.toDomain() = Location(
    id = id,
    name = LocalizedText(name, arabicName),
    city = city,
    country = country,
    latitude = latitude,
    longitude = longitude,
    description = LocalizedText(description, arabicDescription),
    culturalImportance = LocalizedText(culturalImportance, arabicCulturalImportance),
    historicalSummary = LocalizedText(historicalSummary, arabicHistoricalSummary),
    landmarks = LocalizedList(landmarks, arabicLandmarks),
    storyTitle = LocalizedText(storyTitle, arabicStoryTitle),
    story = LocalizedText(story, arabicStory),
    walkthrough = LocalizedText(walkthrough, arabicWalkthrough),
    coverImage = coverImage,
    coverImageCredit = coverImageCredit,
    coverImageLicense = coverImageLicense,
    coverImageSourceUrl = coverImageSourceUrl,
    contentFile = contentFile,
    categoryLabels = categories,
    interests = interests.mapNotNull(TourInterest::fromId),
    citations = citations.mapNotNull { it.decodeCitation() },
)

fun StoryEntity.toDomain() = Story(
    id = id,
    locationId = locationId,
    title = LocalizedText(title, arabicTitle),
    summary = LocalizedText(summary, arabicSummary),
    body = LocalizedText(body, arabicBody),
    audience = audience,
    tone = tone,
    source = source,
    isAiGenerated = isAiGenerated,
    uncertaintyNotes = uncertaintyNotes,
    status = status,
)

fun TimelineEventEntity.toDomain() = TimelineEvent(
    id = id,
    locationId = locationId,
    periodLabel = LocalizedText(periodLabel, arabicPeriodLabel),
    description = LocalizedText(description, arabicDescription),
    sortYear = sortYear,
    sortOrder = sortOrder,
    isTradition = isTradition,
    sourceFile = sourceFile,
)

fun MediaEntity.toDomain() = MediaItem(
    id = id,
    locationId = locationId,
    subject = LocalizedText(subject, arabicSubject),
    description = LocalizedText(description, arabicDescription),
    era = if (era.equals("historical", ignoreCase = true)) MediaEra.HISTORICAL else MediaEra.MODERN,
    pairRole = when (pairRole?.lowercase()) {
        "cover" -> MediaPairRole.COVER
        "before" -> MediaPairRole.BEFORE
        "after" -> MediaPairRole.AFTER
        else -> MediaPairRole.NONE
    },
    assetPath = assetPath,
    remoteUrl = remoteUrl,
    remoteThumbUrl = remoteThumbUrl,
    credit = credit,
    license = license,
    licenseUrl = licenseUrl,
    capturedAt = capturedAt,
    sourceUrl = sourceUrl,
    width = width,
    height = height,
    sortOrder = sortOrder,
)

fun QuizQuestionEntity.toDomain() = QuizQuestion(
    id = id,
    locationId = locationId,
    question = LocalizedText(question, arabicQuestion),
    options = LocalizedList(options, arabicOptions),
    answerIndex = answerIndex,
    explanation = LocalizedText(explanation, arabicExplanation),
    sourceNote = LocalizedText(sourceNote, arabicSourceNote),
    sortOrder = sortOrder,
)

fun MemoryEntity.toDomain() = Memory(
    id = id,
    referenceCode = referenceCode,
    locationId = locationId,
    title = title,
    content = content,
    contributorName = contributorName,
    status = MemoryStatus.fromWire(status),
    submittedAt = submittedAt,
)

fun ArtisanEntity.toDomain() = Artisan(
    id = id,
    locationId = locationId,
    name = LocalizedText(name, arabicName),
    category = ArtisanCategory.fromId(category),
    description = LocalizedText(description, arabicDescription),
    supportNote = LocalizedText(supportNote, arabicSupportNote),
    locationNote = LocalizedText(locationNote, arabicLocationNote),
    imageAlt = LocalizedText(imageAlt, arabicImageAlt),
    assetPath = assetPath,
    remoteImageUrl = remoteImageUrl,
    imageCredit = imageCredit,
    imageLicense = imageLicense,
    imageSourceUrl = imageSourceUrl,
)

fun GeneratedStoryEntity.toDomain() = GeneratedStory(
    title = title,
    summary = summary,
    narrative = narrative,
    audience = AudienceMode.fromId(audience),
    tone = StoryTone.fromId(tone),
    language = AppLanguage.fromTag(language),
    wordCount = wordCount,
    uncertaintyNotes = uncertaintyNotes,
    origin = GenerationOrigin.fromWire(origin),
    sourceFile = sourceFile,
    locationId = locationId,
)

fun GeneratedStory.toEntity(createdAt: Long) = GeneratedStoryEntity(
    id = "$locationId:${audience.id}:${tone.id}:${language.tag}",
    locationId = locationId,
    title = title,
    summary = summary,
    narrative = narrative,
    audience = audience.id,
    tone = tone.id,
    language = language.tag,
    wordCount = wordCount,
    uncertaintyNotes = uncertaintyNotes,
    origin = origin.wire,
    sourceFile = sourceFile,
    createdAt = createdAt,
)
