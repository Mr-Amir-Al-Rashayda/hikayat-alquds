package ps.hikayatalquds.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import ps.hikayatalquds.data.local.MemoryDao
import ps.hikayatalquds.data.local.MemoryEntity
import ps.hikayatalquds.data.mapper.toDomain
import ps.hikayatalquds.data.remote.RemoteContentSource
import ps.hikayatalquds.di.IoDispatcher
import ps.hikayatalquds.domain.model.Memory
import ps.hikayatalquds.domain.model.MemoryReceipt
import ps.hikayatalquds.domain.model.MemoryStatus
import ps.hikayatalquds.domain.model.MemoryStatusReport
import ps.hikayatalquds.domain.model.NewMemory
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** A submission that is written down but has not reached the review queue yet. */
data class QueuedMemory(val memory: Memory, val queuedAt: String)

/**
 * Memories people contribute.
 *
 * Two rules from the web app carry over unchanged. First, nothing a visitor
 * submits appears to anybody until an editor approves it; the app only ever
 * displays `approved` rows. Second, a submission is never faked - if it cannot
 * be sent, it is stored on the device as *queued*, the contributor is told
 * exactly that, and it goes out on the next connection. What is not acceptable
 * is a receipt for something that was never sent.
 */
@Singleton
class MemoryRepository @Inject constructor(
    private val dao: MemoryDao,
    private val remote: RemoteContentSource,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    val approved: Flow<List<Memory>> = dao.observeApproved().map { rows -> rows.map { it.toDomain() } }

    fun approvedFor(locationId: String): Flow<List<Memory>> =
        dao.observeApprovedFor(locationId).map { rows -> rows.map { it.toDomain() } }

    val queued: Flow<List<QueuedMemory>> = dao.observePendingUploads().map { rows ->
        rows.map { QueuedMemory(it.toDomain(), it.submittedAt) }
    }

    /**
     * Sends a memory for review.
     *
     * @return a receipt when the review queue accepted it, or a failure that
     *   says whether it was queued on the device or rejected outright.
     */
    suspend fun submit(memory: NewMemory): SubmissionResult = withContext(io) {
        if (!remote.isConfigured) {
            queue(memory)
            return@withContext SubmissionResult.Queued
        }
        remote.submitMemory(memory).fold(
            onSuccess = { receipt -> SubmissionResult.Accepted(receipt) },
            onFailure = { error ->
                val rejected = (error as? ps.hikayatalquds.data.remote.ApiException)
                    ?.statusCode
                    ?.let { it in 400..499 }
                    ?: false
                if (rejected) {
                    // The server looked at it and said no - queueing would just
                    // replay the same rejection later.
                    SubmissionResult.Rejected(error.message ?: "The submission was not accepted.")
                } else {
                    queue(memory)
                    SubmissionResult.Queued
                }
            },
        )
    }

    private suspend fun queue(memory: NewMemory) {
        dao.upsert(
            MemoryEntity(
                id = "queued-${UUID.randomUUID()}",
                referenceCode = null,
                locationId = memory.locationId,
                title = memory.title,
                content = memory.content,
                contributorName = memory.contributorName,
                status = MemoryStatus.PENDING_REVIEW.wire,
                submittedAt = java.time.Instant.now().toString(),
                pendingUpload = true,
                contributorEmail = memory.contributorEmail,
            ),
        )
    }

    /**
     * Retries everything queued on this device. Called by the sync worker when
     * a connection comes back.
     *
     * @return how many submissions reached the review queue.
     */
    suspend fun flushQueue(): Int = withContext(io) {
        if (!remote.isConfigured) return@withContext 0
        var sent = 0
        dao.pendingUploads().forEach { queuedMemory ->
            val result = remote.submitMemory(
                NewMemory(
                    locationId = queuedMemory.locationId,
                    title = queuedMemory.title,
                    content = queuedMemory.content,
                    contributorName = queuedMemory.contributorName,
                    contributorEmail = queuedMemory.contributorEmail,
                ),
            )
            if (result.isSuccess) {
                dao.delete(queuedMemory.id)
                sent += 1
            }
        }
        sent
    }

    suspend fun lookUpStatus(referenceCode: String): Result<MemoryStatusReport> = withContext(io) {
        remote.memoryStatus(referenceCode)
    }

    suspend fun refresh(locationId: String? = null): Boolean = withContext(io) {
        remote.memories(locationId).fold(
            onSuccess = { rows ->
                if (rows.isNotEmpty()) dao.upsert(rows)
                true
            },
            onFailure = { false },
        )
    }
}

sealed interface SubmissionResult {
    /** The review queue has it; the reference code is how to check on it later. */
    data class Accepted(val receipt: MemoryReceipt) : SubmissionResult

    /** Written down on this device and waiting for a connection. */
    data object Queued : SubmissionResult

    data class Rejected(val message: String) : SubmissionResult
}
