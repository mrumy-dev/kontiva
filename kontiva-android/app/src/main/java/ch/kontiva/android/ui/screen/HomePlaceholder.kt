package ch.kontiva.android.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.kontiva.android.core.l10n.L10nKey
import ch.kontiva.android.core.l10n.LocalLocalizer
import ch.kontiva.android.ui.KontivaViewModel
import ch.kontiva.android.ui.theme.KontivaTheme

/**
 * Placeholder landing for the unlocked app — proves the vault round-trips and the
 * lock/unlock cycle works. The real tab bar (Overview/Planning/Bills/Savings/
 * Settings) is the next stage.
 */
@Composable
fun HomePlaceholder(vm: KontivaViewModel) {
    val loc = LocalLocalizer.current
    val colors = KontivaTheme.colors
    Column(
        Modifier.fillMaxSize().padding(KontivaTheme.spaceLg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Rounded.LockOpen, contentDescription = null, tint = colors.positive, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(KontivaTheme.spaceMd))
        Row {
            Text("Kontiva", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
            Text(".", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = colors.swissRed)
        }
        vm.household?.name?.let { name ->
            Spacer(Modifier.height(KontivaTheme.spaceXs))
            Text(name, fontSize = 18.sp, color = colors.textSecondary)
        }
        Spacer(Modifier.height(KontivaTheme.spaceLg))
        Text(
            "Tresor entsperrt. Übersicht, Monatsplanung, Rechnungen, Sparen und Einstellungen folgen als nächste Stufe.",
            fontSize = 14.sp,
            color = colors.textTertiary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = KontivaTheme.spaceMd),
        )
        Spacer(Modifier.height(KontivaTheme.spaceXl))
        PrimaryButton(loc(L10nKey.actionLock), onClick = { vm.lock() })
    }
}
