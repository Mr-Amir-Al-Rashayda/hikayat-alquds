package ps.hikayatalquds.ui

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
import ps.hikayatalquds.data.preferences.ThemeMode
import ps.hikayatalquds.data.preferences.UserPreferencesRepository
import ps.hikayatalquds.data.remote.NetworkMonitor
import ps.hikayatalquds.data.repository.ArchiveOrigin
import ps.hikayatalquds.data.repository.ContentRepository
import ps.hikayatalquds.domain.model.AppLanguage
import javax.inject.Inject

/** App-wide state every screen needs: language, theme, connection, provenance. */
@Immutable
data class HikayatAppState(
    val language: AppLanguage = AppLanguage.ARABIC,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isOnline: Boolean = false,
    val archiveOrigin: ArchiveOrigin = ArchiveOrigin.BUNDLED,
    val lastSyncedAtEpochMillis: Long = 0L,
    val allowRemoteImages: Boolean = true,
    val narrationRate: Float = 0.92f,
    val narrationPitch: Float = 1.0f,
    val narrationVoice: String = "",
) {
    /** Remote photographs need both a connection and the reader's consent. */
    val canLoadRemoteImages: Boolean get() = isOnline && allowRemoteImages
}

@HiltViewModel
class HikayatAppViewModel @Inject constructor(
    preferences: UserPreferencesRepository,
    networkMonitor: NetworkMonitor,
    content: ContentRepository,
) : ViewModel() {

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    val uiState: StateFlow<HikayatAppState> = combine(
        preferences.settings,
        networkMonitor.isOnline,
        content.origin,
    ) { settings, online, origin ->
        HikayatAppState(
            language = settings.language,
            themeMode = settings.themeMode,
            isOnline = online,
            archiveOrigin = origin,
            lastSyncedAtEpochMillis = settings.lastSyncedAtEpochMillis,
            allowRemoteImages = settings.allowRemoteImages,
            narrationRate = settings.narrationRate,
            narrationPitch = settings.narrationPitch,
            narrationVoice = settings.narrationVoice,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = HikayatAppState(),
    )

    init {
        viewModelScope.launch {
            // The splash stays up until the archive is genuinely readable, so
            // nobody sees an empty home screen fill itself in.
            content.seedIfNeeded()
            _isReady.value = true
        }
    }
}
