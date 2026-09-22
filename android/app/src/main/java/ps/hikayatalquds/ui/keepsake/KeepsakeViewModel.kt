package ps.hikayatalquds.ui.keepsake

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import ps.hikayatalquds.data.repository.JourneyRepository
import ps.hikayatalquds.di.IoDispatcher
import ps.hikayatalquds.domain.model.AppLanguage
import ps.hikayatalquds.domain.model.JerusalemKeepsake
import javax.inject.Inject

@Immutable
data class KeepsakeUiState(val keepsake: JerusalemKeepsake? = null)

@HiltViewModel
class KeepsakeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val journey: JourneyRepository,
    @IoDispatcher private val io: CoroutineDispatcher,
) : ViewModel() {

    val uiState: StateFlow<KeepsakeUiState> = journey.keepsake
        .map { KeepsakeUiState(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = KeepsakeUiState(),
        )

    /** The default file name offered to the system's "create document" picker. */
    val suggestedFileName: String get() = "hikayat-alquds-story.txt"

    suspend fun shareText(language: AppLanguage): String = keepsakeText(language)

    /**
     * Writes the keepsake to wherever the reader chose in the system picker.
     *
     * Deliberately plain text rather than a PDF or an image: a keepsake is
     * words, and a text file stays readable on any device without a library to
     * open it. Going through `ACTION_CREATE_DOCUMENT` means the file lands
     * somewhere the reader can actually find it, and needs no storage
     * permission.
     *
     * @return true when the bytes were written.
     */
    suspend fun writeKeepsake(target: Uri, language: AppLanguage): Boolean = withContext(io) {
        runCatching {
            context.contentResolver.openOutputStream(target)?.use { stream ->
                stream.write(keepsakeText(language).toByteArray(Charsets.UTF_8))
            } ?: return@runCatching false
            true
        }.getOrDefault(false)
    }

    private suspend fun keepsakeText(language: AppLanguage): String {
        val keepsake = uiState.value.keepsake ?: journey.keepsakeNow()
        return buildString {
            appendLine(journey.shareText(keepsake, language))
            appendLine()
            appendLine(
                if (language.isArabic) {
                    "هذا الانعكاس مولَّد من تقدّمك وحده؛ لا يضيف أي ادعاء تاريخي."
                } else {
                    "This reflection is generated only from your own progress; it adds no historical claim."
                },
            )
        }
    }
}
