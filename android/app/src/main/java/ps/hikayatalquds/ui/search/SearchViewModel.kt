package ps.hikayatalquds.ui.search

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import ps.hikayatalquds.data.preferences.UserPreferencesRepository
import ps.hikayatalquds.data.repository.ContentRepository
import ps.hikayatalquds.data.repository.SearchHit
import javax.inject.Inject

@Immutable
data class SearchUiState(
    val query: String = "",
    val hits: List<SearchHit> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    content: ContentRepository,
    preferences: UserPreferencesRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")

    val uiState: StateFlow<SearchUiState> = combine(
        query,
        preferences.settings,
    ) { text, settings -> text to settings.language }
        // A keystroke should not run five queries; the archive is small but the
        // list rebuild is not free.
        .debounce(140)
        .flatMapLatest { (text, language) ->
            content.search(text, language).let { results ->
                combine(query, results) { current, hits -> SearchUiState(current, hits) }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SearchUiState(),
        )

    fun setQuery(value: String) {
        query.value = value
    }
}
