package ps.hikayatalquds.domain.tour

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ps.hikayatalquds.domain.model.AppLanguage
import ps.hikayatalquds.domain.model.Location
import ps.hikayatalquds.domain.model.LocalizedList
import ps.hikayatalquds.domain.model.LocalizedText
import ps.hikayatalquds.domain.model.TourDuration
import ps.hikayatalquds.domain.model.TourInterest
import ps.hikayatalquds.domain.model.TourLanguage
import ps.hikayatalquds.domain.model.TourPreferences
import kotlin.math.abs

/**
 * The planner is the one piece of real algorithm in the app, and its output is
 * a walk somebody will actually take through the Old City, so it is tested for
 * the properties that matter on foot: the right number of stops, no
 * backtracking, plausible distances, and the same answer the web planner gives.
 */
class ItineraryPlannerTest {

    private val planner = ItineraryPlanner()

    private fun place(
        id: String,
        latitude: Double,
        longitude: Double,
        interests: List<TourInterest> = listOf(TourInterest.HISTORY),
    ) = Location(
        id = id,
        name = LocalizedText(id, id),
        city = "Jerusalem",
        country = "Palestine",
        latitude = latitude,
        longitude = longitude,
        description = LocalizedText.Empty,
        culturalImportance = LocalizedText.Empty,
        historicalSummary = LocalizedText.Empty,
        landmarks = LocalizedList(listOf("First", "Second"), listOf("الأول", "الثاني")),
        storyTitle = LocalizedText.Empty,
        story = LocalizedText.Empty,
        walkthrough = LocalizedText.Empty,
        coverImage = null,
        coverImageCredit = null,
        coverImageLicense = null,
        coverImageSourceUrl = null,
        contentFile = null,
        categoryLabels = listOf("حارة تاريخية"),
        interests = interests,
        citations = emptyList(),
    )

    /** The eight real places, so the test exercises the real geography. */
    private val jerusalem = listOf(
        place("muslim-quarter", 31.7806, 35.2339, listOf(TourInterest.HISTORY, TourInterest.ARCHITECTURE)),
        place("christian-quarter", 31.7784, 35.2295, listOf(TourInterest.RELIGIOUS)),
        place("armenian-quarter", 31.7757, 35.2288, listOf(TourInterest.ARCHITECTURE)),
        place("maghariba-quarter", 31.7759, 35.2340, listOf(TourInterest.HISTORY)),
        place("bab-al-amud", 31.7816, 35.2308, listOf(TourInterest.FOOD_MARKETS)),
        place("sheikh-jarrah", 31.7908, 35.2317, listOf(TourInterest.ORAL_HERITAGE)),
        place("silwan", 31.7712, 35.2366, listOf(TourInterest.ORAL_HERITAGE)),
        place("at-tur", 31.7786, 35.2470, listOf(TourInterest.RELIGIOUS)),
    )

    @Test
    fun `takes exactly as many stops as the duration allows`() {
        TourDuration.entries.forEach { duration ->
            val itinerary = planner.build(
                TourPreferences(duration = duration, interests = setOf(TourInterest.HISTORY)),
                jerusalem,
            )
            assertEquals(
                "wrong stop count for $duration",
                minOf(duration.stopCount, jerusalem.size),
                itinerary.stops.size,
            )
        }
    }

    @Test
    fun `numbers the stops from one with no gaps`() {
        val itinerary = planner.build(
            TourPreferences(duration = TourDuration.FULL_DAY, interests = setOf(TourInterest.HISTORY)),
            jerusalem,
        )
        assertEquals(
            (1..itinerary.stops.size).toList(),
            itinerary.stops.map { it.order },
        )
    }

    @Test
    fun `visits each place at most once`() {
        val itinerary = planner.build(
            TourPreferences(duration = TourDuration.WEEKEND, interests = TourInterest.entries.toSet()),
            jerusalem,
        )
        assertEquals(
            itinerary.stops.size,
            itinerary.stops.map { it.locationId }.distinct().size,
        )
    }

    @Test
    fun `the first stop has no walk and later stops do`() {
        val itinerary = planner.build(
            TourPreferences(duration = TourDuration.HALF_DAY, interests = setOf(TourInterest.HISTORY)),
            jerusalem,
        )
        assertEquals(0, itinerary.stops.first().walkFromPreviousMinutes)
        assertEquals(0, itinerary.stops.first().distanceFromPreviousMeters)
        assertNull(itinerary.stops.first().bearingFromPreviousDegrees)
        itinerary.stops.drop(1).forEach { stop ->
            assertTrue("stop ${stop.order} has no walking time", stop.walkFromPreviousMinutes >= 3)
            assertNotNull(stop.bearingFromPreviousDegrees)
        }
    }

    @Test
    fun `nearest-neighbour ordering beats the ranking order for walking distance`() {
        val preferences = TourPreferences(
            duration = TourDuration.FULL_DAY,
            interests = setOf(TourInterest.HISTORY),
        )
        val itinerary = planner.build(preferences, jerusalem)

        // Walking the same stops in score order instead of geographic order.
        val chosen = itinerary.stops.map { stop -> jerusalem.first { it.id == stop.locationId } }
        val byScore = chosen.sortedWith(
            compareByDescending<Location> { planner.score(it, preferences.interests) }
                .thenBy { it.id },
        )
        val scoreOrderWalk = byScore.zipWithNext { a, b ->
            planner.greatCircleKm(a.latitude, a.longitude, b.latitude, b.longitude)
        }.sum()
        val plannedWalk = chosen.zipWithNext { a, b ->
            planner.greatCircleKm(a.latitude, a.longitude, b.latitude, b.longitude)
        }.sum()

        assertTrue(
            "planned walk ${plannedWalk}km should not exceed score order ${scoreOrderWalk}km",
            plannedWalk <= scoreOrderWalk + 0.001,
        )
    }

    @Test
    fun `a weekend route is split across two days and shorter routes are not`() {
        val weekend = planner.build(
            TourPreferences(duration = TourDuration.WEEKEND, interests = setOf(TourInterest.HISTORY)),
            jerusalem,
        )
        assertEquals(2, weekend.days)

        val halfDay = planner.build(
            TourPreferences(duration = TourDuration.HALF_DAY, interests = setOf(TourInterest.HISTORY)),
            jerusalem,
        )
        assertEquals(1, halfDay.days)
    }

    @Test
    fun `interest priority decides the opening stop`() {
        val history = planner.build(
            TourPreferences(interests = setOf(TourInterest.HISTORY)),
            jerusalem,
        )
        // The shared priority list opens "history" on Maghariba.
        assertEquals("maghariba-quarter", history.stops.first().locationId)

        val religious = planner.build(
            TourPreferences(interests = setOf(TourInterest.RELIGIOUS)),
            jerusalem,
        )
        assertEquals("christian-quarter", religious.stops.first().locationId)
    }

    @Test
    fun `total minutes account for both standing still and walking`() {
        val itinerary = planner.build(
            TourPreferences(duration = TourDuration.FULL_DAY, interests = setOf(TourInterest.HISTORY)),
            jerusalem,
        )
        val expected = itinerary.stops.sumOf { it.suggestedMinutes + it.walkFromPreviousMinutes }
        assertEquals(expected, itinerary.totalMinutes)
        assertEquals(
            itinerary.stops.sumOf { it.walkFromPreviousMinutes },
            itinerary.totalWalkingMinutes,
        )
    }

    @Test
    fun `walking legs inside the Old City are minutes, not hours`() {
        val itinerary = planner.build(
            TourPreferences(duration = TourDuration.FULL_DAY, interests = setOf(TourInterest.HISTORY)),
            jerusalem,
        )
        itinerary.stops.drop(1).forEach { stop ->
            assertTrue(
                "leg to ${stop.locationId} is ${stop.walkFromPreviousMinutes} min",
                stop.walkFromPreviousMinutes in 3..45,
            )
        }
    }

    @Test
    fun `every route states what it does not claim, in the tour language`() {
        TourLanguage.entries.forEach { language ->
            val itinerary = planner.build(
                TourPreferences(language = language, interests = setOf(TourInterest.HISTORY)),
                jerusalem,
            )
            assertEquals(2, itinerary.uncertaintyNotes.size)
            assertTrue(itinerary.uncertaintyNotes.all { it.isNotBlank() })
        }
    }

    @Test
    fun `guidance is carried in all three tour languages`() {
        val itinerary = planner.build(
            TourPreferences(language = TourLanguage.FRENCH, interests = setOf(TourInterest.HISTORY)),
            jerusalem,
        )
        val stop = itinerary.stops.first()
        assertEquals("First · Second", stop.guidanceFor(TourLanguage.ENGLISH))
        assertEquals("الأول · الثاني", stop.guidanceFor(TourLanguage.ARABIC))
        assertTrue(stop.guidanceFor(TourLanguage.FRENCH).isNotBlank())
    }

    @Test
    fun `an empty archive produces an empty route rather than crashing`() {
        val itinerary = planner.build(TourPreferences(), emptyList())
        assertTrue(itinerary.stops.isEmpty())
        assertEquals(0, itinerary.totalWalkingMinutes)
    }

    @Test
    fun `nearest picks the closest place and reports a usable distance`() {
        // Standing at Damascus Gate.
        val nearest = planner.nearest(31.7816, 35.2308, jerusalem)
        assertNotNull(nearest)
        assertEquals("bab-al-amud", nearest!!.location.id)
        assertTrue("should be within metres", nearest.kilometres < 0.1)
        assertTrue(nearest.isNearby)
    }

    @Test
    fun `nearest reports far away honestly rather than pretending`() {
        // Amman, roughly.
        val nearest = planner.nearest(31.9539, 35.9106, jerusalem)
        assertNotNull(nearest)
        assertTrue("should be tens of km", nearest!!.kilometres > 50)
        assertTrue(!nearest.isNearby)
    }

    @Test
    fun `great-circle distance matches a known Jerusalem baseline`() {
        // Damascus Gate to Lions' Gate is about 550 m.
        val km = planner.greatCircleKm(31.78165, 35.23085, 31.78088, 35.23688)
        assertTrue("got ${km}km", abs(km - 0.58) < 0.08)
    }

    @Test
    fun `bearing labels read as compass directions in both languages`() {
        assertEquals("north", bearingLabel(0.0, AppLanguage.ENGLISH))
        assertEquals("east", bearingLabel(90.0, AppLanguage.ENGLISH))
        assertEquals("west", bearingLabel(270.0, AppLanguage.ENGLISH))
        assertEquals("شمالاً", bearingLabel(358.0, AppLanguage.ARABIC))
        assertNull(bearingLabel(null, AppLanguage.ENGLISH))
    }

    @Test
    fun `leg labels switch units at a kilometre`() {
        assertEquals("6 min · 420 m", legLabel(6, 420, AppLanguage.ENGLISH))
        assertTrue(legLabel(20, 1500, AppLanguage.ENGLISH).contains("1.5 km"))
        assertTrue(legLabel(6, 420, AppLanguage.ARABIC).contains("م"))
    }
}
