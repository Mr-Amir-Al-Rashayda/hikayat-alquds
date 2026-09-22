package ps.hikayatalquds

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.request.allowHardware
import dagger.hilt.android.testing.HiltTestApplication

/**
 * Swaps [HikayatApplication] for Hilt's test application.
 *
 * Without this the instrumented tests would boot the real application class,
 * which seeds the archive and schedules workers on startup - useful in the app,
 * noise in a test.
 */
class HikayatTestRunner : AndroidJUnitRunner() {

    override fun newApplication(
        classLoader: ClassLoader?,
        className: String?,
        context: Context?,
    ): Application = super.newApplication(classLoader, HiltTestApplication::class.java.name, context)

    override fun callApplicationOnCreate(app: Application) {
        // Photographs are decoded into software bitmaps for the whole test run.
        //
        // Coil defaults to hardware bitmaps, which cannot be drawn into a
        // software Canvas - and the software Canvas is how ScreenshotTest
        // captures a screen on a machine with no GPU. Set here because it has
        // to happen before the first image is loaded, which is before any
        // test's @Before runs, and setUnsafe rather than setSafe so it
        // replaces whatever Coil may already have created.
        SingletonImageLoader.setUnsafe { platformContext: PlatformContext ->
            ImageLoader.Builder(platformContext)
                .allowHardware(false)
                .build()
        }
        super.callApplicationOnCreate(app)
    }
}
