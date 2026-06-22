package ch.kontiva.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import ch.kontiva.android.core.l10n.LocalLocalizer
import ch.kontiva.android.core.l10n.Localizer
import ch.kontiva.android.ui.AppPhase
import ch.kontiva.android.ui.KontivaViewModel
import ch.kontiva.android.ui.screen.HomePlaceholder
import ch.kontiva.android.ui.screen.LockScreen
import ch.kontiva.android.ui.screen.OnboardingFlow
import ch.kontiva.android.ui.theme.KontivaTheme

class MainActivity : ComponentActivity() {
    private val vm: KontivaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { KontivaRoot(vm) }
    }
}

@Composable
private fun KontivaRoot(vm: KontivaViewModel) {
    KontivaTheme(appearance = vm.settings.appearance, accent = vm.settings.accent) {
        // The active language drives the Localizer; the picker (and unlock) update it.
        CompositionLocalProvider(LocalLocalizer provides Localizer(vm.settings.language)) {
            Surface(color = KontivaTheme.colors.pageBackground) {
                when (vm.phase) {
                    AppPhase.ONBOARDING -> OnboardingFlow(vm)
                    AppPhase.LOCKED -> LockScreen(vm)
                    AppPhase.UNLOCKED -> HomePlaceholder(vm)
                }
            }
        }
    }
}
