package ch.kontiva.android.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import ch.kontiva.android.core.l10n.L10nKey
import ch.kontiva.android.core.l10n.LocalLocalizer
import ch.kontiva.android.ui.KontivaViewModel

/** Unlock an existing vault with the PIN/passphrase. Shakes on a wrong code. */
@Composable
fun LockScreen(vm: KontivaViewModel) {
    val loc = LocalLocalizer.current
    var errorSignal by remember { mutableIntStateOf(0) }

    LaunchedEffect(vm.unlockFailed) {
        if (vm.unlockFailed) {
            errorSignal++
            vm.clearUnlockError()
        }
    }

    PinEntry(
        title = loc(L10nKey.lockWelcomeBack),
        note = loc(L10nKey.lockEnterPassphrase),
        busy = vm.busy,
        resetSignal = 0,
        errorSignal = errorSignal,
        onComplete = { vm.unlock(it) },
    )
}
