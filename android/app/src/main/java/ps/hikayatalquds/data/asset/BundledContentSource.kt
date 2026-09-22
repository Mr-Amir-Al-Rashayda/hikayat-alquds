package ps.hikayatalquds.data.asset

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import ps.hikayatalquds.di.IoDispatcher
import javax.inject.Inject
import javax.inject.Singleton

/** The reviewed archive as it ships inside the APK. */
data class BundledContent(
    val locations: List<BundledLocation>,
    val stories: List<BundledStory>,
    val timeline: List<BundledTimelineEvent>,
    val media: List<BundledMedia>,
    val quiz: List<BundledQuizQuestion>,
    val memories: List<BundledMemory>,
    val artisans: List<BundledArtisan>,
)

/**
 * Reads the JSON files in the bundled `content` assets folder.
 *
 * This is the app's floor: with no network, no backend and no database server,
 * these files are what a reader gets, and they are the full reviewed archive
 * rather than a teaser. Everything else - live contributions, generated
 * narratives - is an addition on top, and the app says so when it is missing.
 */
@Singleton
class BundledContentSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    private suspend fun readText(fileName: String): String = withContext(io) {
        context.assets.open("$FOLDER/$fileName").bufferedReader().use { it.readText() }
    }

    suspend fun load(): BundledContent = BundledContent(
        locations = json.decodeFromString(readText("locations.json")),
        stories = json.decodeFromString(readText("stories.json")),
        timeline = json.decodeFromString(readText("timeline.json")),
        media = json.decodeFromString(readText("media.json")),
        quiz = json.decodeFromString(readText("quiz.json")),
        memories = json.decodeFromString(readText("memories.json")),
        artisans = json.decodeFromString(readText("artisans.json")),
    )

    suspend fun voices(): List<BundledVoice> = json.decodeFromString(readText("voices.json"))

    private companion object {
        const val FOLDER = "content"
    }
}
