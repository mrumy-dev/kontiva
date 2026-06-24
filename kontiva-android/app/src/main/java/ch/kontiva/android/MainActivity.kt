package ch.kontiva.android

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import ch.kontiva.android.core.l10n.L10nKey
import ch.kontiva.android.core.l10n.LocalLocalizer
import ch.kontiva.android.core.l10n.Localizer
import ch.kontiva.android.ui.AppPhase
import ch.kontiva.android.ui.KontivaViewModel
import ch.kontiva.android.ui.screen.LockScreen
import ch.kontiva.android.ui.screen.MainScaffold
import ch.kontiva.android.ui.screen.OnboardingFlow
import ch.kontiva.android.ui.theme.KontivaTheme

class MainActivity : FragmentActivity() {
    private val vm: KontivaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Auto-lock: lock the vault after the chosen idle interval in the background.
        lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_STOP -> vm.onAppBackgrounded()
                    Lifecycle.Event.ON_START -> vm.onAppForegrounded()
                    else -> {}
                }
            },
        )
        setContent { KontivaRoot(vm) }
    }
}

@Composable
private fun KontivaRoot(vm: KontivaViewModel) {
    KontivaTheme(
        appearance = vm.settings.appearance, accent = vm.settings.accent,
        themeStyle = vm.settings.themeStyle, accentSecondary = vm.settings.accentSecondary,
        customAccent = ch.kontiva.android.ui.theme.parseHexColor(vm.settings.customAccent),
        customSecondary = ch.kontiva.android.ui.theme.parseHexColor(vm.settings.customAccentSecondary),
    ) {
        // The active language drives the Localizer and the layout direction (RTL for
        // Arabic / Urdu / Pashto), so the whole UI mirrors like iOS.
        val localizer = Localizer(vm.settings.language)
        CompositionLocalProvider(
            LocalLocalizer provides localizer,
            LocalLayoutDirection provides if (localizer.language.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
        ) {
            Box(Modifier.fillMaxSize().background(KontivaTheme.pageBrush)) {
                when (vm.phase) {
                    AppPhase.ONBOARDING -> OnboardingFlow(vm)
                    AppPhase.LOCKED -> LockScreen(vm)
                    AppPhase.UNLOCKED -> MainScaffold(vm)
                }
                if (vm.offerBiometricSetup && vm.phase == AppPhase.UNLOCKED) {
                    AlertDialog(
                        onDismissRequest = { vm.dismissBiometricOffer() },
                        title = { Text(localizer(L10nKey.settingsBiometric)) },
                        confirmButton = {
                            TextButton(onClick = { vm.enableBiometric(); vm.dismissBiometricOffer() }) {
                                Text(localizer(L10nKey.commonActivate))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { vm.dismissBiometricOffer() }) {
                                Text(localizer(L10nKey.onboardingSkip))
                            }
                        },
                    )
                }
            }
        }
    }
}
