package ps.hikayatalquds.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ContentDao {

    // --- reads ---------------------------------------------------------------

    @Query("SELECT * FROM locations WHERE isPublished = 1 ORDER BY sortOrder")
    fun observeLocations(): Flow<List<LocationEntity>>

    @Query("SELECT * FROM locations WHERE id = :id")
    fun observeLocation(id: String): Flow<LocationEntity?>

    @Query("SELECT * FROM locations WHERE id = :id")
    suspend fun findLocation(id: String): LocationEntity?

    @Query("SELECT COUNT(*) FROM locations")
    suspend fun countLocations(): Int

    @Query("SELECT * FROM stories WHERE locationId = :locationId AND status = 'published' ORDER BY createdAt")
    fun observeStories(locationId: String): Flow<List<StoryEntity>>

    @Query("SELECT * FROM stories WHERE status = 'published' ORDER BY id")
    fun observePublishedStories(): Flow<List<StoryEntity>>

    @Query("SELECT * FROM timeline_events WHERE locationId = :locationId ORDER BY sortYear, sortOrder")
    fun observeTimeline(locationId: String): Flow<List<TimelineEventEntity>>

    @Query("SELECT * FROM media WHERE locationId = :locationId ORDER BY sortOrder")
    fun observeMedia(locationId: String): Flow<List<MediaEntity>>

    @Query("SELECT * FROM quiz_questions WHERE locationId = :locationId ORDER BY sortOrder")
    fun observeQuiz(locationId: String): Flow<List<QuizQuestionEntity>>

    @Query("SELECT * FROM artisans WHERE locationId = :locationId ORDER BY id")
    fun observeArtisans(locationId: String): Flow<List<ArtisanEntity>>

    @Query("SELECT locationId, COUNT(*) AS total FROM stories WHERE status = 'published' GROUP BY locationId")
    fun observeStoryCounts(): Flow<List<CountRow>>

    @Query("SELECT locationId, COUNT(*) AS total FROM media GROUP BY locationId")
    fun observeMediaCounts(): Flow<List<CountRow>>

    @Query("SELECT locationId, COUNT(*) AS total FROM timeline_events GROUP BY locationId")
    fun observeTimelineCounts(): Flow<List<CountRow>>

    /**
     * Reading time is estimated from the reviewed English and Arabic bodies at
     * 200 words a minute, the usual figure for prose on a phone.
     */
    @Query("SELECT id AS locationId, (LENGTH(story) - LENGTH(REPLACE(story, ' ', '')) + 1) AS total FROM locations")
    fun observeStoryWordCounts(): Flow<List<CountRow>>

    // --- writes --------------------------------------------------------------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLocations(items: List<LocationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStories(items: List<StoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTimeline(items: List<TimelineEventEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMedia(items: List<MediaEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertQuiz(items: List<QuizQuestionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertArtisans(items: List<ArtisanEntity>)

    @Transaction
    suspend fun replaceArchive(
        locations: List<LocationEntity>,
        stories: List<StoryEntity>,
        timeline: List<TimelineEventEntity>,
        media: List<MediaEntity>,
        quiz: List<QuizQuestionEntity>,
        artisans: List<ArtisanEntity>,
    ) {
        upsertLocations(locations)
        upsertStories(stories)
        upsertTimeline(timeline)
        upsertMedia(media)
        upsertQuiz(quiz)
        upsertArtisans(artisans)
    }
}

data class CountRow(val locationId: String, val total: Int)

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memories WHERE status = 'approved' ORDER BY submittedAt DESC")
    fun observeApproved(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE locationId = :locationId AND status = 'approved' ORDER BY submittedAt DESC")
    fun observeApprovedFor(locationId: String): Flow<List<MemoryEntity>>

    @Query("SELECT locationId, COUNT(*) AS total FROM memories WHERE status = 'approved' GROUP BY locationId")
    fun observeCounts(): Flow<List<CountRow>>

    /** Submissions this device made that have not reached the review queue yet. */
    @Query("SELECT * FROM memories WHERE pendingUpload = 1 ORDER BY submittedAt")
    suspend fun pendingUploads(): List<MemoryEntity>

    @Query("SELECT * FROM memories WHERE pendingUpload = 1 ORDER BY submittedAt")
    fun observePendingUploads(): Flow<List<MemoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(items: List<MemoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: MemoryEntity)

    @Query("DELETE FROM memories WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM memories WHERE pendingUpload = 0 AND status = 'approved'")
    suspend fun clearApproved()
}

@Dao
interface GeneratedStoryDao {
    @Query("SELECT * FROM generated_stories WHERE locationId = :locationId ORDER BY createdAt DESC")
    fun observeFor(locationId: String): Flow<List<GeneratedStoryEntity>>

    @Query(
        "SELECT * FROM generated_stories WHERE locationId = :locationId AND audience = :audience " +
            "AND tone = :tone AND language = :language ORDER BY createdAt DESC LIMIT 1",
    )
    suspend fun findCached(
        locationId: String,
        audience: String,
        tone: String,
        language: String,
    ): GeneratedStoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: GeneratedStoryEntity)

    @Query("DELETE FROM generated_stories")
    suspend fun clear()
}
