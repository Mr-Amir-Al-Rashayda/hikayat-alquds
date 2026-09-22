package ps.hikayatalquds.ui.home

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ps.hikayatalquds.data.repository.ContentRepository
import ps.hikayatalquds.data.repository.JourneyRepository
import ps.hikayatalquds.domain.model.Badge
import ps.hikayatalquds.domain.model.JourneyProgress
import ps.hikayatalquds.domain.model.Location
import ps.hikayatalquds.domain.model.LocationCategory
import ps.hikayatalquds.domain.model.LocationSummary
import ps.hikayatalquds.domain.model.Story
import javax.inject.Inject

@Immutable
data class HomeUiState(
    val loading: Boolean = true,
    val places: List<LocationSummary> = emptyList(),
    val featured: Pair<Story, Location>? = null,
    val progress: JourneyProgress = JourneyProgress(),
    val badges: List<Badge> = emptyList(),
    val categoryFilter: LocationCategory? = null,
) {
    val filtered: List<LocationSummary>
        get() = categoryFilter?.let { filter ->
            places.filter { filter in it.location.categories }
        } ?: places

    val bookmarked: List<LocationSummary>
        get() = places.filter { it.bookmarked }

    /** The last place opened, so "continue" points somewhere real. */
    val continueWith: LocationSummary?
        get() = progress.lastVisitedLocationId
            ?.let { id -> places.firstOrNull { it.location.id == id } }
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val content: ContentRepository,
    private val journey: JourneyRepository,
) : ViewModel() {

    private val filter = MutableStateFlow<LocationCategory?>(null)

    /** A day index rather than a timestamp: the story of the day must not change mid-read. */
    private val dayIndex = System.currentTimeMillis() / 86_400_000L

    private val _rerollSeed = MutableStateFlow(0)
    val rerollSeed: StateFlow<Int> = _rerollSeed.asStateFlow()

    val uiState: StateFlow<HomeUiState> = combine(
        content.locationSummaries,
        journey.progress,
        journey.badges,
        filter,
        combine(content.featuredStory(dayIndex), _rerollSeed, ::Pair),
    ) { places, progress, badges, categoryFilter, (featured, seed) ->
        HomeUiState(
            loading = false,
            places = places.map { summary ->
                summary.copy(
                    visited = summary.location.id in progress.visitedLocationIds,
                    bookmarked = summary.location.id in progress.bookmarkedLocationIds,
                )
            },
            // Seed 0 is today's story for everybody; tapping "another story"
            // moves off it without changing what today's story is.
            featured = if (seed == 0) featured ?: rotate(places, 0) else rotate(places, seed),
            progress = progress,
            badges = badges,
            categoryFilter = categoryFilter,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    fun setCategoryFilter(category: LocationCategory?) {
        filter.value = if (filter.value == category) null else category
    }

    /** "Another story" picks a different place, without changing today's default. */
    fun showAnotherStory() {
        _rerollSeed.value += 1
    }

    fun toggleBookmark(locationId: String) = viewModelScope.launch {
        journey.toggleBookmark(locationId)
    }

    /**
     * Falls back to a place when no story row exists - which should not happen
     * with the bundled archive, but an empty hero is worse than a synthesised
     * one built from the place's own reviewed description.
     */
    private fun rotate(places: List<LocationSummary>, seed: Int): Pair<Story, Location>? {
        if (places.isEmpty()) return null
        val place = places[(dayIndex.toInt() + seed).mod(places.size)].location
        return Story(
            id = "local-${place.id}",
            locationId = place.id,
            title = place.storyTitle,
            summary = place.description,
            body = place.story,
            audience = "general",
            tone = "storytelling",
            source = place.contentFile,
            isAiGenerated = false,
            uncertaintyNotes = emptyList(),
            status = "published",
        ) to place
    }
}
