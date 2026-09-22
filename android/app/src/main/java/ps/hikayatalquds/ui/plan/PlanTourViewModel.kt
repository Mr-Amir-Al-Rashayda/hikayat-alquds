package ps.hikayatalquds.ui.plan

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ps.hikayatalquds.data.repository.ItineraryRepository
import ps.hikayatalquds.domain.model.Itinerary
import ps.hikayatalquds.domain.model.TourDuration
import ps.hikayatalquds.domain.model.TourInterest
import ps.hikayatalquds.domain.model.TourLanguage
import ps.hikayatalquds.domain.model.TourPreferences
import javax.inject.Inject

@Immutable
data class PlanUiState(
    val step: Int = 0,
    val preferences: TourPreferences = TourPreferences(),
    val itinerary: Itinerary? = null,
    val building: Boolean = false,
) {
    val canContinue: Boolean get() = step < LAST_STEP || preferences.interests.isNotEmpty()
    val canBuild: Boolean get() = preferences.interests.isNotEmpty() && !building

    companion object {
        const val LAST_STEP = 2
    }
}

@HiltViewModel
class PlanTourViewModel @Inject constructor(
    private val itineraries: ItineraryRepository,
) : ViewModel() {

    private val step = MutableStateFlow(0)
    private val overrides = MutableStateFlow<TourPreferences?>(null)
    private val building = MutableStateFlow(false)

    val uiState: StateFlow<PlanUiState> = combine(
        step,
        combine(itineraries.lastPreferences, overrides) { stored, override -> override ?: stored },
        itineraries.itinerary,
        building,
    ) { currentStep, preferences, itinerary, isBuilding ->
        PlanUiState(
            step = currentStep,
            preferences = preferences,
            itinerary = itinerary,
            building = isBuilding,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PlanUiState(),
    )

    fun back() {
        step.value = (step.value - 1).coerceAtLeast(0)
    }

    fun next() {
        step.value = (step.value + 1).coerceAtMost(PlanUiState.LAST_STEP)
    }

    fun setLanguage(language: TourLanguage) = update { it.copy(language = language) }

    fun setDuration(duration: TourDuration) = update { it.copy(duration = duration) }

    fun toggleInterest(interest: TourInterest) = update { current ->
        current.copy(
            interests = if (interest in current.interests) {
                current.interests - interest
            } else {
                current.interests + interest
            },
        )
    }

    fun build() {
        if (building.value) return
        viewModelScope.launch {
            building.value = true
            val preferences = overrides.value ?: itineraries.lastPreferences.first()
            itineraries.build(preferences)
            building.value = false
        }
    }

    fun startOver() {
        step.value = 0
        viewModelScope.launch { itineraries.clear() }
    }

    private fun update(transform: (TourPreferences) -> TourPreferences) {
        viewModelScope.launch {
            val current = overrides.value ?: itineraries.lastPreferences.first()
            overrides.value = transform(current)
        }
    }
}
