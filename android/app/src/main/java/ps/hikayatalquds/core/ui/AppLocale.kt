package ps.hikayatalquds.core.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import ps.hikayatalquds.domain.model.AppLanguage
import ps.hikayatalquds.domain.model.LocalizedText
import java.util.Locale

/**
 * The interface language chosen inside the app.
 *
 * Arabic is the default and the app must be able to run in Arabic on a phone
 * whose system language is English, so the language is an app setting rather
 * than something inherited from the system. Content models carry both
 * languages, and this is what picks between them.
 */
val LocalAppLanguage: ProvidableCompositionLocal<AppLanguage> =
    staticCompositionLocalOf { AppLanguage.ARABIC }

/**
 * Re-resolves `strings.xml` against [language] and flips the layout direction
 * with it, so an Arabic interface is right-to-left everywhere without a single
 * `if (isArabic)` in a layout.
 */
@Composable
fun ProvideAppLanguage(language: AppLanguage, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    val localized = remember(context, language, configuration) {
        val locale = Locale.forLanguageTag(language.tag)
        val updated = Configuration(configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        LocalizedContext(context, context.createConfigurationContext(updated).resources)
    }

    CompositionLocalProvider(
        LocalAppLanguage provides language,
        LocalContext provides localized,
        LocalConfiguration provides localized.resources.configuration,
        LocalLayoutDirection provides
            if (language.isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr,
        content = content,
    )
}

/**
 * Serves [language]-resolved resources while keeping the real Activity in the
 * context chain.
 *
 * `createConfigurationContext` returns a fresh context whose base is not the
 * Activity, and things that walk the `ContextWrapper` chain looking for one -
 * `hiltViewModel()`, the activity-result APIs - then fail at runtime. Wrapping
 * the Activity and overriding only the resources keeps both working: the
 * chain still leads to the Activity, and `stringResource` still reads Arabic.
 */
private class LocalizedContext(
    base: Context,
    private val localizedResources: Resources,
) : ContextWrapper(base) {
    override fun getResources(): Resources = localizedResources

    override fun getAssets(): AssetManager = localizedResources.assets
}

/** Reads the side of a bilingual record that matches the current language. */
@Composable
fun LocalizedText.current(): String = this[LocalAppLanguage.current]

/**
 * Walks out to the hosting Activity.
 *
 * Used by the share sheet and by "open system speech settings", which want the
 * real Activity rather than whatever wrapper is in scope.
 */
fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
