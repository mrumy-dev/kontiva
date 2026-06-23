package ch.kontiva.android.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import ch.kontiva.android.core.BillClassifier
import ch.kontiva.android.core.BillState
import ch.kontiva.android.core.DebtItem
import ch.kontiva.android.core.DebtType
import ch.kontiva.android.core.Money
import ch.kontiva.android.core.total
import ch.kontiva.android.core.l10n.L10nKey
import ch.kontiva.android.core.l10n.LocalLocalizer
import ch.kontiva.android.ui.KontivaViewModel
import ch.kontiva.android.ui.theme.KontivaTheme

@Composable
fun DebtsScreen(vm: KontivaViewModel, onBack: () -> Unit) {
    val loc = LocalLocalizer.current
    val colors = KontivaTheme.colors
    var showSheet by remember { mutableStateOf(false) }
    var editDebt by remember { mutableStateOf<DebtItem?>(null) }

    val overdueBills = vm.dataset.bills.filter { BillClassifier.state(it, vm.selectedMonth) == BillState.OVERDUE }
    val overdueTotal = overdueBills.map { it.amount }.total()
    val debts = vm.dataset.debts
    val recordedTotal = debts.map { it.amount }.total()
    val grandTotal = overdueTotal + recordedTotal

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(KontivaTheme.spaceLg),
        verticalArrangement = Arrangement.spacedBy(KontivaTheme.spaceMd),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "back", tint = colors.textPrimary) }
                Text(loc(L10nKey.navSchulden), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { editDebt = null; showSheet = true }) { Icon(Icons.Rounded.Add, contentDescription = null, tint = KontivaTheme.accent) }
            }
        }
        if (overdueBills.isEmpty() && debts.isEmpty()) {
            item { EmptyCard(loc(L10nKey.schuldenEmpty)) }
        } else {
            item {
                Surface(shape = RoundedCornerShape(KontivaTheme.radiusCard), color = colors.cardSurface, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(KontivaTheme.spaceMd)) {
                        Text(loc(L10nKey.schuldenTotal).uppercase(), fontSize = 11.sp, color = colors.textTertiary)
                        Text(grandTotal.formattedCHF(), fontSize = 26.sp, fontWeight = FontWeight.Bold, color = if (grandTotal.isPositive) colors.swissRed else colors.textPrimary)
                        Spacer(Modifier.size(KontivaTheme.spaceSm))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(loc(L10nKey.schuldenOverdueBills), fontSize = 10.sp, color = colors.textTertiary)
                                Text(overdueTotal.formattedCHF(), fontSize = 13.sp, color = colors.textPrimary, fontWeight = FontWeight.Medium)
                            }
                            Column {
                                Text(loc(L10nKey.schuldenRecorded), fontSize = 10.sp, color = colors.textTertiary)
                                Text(recordedTotal.formattedCHF(), fontSize = 13.sp, color = colors.textPrimary, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
            if (overdueBills.isNotEmpty()) {
                item {
                    Surface(shape = RoundedCornerShape(KontivaTheme.radiusCard), color = colors.cardSurface, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(KontivaTheme.spaceMd)) {
                            Text(loc(L10nKey.schuldenOverdueBills), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary)
                            overdueBills.forEach { b ->
                                DebtRow(b.provider, "", b.amount.formattedCHF(), onClick = {}, onDelete = {})
                            }
                            Text(loc(L10nKey.schuldenManagedInBills), fontSize = 12.sp, color = colors.textTertiary, modifier = Modifier.padding(top = KontivaTheme.spaceXs))
                        }
                    }
                }
            }
            if (debts.isNotEmpty()) {
                item {
                    Surface(shape = RoundedCornerShape(KontivaTheme.radiusCard), color = colors.cardSurface, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(KontivaTheme.spaceMd)) {
                            Text(loc(L10nKey.schuldenRecorded), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary)
                            debts.sortedBy { it.type.severityRank }.forEach { d ->
                                DebtRow(
                                    d.creditor, loc(d.type.labelKey), d.amount.formattedCHF(),
                                    onClick = { editDebt = d; showSheet = true },
                                    onDelete = { vm.deleteDebt(d.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
        // Guidance
        item {
            Surface(shape = RoundedCornerShape(KontivaTheme.radiusCard), color = colors.cardSurface, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(KontivaTheme.spaceMd), verticalArrangement = Arrangement.spacedBy(KontivaTheme.spaceSm)) {
                    Text(loc(L10nKey.schuldenGuidanceTitle), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                    Tip(loc(L10nKey.schuldenTipContactTitle), loc(L10nKey.schuldenTipContactBody))
                    Tip(loc(L10nKey.schuldenTipBetreibungTitle), loc(L10nKey.schuldenTipBetreibungBody))
                    Tip(loc(L10nKey.schuldenTipCounselingTitle), loc(L10nKey.schuldenTipCounselingBody))
                }
            }
        }
        item { Text(loc(L10nKey.schuldenDisclaimer), fontSize = 11.sp, color = colors.textTertiary, modifier = Modifier.padding(KontivaTheme.spaceXs)) }
    }

    if (showSheet) {
        val d = editDebt
        DebtSheet(
            onDismiss = { showSheet = false },
            onSave = { c, a, t ->
                if (d != null) vm.updateDebt(d.id, c, a, t) else vm.addDebt(c, a, t)
                showSheet = false
            },
            initialCreditor = d?.creditor ?: "",
            initialAmount = d?.amount?.formattedCHF(false) ?: "",
            initialType = d?.type ?: DebtType.OPEN_CLAIM,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DebtRow(creditor: String, type: String, amount: String, onClick: () -> Unit, onDelete: () -> Unit) {
    val colors = KontivaTheme.colors
    Row(
        Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onDelete).padding(vertical = KontivaTheme.spaceSm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(creditor, fontSize = 15.sp, color = colors.textPrimary)
            if (type.isNotEmpty()) Text(type, fontSize = 12.sp, color = colors.textTertiary)
        }
        Text(amount, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
    }
}

@Composable
private fun Tip(title: String, body: String) {
    val colors = KontivaTheme.colors
    Column {
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
        Text(body, fontSize = 12.sp, color = colors.textSecondary)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DebtSheet(
    onDismiss: () -> Unit,
    onSave: (String, Money, DebtType) -> Unit,
    initialCreditor: String = "",
    initialAmount: String = "",
    initialType: DebtType = DebtType.OPEN_CLAIM,
) {
    val loc = LocalLocalizer.current
    val colors = KontivaTheme.colors
    var creditor by remember { mutableStateOf(initialCreditor) }
    var amountText by remember { mutableStateOf(initialAmount) }
    var type by remember { mutableStateOf(initialType) }
    var menu by remember { mutableStateOf(false) }
    val amount = Money.parse(amountText)
    val canSave = creditor.isNotBlank() && amount != null && !amount.isZero

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = colors.cardSurface, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.padding(horizontal = KontivaTheme.spaceLg).padding(bottom = KontivaTheme.spaceLg).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(KontivaTheme.spaceMd),
        ) {
            Text(loc(L10nKey.schuldenAddCta), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
            OutlinedTextField(creditor, { creditor = it }, label = { Text(loc(L10nKey.debtCreditor)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(amountText, { amountText = it }, label = { Text(loc(L10nKey.formAmount)) }, prefix = { Text("CHF ") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth().clickable { menu = true }.padding(vertical = KontivaTheme.spaceSm), verticalAlignment = Alignment.CenterVertically) {
                Text(loc(L10nKey.debtType), color = colors.textSecondary)
                Spacer(Modifier.weight(1f))
                Text(loc(type.labelKey), color = KontivaTheme.accent, fontWeight = FontWeight.Medium)
                Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, tint = colors.textTertiary)
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DebtType.entries.forEach { t -> DropdownMenuItem(text = { Text(loc(t.labelKey)) }, onClick = { type = t; menu = false }) }
                }
            }
            Button(
                onClick = { if (canSave) onSave(creditor.trim(), amount!!, type) },
                enabled = canSave, modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(KontivaTheme.radiusControl),
                colors = ButtonDefaults.buttonColors(containerColor = KontivaTheme.accent, contentColor = Color.White),
            ) { Text(loc(L10nKey.commonSave), fontWeight = FontWeight.SemiBold) }
        }
    }
}
