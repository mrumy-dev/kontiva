package ch.kontiva.android.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Article
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.draw.clip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
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
import ch.kontiva.android.core.FixedExpenseCategory
import ch.kontiva.android.core.Money
import ch.kontiva.android.core.VariableBudgetCategory
import ch.kontiva.android.core.total
import ch.kontiva.android.ui.theme.icon
import ch.kontiva.android.core.l10n.L10nKey
import ch.kontiva.android.core.l10n.LocalLocalizer
import ch.kontiva.android.ui.KontivaViewModel
import ch.kontiva.android.ui.pressScale
import ch.kontiva.android.ui.theme.KontivaTheme

@Composable
fun DebtsScreen(vm: KontivaViewModel, onBack: () -> Unit) {
    val loc = LocalLocalizer.current
    val colors = KontivaTheme.colors
    var showSheet by remember { mutableStateOf(false) }
    var editDebt by remember { mutableStateOf<DebtItem?>(null) }
    // Start-paying-off: which debt + which destination form to open.
    var payoffDebt by remember { mutableStateOf<DebtItem?>(null) }
    var payoffKind by remember { mutableStateOf<Payoff?>(null) }

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
            item { SchuldenEmptyCard(loc) { editDebt = null; showSheet = true } }
        } else {
            item { EncouragementBanner(loc) }
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
                                DebtRow(b.provider, "", b.amount.formattedCHF(), onClick = {}, onDelete = {}, actionable = false)
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
                                val sub = buildString {
                                    append(loc(d.type.labelKey))
                                    d.reference?.takeIf { it.isNotBlank() }?.let { append(" · "); append(it) }
                                }
                                DebtRow(
                                    d.creditor, sub, d.amount.formattedCHF(),
                                    onClick = { editDebt = d; showSheet = true },
                                    onDelete = { vm.deleteDebt(d.id) },
                                    onPayFixed = { payoffDebt = d; payoffKind = Payoff.FIXED },
                                    onPayBill = { payoffDebt = d; payoffKind = Payoff.BILL },
                                    onPayVariable = { payoffDebt = d; payoffKind = Payoff.VARIABLE },
                                )
                            }
                        }
                    }
                }
            }
        }
        // Guidance — supportive, actionable ways out.
        item {
            Surface(shape = RoundedCornerShape(KontivaTheme.radiusCard), color = colors.cardSurface, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(KontivaTheme.spaceMd), verticalArrangement = Arrangement.spacedBy(KontivaTheme.spaceMd)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Lightbulb, contentDescription = null, tint = KontivaTheme.accent, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.size(KontivaTheme.spaceXs))
                        Text(loc(L10nKey.schuldenGuidanceTitle), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                    }
                    Tip(Icons.Rounded.Forum, colors.positive, loc(L10nKey.schuldenTipContactTitle), loc(L10nKey.schuldenTipContactBody))
                    Tip(Icons.Rounded.Gavel, colors.warning, loc(L10nKey.schuldenTipBetreibungTitle), loc(L10nKey.schuldenTipBetreibungBody))
                    Tip(Icons.Rounded.Shield, colors.chartFixed, loc(L10nKey.schuldenTipExistenzminimumTitle), loc(L10nKey.schuldenTipExistenzminimumBody))
                    Tip(Icons.Rounded.Article, colors.chartFixed, loc(L10nKey.schuldenTipVerlustscheinTitle), loc(L10nKey.schuldenTipVerlustscheinBody))
                    Tip(Icons.Rounded.VolunteerActivism, KontivaTheme.accent, loc(L10nKey.schuldenTipCounselingTitle), loc(L10nKey.schuldenTipCounselingBody))
                }
            }
        }
        item { Text(loc(L10nKey.schuldenDisclaimer), fontSize = 11.sp, color = colors.textTertiary, modifier = Modifier.padding(KontivaTheme.spaceXs)) }
    }

    if (showSheet) {
        val d = editDebt
        DebtSheet(
            onDismiss = { showSheet = false },
            onSave = { c, a, t, date, ref, notes ->
                if (d != null) vm.updateDebt(d.id, c, a, t, date, ref, notes) else vm.addDebt(c, a, t, date, ref, notes)
                showSheet = false
            },
            initialCreditor = d?.creditor ?: "",
            initialAmount = d?.amount?.formattedCHF(false) ?: "",
            initialType = d?.type ?: DebtType.OPEN_CLAIM,
            initialDate = d?.date,
            initialReference = d?.reference ?: "",
            initialNotes = d?.notes ?: "",
        )
    }

    // Start paying off a debt: open the matching add form, pre-filled.
    val pd = payoffDebt
    if (pd != null) when (payoffKind) {
        Payoff.FIXED -> FixedCostSheet(
            categories = FixedExpenseCategory.entries,
            initialCategory = FixedExpenseCategory.RATENZAHLUNG,
            onDismiss = { payoffKind = null; payoffDebt = null },
            onSave = { name, amount, cat, sm, inst -> vm.addFixedCost(name, amount, cat, sm, inst); payoffKind = null; payoffDebt = null },
        )
        Payoff.BILL -> BillSheet(
            initialProvider = pd.creditor,
            initialAmount = pd.amount.formattedCHF(false),
            initialDate = pd.date ?: java.time.LocalDate.now(),
            onDismiss = { payoffKind = null; payoffDebt = null },
            onSave = { p, a, date, paid, notes -> vm.addBill(p, a, date, paid, notes); payoffKind = null; payoffDebt = null },
        )
        Payoff.VARIABLE -> EntrySheet(
            title = loc(L10nKey.planningVariable),
            categories = VariableBudgetCategory.entries,
            categoryLabel = { loc(it.labelKey) },
            categoryIcon = { it.icon() },
            onDismiss = { payoffKind = null; payoffDebt = null },
            onSave = { name, amount, cat -> vm.addVariableBudget(name, amount, cat ?: VariableBudgetCategory.OTHER); payoffKind = null; payoffDebt = null },
        )
        null -> {}
    }
}

/** Which budget entry to create when starting to pay off a debt. */
private enum class Payoff { FIXED, BILL, VARIABLE }

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DebtRow(
    creditor: String, type: String, amount: String, onClick: () -> Unit, onDelete: () -> Unit,
    actionable: Boolean = true,
    onPayFixed: (() -> Unit)? = null, onPayBill: (() -> Unit)? = null, onPayVariable: (() -> Unit)? = null,
) {
    val loc = LocalLocalizer.current
    val colors = KontivaTheme.colors
    var menu by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.fillMaxWidth().pressScale().combinedClickable(onClick = onClick, onLongClick = { if (actionable) menu = true }).padding(vertical = KontivaTheme.spaceSm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(creditor, fontSize = 15.sp, color = colors.textPrimary)
                if (type.isNotEmpty()) Text(type, fontSize = 12.sp, color = colors.textTertiary)
            }
            Text(amount, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
        }
        if (actionable) {
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text(loc(L10nKey.commonEdit)) }, onClick = { menu = false; onClick() })
                onPayFixed?.let { DropdownMenuItem(text = { Text(loc(L10nKey.schuldenAsFixed)) }, leadingIcon = { Icon(Icons.Rounded.CalendarMonth, null, tint = KontivaTheme.accent, modifier = Modifier.size(20.dp)) }, onClick = { menu = false; it() }) }
                onPayBill?.let { DropdownMenuItem(text = { Text(loc(L10nKey.schuldenAsBill)) }, leadingIcon = { Icon(Icons.Rounded.ReceiptLong, null, tint = KontivaTheme.accent, modifier = Modifier.size(20.dp)) }, onClick = { menu = false; it() }) }
                onPayVariable?.let { DropdownMenuItem(text = { Text(loc(L10nKey.schuldenAsVariable)) }, leadingIcon = { Icon(Icons.Rounded.Tune, null, tint = KontivaTheme.accent, modifier = Modifier.size(20.dp)) }, onClick = { menu = false; it() }) }
                DropdownMenuItem(text = { Text(loc(L10nKey.commonDelete), color = colors.swissRed) }, onClick = { menu = false; onDelete() })
            }
        }
    }
}

@Composable
private fun Tip(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, title: String, body: String) {
    val colors = KontivaTheme.colors
    Row(verticalAlignment = Alignment.Top) {
        Box(Modifier.size(30.dp).clip(CircleShape).background(tint.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.size(KontivaTheme.spaceSm))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
            Text(body, fontSize = 12.sp, color = colors.textSecondary)
        }
    }
}

/** Warm banner shown when debts/overdue exist — reassures and frames the steps below. */
@Composable
private fun EncouragementBanner(loc: ch.kontiva.android.core.l10n.Localizer) {
    val colors = KontivaTheme.colors
    Surface(shape = RoundedCornerShape(KontivaTheme.radiusCard), color = KontivaTheme.accent.copy(alpha = 0.08f), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(KontivaTheme.spaceMd), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).clip(CircleShape).background(KontivaTheme.accent.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.DirectionsWalk, contentDescription = null, tint = KontivaTheme.accent, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.size(KontivaTheme.spaceSm))
            Column(Modifier.weight(1f)) {
                Text(loc(L10nKey.schuldenEncourageTitle), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                Text(loc(L10nKey.schuldenEncourageBody), fontSize = 12.sp, color = colors.textSecondary)
            }
        }
    }
}

/** Reassuring empty state with a clear, discoverable way to record a debt. */
@Composable
private fun SchuldenEmptyCard(loc: ch.kontiva.android.core.l10n.Localizer, onAdd: () -> Unit) {
    val colors = KontivaTheme.colors
    Surface(shape = RoundedCornerShape(KontivaTheme.radiusCard), color = colors.cardSurface, modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(KontivaTheme.spaceLg).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(KontivaTheme.spaceMd),
        ) {
            Box(Modifier.size(56.dp).clip(CircleShape).background(colors.positive.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = colors.positive, modifier = Modifier.size(30.dp))
            }
            Text(loc(L10nKey.schuldenEmpty), fontSize = 14.sp, color = colors.textSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            OutlinedButton(
                onClick = onAdd,
                shape = RoundedCornerShape(KontivaTheme.radiusControl),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = KontivaTheme.accent),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(KontivaTheme.spaceXs))
                Text(loc(L10nKey.schuldenAddCta), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DebtSheet(
    onDismiss: () -> Unit,
    onSave: (String, Money, DebtType, java.time.LocalDate?, String?, String?) -> Unit,
    initialCreditor: String = "",
    initialAmount: String = "",
    initialType: DebtType = DebtType.OPEN_CLAIM,
    initialDate: java.time.LocalDate? = null,
    initialReference: String = "",
    initialNotes: String = "",
) {
    val loc = LocalLocalizer.current
    val colors = KontivaTheme.colors
    var creditor by remember { mutableStateOf(initialCreditor) }
    var amountText by remember { mutableStateOf(initialAmount) }
    var type by remember { mutableStateOf(initialType) }
    var menu by remember { mutableStateOf(false) }
    var hasDate by remember { mutableStateOf(initialDate != null) }
    var date by remember { mutableStateOf(initialDate ?: java.time.LocalDate.now()) }
    var reference by remember { mutableStateOf(initialReference) }
    var notes by remember { mutableStateOf(initialNotes) }
    val amount = Money.parse(amountText)
    val canSave = creditor.isNotBlank() && amount != null && !amount.isZero

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = colors.cardSurface, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = KontivaTheme.spaceLg).padding(bottom = KontivaTheme.spaceLg).navigationBarsPadding().imePadding(),
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
            Row(Modifier.fillMaxWidth().padding(vertical = KontivaTheme.spaceXs), verticalAlignment = Alignment.CenterVertically) {
                Text(loc(L10nKey.debtDate), color = colors.textPrimary)
                Spacer(Modifier.weight(1f))
                Switch(checked = hasDate, onCheckedChange = { hasDate = it })
            }
            if (hasDate) DatePickerRow(loc(L10nKey.debtDate), date) { date = it }
            OutlinedTextField(reference, { reference = it }, label = { Text(loc(L10nKey.debtReference)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(notes, { notes = it }, label = { Text(loc(L10nKey.formNotes)) }, minLines = 2, maxLines = 4, modifier = Modifier.fillMaxWidth())
            Button(
                onClick = {
                    if (canSave) onSave(
                        creditor.trim(), amount!!, type,
                        if (hasDate) date else null,
                        reference.trim().ifBlank { null },
                        notes.trim().ifBlank { null },
                    )
                },
                enabled = canSave, modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(KontivaTheme.radiusControl),
                colors = ButtonDefaults.buttonColors(containerColor = KontivaTheme.accent, contentColor = Color.White),
            ) { Text(loc(L10nKey.commonSave), fontWeight = FontWeight.SemiBold) }
        }
    }
}
