package ps.hikayatalquds.data.remote

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import ps.hikayatalquds.data.local.MemoryEntity
import ps.hikayatalquds.di.IoDispatcher
import ps.hikayatalquds.domain.model.AppLanguage
import ps.hikayatalquds.domain.model.AudienceMode
import ps.hikayatalquds.domain.model.ChatTurn
import ps.hikayatalquds.domain.model.GeneratedStory
import ps.hikayatalquds.domain.model.GenerationOrigin
import ps.hikayatalquds.domain.model.GuideAnswer
import ps.hikayatalquds.domain.model.MemoryReceipt
import ps.hikayatalquds.domain.model.MemoryStatus
import ps.hikayatalquds.domain.model.MemoryStatusReport
import ps.hikayatalquds.domain.model.NewMemory
import ps.hikayatalquds.domain.model.StoryTone
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

/** What one refresh from the API brought back. */
data class ArchiveSnapshot(
    val locations: List<LocationDto>,
    val memories: List<MemoryEntity>,
)

/** A request failed and the reader is owed a reason, not a spinner forever. */
class ApiException(message: String, val statusCode: Int? = null) : Exception(message)

/**
 * The API, wrapped so nothing above this layer sees Retrofit.
 *
 * Reads come back as `Result` because the caller always has something to fall
 * back on - the bundled archive. Writes do not get a fallback: pretending a
 * contribution was saved when nothing was stored would be a lie to the person
 * who wrote it, so a failed submission fails visibly.
 */
@Singleton
class RemoteContentSource @Inject constructor(
    private val api: HikayatApi,
    private val baseUrl: BaseUrlInterceptor,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    val isConfigured: Boolean get() = baseUrl.isConfigured

    private suspend fun <T> call(block: suspend () -> T): Result<T> = withContext(io) {
        if (!baseUrl.isConfigured) {
            return@withContext Result.failure(NoApiConfiguredException())
        }
        runCatching { block() }.recoverCatching { error -> throw error.asApiException() }
    }

    suspend fun health(): Result<HealthDto> = call { api.health() }

    suspend fun fetchArchive(): Result<ArchiveSnapshot> = call {
        val locations = api.locations()
        // Approved memories are the part of the archive that genuinely changes
        // between builds, so they are always worth a round trip.
        val memories = runCatching { api.contributions() }.getOrDefault(emptyList())
        ArchiveSnapshot(
            locations = locations,
            memories = memories.map { it.toEntity() },
        )
    }

    suspend fun memories(locationId: String? = null): Result<List<MemoryEntity>> =
        call { api.contributions(locationId).map { it.toEntity() } }

    suspend fun submitMemory(memory: NewMemory): Result<MemoryReceipt> = call {
        val receipt = api.submitContribution(
            NewContributionDto(
                locationId = memory.locationId,
                title = memory.title,
                content = memory.content,
                contributorName = memory.contributorName?.takeIf { it.isNotBlank() },
                contributorEmail = memory.contributorEmail?.takeIf { it.isNotBlank() },
            ),
        )
        MemoryReceipt(
            status = MemoryStatus.fromWire(receipt.status),
            message = receipt.message,
            referenceCode = receipt.referenceCode ?: receipt.contribution?.referenceCode,
        )
    }

    suspend fun memoryStatus(referenceCode: String): Result<MemoryStatusReport> = call {
        val dto = api.contributionStatus(referenceCode.trim().uppercase())
        MemoryStatusReport(
            referenceCode = dto.referenceCode,
            locationId = dto.locationId,
            title = dto.title,
            status = MemoryStatus.fromWire(dto.status),
            submittedAt = dto.submittedAt,
            reviewedAt = dto.reviewedAt,
            message = dto.message,
            reviewNotes = dto.reviewNotes,
        )
    }

    suspend fun generateStory(
        locationId: String,
        audience: AudienceMode,
        tone: StoryTone,
        language: AppLanguage,
    ): Result<GeneratedStory> = call {
        val dto = api.generateStory(
            GenerateStoryRequest(
                locationId = locationId,
                targetAudience = audience.id,
                tone = tone.id,
                language = language.tag,
            ),
        )
        GeneratedStory(
            title = dto.title,
            summary = dto.summary,
            narrative = dto.narrative,
            audience = AudienceMode.fromId(dto.targetAudience),
            tone = StoryTone.fromId(dto.tone),
            language = AppLanguage.fromTag(dto.language),
            wordCount = dto.wordCount,
            uncertaintyNotes = dto.uncertaintyNotes,
            origin = GenerationOrigin.fromWire(dto.generatedBy),
            sourceFile = dto.source?.contentFile ?: dto.source?.summaryFile,
            locationId = locationId,
        )
    }

    suspend fun askGuide(
        locationId: String,
        question: String,
        history: List<ChatTurn>,
        language: AppLanguage,
    ): Result<GuideAnswer> = call {
        val dto = api.askGuide(
            AskGuideRequest(
                locationId = locationId,
                question = question,
                history = history.map { ChatTurnDto(it.question, it.answer) },
                language = language.tag,
            ),
        )
        GuideAnswer(
            answer = dto.answer,
            answeredFromSource = dto.answeredFromSource,
            excerpts = dto.excerpts,
            uncertaintyNotes = dto.uncertaintyNotes,
            origin = GenerationOrigin.fromWire(dto.generatedBy),
        )
    }
}

private fun ContributionDto.toEntity() = MemoryEntity(
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

private fun Throwable.asApiException(): Throwable = when (this) {
    is HttpException -> ApiException(readMessage(), code())
    is NoApiConfiguredException -> this
    is java.net.SocketTimeoutException -> ApiException("The request to the Hikayat AlQuds API timed out.")
    is java.io.IOException -> ApiException(message ?: "Could not reach the Hikayat AlQuds API.")
    else -> this
}

/** Nest sends `message` as a string or as an array of validation failures. */
private fun HttpException.readMessage(): String {
    val body = runCatching { response()?.errorBody()?.string() }.getOrNull()
    if (body.isNullOrBlank()) return "Request failed with status ${code()}."
    val match = Regex("\"message\"\\s*:\\s*(\"([^\"]*)\"|\\[([^]]*)])").find(body)
    val single = match?.groupValues?.getOrNull(2)?.takeIf { it.isNotBlank() }
    val many = match?.groupValues?.getOrNull(3)
        ?.split(',')
        ?.map { it.trim().trim('"') }
        ?.filter { it.isNotBlank() }
        ?.joinToString("; ")
        ?.takeIf { it.isNotBlank() }
    return single ?: many ?: "Request failed with status ${code()}."
}
