package ps.hikayatalquds.ui.contribute

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ps.hikayatalquds.data.repository.ContentRepository
import ps.hikayatalquds.data.repository.MemoryRepository
import ps.hikayatalquds.data.repository.QueuedMemory
import ps.hikayatalquds.data.repository.SubmissionResult
import ps.hikayatalquds.domain.model.Location
import ps.hikayatalquds.domain.model.MemoryStatusReport
import ps.hikayatalquds.domain.model.NewMemory
import javax.inject.Inject

/** What happened to the memory somebody just wrote. */
sealed interface ContributeOutcome {
    data class Accepted(val referenceCode: String?) : ContributeOutcome
    data object Queued : ContributeOutcome
    data class Rejected(val message: String) : ContributeOutcome
}

@Immutable
data class ContributeUiState(
    val locations: List<Location> = emptyList(),
    val selectedLocationId: String? = null,
    val title: String = "",
    val body: String = "",
    val contributorName: String = "",
    val contributorEmail: String = "",
    val submitting: Boolean = false,
    val outcome: ContributeOutcome? = null,
    val queued: List<QueuedMemory> = emptyList(),
    val lookupCode: String = "",
    val lookupBusy: Boolean = false,
    val lookupResult: MemoryStatusReport? = null,
    val lookupError: String? = null,
    /** The queue answered, and it has never seen this code. */
    val lookupCodeUnknown: Boolean = false,
) {
    /** The API requires 20 characters, so the form asks for them up front. */
    val bodyLongEnough: Boolean get() = body.trim().length >= MINIMUM_BODY
    val canSubmit: Boolean
        get() = !submitting && selectedLocationId != null && title.isNotBlank() && bodyLongEnough

    companion object {
        const val MINIMUM_BODY = 20
    }
}

@HiltViewModel
class ContributeViewModel @Inject constructor(
    content: ContentRepository,
    private val memories: MemoryRepository,
) : ViewModel() {

    private val form = MutableStateFlow(FormState())
    private val submitting = MutableStateFlow(false)
    private val outcome = MutableStateFlow<ContributeOutcome?>(null)
    private val lookup = MutableStateFlow(LookupState())

    val uiState: StateFlow<ContributeUiState> = combine(
        content.locations,
        form,
        combine(submitting, outcome, ::Pair),
        memories.queued,
        lookup,
    ) { locations, formState, (isSubmitting, currentOutcome), queued, lookupState ->
        ContributeUiState(
            locations = locations,
            selectedLocationId = formState.locationId ?: locations.firstOrNull()?.id,
            title = formState.title,
            body = formState.body,
            contributorName = formState.name,
            contributorEmail = formState.email,
            submitting = isSubmitting,
            outcome = currentOutcome,
            queued = queued,
            lookupCode = lookupState.code,
            lookupBusy = lookupState.busy,
            lookupResult = lookupState.result,
            lookupError = lookupState.error,
            lookupCodeUnknown = lookupState.unknownCode,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ContributeUiState(),
    )

    fun preselect(locationId: String?) {
        if (locationId != null && form.value.locationId == null) {
            form.value = form.value.copy(locationId = locationId)
        }
    }

    fun selectLocation(locationId: String) {
        form.value = form.value.copy(locationId = locationId)
    }

    fun setTitle(value: String) {
        form.value = form.value.copy(title = value)
    }

    fun setBody(value: String) {
        form.value = form.value.copy(body = value)
    }

    fun setName(value: String) {
        form.value = form.value.copy(name = value)
    }

    fun setEmail(value: String) {
        form.value = form.value.copy(email = value)
    }

    fun dismissOutcome() {
        outcome.value = null
    }

    fun submit() {
        val state = uiState.value
        if (!state.canSubmit) return
        val locationId = state.selectedLocationId ?: return

        viewModelScope.launch {
            submitting.value = true
            outcome.value = null
            val result = memories.submit(
                NewMemory(
                    locationId = locationId,
                    title = state.title.trim(),
                    content = state.body.trim(),
                    contributorName = state.contributorName.trim().ifBlank { null },
                    contributorEmail = state.contributorEmail.trim().ifBlank { null },
                ),
            )
            outcome.value = when (result) {
                is SubmissionResult.Accepted ->
                    ContributeOutcome.Accepted(result.receipt.referenceCode)

                SubmissionResult.Queued -> ContributeOutcome.Queued
                is SubmissionResult.Rejected -> ContributeOutcome.Rejected(result.message)
            }
            // The form is only cleared when the memory actually went somewhere;
            // a rejection leaves the text in place so it is not lost.
            if (result !is SubmissionResult.Rejected) {
                form.value = form.value.copy(title = "", body = "")
            }
            submitting.value = false
        }
    }

    fun setLookupCode(value: String) {
        lookup.value = lookup.value.copy(code = value, error = null)
    }

    fun lookUp() {
        val code = lookup.value.code.trim()
        if (code.isEmpty() || lookup.value.busy) return
        viewModelScope.launch {
            lookup.value = lookup.value.copy(
                busy = true,
                error = null,
                result = null,
                unknownCode = false,
            )
            memories.lookUpStatus(code).fold(
                onSuccess = { report ->
                    lookup.value = lookup.value.copy(busy = false, result = report)
                },
                onFailure = { error ->
                    // A 404 is a real answer from a working queue, not a fault:
                    // the code is simply not one of ours. The UI says so in
                    // plain words rather than showing an HTTP message.
                    val unknownCode = (error as? ps.hikayatalquds.data.remote.ApiException)
                        ?.statusCode == 404
                    lookup.value = lookup.value.copy(
                        busy = false,
                        error = error.message,
                        unknownCode = unknownCode,
                    )
                },
            )
        }
    }

    private data class FormState(
        val locationId: String? = null,
        val title: String = "",
        val body: String = "",
        val name: String = "",
        val email: String = "",
    )

    private data class LookupState(
        val code: String = "",
        val busy: Boolean = false,
        val result: MemoryStatusReport? = null,
        val error: String? = null,
        val unknownCode: Boolean = false,
    )
}
