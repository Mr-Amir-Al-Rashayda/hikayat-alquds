package ps.hikayatalquds

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.request.allowHardware
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import ps.hikayatalquds.core.ui.TestTags
import ps.hikayatalquds.ui.detail.DetailTab
import ps.hikayatalquds.ui.navigation.TopLevelDestination
import java.io.File

/**
 * Writes a PNG of each main screen to the device's external files directory.
 *
 * Deliberately draws the view hierarchy into a software `Canvas` rather than
 * using `screencap` or `captureToImage`: both of those read the window's
 * surface, which a headless emulator without a GPU returns as solid black.
 * Drawing the view re-renders it on the CPU, so the picture is real wherever
 * this runs.
 *
 * Not an assertion - it is a way to actually look at the app.
 */
@LargeTest
@HiltAndroidTest
class ScreenshotTest {

    companion object {
        init {
            // Runs when this class is loaded, which is before the compose rule
            // launches the Activity and therefore before Coil decodes anything.
            // Coil's default is a hardware bitmap, and a hardware bitmap cannot
            // be drawn into the software Canvas this capture relies on.
            SingletonImageLoader.setUnsafe { platformContext ->
                ImageLoader.Builder(platformContext).allowHardware(false).build()
            }
        }
    }

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val outputDir: File
        get() = InstrumentationRegistry.getInstrumentation()
            .targetContext
            .getExternalFilesDir(null)!!
            .resolve("screenshots")
            .apply { mkdirs() }

    private fun awaitTag(tag: String, timeoutMillis: Long = 15_000) {
        composeRule.waitUntil(timeoutMillis) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun shoot(name: String) {
        composeRule.waitForIdle()
        val root: View = composeRule.activity.window.decorView
        val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            // Compose records clipped containers - every scrolling list - into
            // RenderNodes, which a plain software draw skips, leaving a page
            // with only its unlayered chrome. Switching the hierarchy to a
            // software layer for the capture makes Compose take its own
            // non-RenderNode path, so the whole screen actually appears.
            root.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            root.draw(Canvas(bitmap))
            root.setLayerType(View.LAYER_TYPE_NONE, null)
        }
        File(outputDir, "$name.png").outputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
        bitmap.recycle()
    }

    @Test
    fun capture_every_main_screen() {
        awaitTag(TestTags.HOME_LIST)
        shoot("01-home")

        composeRule.onNodeWithTag(TestTags.navItem(TopLevelDestination.MAP.name)).performClick()
        awaitTag(TestTags.MAP_CANVAS)
        composeRule.waitForIdle()
        shoot("02-map")

        composeRule.onNodeWithTag(TestTags.navItem(TopLevelDestination.CONNECTIONS.name))
            .performClick()
        awaitTag(TestTags.CONSTELLATION_CANVAS)
        shoot("03-connections")

        composeRule.onNodeWithTag(TestTags.navItem(TopLevelDestination.PLAN.name)).performClick()
        awaitTag(TestTags.PLAN_NEXT)
        shoot("04-plan")

        composeRule.onNodeWithTag(TestTags.navItem(TopLevelDestination.HOME.name)).performClick()
        awaitTag(TestTags.HOME_LIST)
        composeRule.onNodeWithTag(TestTags.HOME_LIST).performScrollToKey("muslim-quarter")
        composeRule.onNodeWithTag(TestTags.placeCard("muslim-quarter")).performClick()
        awaitTag(TestTags.DETAIL_LIST)
        shoot("05-detail-story")

        composeRule.onNodeWithTag(TestTags.detailTab(DetailTab.TIMELINE.name)).performClick()
        composeRule.waitForIdle()
        shoot("06-detail-timeline")

        composeRule.onNodeWithTag(TestTags.detailTab(DetailTab.GALLERY.name)).performClick()
        composeRule.waitForIdle()
        shoot("07-detail-gallery")
    }

    /**
     * The map on its own.
     *
     * Separate from the walk through every screen because the map is the one
     * screen drawn entirely by hand, with no photographs on it - so it captures
     * cleanly even where a hardware-decoded bitmap would not.
     */
    @Test
    fun capture_the_map() {
        awaitTag(TestTags.HOME_LIST)
        composeRule.onNodeWithTag(TestTags.navItem(TopLevelDestination.MAP.name)).performClick()
        awaitTag(TestTags.MAP_CANVAS)
        composeRule.waitForIdle()
        shoot("map")

        // And again with a place selected, which is when the labels have to
        // make room for a larger name.
        composeRule.onNodeWithTag(TestTags.MAP_CANVAS).performTouchInput { click(center) }
        composeRule.waitForIdle()
        shoot("map-selected")
    }
}
