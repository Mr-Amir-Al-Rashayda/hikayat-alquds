package ps.hikayatalquds.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import ps.hikayatalquds.data.repository.ContentRepository
import ps.hikayatalquds.data.repository.MemoryRepository
import java.util.concurrent.TimeUnit

/**
 * Brings the archive up to date, and sends anything this device has queued.
 *
 * Runs only when a network is available and only when an API address is
 * configured. It is not how the app gets its content - the APK already has all
 * of it - so a failure here is logged and retried, never surfaced as an error
 * that stops somebody reading.
 */
@HiltWorker
class ContentSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val content: ContentRepository,
    private val memories: MemoryRepository,
) : CoroutineWorker(context, parameters) {

    override suspend fun doWork(): Result {
        // A contribution somebody wrote goes out first: it is the only thing
        // here that a person is waiting on.
        val flushed = runCatching { memories.flushQueue() }.getOrDefault(0)
        val outcome = content.sync()
        return when {
            outcome.failureReason == null -> Result.success()
            flushed > 0 -> Result.success()
            runAttemptCount >= MAX_ATTEMPTS -> Result.success()
            else -> Result.retry()
        }
    }

    companion object {
        private const val MAX_ATTEMPTS = 4
        private const val PERIODIC_WORK = "hikayat-content-sync"
        private const val ONE_SHOT_WORK = "hikayat-content-sync-now"

        private val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        /** Once a day is plenty: reviewed heritage content does not churn. */
        fun schedule(workManager: WorkManager) {
            workManager.enqueueUniquePeriodicWork(
                PERIODIC_WORK,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<ContentSyncWorker>(1, TimeUnit.DAYS)
                    .setConstraints(constraints)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                    .build(),
            )
        }

        /** Used by pull-to-refresh and by Settings after the API address changes. */
        fun syncNow(workManager: WorkManager) {
            workManager.enqueueUniqueWork(
                ONE_SHOT_WORK,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<ContentSyncWorker>()
                    .setConstraints(constraints)
                    .setBackoffCriteria(BackoffPolicy.LINEAR, 20, TimeUnit.SECONDS)
                    .build(),
            )
        }

        fun cancel(workManager: WorkManager) {
            workManager.cancelUniqueWork(PERIODIC_WORK)
        }
    }
}
