package ps.hikayatalquds.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import ps.hikayatalquds.data.local.GeneratedStoryDao
import ps.hikayatalquds.data.mapper.toDomain
import ps.hikayatalquds.data.mapper.toEntity
import ps.hikayatalquds.data.remote.RemoteContentSource
import ps.hikayatalquds.di.IoDispatcher
import ps.hikayatalquds.domain.model.AppLanguage
import ps.hikayatalquds.domain.model.AudienceMode
import ps.hikayatalquds.domain.model.ChatTurn
import ps.hikayatalquds.domain.model.GeneratedStory
import ps.hikayatalquds.domain.model.GuideAnswer
import ps.hikayatalquds.domain.model.StoryTone
import ps.hikayatalquds.domain.narrative.OnDeviceNarrator
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Story generation and the heritage guide.
 *
 * The order is deliberate: ask the backend if one is configured, because it can
 * reach the reviewed corpus and a language model; otherwise retell the stored
 * record on the device. Both paths are grounded in reviewed content and both
 * report which one produced the text, so a reader is never left guessing
 * whether they are reading a model's prose or their own device's.
 *
 * A generated narrative is cached per (place, audience, tone, language) so the
 * same request on a train does not come back empty.
 */
@Singleton
class NarrativeRepository @Inject constructor(
    private val remote: RemoteContentSource,
    private val narrator: OnDeviceNarrator,
    private val content: ContentRepository,
    private val dao: GeneratedStoryDao,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    suspend fun generateStory(
        locationId: String,
        audience: AudienceMode,
        tone: StoryTone,
        language: AppLanguage,
        forceRefresh: Boolean = false,
    ): Result<GeneratedStory> = withContext(io) {
        val location = content.location(locationId).first()
            ?: return@withContext Result.failure(
                IllegalArgumentException("No reviewed record for location \"$locationId\"."),
            )

        if (!forceRefresh) {
            dao.findCached(locationId, audience.id, tone.id, language.tag)?.let { cached ->
                return@withContext Result.success(cached.toDomain())
            }
        }

        val remoteResult = remote.generateStory(locationId, audience, tone, language)
        val story = remoteResult.getOrNull()
            ?.takeIf { it.narrative.isNotBlank() }
            ?: narrator.generate(location, audience, tone, language)

        dao.upsert(story.toEntity(System.currentTimeMillis()))
        Result.success(story)
    }

    fun cachedStories(locationId: String) =
        dao.observeFor(locationId)

    suspend fun askGuide(
        locationId: String,
        question: String,
        history: List<ChatTurn>,
        language: AppLanguage,
    ): GuideAnswer = withContext(io) {
        val remoteAnswer = remote.askGuide(locationId, question, history, language).getOrNull()
        if (remoteAnswer != null && remoteAnswer.answer.isNotBlank()) return@withContext remoteAnswer

        val location = content.location(locationId).first()
        if (location == null) {
            return@withContext GuideAnswer(
                answer = if (language.isArabic) {
                    "لا تتوفر معلومات مراجعة لهذا المكان بعد."
                } else {
                    "There is no reviewed record for this place yet."
                },
                answeredFromSource = false,
                excerpts = emptyList(),
                uncertaintyNotes = emptyList(),
                origin = ps.hikayatalquds.domain.model.GenerationOrigin.ON_DEVICE,
            )
        }
        narrator.answer(location, content.timeline(locationId).first(), question, language)
    }

    suspend fun clearGeneratedStories() = withContext(io) { dao.clear() }
}
