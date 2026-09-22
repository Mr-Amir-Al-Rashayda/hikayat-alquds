package ps.hikayatalquds

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ps.hikayatalquds.core.designsystem.HikayatTheme
import ps.hikayatalquds.core.ui.ProvideAppLanguage
import ps.hikayatalquds.data.preferences.ThemeMode
import ps.hikayatalquds.narration.NarrationController
import ps.hikayatalquds.ui.HikayatApp
import ps.hikayatalquds.ui.HikayatAppViewModel
import javax.inject.Inject

/**
 * The only Activity.
 *
 * It holds the splash screen until the archive has been read out of the
 * database once, so the first frame is the real home screen rather than an
 * empty one that fills in - the same blank-first-load problem the web app had
 * to fix on mobile.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var narration: NarrationController

    private val viewModel: HikayatAppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        splash.setKeepOnScreenCondition { !viewModel.isReady.value }

        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val dark = when (state.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            ProvideAppLanguage(state.language) {
                HikayatTheme(darkTheme = dark) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                    ) {
                        HikayatApp(appState = state)
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // The speech engine takes a moment to connect; warming it here means
        // the first tap on "listen" plays instead of waiting.
        lifecycleScope.launch {
            val settings = viewModel.uiState.value
            narration.prepare(settings.language)
            narration.applyPreferences(
                rate = settings.narrationRate,
                pitch = settings.narrationPitch,
                voiceId = settings.narrationVoice,
            )
        }
    }

    override fun onDestroy() {
        // A narration belongs to the reading session, not to the process.
        if (isFinishing) narration.stop()
        super.onDestroy()
    }
}
