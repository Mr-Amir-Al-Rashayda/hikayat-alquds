package ps.hikayatalquds.ui.map

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ps.hikayatalquds.R
import ps.hikayatalquds.domain.model.AppLanguage
import ps.hikayatalquds.domain.model.Location
import ps.hikayatalquds.domain.model.LocationCategory
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The illustrated map of the Old City and the neighbourhoods around it.
 *
 * Drawn rather than tiled: the point is a map of *this* archive - the walls,
 * the four named gates, the eight places and the walking route between them -
 * and drawing it needs no tile server, no API key, and works with the radio
 * off.
 *
 * Two things it does properly, because a map that gets them wrong is a picture
 * rather than a map. It is **to scale**: coordinates become metres and are
 * scaled by one factor on both axes, so the wall circuit has the shape it has
 * on the ground and the scale bar means something. And every dimension is in
 * **dp, not raw pixels**, so a pin is the same size on a dense phone as on a
 * tablet instead of shrinking to a speck.
 */

/**
 * The Ottoman wall circuit.
 *
 * Anchored on the four gates the archive itself names, so a gate is drawn *on*
 * the wall rather than floating inside it - the previous outline put Bab
 * al-Amud some 180 m inside the city. The corners between the gates are an
 * approximation, chosen so the circuit comes out at 3.9 km around and 0.93 km
 * square, against a real 4.0 km and 0.87 km square.
 */
private val OLD_CITY_WALL = listOf(
    31.78030 to 35.22690, // north-west corner, west of Bab al-Jadid
    31.78165 to 35.23085, // Bab al-Amud
    31.78255 to 35.23290, // Bab al-Sahira
    31.78250 to 35.23690, // north-east corner
    31.78088 to 35.23688, // Bab al-Asbat
    31.77880 to 35.23745, // the sealed eastern gate
    31.77420 to 35.23650, // south-east corner
    31.77355 to 35.23405, // Bab al-Maghariba
    31.77165 to 35.22990, // south wall, by Bab al-Nabi Dawud
    31.77140 to 35.22720, // south-west corner
    31.77675 to 35.22753, // Bab al-Khalil
)

data class MapGate(
    val latitude: Double,
    val longitude: Double,
    val arabicName: String,
    val englishName: String,
) {
    fun name(language: AppLanguage) = if (language.isArabic) arabicName else englishName
}

val JERUSALEM_GATES = listOf(
    MapGate(31.78165, 35.23085, "باب العامود", "Damascus Gate"),
    MapGate(31.78088, 35.23688, "باب الأسباط", "Lions' Gate"),
    MapGate(31.77675, 35.22753, "باب الخليل", "Jaffa Gate"),
    MapGate(31.77355, 35.23405, "باب المغاربة", "Maghariba Gate"),
)

private const val MIN_ZOOM = 1f
private const val MAX_ZOOM = 4f
private const val ZOOM_STEP = 0.6f
private val TAP_TARGET = 44.dp

/** What the map needs to know about one pin. */
data class MapPin(
    val location: Location,
    val visited: Boolean,
    val selected: Boolean,
    val routeOrder: Int?,
)

/**
 * Turns coordinates into canvas pixels through metres, so both axes share one
 * scale and the drawing is true to the ground.
 */
private class MapProjection(
    private val referenceLatitude: Double,
    private val referenceLongitude: Double,
    private val pixelsPerMetre: Float,
    private val centre: Offset,
    private val zoom: Float,
    private val pan: Offset,
) {
    fun toCanvas(latitude: Double, longitude: Double): Offset {
        val eastMetres = (longitude - referenceLongitude) * METRES_PER_DEGREE_LNG *
            cos(Math.toRadians(referenceLatitude))
        val northMetres = (latitude - referenceLatitude) * METRES_PER_DEGREE_LAT
        return Offset(
            centre.x + (eastMetres * pixelsPerMetre * zoom).toFloat() + pan.x,
            centre.y - (northMetres * pixelsPerMetre * zoom).toFloat() + pan.y,
        )
    }

    /** How many metres one pixel covers right now, for the scale bar. */
    val metresPerPixel: Float get() = 1f / (pixelsPerMetre * zoom)

    companion object {
        const val METRES_PER_DEGREE_LAT = 110_574.0
        const val METRES_PER_DEGREE_LNG = 111_320.0
    }
}

@Composable
fun JerusalemMap(
    pins: List<MapPin>,
    /**
     * Everything the frame is fitted to, filter or no filter.
     *
     * Separate from [pins] on purpose: filtering by category should change
     * which places are pinned, not re-frame the city underneath them.
     */
    framingLocations: List<Location>,
    routeIds: List<String>,
    language: AppLanguage,
    showLabels: Boolean,
    onSelect: (Location) -> Unit,
    modifier: Modifier = Modifier,
    categoryColors: Map<LocationCategory, Color>,
    wallColor: Color,
    groundColor: Color,
    labelColor: Color,
    routeColor: Color,
    gateColor: Color,
) {
    var zoom by remember { mutableFloatStateOf(MIN_ZOOM) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val rightToLeft = LocalLayoutDirection.current == LayoutDirection.Rtl

    BoxWithConstraints(modifier = modifier.background(groundColor)) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val animatedZoom by animateFloatAsState(zoom, label = "map-zoom")

        // The frame is fitted to what is actually on the map, so the Old City
        // fills it instead of floating inside an arbitrary bounding box.
        val fit = remember(framingLocations, widthPx, heightPx) {
            fitToContent(framingLocations, widthPx, heightPx)
        }
        val projection = MapProjection(
            referenceLatitude = fit.centreLatitude,
            referenceLongitude = fit.centreLongitude,
            pixelsPerMetre = fit.pixelsPerMetre,
            centre = Offset(widthPx / 2f, heightPx / 2f),
            zoom = animatedZoom,
            pan = pan,
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(widthPx, heightPx) {
                    // Written out rather than using detectTransformGestures so
                    // the map can decline a gesture. The map lives inside a
                    // scrolling page, and at the opening zoom there is nothing
                    // to pan - swallowing one-finger drags there would turn the
                    // biggest thing on screen into a dead zone. So: a pinch is
                    // always ours, a one-finger drag only once zoomed in, and
                    // anything else passes through to the page.
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        do {
                            val event = awaitPointerEvent()
                            val pressed = event.changes.count { it.pressed }
                            val gestureZoom = event.calculateZoom()
                            val drag = event.calculatePan()
                            val ours = pressed > 1 || zoom > MIN_ZOOM
                            if (ours && (gestureZoom != 1f || drag != Offset.Zero)) {
                                val next = (zoom * gestureZoom).coerceIn(MIN_ZOOM, MAX_ZOOM)
                                // Keep whatever is under the fingers still.
                                val focus = event.calculateCentroid(useCurrent = false) -
                                    Offset(widthPx / 2f, heightPx / 2f)
                                pan = clampPan(
                                    pan + drag + focus * (1f - next / zoom),
                                    next,
                                    widthPx,
                                    heightPx,
                                )
                                zoom = next
                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
                .pointerInput(pins, animatedZoom, pan) {
                    detectTapGestures { tap ->
                        val threshold = with(density) { TAP_TARGET.toPx() / 2 }
                        val nearest = pins.minByOrNull { pin ->
                            (tap - projection.toCanvas(
                                pin.location.latitude,
                                pin.location.longitude,
                            )).getDistance()
                        } ?: return@detectTapGestures
                        val distance = (
                            tap - projection.toCanvas(
                                nearest.location.latitude,
                                nearest.location.longitude,
                            )
                            ).getDistance()
                        if (distance <= threshold) onSelect(nearest.location)
                    }
                },
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val style = MapStyle(density, wallColor, labelColor, gateColor, routeColor)
                drawWalls(projection, style)
                drawRoute(pins, routeIds, projection, style)
                drawGates(projection, style, language, textMeasurer, showLabels, animatedZoom)
                drawPins(pins, projection, style, categoryColors, textMeasurer)
                if (showLabels) {
                    drawPlaceLabels(pins, projection, style, language, textMeasurer)
                }
                drawScaleBar(projection, style, language, textMeasurer, rightToLeft)
                drawNorthArrow(style, language, textMeasurer, rightToLeft)
            }

            // A drawing is invisible to a screen reader, so each pin also exists
            // as a real focusable target over the canvas: announced by name and
            // selectable without having to find a dot by touch.
            val halfTarget = with(density) { TAP_TARGET.toPx() / 2 }
            pins.forEach { pin ->
                val point = projection.toCanvas(pin.location.latitude, pin.location.longitude)
                val description = stringResource(R.string.cd_map_pin, pin.location.name[language])
                val selectedLabel = stringResource(R.string.cd_selected)
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (point.x - halfTarget).roundToInt(),
                                (point.y - halfTarget).roundToInt(),
                            )
                        }
                        .size(TAP_TARGET)
                        .semantics(mergeDescendants = true) {
                            contentDescription =
                                if (pin.selected) "$description, $selectedLabel" else description
                            role = Role.Button
                            if (pin.selected) selected = true
                        }
                        .clickable { onSelect(pin.location) },
                )
            }
        }

        MapControls(
            zoom = zoom,
            onZoomIn = {
                zoom = (zoom + ZOOM_STEP).coerceAtMost(MAX_ZOOM)
                pan = clampPan(pan, zoom, widthPx, heightPx)
            },
            onZoomOut = {
                zoom = (zoom - ZOOM_STEP).coerceAtLeast(MIN_ZOOM)
                pan = clampPan(pan, zoom, widthPx, heightPx)
            },
            onReset = {
                zoom = MIN_ZOOM
                pan = Offset.Zero
            },
        )
    }
}

/** Every dimension the map draws, resolved from dp once per frame. */
private class MapStyle(
    density: Density,
    val wall: Color,
    val label: Color,
    val gate: Color,
    val route: Color,
) {
    val wallStroke = with(density) { 2.5.dp.toPx() }
    val crenellation = with(density) { 3.5.dp.toPx() }
    val hairline = with(density) { 1.5.dp.toPx() }
    val crenellationSpacing = with(density) { 9.dp.toPx() }
    val pinRadius = with(density) { 7.dp.toPx() }
    val pinRadiusSelected = with(density) { 9.dp.toPx() }
    val pinRing = with(density) { 2.5.dp.toPx() }
    val visitedDot = with(density) { 2.5.dp.toPx() }
    val gateSize = with(density) { 8.dp.toPx() }
    val routeStroke = with(density) { 2.5.dp.toPx() }
    val routeDash = with(density) { 6.dp.toPx() }
    val routeGap = with(density) { 4.dp.toPx() }
    val labelGap = with(density) { 5.dp.toPx() }
    val labelPadding = with(density) { 3.dp.toPx() }
    val edgeInset = with(density) { 12.dp.toPx() }
    val scaleBarHeight = with(density) { 4.dp.toPx() }
    val northArrow = with(density) { 9.dp.toPx() }
}

/** The frame the map opens on: everything it draws, with room around the edges. */
private class MapFit(
    val centreLatitude: Double,
    val centreLongitude: Double,
    val pixelsPerMetre: Float,
)

/**
 * Fits the wall, the gates and every place into the canvas at one scale.
 *
 * One scale for both axes is the whole point: scaling each axis to fill the box
 * would stretch the wall circuit into a shape Jerusalem does not have.
 */
private fun fitToContent(locations: List<Location>, width: Float, height: Float): MapFit {
    val latitudes = OLD_CITY_WALL.map { it.first } +
        JERUSALEM_GATES.map { it.latitude } +
        locations.map { it.latitude }
    val longitudes = OLD_CITY_WALL.map { it.second } +
        JERUSALEM_GATES.map { it.longitude } +
        locations.map { it.longitude }

    if (latitudes.isEmpty() || width <= 0f || height <= 0f) {
        return MapFit(31.7783, 35.2323, 0.2f)
    }

    val centreLatitude = (latitudes.min() + latitudes.max()) / 2
    val centreLongitude = (longitudes.min() + longitudes.max()) / 2

    val spanMetresY = (latitudes.max() - latitudes.min()) * MapProjection.METRES_PER_DEGREE_LAT
    val spanMetresX = (longitudes.max() - longitudes.min()) *
        MapProjection.METRES_PER_DEGREE_LNG * cos(Math.toRadians(centreLatitude))

    // Labels sit beside their pins, so the geography gets 78% of the frame.
    val usable = 0.78f
    val scaleX = if (spanMetresX > 0) (width * usable / spanMetresX).toFloat() else 1f
    val scaleY = if (spanMetresY > 0) (height * usable / spanMetresY).toFloat() else 1f
    return MapFit(centreLatitude, centreLongitude, min(scaleX, scaleY))
}

/** Stops the drawing being dragged off the viewport entirely. */
private fun clampPan(pan: Offset, zoom: Float, width: Float, height: Float): Offset {
    val slackX = max(0f, (width * zoom - width) / 2f)
    val slackY = max(0f, (height * zoom - height) / 2f)
    return Offset(pan.x.coerceIn(-slackX, slackX), pan.y.coerceIn(-slackY, slackY))
}

private fun DrawScope.drawWalls(projection: MapProjection, style: MapStyle) {
    val points = OLD_CITY_WALL.map { (lat, lng) -> projection.toCanvas(lat, lng) }
    val path = Path()
    points.forEachIndexed { index, point ->
        if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
    }
    path.close()

    drawPath(path = path, color = style.wall.copy(alpha = 0.10f))
    drawPath(path = path, color = style.wall, style = Stroke(width = style.wallStroke))

    // Crenellations: short ticks along each segment, which is what makes the
    // outline read as a wall rather than as a polygon.
    points.forEachIndexed { index, from ->
        val to = points[(index + 1) % points.size]
        val segment = to - from
        val length = segment.getDistance()
        if (length <= 0f) return@forEachIndexed
        val normal = Offset(-segment.y / length, segment.x / length)
        val ticks = (length / style.crenellationSpacing).toInt().coerceAtLeast(1)
        for (tick in 0 until ticks) {
            val base = from + segment * ((tick + 0.5f) / ticks)
            drawLine(
                color = style.wall.copy(alpha = 0.65f),
                start = base,
                end = base + normal * style.crenellation,
                strokeWidth = style.hairline,
                cap = StrokeCap.Round,
            )
        }
    }
}

private fun DrawScope.drawRoute(
    pins: List<MapPin>,
    routeIds: List<String>,
    projection: MapProjection,
    style: MapStyle,
) {
    if (routeIds.size < 2) return
    val ordered = routeIds.mapNotNull { id -> pins.firstOrNull { it.location.id == id } }
    if (ordered.size < 2) return

    val path = Path()
    ordered.forEachIndexed { index, pin ->
        val point = projection.toCanvas(pin.location.latitude, pin.location.longitude)
        if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
    }
    drawPath(
        path = path,
        color = style.route,
        style = Stroke(
            width = style.routeStroke,
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(style.routeDash, style.routeGap)),
        ),
    )
}

private fun DrawScope.drawGates(
    projection: MapProjection,
    style: MapStyle,
    language: AppLanguage,
    textMeasurer: TextMeasurer,
    showLabels: Boolean,
    zoom: Float,
) {
    JERUSALEM_GATES.forEach { gate ->
        val point = projection.toCanvas(gate.latitude, gate.longitude)
        val half = style.gateSize / 2

        // A gate reads as an arch in the wall: a rounded top on a square base.
        val arch = Path().apply {
            moveTo(point.x - half, point.y + half)
            lineTo(point.x - half, point.y)
            quadraticTo(point.x, point.y - half * 1.7f, point.x + half, point.y)
            lineTo(point.x + half, point.y + half)
            close()
        }
        drawPath(arch, color = Color.White)
        drawPath(arch, color = style.gate, style = Stroke(width = style.hairline))

        // Gate names only once there is room for them not to sit on a pin.
        if (showLabels && zoom >= 1.8f) {
            val layout = textMeasurer.measure(
                text = gate.name(language),
                style = TextStyle(fontSize = 9.sp, color = style.gate),
            )
            drawText(
                textLayoutResult = layout,
                topLeft = point + Offset(-layout.size.width / 2f, half + style.labelGap),
            )
        }
    }
}

private fun DrawScope.drawPins(
    pins: List<MapPin>,
    projection: MapProjection,
    style: MapStyle,
    categoryColors: Map<LocationCategory, Color>,
    textMeasurer: TextMeasurer,
) {
    pins.forEach { pin ->
        val point = projection.toCanvas(pin.location.latitude, pin.location.longitude)
        val accent = categoryColors[pin.location.primaryCategory] ?: style.label
        val radius = if (pin.selected) style.pinRadiusSelected else style.pinRadius

        if (pin.selected) {
            drawCircle(color = accent.copy(alpha = 0.20f), radius = radius * 2.2f, center = point)
        }
        drawCircle(color = Color.White, radius = radius + style.pinRing, center = point)
        drawCircle(color = accent, radius = radius, center = point)

        when {
            pin.routeOrder != null -> {
                val layout = textMeasurer.measure(
                    text = pin.routeOrder.toString(),
                    style = TextStyle(
                        fontSize = 10.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                drawText(
                    textLayoutResult = layout,
                    topLeft = point + Offset(
                        -layout.size.width / 2f,
                        -layout.size.height / 2f,
                    ),
                )
            }

            // A hollow centre marks a place already opened.
            pin.visited -> drawCircle(
                color = Color.White,
                radius = style.visitedDot,
                center = point,
            )
        }
    }
}

/**
 * Draws the place names, dropping any that cannot be placed without landing on
 * another one.
 *
 * Four quarters sit inside a walled square about 400 m across, so at the
 * opening zoom their labels genuinely cannot all fit. Dropping the ones that
 * would collide is honest - the pin is still there, still tappable, and still
 * announced to a screen reader - whereas overlapping text is unreadable for
 * every name involved. Zooming in makes room and they come back.
 */
private fun DrawScope.drawPlaceLabels(
    pins: List<MapPin>,
    projection: MapProjection,
    style: MapStyle,
    language: AppLanguage,
    textMeasurer: TextMeasurer,
) {
    val placed = mutableListOf<Rect>()

    // Selected first: if a name has to be dropped, never the one just tapped.
    pins.sortedByDescending { it.selected }.forEach { pin ->
        val point = projection.toCanvas(pin.location.latitude, pin.location.longitude)
        val layout: TextLayoutResult = textMeasurer.measure(
            text = pin.location.name[language],
            style = TextStyle(
                fontSize = if (pin.selected) 12.sp else 10.sp,
                color = style.label,
                fontWeight = if (pin.selected) FontWeight.Bold else FontWeight.Medium,
            ),
        )
        val width = layout.size.width.toFloat()
        val height = layout.size.height.toFloat()
        val radius = if (pin.selected) style.pinRadiusSelected else style.pinRadius
        val gap = radius + style.pinRing + style.labelGap

        val candidates = listOf(
            Offset(point.x - width / 2, point.y + gap),           // below
            Offset(point.x - width / 2, point.y - gap - height),  // above
            Offset(point.x + gap, point.y - height / 2),          // right
            Offset(point.x - gap - width, point.y - height / 2),  // left
        )

        val spot = candidates.firstOrNull { candidate ->
            val rect = Rect(candidate, Size(width, height)).inflate(style.labelPadding)
            rect.left >= 0f && rect.top >= 0f &&
                rect.right <= size.width && rect.bottom <= size.height &&
                placed.none { it.overlaps(rect) }
        } ?: return@forEach

        val rect = Rect(spot, Size(width, height)).inflate(style.labelPadding)
        placed += rect

        // A soft plate behind the text keeps it readable over the wall fill.
        drawRoundRect(
            color = Color.White.copy(alpha = 0.78f),
            topLeft = rect.topLeft,
            size = rect.size,
            cornerRadius = CornerRadius(style.labelPadding),
        )
        drawText(textLayoutResult = layout, topLeft = spot)
    }
}

/**
 * A scale bar - which is what makes this a map rather than a diagram, and only
 * means anything because both axes share one scale.
 */
private fun DrawScope.drawScaleBar(
    projection: MapProjection,
    style: MapStyle,
    language: AppLanguage,
    textMeasurer: TextMeasurer,
    rightToLeft: Boolean,
) {
    val metresPerPixel = projection.metresPerPixel
    if (!metresPerPixel.isFinite() || metresPerPixel <= 0f) return

    // A round distance landing near a quarter of the frame, so the bar never
    // reads "137 m".
    val target = size.width * 0.25f * metresPerPixel
    val metres = ROUND_DISTANCES.minByOrNull { abs(it - target) } ?: return
    val barWidth = metres / metresPerPixel
    if (barWidth > size.width * 0.6f) return

    val left = if (rightToLeft) size.width - style.edgeInset - barWidth else style.edgeInset
    val bottom = size.height - style.edgeInset
    val ink = style.label.copy(alpha = 0.75f)

    drawLine(
        color = ink,
        start = Offset(left, bottom),
        end = Offset(left + barWidth, bottom),
        strokeWidth = style.scaleBarHeight / 2,
        cap = StrokeCap.Round,
    )
    listOf(left, left + barWidth).forEach { x ->
        drawLine(
            color = ink,
            start = Offset(x, bottom - style.scaleBarHeight),
            end = Offset(x, bottom + style.scaleBarHeight),
            strokeWidth = style.scaleBarHeight / 2,
            cap = StrokeCap.Round,
        )
    }

    val text = if (metres >= 1000) {
        val km = metres / 1000
        if (language.isArabic) "$km كم" else "$km km"
    } else {
        if (language.isArabic) "$metres م" else "$metres m"
    }
    val layout = textMeasurer.measure(text = text, style = TextStyle(fontSize = 9.sp, color = ink))
    drawText(
        textLayoutResult = layout,
        topLeft = Offset(
            left + barWidth / 2 - layout.size.width / 2,
            bottom - style.scaleBarHeight - layout.size.height - style.labelPadding,
        ),
    )
}

private fun DrawScope.drawNorthArrow(
    style: MapStyle,
    language: AppLanguage,
    textMeasurer: TextMeasurer,
    rightToLeft: Boolean,
) {
    val inset = style.edgeInset + style.northArrow
    val x = if (rightToLeft) inset else size.width - inset
    val y = inset
    val ink = style.label.copy(alpha = 0.55f)

    drawPath(
        Path().apply {
            moveTo(x, y - style.northArrow)
            lineTo(x + style.northArrow * 0.62f, y + style.northArrow)
            lineTo(x, y + style.northArrow * 0.45f)
            lineTo(x - style.northArrow * 0.62f, y + style.northArrow)
            close()
        },
        color = ink,
    )

    val layout = textMeasurer.measure(
        text = if (language.isArabic) "ش" else "N",
        style = TextStyle(fontSize = 9.sp, color = ink),
    )
    drawText(
        textLayoutResult = layout,
        topLeft = Offset(x - layout.size.width / 2f, y + style.northArrow + style.labelPadding),
    )
}

/** Distances a reader recognises on a scale bar. */
private val ROUND_DISTANCES = listOf(100, 200, 250, 500, 750, 1000, 2000)
