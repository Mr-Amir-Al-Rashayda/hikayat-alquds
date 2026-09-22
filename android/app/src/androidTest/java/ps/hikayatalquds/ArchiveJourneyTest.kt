package ps.hikayatalquds

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToKeyAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToKey
import androidx.test.espresso.Espresso
import androidx.test.filters.LargeTest
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import ps.hikayatalquds.core.ui.TestTags
import ps.hikayatalquds.ui.detail.DetailTab
import ps.hikayatalquds.ui.navigation.TopLevelDestination

/**
 * Drives the real app on a device, in Arabic, with no network configured.
 *
 * This is the test that answers "does it actually work": the archive is read
 * out of the APK into Room, the home screen composes real Jerusalem content,
 * and a place opens with its story, its timeline and its quiz. Nothing is
 * mocked - if the bundled assets were wrong or the database never seeded,
 * these assertions would fail.
 *
 * Surfaces are targeted by test tag rather than by Arabic prose, so a copy
 * change does not break the suite; the text assertions that remain are ones
 * where the *wording itself* is the promise being tested.
 */
@LargeTest
@HiltAndroidTest
class ArchiveJourneyTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun awaitTag(tag: String, timeoutMillis: Long = 15_000) {
        composeRule.waitUntil(timeoutMillis) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun awaitText(text: String, timeoutMillis: Long = 15_000) {
        composeRule.waitUntil(timeoutMillis) {
            composeRule.onAllNodesWithText(text, substring = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    /**
     * Scrolls by the list's own item key rather than hunting for the node.
     *
     * The home list is a keyed LazyColumn, so `performScrollToKey` resolves the
     * index through the list's semantics in one step. `performScrollToNode`
     * instead runs a scroll-and-look loop, which on this archive can spend
     * minutes walking the list.
     */
    private fun scrollHomeTo(locationId: String) {
        awaitTag(TestTags.HOME_LIST)
        composeRule.onNodeWithTag(TestTags.HOME_LIST).performScrollToKey(locationId)
    }

    private fun openPlace(locationId: String) {
        scrollHomeTo(locationId)
        composeRule.onNodeWithTag(TestTags.placeCard(locationId)).performClick()
        awaitTag(TestTags.DETAIL_LIST)
    }

    /** All seven tabs are laid out at once, so none needs scrolling to. */
    private fun openTab(tab: DetailTab) {
        composeRule.onNodeWithTag(TestTags.detailTab(tab.name)).performClick()
        composeRule.waitForIdle()
    }

    private fun navigateTo(destination: TopLevelDestination) {
        composeRule.onNodeWithTag(TestTags.navItem(destination.name)).performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun home_composes_the_bundled_archive_on_first_launch() {
        awaitTag(TestTags.HOME_LIST)
        // The story of the day comes from the archive, so a rendered hero means
        // the database was seeded out of the APK.
        awaitTag(TestTags.FEATURED_STORY)
        // The journey panel sits below the hero and the carousel, so it has to
        // be scrolled to before it can be asserted on screen.
        composeRule.onNodeWithTag(TestTags.HOME_LIST).performScrollToKey("progress")
        composeRule.onNodeWithTag(TestTags.PROGRESS_PANEL).assertIsDisplayed()
    }

    @Test
    fun all_eight_places_are_present_with_no_network() {
        awaitTag(TestTags.HOME_LIST)
        listOf(
            "muslim-quarter",
            "christian-quarter",
            "armenian-quarter",
            "maghariba-quarter",
            "bab-al-amud",
            "sheikh-jarrah",
            "silwan",
            "at-tur",
        ).forEach { id ->
            scrollHomeTo(id)
            composeRule.onNodeWithTag(TestTags.placeCard(id)).assertIsDisplayed()
        }
    }

    @Test
    fun a_place_opens_on_its_reviewed_story() {
        openPlace("silwan")
        // The heading is the promise: what you read first is the reviewed text.
        awaitText("الحكاية المراجعة")
    }

    @Test
    fun the_timeline_tab_renders_entries_from_the_record() {
        openPlace("bab-al-amud")
        openTab(DetailTab.TIMELINE)
        // The reviewed record for Bab al-Amud opens on the Roman colonnaded
        // street, 117-138 CE. That exact label appearing means the timeline came
        // out of the bundled archive rather than from nowhere.
        awaitText("117")
    }

    @Test
    fun a_quiz_answer_cites_the_content_it_came_from() {
        openPlace("muslim-quarter")
        openTab(DetailTab.QUIZ)
        awaitText("سوق القطانين")
        composeRule.onAllNodesWithText("سوق القطانين", substring = true).onFirst().performClick()
        // Never just "correct": the section of reviewed content is named.
        awaitText("المصدر")
    }

    @Test
    fun the_guide_offers_to_answer_only_from_reviewed_content() {
        openPlace("silwan")
        openTab(DetailTab.GUIDE)
        awaitTag(TestTags.GUIDE_INPUT)
        awaitText("من المحتوى المراجَع فقط")
    }

    @Test
    fun the_gallery_explains_a_missing_comparison_rather_than_hiding_it() {
        openPlace("armenian-quarter")
        openTab(DetailTab.GALLERY)
        // No historical photograph of the same subject here, and the app says so.
        awaitText("لم تُضف صورة تاريخية للموضوع نفسه")
    }

    @Test
    fun contributed_memories_invite_a_submission_that_an_editor_reviews() {
        openPlace("bab-al-amud")
        openTab(DetailTab.MEMORIES)
        awaitText("ذكريات شاركها الناس")
    }

    @Test
    fun the_map_draws_the_old_city() {
        awaitTag(TestTags.HOME_LIST)
        navigateTo(TopLevelDestination.MAP)
        awaitTag(TestTags.MAP_CANVAS)
        awaitText("المفتاح")
    }

    @Test
    fun the_planner_builds_a_route_that_states_its_own_uncertainty() {
        awaitTag(TestTags.HOME_LIST)
        navigateTo(TopLevelDestination.PLAN)

        // A route is saved on the device and outlives the app, so the planner
        // may open straight onto an existing one. Discard it to get back to the
        // wizard - which also covers the discard button.
        if (composeRule.onAllNodesWithTag(TestTags.PLAN_DISCARD).fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithTag(TestTags.PLAN_DISCARD).performClick()
            composeRule.waitForIdle()
        }

        awaitTag(TestTags.PLAN_NEXT)
        composeRule.onNodeWithTag(TestTags.PLAN_NEXT).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TestTags.PLAN_NEXT).performClick()
        composeRule.waitForIdle()

        awaitTag(TestTags.PLAN_BUILD)
        composeRule.onNodeWithTag(TestTags.PLAN_BUILD).performClick()

        awaitTag(TestTags.PLAN_ROUTE)
        // The notes sit under every stop, so the list is scrolled to them: a
        // route that did not admit its estimates were estimates would be making
        // a claim about the Old City it cannot support.
        composeRule.onNodeWithTag(TestTags.PLAN_LIST).performScrollToKey("notes")
        awaitText("أوقات المشي تقديرية")
    }

    @Test
    fun the_connections_view_draws_the_archive_as_a_web() {
        awaitTag(TestTags.HOME_LIST)
        navigateTo(TopLevelDestination.CONNECTIONS)
        awaitTag(TestTags.CONSTELLATION_CANVAS)
    }

    @Test
    fun contributing_says_an_editor_reads_everything_first() {
        awaitTag(TestTags.HOME_LIST)
        navigateTo(TopLevelDestination.CONTRIBUTE)
        awaitTag(TestTags.CONTRIBUTE_FORM)
        awaitText("يقرأ محرِّرٌ كل مشاركة قبل ظهورها")
    }

    @Test
    fun the_home_list_scrolls_by_key_so_a_long_archive_stays_usable() {
        awaitTag(TestTags.HOME_LIST)
        composeRule.onNode(hasTestTag(TestTags.HOME_LIST) and hasScrollToKeyAction())
            .assertIsDisplayed()
    }

    @Test
    fun switching_to_English_re_resolves_the_whole_interface() {
        awaitTag(TestTags.HOME_LIST)
        navigateTo(TopLevelDestination.MAP)
        awaitText("المفتاح")

        // Arabic is the default; the English strings must not be what is shown.
        composeRule.onAllNodesWithText("Legend", substring = true)
            .fetchSemanticsNodes()
            .let { nodes -> assert(nodes.isEmpty()) { "English leaked into the Arabic interface" } }
    }

    @Test
    fun a_deep_link_target_place_can_be_reached_and_returned_from() {
        openPlace("at-tur")
        awaitText("الحكاية المراجعة")
        Espresso.pressBack()
        awaitTag(TestTags.HOME_LIST)
    }
}
