package ps.hikayatalquds.domain.model

import androidx.compose.runtime.Immutable

/** The two interface languages. Arabic is the default; this is a Jerusalem archive. */
enum class AppLanguage(val tag: String) {
    ARABIC("ar"),
    ENGLISH("en");

    val isArabic: Boolean get() = this == ARABIC

    companion object {
        fun fromTag(tag: String?): AppLanguage = if (tag == ENGLISH.tag) ENGLISH else ARABIC
    }
}

/**
 * A piece of reviewed content in both languages.
 *
 * The Arabic and English texts are separate reviewed writings, not translations
 * generated at runtime, so both travel together and the app only ever chooses
 * between them. Where a record genuinely has no Arabic text the English is
 * shown rather than a blank - saying nothing would hide reviewed content.
 */
@Immutable
data class LocalizedText(val en: String, val ar: String) {
    operator fun get(language: AppLanguage): String =
        if (language.isArabic) ar.ifBlank { en } else en.ifBlank { ar }

    val isEmpty: Boolean get() = en.isBlank() && ar.isBlank()

    companion object {
        val Empty = LocalizedText("", "")
        fun of(en: String?, ar: String?) = LocalizedText(en.orEmpty(), ar.orEmpty())
    }
}

@Immutable
data class LocalizedList(val en: List<String>, val ar: List<String>) {
    operator fun get(language: AppLanguage): List<String> =
        if (language.isArabic) ar.ifEmpty { en } else en.ifEmpty { ar }

    companion object {
        val Empty = LocalizedList(emptyList(), emptyList())
    }
}
