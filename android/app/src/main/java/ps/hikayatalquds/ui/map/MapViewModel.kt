package ps.hikayatalquds.ui.map

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location as AndroidLocation
import android.location.LocationManager
import androidx.compose.runtime.Immutable
import androidx.core.content.getSystemService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ps.hikayatalquds.data.repository.ContentRepository
import ps.hikayatalquds.data.repository.ItineraryRepository
import ps.hikayatalquds.data.repository.JourneyRepository
import ps.hikayatalquds.domain.model.Itinerary
import ps.hikayatalquds.domain.model.Location
import ps.hikayatalquds.domain.model.LocationCategory
import ps.hikayatalquds.domain.model.NearestLocation
import ps.hikayatalquds.domain.tour.ItineraryPlanner
import javax.inject.Inject

/** How the "are you nearby?" check is going. */
sealed interface NearbyState {
    data object Idle : NearbyState
    data object Locating : NearbyState
    data class Found(val nearest: NearestLocation) : NearbyState
    data class Denied(val permanent: Boolean) : NearbyState
    data object Unavailable : NearbyState
}

@Immutable
data class MapUiState(
    val locations: List<Location> = emptyList(),
    val visited: Set<String> = emptySet(),
    val selectedId: String? = null,
    val categoryFilter: LocationCategory? = null,
    val showLabels: Boolean = true,
    val itinerary: Itinerary? = null,
    val nearby: NearbyState = NearbyState.Idle,
) {
    val visibleLocations: List<Location>
        get() = categoryFilter?.let { filter ->
            locations.filter { filter in it.categories }
        } ?: locations

    val routeIds: List<String>
        get() = itinerary?.stops?.sortedBy { it.order }?.map { it.locationId }.orEmpty()

    val selected: Location?
        get() = selectedId?.let { id -> locations.firstOrNull { it.id == id } }

    fun pins(): List<MapPin> {
        val order = routeIds.withIndex().associate { (index, id) -> id to index + 1 }
        return visibleLocations.map { location ->
            MapPin(
                location = location,
                visited = location.id in visited,
                selected = location.id == selectedId,
                routeOrder = order[location.id],
            )
        }
    }
}

@HiltViewModel
class MapViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val content: ContentRepository,
    private val journey: JourneyRepository,
    private val itineraries: ItineraryRepository,
    private val planner: ItineraryPlanner,
) : ViewModel() {

    private val selectedId = MutableStateFlow<String?>(null)
    private val filter = MutableStateFlow<LocationCategory?>(null)
    private val labels = MutableStateFlow(true)
    private val nearby = MutableStateFlow<NearbyState>(NearbyState.Idle)

    val uiState: StateFlow<MapUiState> = combine(
        content.locations,
        journey.progress,
        itineraries.itinerary,
        combine(selectedId, filter, labels, ::Triple),
        nearby,
    ) { locations, progress, itinerary, (selected, categoryFilter, showLabels), nearbyState ->
        MapUiState(
            locations = locations,
            visited = progress.visitedLocationIds,
            selectedId = selected,
            categoryFilter = categoryFilter,
            showLabels = showLabels,
            itinerary = itinerary,
            nearby = nearbyState,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MapUiState(),
    )

    fun select(locationId: String?) {
        selectedId.value = if (selectedId.value == locationId) null else locationId
    }

    fun setFilter(category: LocationCategory?) {
        filter.value = if (filter.value == category) null else category
    }

    fun toggleLabels() {
        labels.value = !labels.value
    }

    fun clearNearby() {
        nearby.value = NearbyState.Idle
    }

    fun onLocationPermissionDenied(permanent: Boolean) {
        nearby.value = NearbyState.Denied(permanent)
    }

    /**
     * Works out which place the reader is closest to.
     *
     * Reads the last known fix rather than requesting a live one: this is a
     * "which quarter am I in" question, not navigation, and a cached coarse fix
     * answers it without keeping the radio awake. The comparison happens here,
     * on the device - the coordinate is never sent anywhere.
     */
    @SuppressLint("MissingPermission")
    fun findNearest() {
        nearby.value = NearbyState.Locating
        viewModelScope.launch {
            val manager = context.getSystemService<LocationManager>()
            if (manager == null) {
                nearby.value = NearbyState.Unavailable
                return@launch
            }

            val fix = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
                .asSequence()
                .mapNotNull { provider ->
                    runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
                }
                .maxByOrNull(AndroidLocation::getTime)

            if (fix == null) {
                nearby.value = NearbyState.Unavailable
                return@launch
            }

            val nearest = planner.nearest(fix.latitude, fix.longitude, content.locations.first())
            nearby.value = nearest?.let { NearbyState.Found(it) } ?: NearbyState.Unavailable
        }
    }

    fun markVisited(locationId: String) = viewModelScope.launch {
        journey.markVisited(locationId)
    }
}
