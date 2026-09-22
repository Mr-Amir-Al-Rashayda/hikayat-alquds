package ps.hikayatalquds.data.sync

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import ps.hikayatalquds.R
import ps.hikayatalquds.data.preferences.UserPreferencesRepository
import ps.hikayatalquds.data.repository.ContentRepository
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * A quiet daily nudge with the story of the day.
 *
 * Off unless the reader turns it on, and it carries the actual story title
 * rather than a generic "come back" - if it is not worth reading, it is not
 * worth sending.
 */
@HiltWorker
class DailyStoryWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted parameters: WorkerParameters,
    private val content: ContentRepository,
    private val preferences: UserPreferencesRepository,
) : CoroutineWorker(context, parameters) {

    override suspend fun doWork(): Result {
        val settings = preferences.current()
        if (!settings.dailyStoryReminder) return Result.success()
        // Checked here rather than in a helper so the static analysis can see
        // it, and because the reader can revoke the permission between this
        // worker being scheduled and it running.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return Result.success()
        }

        val dayIndex = System.currentTimeMillis() / DAY_MILLIS
        val featured = content.featuredStory(dayIndex).first() ?: return Result.success()
        val (story, location) = featured
        val language = settings.language

        val title = if (language.isArabic) "حكاية اليوم" else "Story of the day"
        val body = "${story.title[language]} · ${location.name[language]}"

        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(Intent.ACTION_VIEW, Uri.parse("hikayat://locations/${location.id}")).apply {
                setPackage(context.packageName)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        createChannel(context, language.isArabic)
        NotificationManagerCompat.from(context).notify(
            NOTIFICATION_ID,
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(story.summary[language]))
                .setContentIntent(open)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build(),
        )
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "hikayat-daily-story"
        private const val CHANNEL_ID = "daily-story"
        private const val NOTIFICATION_ID = 4201
        private const val DAY_MILLIS = 86_400_000L

        /** Nine in the morning, local time - a reading hour, not a commute alarm. */
        private const val TARGET_HOUR = 9

        fun createChannel(context: Context, arabic: Boolean) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    if (arabic) "حكاية اليوم" else "Story of the day",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = if (arabic) {
                        "تذكير يومي واحد بحكاية من أرشيف القدس."
                    } else {
                        "One daily reminder with a story from the Jerusalem archive."
                    }
                },
            )
        }

        fun schedule(workManager: WorkManager) {
            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                PeriodicWorkRequestBuilder<DailyStoryWorker>(1, TimeUnit.DAYS)
                    .setInitialDelay(millisUntilNextNine(), TimeUnit.MILLISECONDS)
                    .build(),
            )
        }

        fun cancel(workManager: WorkManager) = workManager.cancelUniqueWork(WORK_NAME)

        private fun millisUntilNextNine(): Long {
            val now = Calendar.getInstance()
            val target = (now.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, TARGET_HOUR)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
            }
            return target.timeInMillis - now.timeInMillis
        }
    }
}
