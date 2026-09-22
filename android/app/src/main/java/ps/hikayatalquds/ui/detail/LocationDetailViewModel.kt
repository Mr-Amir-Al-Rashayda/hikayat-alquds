package ps.hikayatalquds.ui.detail

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
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
import ps.hikayatalquds.data.preferences.UserPreferencesRepository
import ps.hikayatalquds.data.repository.ContentRepository
import ps.hikayatalquds.data.repository.JourneyRepository
import ps.hikayatalquds.data.repository.NarrativeRepository
import ps.hikayatalquds.domain.model.AppLanguage
import ps.hikayatalquds.domain.model.AudienceMode
import ps.hikayatalquds.domain.model.ChatTurn
import ps.hikayatalquds.domain.model.GeneratedStory
import ps.hikayatalquds.domain.model.GuideAnswer
import ps.hikayatalquds.domain.model.JourneyProgress
import ps.hikayatalquds.domain.model.LocationDetail
import ps.hikayatalquds.domain.model.StoryTone
import ps.hikayatalquds.narration.NarrationController
import androidx.navigation.toRoute
import ps.hikayatalquds.ui.navigation.LocationRoute
import javax.inject.Inject

/** The seven things a place has to say, as tabs. */
enum class DetailTab { STORY, GUIDE, TIMELINE, GALLERY, ARTISANS, QUIZ, MEMORIES }

/** A question and its answer, plus whether the record actually covered it. */
@Immutable
data class GuideExchange(
    val question: String,
    val answer: GuideAnswer?,
    val pending: Boolean = false,
)

@Immutable
data class DetailUiState(
    val loading: Boolean = true,
    val detail: LocationDetail? = null,
    val progress: JourneyProgress = JourneyProgress(),
    val tab: DetailTab = DetailTab.STORY,
    val audience: AudienceMode = AudienceMode.GENERAL,
    val tone: StoryTone = StoryTone.STORYTELLING,
    val generating: Boolean = false,
    val generated: GeneratedStory? = null,
    val generationError: String? = null,
    val conversation: List<GuideExchange> = emptyList(),
    val guidePending: Boolean = false,
    val revealedMemoryIds: Set<String> = emptySet(),
) {
    val bookmarked: Boolean
        get() = detail?.location?.id?.let { it in progress.bookmarkedLocationIds } == true

    val quizCompleted: Boolean
        get() = detail?.location?.id?.let { it in progress.completedQuizLocationIds } == true
}

@HiltViewModel
class LocationDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val content: ContentRepository,
    private val journey: JourneyRepository,
    private val narrative: NarrativeRepository,
    private val preferences: UserPreferencesRepository,
    private val narration: NarrationController,
) : ViewModel() {

    private val locationId: String = savedStateHandle.toRoute<LocationRoute>().locationId

    private val tab = MutableStateFlow(DetailTab.STORY)
    private val audience = MutableStateFlow(AudienceMode.GENERAL)
    private val tone = MutableStateFlow(StoryTone.STORYTELLING)
    private val generating = MutableStateFlow(false)
    private val generated = MutableStateFlow<GeneratedStory?>(null)
    private val generationError = MutableStateFlow<String?>(null)
    private val conversation = MutableStateFlow<List<GuideExchange>>(emptyList())
    private val revealed = MutableStateFlow<Set<String>>(emptySet())

    val narrationState = narration.state

    val uiState: StateFlow<DetailUiState> = combine(
        content.detail(locationId),
        journey.progress,
        combine(tab, audience, tone, ::Triple),
        combine(generating, generated, generationError, ::Triple),
        combine(conversation, revealed, ::Pair),
    ) { detail, progress, (currentTab, currentAudience, currentTone),
        (isGenerating, story, error), (chat, revealedIds) ->
        DetailUiState(
            loading = false,
            detail = detail,
            progress = progress,
            tab = currentTab,
            audience = currentAudience,
            tone = currentTone,
            generating = isGenerating,
            generated = story,
            generationError = error,
            conversation = chat,
            guidePending = chat.any { it.pending },
            revealedMemoryIds = revealedIds,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DetailUiState(),
    )

    init {
        // Opening the page is what "visited" means; there is nothing else to opt into.
        viewModelScope.launch { journey.markVisited(locationId) }
    }

    fun selectTab(next: DetailTab) {
        tab.value = next
    }

    fun setAudience(next: AudienceMode) {
        audience.value = next
        generated.value = null
    }

    fun setTone(next: StoryTone) {
        tone.value = next
        generated.value = null
    }

    fun toggleBookmark() = viewModelScope.launch { journey.toggleBookmark(locationId) }

    fun generateStory(language: AppLanguage, forceRefresh: Boolean = false) {
        if (generating.value) return
        viewModelScope.launch {
            generating.value = true
            generationError.value = null
            narrative.generateStory(
                locationId = locationId,
                audience = audience.value,
                tone = tone.value,
                language = language,
                forceRefresh = forceRefresh,
            ).fold(
                onSuccess = { generated.value = it },
                onFailure = { generationError.value = it.message },
            )
            generating.value = false
        }
    }

    fun ask(question: String, language: AppLanguage) {
        val trimmed = question.trim()
        if (trimmed.isEmpty() || conversation.value.any { it.pending }) return

        conversation.value += GuideExchange(question = trimmed, answer = null, pending = true)
        viewModelScope.launch {
            val history = conversation.value
                .mapNotNull { exchange ->
                    exchange.answer?.let { ChatTurn(exchange.question, it.answer) }
                }
                .takeLast(HISTORY_TURNS)

            val answer = narrative.askGuide(locationId, trimmed, history, language)
            conversation.value = conversation.value.map { exchange ->
                if (exchange.pending && exchange.question == trimmed) {
                    exchange.copy(answer = answer, pending = false)
                } else {
                    exchange
                }
            }
        }
    }

    fun clearConversation() {
        conversation.value = emptyList()
    }

    /**
     * Reveals a contributed memory.
     *
     * Hidden until tapped on purpose: a memory somebody gave to this archive is
     * worth arriving at deliberately, not scrolled past. The reveal is also what
     * counts towards the keepsake.
     */
    fun revealMemory(memoryId: String) {
        if (memoryId in revealed.value) return
        revealed.value += memoryId
        viewModelScope.launch { journey.markMemoryUncovered(memoryId) }
    }

    fun onQuizCompleted() = viewModelScope.launch { journey.markQuizCompleted(locationId) }

    // --- narration ----------------------------------------------------------

    /** Reads the text currently on the story tab, generated or reviewed. */
    fun narrate(language: AppLanguage) {
        viewModelScope.launch {
            val detail = uiState.value.detail ?: content.detail(locationId).first() ?: return@launch
            val settings = preferences.current()
            val story = generated.value
            val title = story?.title ?: detail.location.storyTitle[language]
            val body = story?.narrative
                ?: detail.stories.firstOrNull()?.body?.get(language)
                ?: detail.location.story[language]

            narration.applyPreferences(
                rate = settings.narrationRate,
                pitch = settings.narrationPitch,
                voiceId = settings.narrationVoice,
            )
            narration.speak(
                locationId = locationId,
                title = title,
                text = listOfNotNull(title, body).joinToString(". "),
                language = language,
            )
        }
    }

    fun toggleNarration() = narration.toggle()

    fun stopNarration() = narration.stop()

    fun skipToSegment(index: Int) = narration.skipToSegment(index)

    fun setNarrationRate(rate: Float) {
        narration.setRate(rate)
        viewModelScope.launch { preferences.setNarrationRate(rate) }
    }

    fun setNarrationVoice(voiceId: String) {
        narration.setVoice(voiceId)
        viewModelScope.launch { preferences.setNarrationVoice(voiceId) }
    }

    private companion object {
        /** Enough for the guide to follow a thread without resending an essay. */
        const val HISTORY_TURNS = 4
    }
}
