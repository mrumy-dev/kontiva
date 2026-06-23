package ch.kontiva.android.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import ch.kontiva.android.core.l10n.L10nKey
import ch.kontiva.android.core.l10n.LocalLocalizer
import ch.kontiva.android.security.BiometricAuth
import ch.kontiva.android.ui.KontivaViewModel

/** Unlock an existing vault with the PIN/passphrase (or biometrics). Shakes on a wrong code. */
@Composable
fun LockScreen(vm: KontivaViewModel) {
    val loc = LocalLocalizer.current
    val activity = LocalContext.current as? FragmentActivity
    var errorSignal by remember { mutableIntStateOf(0) }

    val canBiometric = vm.biometricEnabled && vm.biometricAvailable && activity != null

    fun triggerBiometric() {
        val act = activity ?: return
        BiometricAuth.authenticate(
            activity = act,
            title = loc(L10nKey.appName),
            subtitle = loc(L10nKey.lockWelcomeBack),
            negative = loc(L10nKey.commonCancel),
            onSuccess = { vm.biometricPassphrase()?.let { vm.unlock(it) } },
            onFail = {},
        )
    }

    LaunchedEffect(vm.unlockFailed) {
        if (vm.unlockFailed) {
            errorSignal++
            vm.clearUnlockError()
        }
    }
    // Auto-present the biometric prompt on appear (mirrors the iOS LockView).
    LaunchedEffect(Unit) { if (canBiometric) triggerBiometric() }

    PinEntry(
        title = loc(L10nKey.lockWelcomeBack),
        note = loc(L10nKey.lockEnterPassphrase),
        busy = vm.busy,
        resetSignal = 0,
        errorSignal = errorSignal,
        onComplete = { vm.unlock(it) },
        onBiometric = if (canBiometric) ::triggerBiometric else null,
    )
}
