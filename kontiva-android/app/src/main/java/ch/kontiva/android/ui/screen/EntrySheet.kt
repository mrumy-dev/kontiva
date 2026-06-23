package ch.kontiva.android.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.kontiva.android.core.Money
import ch.kontiva.android.core.l10n.L10nKey
import ch.kontiva.android.core.l10n.LocalLocalizer
import ch.kontiva.android.ui.theme.KontivaTheme

/** Add-entry sheet: name + amount (+ optional category). Reused for income/fixed/variable. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <C> EntrySheet(
    title: String,
    categories: List<C>?,
    categoryLabel: (C) -> String,
    onDismiss: () -> Unit,
    onSave: (name: String, amount: Money, category: C?) -> Unit,
    initialName: String = "",
    initialAmount: String = "",
    initialCategory: C? = null,
) {
    val loc = LocalLocalizer.current
    val colors = KontivaTheme.colors
    var name by remember { mutableStateOf(initialName) }
    var amountText by remember { mutableStateOf(initialAmount) }
    var category by remember { mutableStateOf(initialCategory ?: categories?.firstOrNull()) }
    var menuOpen by remember { mutableStateOf(false) }

    val amount = Money.parse(amountText)
    val canSave = name.isNotBlank() && amount != null && !amount.isZero

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.cardSurface,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .padding(horizontal = KontivaTheme.spaceLg)
                .padding(bottom = KontivaTheme.spaceLg)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(KontivaTheme.spaceMd),
        ) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)

            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text(loc(L10nKey.formName)) },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = amountText, onValueChange = { amountText = it },
                label = { Text(loc(L10nKey.formAmount)) },
                prefix = { Text("CHF ") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            if (categories != null && category != null) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { menuOpen = true }
                        .padding(vertical = KontivaTheme.spaceSm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(loc(L10nKey.formCategory), color = colors.textSecondary)
                    Spacer(Modifier.weight(1f))
                    Text(categoryLabel(category as C), color = KontivaTheme.accent, fontWeight = FontWeight.Medium)
                    Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, tint = colors.textTertiary)
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        categories.forEach { c ->
                            DropdownMenuItem(
                                text = { Text(categoryLabel(c)) },
                                onClick = { category = c; menuOpen = false },
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(KontivaTheme.spaceXs))
            Button(
                onClick = { if (canSave) onSave(name.trim(), amount!!, category) },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(KontivaTheme.radiusControl),
                colors = ButtonDefaults.buttonColors(containerColor = KontivaTheme.accent, contentColor = Color.White),
            ) {
                Text(loc(L10nKey.commonSave), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
