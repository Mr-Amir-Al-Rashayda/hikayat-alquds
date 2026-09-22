package ps.hikayatalquds.ui.settings

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ps.hikayatalquds.data.preferences.ThemeMode
import ps.hikayatalquds.data.preferences.UserPreferencesRepository
import ps.hikayatalquds.data.preferences.UserSettings
import ps.hikayatalquds.data.remote.BaseUrlInterceptor
import ps.hikayatalquds.data.remote.RemoteContentSource
import ps.hikayatalquds.data.repository.ContentRepository
import ps.hikayatalquds.data.repository.JourneyRepository
import ps.hikayatalquds.data.repository.NarrativeRepository
import ps.hikayatalquds.data.sync.ContentSyncWorker
import ps.hikayatalquds.data.sync.DailyStoryWorker
import ps.hikayatalquds.domain.model.AppLanguage
import ps.hikayatalquds.narration.NarrationController
import javax.inject.Inject

/** The result of pressing "test connection". */
sealed interface ConnectionCheck {
    data object Idle : ConnectionCheck
    data object Busy : ConnectionCheck
    data class Reachable(val dataSource: String) : ConnectionCheck
    data class Failed(val reason: String) : ConnectionCheck
    data object InvalidAddress : ConnectionCheck
}

@Immutable
data class SettingsUiState(
    val settings: UserSettings = UserSettings(),
    val apiDraft: String = "",
    val connection: ConnectionCheck = ConnectionCheck.Idle,
    val reseeding: Boolean = false,
    val voices: List<ps.hikayatalquds.narration.NarrationVoice> = emptyList(),
    val selectedVoice: String = "",
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: UserPreferencesRepository,
    private val content: ContentRepository,
    private val journey: JourneyRepository,
    private val narrative: NarrativeRepository,
    private val remote: RemoteContentSource,
    private val baseUrl: BaseUrlInterceptor,
    private val narration: NarrationController,
) : ViewModel() {

    private val apiDraft = MutableStateFlow<String?>(null)
    private val connection = MutableStateFlow<ConnectionCheck>(ConnectionCheck.Idle)
    private val reseeding = MutableStateFlow(false)

    val uiState: StateFlow<SettingsUiState> = combine(
        preferences.settings,
        apiDraft,
        connection,
        reseeding,
        narration.state,
    ) { settings, draft, connectionState, isReseeding, narrationState ->
        SettingsUiState(
            settings = settings,
            apiDraft = draft ?: settings.apiBaseUrl,
            connection = connectionState,
            reseeding = isReseeding,
            voices = narrationState.voices,
            selectedVoice = narrationState.selectedVoice,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(),
    )

    fun setLanguage(language: AppLanguage) = viewModelScope.launch {
        preferences.setLanguage(language)
        // The narration voice has to follow the language, or Arabic text gets
        // read by an English voice, which is unintelligible rather than merely wrong.
        narration.prepare(language)
    }

    fun setTheme(mode: ThemeMode) = viewModelScope.launch { preferences.setThemeMode(mode) }

    fun setAllowRemoteImages(allow: Boolean) = viewModelScope.launch {
        preferences.setAllowRemoteImages(allow)
    }

    fun setApiDraft(value: String) {
        apiDraft.value = value
        connection.value = ConnectionCheck.Idle
    }

    fun saveApiAddress() = viewModelScope.launch {
        val draft = apiDraft.value ?: return@launch
        if (!baseUrl.update(draft)) {
            connection.value = ConnectionCheck.InvalidAddress
            return@launch
        }
        preferences.setApiBaseUrl(draft)
        apiDraft.value = null
        if (draft.isNotBlank()) checkConnection()
    }

    fun checkConnection() = viewModelScope.launch {
        connection.value = ConnectionCheck.Busy
        remote.health().fold(
            onSuccess = { health ->
                connection.value = ConnectionCheck.Reachable(
                    health.dataSource ?: health.status,
                )
                content.sync()
            },
            onFailure = { error ->
                connection.value = ConnectionCheck.Failed(
                    error.message ?: error::class.simpleName.orEmpty(),
                )
            },
        )
    }

    fun syncNow() {
        ContentSyncWorker.syncNow(WorkManager.getInstance(context))
    }

    fun setDailyStoryReminder(enabled: Boolean) = viewModelScope.launch {
        preferences.setDailyStoryReminder(enabled)
        val workManager = WorkManager.getInstance(context)
        if (enabled) {
            DailyStoryWorker.createChannel(context, preferences.current().language.isArabic)
            DailyStoryWorker.schedule(workManager)
        } else {
            DailyStoryWorker.cancel(workManager)
        }
    }

    fun setNarrationRate(rate: Float) = viewModelScope.launch {
        narration.setRate(rate)
        preferences.setNarrationRate(rate)
    }

    fun setNarrationVoice(voiceId: String) = viewModelScope.launch {
        narration.setVoice(voiceId)
        preferences.setNarrationVoice(voiceId)
    }

    fun resetJourney() = viewModelScope.launch { journey.reset() }

    fun clearGeneratedStories() = viewModelScope.launch { narrative.clearGeneratedStories() }

    /** Puts the bundled archive back, for a database that has drifted or broken. */
    fun reinstallArchive() = viewModelScope.launch {
        reseeding.value = true
        content.seedIfNeeded(force = true)
        reseeding.value = false
    }
}
