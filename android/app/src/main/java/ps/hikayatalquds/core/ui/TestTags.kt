package ps.hikayatalquds.core.ui

/**
 * Stable handles for the instrumented tests.
 *
 * Kept in one place and applied with `Modifier.testTag`, so a UI test targets a
 * known surface instead of matching Arabic prose that a copy change would
 * quietly break. The few text assertions left in the suite are ones where the
 * wording itself is the thing being tested.
 */
object TestTags {
    const val HOME_LIST = "home:list"
    const val FEATURED_STORY = "home:featured"
    const val PROGRESS_PANEL = "home:progress"

    /** Suffixed with the location id, so a card is `place:silwan`. */
    const val PLACE_CARD_PREFIX = "place:"

    /** Suffixed with the destination name, so the map tab is `nav:MAP`. */
    const val NAV_ITEM_PREFIX = "nav:"

    /** Suffixed with the tab name, so the timeline chip is `tab:TIMELINE`. */
    const val DETAIL_TAB_PREFIX = "tab:"

    const val DETAIL_LIST = "detail:list"
    const val DETAIL_TABS = "detail:tabs"
    const val GUIDE_INPUT = "detail:guide-input"

    const val MAP_CANVAS = "map:canvas"
    const val PLAN_LIST = "plan:list"
    const val PLAN_NEXT = "plan:next"
    const val PLAN_BUILD = "plan:build"
    const val PLAN_ROUTE = "plan:route"
    const val PLAN_DISCARD = "plan:discard"
    const val CONSTELLATION_CANVAS = "connections:canvas"
    const val CONTRIBUTE_FORM = "contribute:form"

    fun placeCard(locationId: String) = "$PLACE_CARD_PREFIX$locationId"

    fun navItem(destinationName: String) = "$NAV_ITEM_PREFIX$destinationName"

    fun detailTab(tabName: String) = "$DETAIL_TAB_PREFIX$tabName"
}
