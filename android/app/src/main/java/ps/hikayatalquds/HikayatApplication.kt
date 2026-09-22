package ps.hikayatalquds

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.crossfade
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import okio.Path.Companion.toOkioPath
import ps.hikayatalquds.data.preferences.UserPreferencesRepository
import ps.hikayatalquds.data.remote.BaseUrlInterceptor
import ps.hikayatalquds.data.repository.ContentRepository
import ps.hikayatalquds.data.repository.JourneyRepository
import ps.hikayatalquds.data.sync.ContentSyncWorker
import ps.hikayatalquds.di.ApplicationScope
import ps.hikayatalquds.narration.NarrationController
import javax.inject.Inject

@HiltAndroidApp
class HikayatApplication :
    Application(),
    Configuration.Provider,
    SingletonImageLoader.Factory {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var contentRepository: ContentRepository
    @Inject lateinit var journeyRepository: JourneyRepository
    @Inject lateinit var preferences: UserPreferencesRepository
    @Inject lateinit var baseUrl: BaseUrlInterceptor
    @Inject lateinit var narration: NarrationController

    @Inject
    @ApplicationScope
    lateinit var scope: CoroutineScope

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        scope.launch {
            // The archive has to be readable on the very first launch with no
            // network, so it is imported from the bundled JSON before anything
            // asks the database a question.
            contentRepository.seedIfNeeded()

            // Keep the HTTP client pointed at whatever address Settings holds.
            preferences.settings
                .map { it.apiBaseUrl }
                .distinctUntilChanged()
                .collect { url ->
                    baseUrl.update(url)
                    if (url.isNotBlank()) {
                        ContentSyncWorker.syncNow(WorkManager.getInstance(this@HikayatApplication))
                    }
                }
        }

        ContentSyncWorker.schedule(WorkManager.getInstance(this))

        // Finishing a narration is how "I listened to this place" gets recorded.
        narration.setOnFinishedListener { locationId ->
            scope.launch { journeyRepository.markNarrationHeard(locationId) }
        }
    }

    /**
     * Coil is configured once here rather than per screen.
     *
     * Deliberately *not* given the app's OkHttp client: that one rewrites every
     * request to the configured API address, which is exactly wrong for a
     * photograph hosted on Wikimedia Commons. The disk cache is modest because
     * the photographs that matter already ship inside the APK; this only holds
     * the Commons images that do not.
     */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.20)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image-cache").toOkioPath())
                    .maxSizeBytes(48L * 1024 * 1024)
                    .build()
            }
            .crossfade(true)
            .build()
}
