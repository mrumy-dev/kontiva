package ch.kontiva.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import ch.kontiva.android.core.AppLanguage
import ch.kontiva.android.core.AppSettings
import ch.kontiva.android.core.l10n.LocalLocalizer
import ch.kontiva.android.core.l10n.Localizer
import ch.kontiva.android.ui.screen.WelcomeScreen
import ch.kontiva.android.ui.theme.KontivaTheme
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { KontivaRoot() }
    }
}

@Composable
private fun KontivaRoot() {
    // Default the UI language to the device's preferred language (mirrors iOS
    // bestForDevice); the in-app picker will override this later.
    val config = LocalConfiguration.current
    val deviceLocales: List<Locale> = remember(config) {
        val list = config.locales
        (0 until list.size()).map { list[it] }.ifEmpty { listOf(Locale.getDefault()) }
    }
    var settings by remember {
        mutableStateOf(AppSettings(language = AppLanguage.bestForDevice(deviceLocales)))
    }

    KontivaTheme(appearance = settings.appearance, accent = settings.accent) {
        CompositionLocalProvider(LocalLocalizer provides Localizer(settings.language)) {
            Surface(color = KontivaTheme.colors.pageBackground) {
                // First onboarding step. Profile/code steps + the unlocked app follow
                // as the port progresses (see kontiva-android/README.md roadmap).
                WelcomeScreen(onStart = { /* TODO: advance to language/profile/code */ })
            }
        }
    }
}
