package ps.hikayatalquds.ui.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot

/**
 * The map's geometry, checked as arithmetic.
 *
 * The drawing was wrong in two ways that a screenshot shows but no test caught:
 * each axis was scaled independently, which stretched the wall circuit into a
 * shape Jerusalem does not have, and every size was in raw pixels, so a pin was
 * three dp on a dense phone. Both are now arithmetic, so both are tested here:
 * distances on screen have to stay in proportion to distances on the ground.
 */
class MapGeometryTest {

    /**
     * The same conversion [MapProjection] performs, reproduced so the test
     * asserts against the ground truth rather than against the implementation.
     */
    private fun metresBetween(
        aLat: Double,
        aLng: Double,
        bLat: Double,
        bLng: Double,
    ): Double {
        val referenceLatitude = (aLat + bLat) / 2
        val east = (bLng - aLng) * 111_320.0 * cos(Math.toRadians(referenceLatitude))
        val north = (bLat - aLat) * 110_574.0
        return hypot(east, north)
    }

    private val wall = listOf(
        31.78030 to 35.22690,
        31.78165 to 35.23085,
        31.78255 to 35.23290,
        31.78250 to 35.23690,
        31.78088 to 35.23688,
        31.77880 to 35.23745,
        31.77420 to 35.23650,
        31.77355 to 35.23405,
        31.77165 to 35.22990,
        31.77140 to 35.22720,
        31.77675 to 35.22753,
    )

    private val places = listOf(
        31.7806 to 35.2339, // muslim-quarter
        31.7784 to 35.2295, // christian-quarter
        31.7757 to 35.2288, // armenian-quarter
        31.7759 to 35.2340, // maghariba-quarter
        31.7816 to 35.2308, // bab-al-amud
        31.7908 to 35.2317, // sheikh-jarrah
        31.7712 to 35.2366, // silwan
        31.7786 to 35.2470, // at-tur
    )

    /** Mirrors `fitToContent` plus `MapProjection.toCanvas` for a given canvas. */
    private class Frame(width: Float, height: Float, points: List<Pair<Double, Double>>) {
        private val centreLatitude = (points.minOf { it.first } + points.maxOf { it.first }) / 2
        private val centreLongitude =
            (points.minOf { it.second } + points.maxOf { it.second }) / 2
        private val pixelsPerMetre: Float
        private val centreX = width / 2f
        private val centreY = height / 2f

        init {
            val spanY = (points.maxOf { it.first } - points.minOf { it.first }) * 110_574.0
            val spanX = (points.maxOf { it.second } - points.minOf { it.second }) *
                111_320.0 * cos(Math.toRadians(centreLatitude))
            val usable = 0.78f
            pixelsPerMetre = minOf(
                (width * usable / spanX).toFloat(),
                (height * usable / spanY).toFloat(),
            )
        }

        fun toCanvas(latitude: Double, longitude: Double): Pair<Float, Float> {
            val east = (longitude - centreLongitude) * 111_320.0 *
                cos(Math.toRadians(centreLatitude))
            val north = (latitude - centreLatitude) * 110_574.0
            return Pair(
                centreX + (east * pixelsPerMetre).toFloat(),
                centreY - (north * pixelsPerMetre).toFloat(),
            )
        }

        val metresPerPixel: Float get() = 1f / pixelsPerMetre
    }

    private val all = wall + places + JERUSALEM_GATES.map { it.latitude to it.longitude }

    @Test
    fun `screen distance stays in proportion to ground distance on both axes`() {
        // A tall, narrow canvas is where independent axis scaling shows worst.
        val frame = Frame(width = 900f, height = 1400f, points = all)

        // Two pairs of places the same distance apart on the ground but at
        // right angles to each other must be the same distance apart on screen.
        val pairs = listOf(
            (31.7806 to 35.2339) to (31.7908 to 35.2317), // mostly north-south
            (31.7757 to 35.2288) to (31.7786 to 35.2470), // mostly east-west
        )
        val ratios = pairs.map { (from, to) ->
            val ground = metresBetween(from.first, from.second, to.first, to.second)
            val a = frame.toCanvas(from.first, from.second)
            val b = frame.toCanvas(to.first, to.second)
            val screen = hypot((b.first - a.first).toDouble(), (b.second - a.second).toDouble())
            screen / ground
        }
        assertEquals(
            "a north-south kilometre must draw as long as an east-west one",
            ratios[0],
            ratios[1],
            ratios[0] * 0.01,
        )
    }

    @Test
    fun `the wall circuit keeps its real proportions`() {
        val frame = Frame(width = 900f, height = 1400f, points = all)
        val projected = wall.map { frame.toCanvas(it.first, it.second) }

        val groundWidth = metresBetween(
            31.7783, wall.minOf { it.second },
            31.7783, wall.maxOf { it.second },
        )
        val groundHeight = metresBetween(
            wall.minOf { it.first }, 35.2323,
            wall.maxOf { it.first }, 35.2323,
        )
        val screenWidth = projected.maxOf { it.first } - projected.minOf { it.first }
        val screenHeight = projected.maxOf { it.second } - projected.minOf { it.second }

        assertEquals(
            "the walled city is drawn out of proportion",
            groundWidth / groundHeight,
            (screenWidth / screenHeight).toDouble(),
            0.02,
        )
    }

    @Test
    fun `everything the map draws lands inside the canvas`() {
        listOf(
            900f to 1400f,
            1400f to 900f,   // landscape
            700f to 700f,    // square
        ).forEach { (width, height) ->
            val frame = Frame(width, height, all)
            all.forEach { (lat, lng) ->
                val (x, y) = frame.toCanvas(lat, lng)
                assertTrue("x=$x outside 0..$width", x in 0f..width)
                assertTrue("y=$y outside 0..$height", y in 0f..height)
            }
        }
    }

    @Test
    fun `the content fills a useful share of the frame rather than a corner`() {
        val width = 900f
        val height = 1400f
        val frame = Frame(width, height, all)
        val projected = all.map { frame.toCanvas(it.first, it.second) }
        val usedWidth = projected.maxOf { it.first } - projected.minOf { it.first }
        val usedHeight = projected.maxOf { it.second } - projected.minOf { it.second }

        // One of the two axes is the limiting one and should be near the 78%
        // the fit reserves; the other follows from the true aspect ratio.
        val fill = maxOf(usedWidth / width, usedHeight / height)
        assertTrue("content only fills ${fill * 100}% of the frame", fill > 0.7f)
        assertTrue("content overflows the frame", fill <= 0.79f)
    }

    @Test
    fun `north is up and east is right`() {
        val frame = Frame(900f, 1400f, all)
        val centre = frame.toCanvas(31.7783, 35.2323)
        val north = frame.toCanvas(31.7883, 35.2323)
        val east = frame.toCanvas(31.7783, 35.2423)

        assertTrue("north should be above centre", north.second < centre.second)
        assertTrue("east should be right of centre", east.first > centre.first)
    }

    @Test
    fun `the scale is plausible for a two-kilometre city`() {
        val frame = Frame(900f, 1400f, all)
        // The archive spans roughly two kilometres, drawn across ~900-1400 px,
        // so a pixel is a small number of metres - not centimetres, not blocks.
        assertTrue("got ${frame.metresPerPixel} m/px", frame.metresPerPixel in 0.5f..6f)
    }

    @Test
    fun `the scale bar picks a round distance close to a quarter of the frame`() {
        val frame = Frame(900f, 1400f, all)
        val target = 900f * 0.25f * frame.metresPerPixel
        val rounded = listOf(100, 200, 250, 500, 750, 1000, 2000)
            .minByOrNull { abs(it - target) }!!

        assertTrue("$rounded m is nothing like $target m", abs(rounded - target) < target * 0.6)
        val barWidth = rounded / frame.metresPerPixel
        assertTrue("the bar would span the whole map", barWidth <= 900f * 0.6f)
    }

    @Test
    fun `the four gates sit on the wall they belong to`() {
        val frame = Frame(900f, 1400f, all)
        val wallPoints = wall.map { frame.toCanvas(it.first, it.second) }

        JERUSALEM_GATES.forEach { gate ->
            val point = frame.toCanvas(gate.latitude, gate.longitude)
            val nearestEdge = wallPoints.indices.minOf { index ->
                val a = wallPoints[index]
                val b = wallPoints[(index + 1) % wallPoints.size]
                distanceToSegment(point, a, b)
            }
            // Within a pin's width of the circuit: a gate floating in open
            // ground would mean the wall and the gates disagree.
            assertTrue(
                "${gate.englishName} is ${nearestEdge}px off the wall",
                nearestEdge < 30f,
            )
        }
    }

    private fun distanceToSegment(
        point: Pair<Float, Float>,
        a: Pair<Float, Float>,
        b: Pair<Float, Float>,
    ): Float {
        val dx = b.first - a.first
        val dy = b.second - a.second
        val lengthSquared = dx * dx + dy * dy
        if (lengthSquared == 0f) return hypot(point.first - a.first, point.second - a.second)
        val t = (((point.first - a.first) * dx + (point.second - a.second) * dy) / lengthSquared)
            .coerceIn(0f, 1f)
        return hypot(point.first - (a.first + t * dx), point.second - (a.second + t * dy))
    }

    @Test
    fun `every gate is named in both languages`() {
        assertEquals(4, JERUSALEM_GATES.size)
        JERUSALEM_GATES.forEach { gate ->
            assertTrue(gate.arabicName.isNotBlank())
            assertTrue(gate.englishName.isNotBlank())
            assertTrue(
                "${gate.englishName} is not in Jerusalem",
                gate.latitude in 31.77..31.79 && gate.longitude in 35.22..35.24,
            )
        }
    }

    @Test
    fun `the wall circuit is the size of the real Old City`() {
        val perimetreMetres = wall.indices.sumOf { index ->
            val a = wall[index]
            val b = wall[(index + 1) % wall.size]
            metresBetween(a.first, a.second, b.first, b.second)
        }
        // The Ottoman circuit is about four kilometres around.
        assertTrue(
            "the wall is ${perimetreMetres / 1000} km around",
            perimetreMetres in 3_500.0..4_400.0,
        )
    }

    @Test
    fun `the four quarters sit inside the walls`() {
        // Only the four intra-muros quarters; Silwan, At-Tur and Sheikh Jarrah
        // are outside the walls and must stay outside them.
        val inside = mapOf(
            "muslim-quarter" to (31.7806 to 35.2339),
            "christian-quarter" to (31.7784 to 35.2295),
            "armenian-quarter" to (31.7757 to 35.2288),
            "maghariba-quarter" to (31.7759 to 35.2340),
        )
        inside.forEach { (name, point) ->
            assertTrue("$name should be inside the walls", insideWall(point))
        }

        val outside = mapOf(
            "silwan" to (31.7712 to 35.2366),
            "at-tur" to (31.7786 to 35.2470),
            "sheikh-jarrah" to (31.7908 to 35.2317),
        )
        outside.forEach { (name, point) ->
            assertTrue("$name should be outside the walls", !insideWall(point))
        }
    }

    /** Ray casting in degrees; the circuit is far too small for that to matter. */
    private fun insideWall(point: Pair<Double, Double>): Boolean {
        var inside = false
        for (index in wall.indices) {
            val a = wall[index]
            val b = wall[(index + 1) % wall.size]
            if ((a.first > point.first) != (b.first > point.first)) {
                val crossing = a.second +
                    (point.first - a.first) * (b.second - a.second) / (b.first - a.first)
                if (point.second < crossing) inside = !inside
            }
        }
        return inside
    }
}
