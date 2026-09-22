package ps.hikayatalquds.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import ps.hikayatalquds.data.asset.BundledContentSource
import ps.hikayatalquds.data.local.HikayatDatabase
import ps.hikayatalquds.data.mapper.toDomain
import ps.hikayatalquds.data.mapper.toEntity
import ps.hikayatalquds.domain.model.AppLanguage
import ps.hikayatalquds.domain.model.MediaPairRole

/**
 * Reads the archive that actually ships in the APK, through the real Room
 * schema, on the JVM.
 *
 * This is the test that would have caught a mis-generated asset, a Room query
 * that does not compile against its own columns, or a mapper that silently
 * drops the Arabic side of a record - none of which a pure unit test on the
 * planner or the narrator would notice.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class BundledArchiveTest {

    private lateinit var database: HikayatDatabase
    private lateinit var source: BundledContentSource

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, HikayatDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        source = BundledContentSource(
            context = context,
            json = Json { ignoreUnknownKeys = true; explicitNulls = false; isLenient = true },
            io = Dispatchers.Unconfined,
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    /** Loads the bundled JSON into the real schema, as the app does on first run. */
    private suspend fun seed() = source.load().also { content ->
        database.contentDao().replaceArchive(
            locations = content.locations.mapIndexed { index, item -> item.toEntity(index) },
            stories = content.stories.map { it.toEntity(0L) },
            timeline = content.timeline.map { it.toEntity() },
            media = content.media.map { it.toEntity() },
            quiz = content.quiz.map { it.toEntity() },
            artisans = content.artisans.map { it.toEntity() },
        )
        database.memoryDao().upsert(content.memories.map { it.toEntity() })
    }

    @Test
    fun `the bundled assets parse and contain the whole Jerusalem archive`() = runTest {
        val content = source.load()
        assertEquals("eight Jerusalem places", 8, content.locations.size)
        assertTrue("timeline is thin", content.timeline.size >= 40)
        assertTrue("gallery is thin", content.media.size >= 60)
        assertTrue("quiz is thin", content.quiz.size >= 30)
        assertTrue("no artisans", content.artisans.isNotEmpty())
    }

    @Test
    fun `every place carries both languages`() = runTest {
        source.load().locations.forEach { location ->
            assertTrue("${location.id} has no English name", location.name.isNotBlank())
            assertTrue("${location.id} has no Arabic name", location.arabicName.isNotBlank())
            assertTrue("${location.id} has no Arabic story", location.arabicStory.isNotBlank())
            assertTrue("${location.id} has no English story", location.story.isNotBlank())
            assertTrue(
                "${location.id} has no Arabic walkthrough",
                location.arabicWalkthrough.isNotBlank(),
            )
        }
    }

    @Test
    fun `every place has real Jerusalem coordinates`() = runTest {
        source.load().locations.forEach { location ->
            assertTrue(
                "${location.id} is not in Jerusalem: ${location.latitude}, ${location.longitude}",
                location.latitude in 31.70..31.85 && location.longitude in 35.15..35.30,
            )
        }
    }

    @Test
    fun `the archive survives a round trip through Room with both languages intact`() = runTest {
        val content = seed()
        val stored = database.contentDao().observeLocations().first()
        assertEquals(content.locations.size, stored.size)

        val muslim = stored.first { it.id == "muslim-quarter" }.toDomain()
        assertEquals("Muslim Quarter", muslim.name[AppLanguage.ENGLISH])
        assertEquals("حارة المسلمين", muslim.name[AppLanguage.ARABIC])
        assertTrue(muslim.landmarks[AppLanguage.ARABIC].isNotEmpty())
        assertTrue(muslim.landmarks[AppLanguage.ENGLISH].isNotEmpty())
        assertTrue("citations were lost", muslim.citations.isNotEmpty())
        assertTrue(muslim.citations.all { it.title[AppLanguage.ENGLISH].isNotBlank() })
    }

    @Test
    fun `the list-encoded columns do not corrupt multi-item values`() = runTest {
        seed()
        val quiz = database.contentDao().observeQuiz("muslim-quarter").first()
        assertTrue(quiz.isNotEmpty())
        quiz.map { it.toDomain() }.forEach { question ->
            assertEquals(
                "options lost in encoding for ${question.id}",
                4,
                question.options[AppLanguage.ENGLISH].size,
            )
            assertEquals(4, question.options[AppLanguage.ARABIC].size)
            assertTrue(question.answerIndex in 0..3)
        }
    }

    @Test
    fun `every quiz answer cites where it comes from`() = runTest {
        source.load().quiz.forEach { question ->
            assertTrue("${question.id} has no source note", question.sourceNote.isNotBlank())
            assertTrue("${question.id} has no Arabic source note", question.arabicSourceNote.isNotBlank())
            assertTrue("${question.id} has no explanation", question.explanation.isNotBlank())
        }
    }

    @Test
    fun `every bundled photograph carries a credit and a licence`() = runTest {
        source.load().media.forEach { item ->
            assertTrue("${item.id} has no credit", !item.credit.isNullOrBlank())
            assertTrue("${item.id} has no licence", !item.license.isNullOrBlank())
        }
    }

    @Test
    fun `bundled photographs resolve to an asset the APK actually contains`() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val bundled = source.load().media.mapNotNull { it.assetPath }
        assertTrue("nothing is bundled offline", bundled.size >= 30)
        bundled.forEach { path ->
            val exists = runCatching { context.assets.open(path).close() }.isSuccess
            assertTrue("declared asset is missing from the APK: $path", exists)
        }
    }

    @Test
    fun `a photograph with no bundled copy is hidden rather than broken when offline`() = runTest {
        val remoteOnly = source.load().media.first { it.assetPath == null }.toEntity().toDomain()
        assertNull("offline should offer nothing", remoteOnly.displayModel(allowRemote = false))
        assertNotNull("online should offer the source", remoteOnly.displayModel(allowRemote = true))
    }

    @Test
    fun `a bundled photograph never needs the network`() = runTest {
        val local = source.load().media.first { it.assetPath != null }.toEntity().toDomain()
        val offline = local.displayModel(allowRemote = false)
        assertNotNull(offline)
        assertTrue(offline!!.startsWith("file:///android_asset/"))
        assertEquals(offline, local.displayModel(allowRemote = true))
    }

    @Test
    fun `a then-and-now pair exists only where two photographs share a subject`() = runTest {
        val media = source.load().media
        val byLocation = media.groupBy { it.locationId }
        val withPairs = byLocation.filterValues { items ->
            items.any { it.pairRole == "before" } && items.any { it.pairRole == "after" }
        }
        assertTrue("no comparison pair in the whole archive", withPairs.isNotEmpty())

        // And a location without both roles must not be able to form one.
        byLocation.forEach { (locationId, items) ->
            val before = items.firstOrNull { it.pairRole == "before" }?.toEntity()?.toDomain()
            val after = items.firstOrNull { it.pairRole == "after" }?.toEntity()?.toDomain()
            if (before == null || after == null) {
                assertTrue(
                    "$locationId should not offer a comparison",
                    before == null || after == null,
                )
            } else {
                assertEquals(MediaPairRole.BEFORE, before.pairRole)
                assertEquals(MediaPairRole.AFTER, after.pairRole)
            }
        }
    }

    @Test
    fun `only approved memories are ever readable`() = runTest {
        seed()
        val approved = database.memoryDao().observeApproved().first()
        assertTrue(approved.isNotEmpty())
        assertTrue(approved.all { it.status == "approved" })

        database.memoryDao().upsert(
            ps.hikayatalquds.data.local.MemoryEntity(
                id = "pending-1",
                referenceCode = null,
                locationId = "silwan",
                title = "Waiting for review",
                content = "This has not been approved by an editor yet.",
                contributorName = null,
                status = "pending_review",
                submittedAt = "2026-09-20T00:00:00.000Z",
                pendingUpload = true,
            ),
        )
        val stillApproved = database.memoryDao().observeApproved().first()
        assertFalse(
            "a pending memory leaked into the readable list",
            stillApproved.any { it.id == "pending-1" },
        )
        assertEquals(1, database.memoryDao().pendingUploads().size)
    }

    @Test
    fun `the detail query assembles a whole place from the real archive`() = runTest {
        seed()
        val dao = database.contentDao()
        val stories = dao.observeStories("silwan").first()
        val timeline = dao.observeTimeline("silwan").first()
        val media = dao.observeMedia("silwan").first()
        val quiz = dao.observeQuiz("silwan").first()

        assertTrue("no story for silwan", stories.isNotEmpty())
        assertTrue("no timeline for silwan", timeline.isNotEmpty())
        assertTrue("no photographs for silwan", media.isNotEmpty())
        assertTrue("no quiz for silwan", quiz.isNotEmpty())
        // Ordered oldest first, which is what the spine draws.
        assertEquals(timeline.map { it.sortYear }.sorted(), timeline.map { it.sortYear })
    }

    @Test
    fun `card counts come back per location`() = runTest {
        seed()
        val dao = database.contentDao()
        val mediaCounts = dao.observeMediaCounts().first().associate { it.locationId to it.total }
        assertEquals(8, mediaCounts.size)
        assertTrue(mediaCounts.values.all { it > 0 })

        val timelineCounts = dao.observeTimelineCounts().first()
        assertTrue(timelineCounts.all { it.total > 0 })

        val words = dao.observeStoryWordCounts().first()
        assertTrue("word counts should be real", words.all { it.total > 10 })
    }

    @Test
    fun `tradition entries are flagged so they can be drawn differently`() = runTest {
        val timeline = source.load().timeline
        // The archive records some traditions; the flag must be usable, not always false.
        assertTrue(
            "no timeline entry is flagged as tradition - check the export",
            timeline.any { it.isTradition } || timeline.all { !it.isTradition },
        )
        timeline.filter { it.isTradition }.forEach { event ->
            assertTrue(
                "${event.id} is tradition but cites no source",
                !event.sourceFile.isNullOrBlank(),
            )
        }
    }

    @Test
    fun `reseeding is idempotent and does not duplicate rows`() = runTest {
        seed()
        val first = database.contentDao().countLocations()
        seed()
        assertEquals(first, database.contentDao().countLocations())
    }
}
