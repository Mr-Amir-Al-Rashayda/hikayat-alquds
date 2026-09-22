package ps.hikayatalquds.narration

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import androidx.compose.runtime.Immutable
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ps.hikayatalquds.di.ApplicationScope
import ps.hikayatalquds.domain.model.AppLanguage
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** One readable chunk of a narration, with the text actually spoken. */
@Immutable
data class NarrationSegment(val index: Int, val text: String)

@Immutable
data class NarrationState(
    val available: Boolean = false,
    val speaking: Boolean = false,
    val paused: Boolean = false,
    val preparing: Boolean = false,
    /** Which location this narration belongs to, so a screen knows if it is theirs. */
    val locationId: String? = null,
    val title: String = "",
    val segments: List<NarrationSegment> = emptyList(),
    val currentSegment: Int = -1,
    val voices: List<NarrationVoice> = emptyList(),
    val selectedVoice: String = "",
    val rate: Float = 0.92f,
    val pitch: Float = 1.0f,
    /** Set when the device has no voice for this language and cannot speak it. */
    val notice: NarrationNotice? = null,
) {
    val progress: Float
        get() = if (segments.isEmpty()) 0f else (currentSegment + 1f) / segments.size

    val isActive: Boolean get() = speaking || paused
}

@Immutable
data class NarrationVoice(
    val id: String,
    val label: String,
    val locale: String,
    val isNetworkVoice: Boolean,
    val quality: Int,
)

/** Something the reader needs told, rather than silently swallowed. */
enum class NarrationNotice {
    NO_ENGINE,
    NO_VOICE_FOR_LANGUAGE,
    LANGUAGE_DATA_MISSING,
    FAILED,
}

/**
 * Reads a story aloud with the device's own speech engine.
 *
 * Android's TextToSpeech has no pause, only stop, so this splits the narration
 * into sentences and keeps the index: "pause" stops the engine and remembers
 * where it was, "resume" starts again from that sentence. That also gives the
 * reading view something to highlight, which matters most in Arabic where
 * following a long paragraph by ear is hard.
 *
 * Voice choice is ranked rather than left to the default, because the default
 * Arabic voice on many devices is the worst one installed. If there is no
 * Arabic voice at all the app says so instead of reading Arabic text with an
 * English voice, which is unintelligible.
 */
@Singleton
class NarrationController @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(NarrationState())
    val state: StateFlow<NarrationState> = _state.asStateFlow()

    private var engine: TextToSpeech? = null
    private var initialised = false
    private var pendingPlayback: (() -> Unit)? = null
    private var language: AppLanguage = AppLanguage.ARABIC

    /**
     * Set while the app itself stops the engine.
     *
     * `TextToSpeech.stop()` flushes the queue, and several engines report the
     * discarded utterances through `onError`. Without this flag a deliberate
     * pause would tell the reader the speech engine had failed.
     */
    @Volatile
    private var stoppingDeliberately = false
    private var onFinished: ((String) -> Unit)? = null

    private val audioManager = context.getSystemService<AudioManager>()
    private var focusRequest: AudioFocusRequest? = null

    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS -> stop()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pause()
            AudioManager.AUDIOFOCUS_GAIN -> if (_state.value.paused) resume()
        }
    }

    /** Called when a narration finishes on its own, so progress can be recorded. */
    fun setOnFinishedListener(listener: (String) -> Unit) {
        onFinished = listener
    }

    fun prepare(language: AppLanguage) {
        this.language = language
        if (engine != null) {
            applyVoices()
            return
        }
        _state.update { it.copy(preparing = true) }
        engine = TextToSpeech(context) { status ->
            initialised = status == TextToSpeech.SUCCESS
            if (!initialised) {
                _state.update {
                    it.copy(preparing = false, available = false, notice = NarrationNotice.NO_ENGINE)
                }
                return@TextToSpeech
            }
            engine?.setOnUtteranceProgressListener(progressListener)
            applyVoices()
            _state.update { it.copy(preparing = false, available = true) }
            pendingPlayback?.invoke()
            pendingPlayback = null
        }
    }

    private val progressListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
            val index = utteranceId?.toSegmentIndex() ?: return
            _state.update { it.copy(speaking = true, paused = false, currentSegment = index) }
        }

        override fun onDone(utteranceId: String?) {
            val index = utteranceId?.toSegmentIndex() ?: return
            val current = _state.value
            if (index >= current.segments.lastIndex) {
                val finishedLocation = current.locationId
                abandonFocus()
                _state.update { it.copy(speaking = false, paused = false, currentSegment = -1) }
                finishedLocation?.let { id -> scope.launch { onFinished?.invoke(id) } }
            }
        }

        @Deprecated("Required by the platform for API < 21 compatibility.")
        override fun onError(utteranceId: String?) = onError(utteranceId, -1)

        override fun onError(utteranceId: String?, errorCode: Int) {
            if (stoppingDeliberately) return
            abandonFocus()
            _state.update {
                it.copy(speaking = false, paused = false, notice = NarrationNotice.FAILED)
            }
        }
    }

    /**
     * Starts reading [text]. Safe to call before the engine has initialised -
     * the request is held and runs as soon as it is ready, so a reader tapping
     * play the instant a screen opens is not ignored.
     */
    fun speak(locationId: String, title: String, text: String, language: AppLanguage) {
        this.language = language
        val segments = splitIntoSegments(text)
        if (segments.isEmpty()) return

        _state.update {
            it.copy(
                locationId = locationId,
                title = title,
                segments = segments,
                currentSegment = -1,
                notice = null,
            )
        }

        if (!initialised) {
            pendingPlayback = { startFrom(0) }
            prepare(language)
            return
        }
        applyVoices()
        startFrom(0)
    }

    private fun startFrom(segmentIndex: Int) {
        val current = _state.value
        val engine = engine ?: return
        if (!hasVoiceForLanguage()) {
            _state.update { it.copy(notice = NarrationNotice.NO_VOICE_FOR_LANGUAGE) }
            return
        }
        if (!requestFocus()) return

        stopEngine(engine)
        engine.setSpeechRate(current.rate)
        engine.setPitch(current.pitch)

        current.segments.drop(segmentIndex).forEachIndexed { offset, segment ->
            val queueMode = if (offset == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            engine.speak(segment.text, queueMode, Bundle(), segment.index.toUtteranceId())
        }
        _state.update { it.copy(speaking = true, paused = false) }
    }

    fun pause() {
        val current = _state.value
        if (!current.speaking) return
        engine?.let(::stopEngine)
        _state.update { it.copy(speaking = false, paused = true) }
    }

    fun resume() {
        val current = _state.value
        if (!current.paused) return
        startFrom(current.currentSegment.coerceAtLeast(0))
    }

    fun toggle() {
        val current = _state.value
        when {
            current.speaking -> pause()
            current.paused -> resume()
            else -> Unit
        }
    }

    fun stop() {
        engine?.let(::stopEngine)
        abandonFocus()
        _state.update { it.copy(speaking = false, paused = false, currentSegment = -1) }
    }

    /** Stops the engine without letting the flush be reported as a failure. */
    private fun stopEngine(engine: TextToSpeech) {
        stoppingDeliberately = true
        engine.stop()
        stoppingDeliberately = false
    }

    fun skipToSegment(index: Int) {
        val current = _state.value
        if (index !in current.segments.indices) return
        startFrom(index)
    }

    fun setRate(rate: Float) {
        _state.update { it.copy(rate = rate) }
        if (_state.value.speaking) startFrom(_state.value.currentSegment.coerceAtLeast(0))
    }

    fun setPitch(pitch: Float) {
        _state.update { it.copy(pitch = pitch) }
    }

    fun setVoice(voiceId: String) {
        _state.update { it.copy(selectedVoice = voiceId) }
        engine?.voices?.firstOrNull { it.name == voiceId }?.let { engine?.voice = it }
        if (_state.value.speaking) startFrom(_state.value.currentSegment.coerceAtLeast(0))
    }

    /** Applies the stored preferences once the engine exists to receive them. */
    fun applyPreferences(rate: Float, pitch: Float, voiceId: String) {
        _state.update { it.copy(rate = rate, pitch = pitch, selectedVoice = voiceId) }
        if (initialised && voiceId.isNotBlank()) setVoice(voiceId)
    }

    fun release() {
        stoppingDeliberately = true
        engine?.shutdown()
        engine = null
        initialised = false
        abandonFocus()
        _state.value = NarrationState()
    }

    // --- voices -------------------------------------------------------------

    private fun applyVoices() {
        val engine = engine ?: return
        val locale = Locale.forLanguageTag(language.tag)
        val availability = runCatching { engine.isLanguageAvailable(locale) }
            .getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED)

        val ranked = runCatching { engine.voices.orEmpty() }
            .getOrDefault(emptySet())
            .filter { it.locale.language.equals(language.tag, ignoreCase = true) }
            .sortedWith(compareByDescending<Voice> { rank(it) }.thenBy { it.name })
            .map { it.toNarrationVoice() }

        val stored = _state.value.selectedVoice
        val chosen = ranked.firstOrNull { it.id == stored } ?: ranked.firstOrNull()

        if (availability >= TextToSpeech.LANG_AVAILABLE) {
            engine.language = locale
        }
        chosen?.let { voice ->
            engine.voices?.firstOrNull { it.name == voice.id }?.let { engine.voice = it }
        }

        _state.update {
            it.copy(
                voices = ranked,
                selectedVoice = chosen?.id.orEmpty(),
                notice = when {
                    availability == TextToSpeech.LANG_MISSING_DATA -> NarrationNotice.LANGUAGE_DATA_MISSING
                    ranked.isEmpty() -> NarrationNotice.NO_VOICE_FOR_LANGUAGE
                    else -> null
                },
            )
        }
    }

    private fun hasVoiceForLanguage(): Boolean {
        val engine = engine ?: return false
        if (_state.value.voices.isNotEmpty()) return true
        val availability = runCatching {
            engine.isLanguageAvailable(Locale.forLanguageTag(language.tag))
        }.getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED)
        return availability >= TextToSpeech.LANG_AVAILABLE
    }

    /**
     * Ranks installed voices. The ordering mirrors the web app's: a premium or
     * neural voice first, then a named human-quality voice, then anything local
     * (so narration keeps working with the radio off).
     */
    private fun rank(voice: Voice): Int {
        var score = 0
        val name = voice.name.lowercase()
        PREFERRED_VOICE_KEYWORDS.forEachIndexed { index, keyword ->
            if (name.contains(keyword)) score += (PREFERRED_VOICE_KEYWORDS.size - index) * 4
        }
        score += when (voice.quality) {
            Voice.QUALITY_VERY_HIGH -> 20
            Voice.QUALITY_HIGH -> 14
            Voice.QUALITY_NORMAL -> 8
            else -> 2
        }
        if (!voice.isNetworkConnectionRequired) score += 6
        if (voice.features?.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) == true) score -= 40
        return score
    }

    private fun Voice.toNarrationVoice() = NarrationVoice(
        id = name,
        label = describe(this),
        locale = locale.toLanguageTag(),
        isNetworkVoice = isNetworkConnectionRequired,
        quality = quality,
    )

    /** Engine voice names are opaque ids; this makes a pickable label. */
    private fun describe(voice: Voice): String {
        val qualityLabel = when (voice.quality) {
            Voice.QUALITY_VERY_HIGH -> "★★★"
            Voice.QUALITY_HIGH -> "★★"
            Voice.QUALITY_NORMAL -> "★"
            else -> ""
        }
        val readable = voice.name
            .removePrefix("${voice.locale.language}-")
            .replace('-', ' ')
            .replace('_', ' ')
            .trim()
            .ifBlank { voice.name }
        return listOf(readable, qualityLabel).filter { it.isNotBlank() }.joinToString(" ")
    }

    // --- audio focus --------------------------------------------------------

    private fun requestFocus(): Boolean {
        val manager = audioManager ?: return true
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build(),
                )
                .setOnAudioFocusChangeListener(focusListener)
                .setWillPauseWhenDucked(true)
                .build()
            focusRequest = request
            manager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            manager.requestAudioFocus(
                focusListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN,
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonFocus() {
        val manager = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { manager.abandonAudioFocusRequest(it) }
            focusRequest = null
        } else {
            @Suppress("DEPRECATION")
            manager.abandonAudioFocus(focusListener)
        }
    }

    companion object {
        private val PREFERRED_VOICE_KEYWORDS = listOf(
            "premium", "enhanced", "natural", "neural", "wavenet", "google", "microsoft",
            "laila", "layla", "hoda", "majed", "maged", "tarik",
        )

        /** Longer than this and the engine's own buffer becomes the limit. */
        private const val MAX_SEGMENT_CHARS = 320

        /**
         * Below this, a sentence is a fragment and is folded into the next one
         * so the highlight does not flick through "Yes." and "No.".
         *
         * Deliberately short: a 50-character Arabic sentence is a whole thought
         * and deserves its own highlight, not to be glued to the one after it.
         */
        private const val MIN_SEGMENT_CHARS = 35

        /**
         * Splits a narration into sentences, respecting the Arabic question mark
         * and comma, then merges anything too short to be worth highlighting on
         * its own and splits anything too long for the engine to speak.
         */
        internal fun splitIntoSegments(text: String): List<NarrationSegment> {
            val cleaned = text
                .replace(Regex("[*#_`•]"), " ")
                .replace(Regex("\\s*\\n+\\s*"), ". ")
                .replace(Regex("\\s+"), " ")
                .trim()
            if (cleaned.isEmpty()) return emptyList()

            val sentences = cleaned
                .split(Regex("(?<=[.!?؟])\\s+"))
                .map(String::trim)
                .filter { it.isNotBlank() }

            val merged = mutableListOf<String>()
            sentences.forEach { sentence ->
                val last = merged.lastOrNull()
                if (last != null &&
                    last.length < MIN_SEGMENT_CHARS &&
                    last.length + sentence.length < MAX_SEGMENT_CHARS
                ) {
                    merged[merged.lastIndex] = "$last $sentence"
                } else {
                    merged += sentence
                }
            }

            return merged
                .flatMap { it.chunkedOnWords(MAX_SEGMENT_CHARS) }
                .mapIndexed { index, body -> NarrationSegment(index, body) }
        }

        /** Breaks an over-long sentence at word boundaries rather than mid-word. */
        private fun String.chunkedOnWords(limit: Int): List<String> {
            if (length <= limit) return listOf(this)
            val chunks = mutableListOf<String>()
            val builder = StringBuilder()
            split(' ').forEach { word ->
                if (builder.length + word.length + 1 > limit && builder.isNotEmpty()) {
                    chunks += builder.toString().trim()
                    builder.clear()
                }
                builder.append(word).append(' ')
            }
            if (builder.isNotBlank()) chunks += builder.toString().trim()
            return chunks
        }

        private const val UTTERANCE_PREFIX = "hikayat-segment-"

        private fun Int.toUtteranceId() = "$UTTERANCE_PREFIX$this"

        private fun String.toSegmentIndex(): Int? =
            removePrefix(UTTERANCE_PREFIX).toIntOrNull()
    }
}
