package ch.kontiva.android.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.kontiva.android.core.BillClassifier
import ch.kontiva.android.core.BillSort
import ch.kontiva.android.core.BillState
import ch.kontiva.android.core.BillStatus
import ch.kontiva.android.core.Money
import ch.kontiva.android.core.OneOffBill
import ch.kontiva.android.core.total
import ch.kontiva.android.core.l10n.L10nKey
import ch.kontiva.android.core.l10n.LocalLocalizer
import ch.kontiva.android.ui.KontivaViewModel
import ch.kontiva.android.ui.pressScale
import ch.kontiva.android.ui.theme.KontivaTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val dateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

@Composable
fun BillsScreen(vm: KontivaViewModel) {
    val loc = LocalLocalizer.current
    val colors = KontivaTheme.colors
    val bills = vm.dataset.bills
    var showSheet by remember { mutableStateOf(false) }
    var editBill by remember { mutableStateOf<OneOffBill?>(null) }
    var sortMenu by remember { mutableStateOf(false) }
    val sort = vm.settings.billSort

    val month = vm.selectedMonth
    val overdue = bills.filter { BillClassifier.state(it, month) == BillState.OVERDUE }
    val due = bills.filter { BillClassifier.state(it, month) == BillState.DUE_THIS_MONTH }
    val future = bills.filter { BillClassifier.state(it, month) == BillState.FUTURE }
    val paid = bills.filter { BillClassifier.state(it, month) == BillState.PAID }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(KontivaTheme.spaceLg),
            verticalArrangement = Arrangement.spacedBy(KontivaTheme.spaceMd),
        ) {
        item { MonthHeader(loc(L10nKey.billsTitle), vm) }
        if (bills.isEmpty()) {
            item { EmptyCard(loc(L10nKey.billsEmpty)) }
            return@LazyColumn
        }
        item {
            Surface(shape = RoundedCornerShape(KontivaTheme.radiusCard), color = colors.cardSurface, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(KontivaTheme.spaceMd)) {
                    Text(loc(L10nKey.billsOpenTotal).uppercase(), fontSize = 11.sp, color = colors.textTertiary)
                    Text((overdue + due + future).map { it.amount }.total().formattedCHF(), fontSize = 26.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    Spacer(Modifier.size(KontivaTheme.spaceSm))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        MiniLabel(loc(L10nKey.billsStateOverdue), overdue.map { it.amount }.total().formattedCHF(), if (overdue.isEmpty()) colors.textSecondary else colors.swissRed)
                        MiniLabel(loc(L10nKey.billsStateDueThisMonth), due.map { it.amount }.total().formattedCHF(), colors.warning)
                        MiniLabel(loc(L10nKey.billsStatusPaid), paid.map { it.amount }.total().formattedCHF(), colors.positive)
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
                Text(loc(L10nKey.sparenSortBy), fontSize = 12.sp, color = colors.textTertiary)
                Spacer(Modifier.size(KontivaTheme.spaceXs))
                Box(Modifier.clip(RoundedCornerShape(KontivaTheme.radiusControl)).clickable { sortMenu = true }.padding(horizontal = KontivaTheme.spaceSm, vertical = 4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(loc(sort.labelKey), fontSize = 13.sp, color = KontivaTheme.accent, fontWeight = FontWeight.Medium)
                        Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, tint = KontivaTheme.accent)
                    }
                    DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                        BillSort.entries.forEach { s ->
                            DropdownMenuItem(text = { Text(loc(s.labelKey)) }, onClick = { vm.setBillSort(s); sortMenu = false })
                        }
                    }
                }
            }
        }
        val onEdit: (OneOffBill) -> Unit = { editBill = it; showSheet = true }
        billSection(loc(L10nKey.billsStateDueThisMonth), sort.apply(due), vm, onEdit)
        billSection(loc(L10nKey.billsStateOverdue), sort.apply(overdue), vm, onEdit)
        billSection(loc(L10nKey.billsStateFuture), sort.apply(future), vm, onEdit)
        billSection(loc(L10nKey.billsStatusPaid), sort.apply(paid), vm, onEdit)
        }
        AddFab { editBill = null; showSheet = true }
    }

    if (showSheet) {
        val b = editBill
        BillSheet(
            onDismiss = { showSheet = false },
            onSave = { p, a, d, paidFlag, notes ->
                if (b != null) vm.updateBill(b.id, p, a, d, paidFlag, notes) else vm.addBill(p, a, d, paidFlag, notes)
                showSheet = false
            },
            initialProvider = b?.provider ?: "",
            initialAmount = b?.amount?.formattedCHF(false) ?: "",
            initialDate = b?.dueDate ?: LocalDate.now(),
            initialPaid = b?.status == BillStatus.PAID,
            initialNotes = b?.notes ?: "",
        )
    }
}

private fun LazyListScope.billSection(title: String, items: List<OneOffBill>, vm: KontivaViewModel, onEdit: (OneOffBill) -> Unit) {
    if (items.isEmpty()) return
    item {
        Surface(shape = RoundedCornerShape(KontivaTheme.radiusCard), color = KontivaTheme.colors.cardSurface, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(KontivaTheme.spaceMd)) {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = KontivaTheme.colors.textSecondary)
                items.forEach { b -> BillRow(b, onEdit = { onEdit(b) }, onToggle = { vm.toggleBillPaid(b.id) }, onDelete = { vm.deleteBill(b.id) }) }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BillRow(bill: OneOffBill, onEdit: () -> Unit, onToggle: () -> Unit, onDelete: () -> Unit) {
    val colors = KontivaTheme.colors
    val isPaid = bill.status == BillStatus.PAID
    var menu by remember { mutableStateOf(false) }
    Box {
    Row(
        Modifier
            .fillMaxWidth()
            .pressScale()
            .combinedClickable(onClick = onEdit, onLongClick = { menu = true })
            .padding(vertical = KontivaTheme.spaceSm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(24.dp).clip(CircleShape)
                .background(if (isPaid) colors.positive else Color.Transparent)
                .border(1.5.dp, if (isPaid) colors.positive else colors.softBorder, CircleShape)
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.Center,
        ) {
            if (isPaid) Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
        }
        Spacer(Modifier.size(KontivaTheme.spaceSm))
        Column(Modifier.weight(1f)) {
            Text(bill.provider, fontSize = 15.sp, color = colors.textPrimary)
            Text(bill.dueDate.format(dateFmt), fontSize = 12.sp, color = colors.textTertiary)
        }
        Text(
            bill.amount.formattedCHF(),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = if (isPaid) colors.textTertiary else colors.textPrimary,
            textDecoration = if (isPaid) TextDecoration.LineThrough else null,
        )
    }
        RowActionsMenu(menu, { menu = false }, onEdit, onDelete)
    }
}

@Composable
private fun MiniLabel(label: String, value: String, color: Color) {
    Column {
        Text(label, fontSize = 10.sp, color = KontivaTheme.colors.textTertiary)
        Text(value, fontSize = 13.sp, color = color, fontWeight = FontWeight.Medium)
    }
}

@Composable
internal fun EmptyCard(text: String) {
    Surface(shape = RoundedCornerShape(KontivaTheme.radiusCard), color = KontivaTheme.colors.cardSurface, modifier = Modifier.fillMaxWidth()) {
        Text(text, color = KontivaTheme.colors.textSecondary, modifier = Modifier.padding(KontivaTheme.spaceLg))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BillSheet(
    onDismiss: () -> Unit,
    onSave: (String, Money, LocalDate, Boolean, String?) -> Unit,
    initialProvider: String = "",
    initialAmount: String = "",
    initialDate: LocalDate = LocalDate.now(),
    initialPaid: Boolean = false,
    initialNotes: String = "",
) {
    val loc = LocalLocalizer.current
    val colors = KontivaTheme.colors
    var provider by remember { mutableStateOf(initialProvider) }
    var amountText by remember { mutableStateOf(initialAmount) }
    var dueDate by remember { mutableStateOf(initialDate) }
    var paid by remember { mutableStateOf(initialPaid) }
    var notes by remember { mutableStateOf(initialNotes) }
    var showPicker by remember { mutableStateOf(false) }
    val amount = Money.parse(amountText)
    val canSave = provider.isNotBlank() && amount != null && !amount.isZero

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.cardSurface,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = KontivaTheme.spaceLg).padding(bottom = KontivaTheme.spaceLg).navigationBarsPadding().imePadding(),
            verticalArrangement = Arrangement.spacedBy(KontivaTheme.spaceMd),
        ) {
            Text(loc(L10nKey.billsTitle), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
            OutlinedTextField(provider, { provider = it }, label = { Text(loc(L10nKey.billsProvider)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                amountText, { amountText = it }, label = { Text(loc(L10nKey.formAmount)) }, prefix = { Text("CHF ") },
                singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth().clickable { showPicker = true }.padding(vertical = KontivaTheme.spaceSm), verticalAlignment = Alignment.CenterVertically) {
                Text(loc(L10nKey.billsDueDate), color = colors.textSecondary)
                Spacer(Modifier.weight(1f))
                Text(dueDate.format(dateFmt), color = KontivaTheme.accent, fontWeight = FontWeight.Medium)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(KontivaTheme.spaceXs)) {
                StatusChip(loc(L10nKey.billsStatusOpen), !paid) { paid = false }
                StatusChip(loc(L10nKey.billsStatusPaid), paid) { paid = true }
            }
            OutlinedTextField(notes, { notes = it }, label = { Text(loc(L10nKey.formNotes)) }, minLines = 2, maxLines = 4, modifier = Modifier.fillMaxWidth())
            Button(
                onClick = { if (canSave) onSave(provider.trim(), amount!!, dueDate, paid, notes.trim().ifBlank { null }) },
                enabled = canSave, modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(KontivaTheme.radiusControl),
                colors = ButtonDefaults.buttonColors(containerColor = KontivaTheme.accent, contentColor = Color.White),
            ) { Text(loc(L10nKey.commonSave), fontWeight = FontWeight.SemiBold) }
        }
    }

    if (showPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = dueDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { dueDate = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                    showPicker = false
                }) { Text(loc(L10nKey.commonSave)) }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text(loc(L10nKey.commonCancel)) } },
        ) { DatePicker(state = state) }
    }
}

@Composable
private fun StatusChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = KontivaTheme.colors
    Box(
        Modifier
            .clip(RoundedCornerShape(KontivaTheme.radiusControl))
            .background(if (selected) KontivaTheme.accent.copy(alpha = 0.14f) else colors.pageBackground)
            .border(1.dp, if (selected) KontivaTheme.accent else colors.softBorder, RoundedCornerShape(KontivaTheme.radiusControl))
            .clickable(onClick = onClick)
            .padding(horizontal = KontivaTheme.spaceMd, vertical = KontivaTheme.spaceSm),
    ) {
        Text(label, color = if (selected) KontivaTheme.accent else colors.textSecondary, fontWeight = FontWeight.Medium)
    }
}
