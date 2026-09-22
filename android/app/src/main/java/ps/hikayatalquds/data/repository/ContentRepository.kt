package ps.hikayatalquds.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import ps.hikayatalquds.data.asset.BundledContentSource
import ps.hikayatalquds.data.local.ContentDao
import ps.hikayatalquds.data.local.CountRow
import ps.hikayatalquds.data.local.MemoryDao
import ps.hikayatalquds.data.mapper.toDomain
import ps.hikayatalquds.data.mapper.toEntity
import ps.hikayatalquds.data.preferences.UserPreferencesRepository
import ps.hikayatalquds.data.remote.RemoteContentSource
import ps.hikayatalquds.di.IoDispatcher
import ps.hikayatalquds.domain.model.AppLanguage
import ps.hikayatalquds.domain.model.Artisan
import ps.hikayatalquds.domain.model.BeforeAfterPair
import ps.hikayatalquds.domain.model.Location
import ps.hikayatalquds.domain.model.LocationDetail
import ps.hikayatalquds.domain.model.LocationStats
import ps.hikayatalquds.domain.model.LocationSummary
import ps.hikayatalquds.domain.model.MediaItem
import ps.hikayatalquds.domain.model.MediaPairRole
import ps.hikayatalquds.domain.model.Memory
import ps.hikayatalquds.domain.model.QuizQuestion
import ps.hikayatalquds.domain.model.Story
import ps.hikayatalquds.domain.model.TimelineEvent
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Where the archive on screen came from.
 *
 * The web app shows a banner whenever it is using sample data; this is the same
 * promise on a phone. `Bundled` is not a degraded mode - it is the full reviewed
 * archive shipped in the APK - but the reader is still told, because "live" and
 * "as built" are different claims and only one of them is true at a time.
 */
enum class ArchiveOrigin { BUNDLED, LIVE }

data class SyncOutcome(
    val origin: ArchiveOrigin,
    val syncedAtEpochMillis: Long?,
    val failureReason: String? = null,
)

/** Bumped when the bundled JSON changes so an update reseeds the database. */
const val BUNDLED_CONTENT_VERSION = 1

@Singleton
class ContentRepository @Inject constructor(
    private val contentDao: ContentDao,
    private val memoryDao: MemoryDao,
    private val bundled: BundledContentSource,
    private val remote: RemoteContentSource,
    private val preferences: UserPreferencesRepository,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    private val _origin = MutableStateFlow(ArchiveOrigin.BUNDLED)
    val origin: StateFlow<ArchiveOrigin> = _origin.asStateFlow()

    private val _lastSyncFailure = MutableStateFlow<String?>(null)
    val lastSyncFailure: StateFlow<String?> = _lastSyncFailure.asStateFlow()

    // --- seeding ------------------------------------------------------------

    /**
     * Puts the bundled archive into the database the first time the app runs,
     * and again after an update that ships new content. Everything the app
     * shows is read from the database, so this is what makes the first launch
     * work with the radio switched off.
     */
    suspend fun seedIfNeeded(force: Boolean = false): Boolean = withContext(io) {
        val settings = preferences.current()
        val alreadySeeded = settings.seededContentVersion >= BUNDLED_CONTENT_VERSION &&
            contentDao.countLocations() > 0
        if (alreadySeeded && !force) return@withContext false

        val content = bundled.load()
        val now = System.currentTimeMillis()
        contentDao.replaceArchive(
            locations = content.locations.mapIndexed { index, item -> item.toEntity(index) },
            stories = content.stories.map { it.toEntity(now) },
            timeline = content.timeline.map { it.toEntity() },
            media = content.media.map { it.toEntity() },
            quiz = content.quiz.map { it.toEntity() },
            artisans = content.artisans.map { it.toEntity() },
        )
        memoryDao.upsert(content.memories.map { it.toEntity() })
        preferences.setSeededContentVersion(BUNDLED_CONTENT_VERSION)
        true
    }

    // --- reads --------------------------------------------------------------

    val locations: Flow<List<Location>> =
        contentDao.observeLocations().map { rows -> rows.map { it.toDomain() } }

    /**
     * Cards carry counts so a reader knows what is waiting inside. They come
     * from four grouped queries rather than one per location, so the cost does
     * not grow with the archive.
     */
    val locationSummaries: Flow<List<LocationSummary>> = combine(
        contentDao.observeLocations(),
        contentDao.observeStoryCounts(),
        contentDao.observeMediaCounts(),
        contentDao.observeTimelineCounts(),
        combine(memoryDao.observeCounts(), contentDao.observeStoryWordCounts(), ::Pair),
    ) { locations, stories, media, timeline, (memories, words) ->
        val storyCounts = stories.toMap()
        val mediaCounts = media.toMap()
        val timelineCounts = timeline.toMap()
        val memoryCounts = memories.toMap()
        val wordCounts = words.toMap()
        locations.map { row ->
            LocationSummary(
                location = row.toDomain(),
                stats = LocationStats(
                    storyCount = storyCounts[row.id] ?: 0,
                    imageCount = mediaCounts[row.id] ?: 0,
                    timelineEventCount = timelineCounts[row.id] ?: 0,
                    memoryCount = memoryCounts[row.id] ?: 0,
                    // 200 words a minute, and never rounded down to zero: a
                    // "0 min read" reads as "there is nothing here".
                    readingTimeMinutes = ((wordCounts[row.id] ?: 0) / 200).coerceAtLeast(1),
                ),
            )
        }
    }

    fun location(id: String): Flow<Location?> =
        contentDao.observeLocation(id).map { it?.toDomain() }

    fun stories(locationId: String): Flow<List<Story>> =
        contentDao.observeStories(locationId).map { rows -> rows.map { it.toDomain() } }

    fun timeline(locationId: String): Flow<List<TimelineEvent>> =
        contentDao.observeTimeline(locationId).map { rows -> rows.map { it.toDomain() } }

    fun media(locationId: String): Flow<List<MediaItem>> =
        contentDao.observeMedia(locationId).map { rows -> rows.map { it.toDomain() } }

    fun quiz(locationId: String): Flow<List<QuizQuestion>> =
        contentDao.observeQuiz(locationId).map { rows -> rows.map { it.toDomain() } }

    fun artisans(locationId: String): Flow<List<Artisan>> =
        contentDao.observeArtisans(locationId).map { rows -> rows.map { it.toDomain() } }

    fun memories(locationId: String): Flow<List<Memory>> =
        memoryDao.observeApprovedFor(locationId).map { rows -> rows.map { it.toDomain() } }

    val allMemories: Flow<List<Memory>> =
        memoryDao.observeApproved().map { rows -> rows.map { it.toDomain() } }

    /**
     * Everything one location screen needs, assembled in a single flow so the
     * page renders once rather than in nine separate flickers.
     */
    fun detail(locationId: String): Flow<LocationDetail?> {
        val core = combine(
            contentDao.observeLocation(locationId),
            stories(locationId),
            timeline(locationId),
            media(locationId),
        ) { location, stories, timeline, media -> Quad(location, stories, timeline, media) }

        val extras = combine(
            quiz(locationId),
            memories(locationId),
            artisans(locationId),
            contentDao.observeLocations(),
        ) { quiz, memories, artisans, all -> Quad(quiz, memories, artisans, all) }

        return combine(core, extras) { (locationRow, stories, timelineEvents, mediaItems), (quiz, memories, artisans, all) ->
            val row = locationRow ?: return@combine null
            val location = row.toDomain()
            val categories = location.categoryLabels.toSet()
            LocationDetail(
                location = location,
                stats = LocationStats(
                    storyCount = stories.size,
                    imageCount = mediaItems.size,
                    timelineEventCount = timelineEvents.size,
                    memoryCount = memories.size,
                    readingTimeMinutes = (row.story.split(' ').size / 200).coerceAtLeast(1),
                ),
                stories = stories,
                timeline = timelineEvents,
                media = mediaItems,
                beforeAfter = pairOf(mediaItems),
                quiz = quiz,
                memories = memories,
                artisans = artisans,
                related = all.asSequence()
                    .filter { it.id != locationId }
                    .map { it.toDomain() }
                    .filter { candidate -> candidate.categoryLabels.any(categories::contains) }
                    .toList(),
            )
        }
    }

    /**
     * A then-and-now comparison is only offered where the archive holds two
     * photographs of the *same* subject. Two pictures of the same quarter are
     * not the same thing, and pairing them would make a claim the record does
     * not support.
     */
    private fun pairOf(media: List<MediaItem>): BeforeAfterPair? {
        val before = media.firstOrNull { it.pairRole == MediaPairRole.BEFORE } ?: return null
        val after = media.firstOrNull { it.pairRole == MediaPairRole.AFTER } ?: return null
        return BeforeAfterPair(before, after)
    }

    /** The story of the day: the same one for everybody, changing daily. */
    fun featuredStory(dayIndex: Long): Flow<Pair<Story, Location>?> = combine(
        contentDao.observePublishedStories(),
        contentDao.observeLocations(),
    ) { stories, locations ->
        if (stories.isEmpty()) return@combine null
        val story = stories[(dayIndex.mod(stories.size.toLong())).toInt()]
        val location = locations.firstOrNull { it.id == story.locationId } ?: return@combine null
        story.toDomain() to location.toDomain()
    }

    /** Free-text search across names, descriptions, landmarks and timelines. */
    fun search(query: String, language: AppLanguage): Flow<List<SearchHit>> {
        val trimmed = query.trim()
        if (trimmed.length < 2) return flowOf(emptyList())
        return combine(
            contentDao.observeLocations(),
            contentDao.observePublishedStories(),
        ) { locations, stories ->
            val needle = trimmed.normaliseForSearch()
            val locationHits = locations.mapNotNull { row ->
                val location = row.toDomain()
                val haystack = listOf(
                    location.name[language],
                    location.description[language],
                    location.historicalSummary[language],
                    location.culturalImportance[language],
                ).plus(location.landmarks[language]).joinToString(" ").normaliseForSearch()
                if (haystack.contains(needle)) {
                    SearchHit.LocationHit(location, location.description[language])
                } else {
                    null
                }
            }
            val storyHits = stories.mapNotNull { row ->
                val story = row.toDomain()
                val haystack = (story.title[language] + " " + story.body[language]).normaliseForSearch()
                if (!haystack.contains(needle)) return@mapNotNull null
                val location = locations.firstOrNull { it.id == story.locationId } ?: return@mapNotNull null
                SearchHit.StoryHit(story, location.toDomain(), story.summary[language])
            }
            locationHits + storyHits
        }
    }

    // --- sync ---------------------------------------------------------------

    /**
     * Refreshes from the API when one is configured and reachable.
     *
     * A failure is not an error state for the reader: the bundled archive is
     * still complete and correct. It is recorded so Settings can show why the
     * app is not showing live data instead of leaving them guessing.
     */
    suspend fun sync(): SyncOutcome = withContext(io) {
        val result = remote.fetchArchive()
        result.fold(
            onSuccess = { snapshot ->
                if (snapshot.locations.isNotEmpty()) {
                    contentDao.upsertLocations(
                        snapshot.locations.mapIndexed { index, dto ->
                            merge(dto, contentDao.findLocation(dto.id), index)
                        },
                    )
                }
                if (snapshot.memories.isNotEmpty()) {
                    memoryDao.upsert(snapshot.memories)
                }
                val now = System.currentTimeMillis()
                preferences.setLastSynced(now)
                _origin.value = ArchiveOrigin.LIVE
                _lastSyncFailure.value = null
                SyncOutcome(ArchiveOrigin.LIVE, now)
            },
            onFailure = { error ->
                _origin.value = ArchiveOrigin.BUNDLED
                _lastSyncFailure.value = error.message ?: error::class.simpleName
                SyncOutcome(ArchiveOrigin.BUNDLED, null, _lastSyncFailure.value)
            },
        )
    }

    /**
     * The API carries the English record plus an Arabic name; the bundle also
     * carries the reviewed Arabic prose. Merging keeps the richer text rather
     * than letting a sync blank out content the reader could see a moment ago.
     */
    private fun merge(
        dto: ps.hikayatalquds.data.remote.LocationDto,
        existing: ps.hikayatalquds.data.local.LocationEntity?,
        index: Int,
    ) = existing?.copy(
        name = dto.name,
        arabicName = dto.arabicName ?: existing.arabicName,
        city = dto.city ?: existing.city,
        country = dto.country,
        latitude = dto.latitude ?: existing.latitude,
        longitude = dto.longitude ?: existing.longitude,
        description = dto.description ?: existing.description,
        categories = dto.categories.ifEmpty { existing.categories },
        contentFile = dto.contentFile ?: existing.contentFile,
        isPublished = dto.isPublished,
        sortOrder = existing.sortOrder,
    ) ?: ps.hikayatalquds.data.local.LocationEntity(
        id = dto.id,
        name = dto.name,
        arabicName = dto.arabicName.orEmpty(),
        city = dto.city ?: "Jerusalem",
        country = dto.country,
        latitude = dto.latitude ?: 0.0,
        longitude = dto.longitude ?: 0.0,
        description = dto.description.orEmpty(),
        arabicDescription = "",
        culturalImportance = "",
        arabicCulturalImportance = "",
        historicalSummary = "",
        arabicHistoricalSummary = "",
        landmarks = emptyList(),
        arabicLandmarks = emptyList(),
        storyTitle = dto.name,
        arabicStoryTitle = dto.arabicName.orEmpty(),
        story = dto.description.orEmpty(),
        arabicStory = "",
        walkthrough = "",
        arabicWalkthrough = "",
        coverImage = dto.coverImageUrl,
        coverImageCredit = null,
        coverImageLicense = null,
        coverImageSourceUrl = null,
        contentFile = dto.contentFile,
        categories = dto.categories,
        interests = emptyList(),
        isPublished = dto.isPublished,
        citations = emptyList(),
        sortOrder = 1_000 + index,
    )

    suspend fun locationCount(): Int = withContext(io) { contentDao.countLocations() }

    suspend fun locationsNow(): List<Location> = locations.first()
}

/** A search result, keeping the kind so the list can render it differently. */
sealed interface SearchHit {
    val location: Location
    val snippet: String

    data class LocationHit(override val location: Location, override val snippet: String) : SearchHit
    data class StoryHit(val story: Story, override val location: Location, override val snippet: String) : SearchHit
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

private fun List<CountRow>.toMap(): Map<String, Int> = associate { it.locationId to it.total }

/**
 * Arabic is typed with and without diacritics and with different alef forms;
 * folding them means a search for "القدس" finds "القُدس".
 */
internal fun String.normaliseForSearch(): String = lowercase()
    .replace(Regex("[\\u064B-\\u0652\\u0640]"), "")
    .replace('أ', 'ا')
    .replace('إ', 'ا')
    .replace('آ', 'ا')
    .replace('ى', 'ي')
    .replace('ة', 'ه')
    .trim()
