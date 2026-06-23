package ch.kontiva.android.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import ch.kontiva.android.core.FixedExpenseCategory
import ch.kontiva.android.core.Money
import ch.kontiva.android.core.l10n.L10nKey
import ch.kontiva.android.core.l10n.LocalLocalizer
import ch.kontiva.android.ui.theme.KontivaTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

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

/** Income add/edit sheet: name + amount + optional 13th-salary instalment. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeSheet(
    initialName: String = "",
    initialAmount: String = "",
    initialThirteenth: String = "",
    onDismiss: () -> Unit,
    onSave: (name: String, amount: Money, thirteenth: Money?) -> Unit,
) {
    val loc = LocalLocalizer.current
    val colors = KontivaTheme.colors
    var name by remember { mutableStateOf(initialName) }
    var amountText by remember { mutableStateOf(initialAmount) }
    var hasThirteenth by remember { mutableStateOf(initialThirteenth.isNotBlank()) }
    var thirteenthText by remember { mutableStateOf(initialThirteenth) }

    val amount = Money.parse(amountText)
    val canSave = name.isNotBlank() && amount != null && !amount.isZero

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.cardSurface,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier.padding(horizontal = KontivaTheme.spaceLg).padding(bottom = KontivaTheme.spaceLg).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(KontivaTheme.spaceMd),
        ) {
            Text(loc(L10nKey.planningIncome), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
            OutlinedTextField(name, { name = it }, label = { Text(loc(L10nKey.formName)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                amountText, { amountText = it }, label = { Text(loc(L10nKey.formAmount)) }, prefix = { Text("CHF ") },
                singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth().padding(vertical = KontivaTheme.spaceXs), verticalAlignment = Alignment.CenterVertically) {
                Text(loc(L10nKey.formThirteenthAmount), color = colors.textPrimary)
                Spacer(Modifier.weight(1f))
                Switch(checked = hasThirteenth, onCheckedChange = { hasThirteenth = it })
            }
            if (hasThirteenth) {
                OutlinedTextField(
                    thirteenthText, { thirteenthText = it }, label = { Text(loc(L10nKey.formThirteenthAmount)) }, prefix = { Text("CHF ") },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(),
                )
            }
            Button(
                onClick = { if (canSave) onSave(name.trim(), amount!!, if (hasThirteenth) Money.parse(thirteenthText) else null) },
                enabled = canSave, modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(KontivaTheme.radiusControl),
                colors = ButtonDefaults.buttonColors(containerColor = KontivaTheme.accent, contentColor = Color.White),
            ) { Text(loc(L10nKey.commonSave), fontWeight = FontWeight.SemiBold) }
        }
    }
}

/** Fixed-cost add/edit sheet: name + amount + category + optional limited standing
 *  order (start month + instalments), 1:1 with the iOS FixedExpenseFormSheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FixedCostSheet(
    categories: List<FixedExpenseCategory>,
    onDismiss: () -> Unit,
    onSave: (name: String, amount: Money, category: FixedExpenseCategory, startMonth: LocalDate?, installments: Int?) -> Unit,
    initialName: String = "",
    initialAmount: String = "",
    initialCategory: FixedExpenseCategory? = null,
    initialLimited: Boolean = false,
    initialStartMonth: LocalDate? = null,
    initialInstallments: Int? = null,
) {
    val loc = LocalLocalizer.current
    val colors = KontivaTheme.colors
    var name by remember { mutableStateOf(initialName) }
    var amountText by remember { mutableStateOf(initialAmount) }
    var category by remember { mutableStateOf(initialCategory ?: categories.first()) }
    var menuOpen by remember { mutableStateOf(false) }
    var limited by remember { mutableStateOf(initialLimited) }
    var startMonth by remember { mutableStateOf(initialStartMonth ?: LocalDate.now().withDayOfMonth(1)) }
    var installments by remember { mutableStateOf(initialInstallments ?: 6) }
    var showDate by remember { mutableStateOf(false) }

    val amount = Money.parse(amountText)
    val canSave = name.isNotBlank() && amount != null && !amount.isZero && (!limited || installments >= 1)
    val monthFmt = DateTimeFormatter.ofPattern("LLLL yyyy", loc.language.locale)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.cardSurface,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .padding(horizontal = KontivaTheme.spaceLg)
                .padding(bottom = KontivaTheme.spaceLg)
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(KontivaTheme.spaceMd),
        ) {
            Text(loc(L10nKey.planningFixed), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
            OutlinedTextField(name, { name = it }, label = { Text(loc(L10nKey.formName)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                amountText, { amountText = it }, label = { Text(loc(L10nKey.formAmount)) }, prefix = { Text("CHF ") },
                singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(),
            )
            Row(
                Modifier.fillMaxWidth().clickable { menuOpen = true }.padding(vertical = KontivaTheme.spaceSm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(loc(L10nKey.formCategory), color = colors.textSecondary)
                Spacer(Modifier.weight(1f))
                Text(loc(category.labelKey), color = KontivaTheme.accent, fontWeight = FontWeight.Medium)
                Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, tint = colors.textTertiary)
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    categories.forEach { c ->
                        DropdownMenuItem(text = { Text(loc(c.labelKey)) }, onClick = { category = c; menuOpen = false })
                    }
                }
            }

            // Limited standing order (befristeter Dauerauftrag).
            Row(Modifier.fillMaxWidth().padding(vertical = KontivaTheme.spaceXs), verticalAlignment = Alignment.CenterVertically) {
                Text(loc(L10nKey.formLimitedDuration), color = colors.textPrimary)
                Spacer(Modifier.weight(1f))
                Switch(checked = limited, onCheckedChange = { limited = it })
            }
            if (limited) {
                Row(
                    Modifier.fillMaxWidth().clickable { showDate = true }.padding(vertical = KontivaTheme.spaceSm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(loc(L10nKey.formStartMonth), color = colors.textSecondary)
                    Spacer(Modifier.weight(1f))
                    Text(startMonth.format(monthFmt).replaceFirstChar { it.uppercase() }, color = KontivaTheme.accent, fontWeight = FontWeight.Medium)
                }
                Row(Modifier.fillMaxWidth().padding(vertical = KontivaTheme.spaceSm), verticalAlignment = Alignment.CenterVertically) {
                    Text(loc(L10nKey.formInstallments), color = colors.textSecondary)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { if (installments > 1) installments-- }) {
                        Icon(Icons.Rounded.Remove, contentDescription = "−", tint = KontivaTheme.accent)
                    }
                    Text("$installments", color = colors.textPrimary, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = KontivaTheme.spaceXs))
                    IconButton(onClick = { if (installments < 120) installments++ }) {
                        Icon(Icons.Rounded.Add, contentDescription = "+", tint = KontivaTheme.accent)
                    }
                }
            }
            Text(loc(L10nKey.formLimitedDurationHint), fontSize = 12.sp, color = colors.textTertiary)

            Button(
                onClick = {
                    if (canSave) onSave(
                        name.trim(), amount!!, category,
                        if (limited) startMonth.withDayOfMonth(1) else null,
                        if (limited) installments else null,
                    )
                },
                enabled = canSave, modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(KontivaTheme.radiusControl),
                colors = ButtonDefaults.buttonColors(containerColor = KontivaTheme.accent, contentColor = Color.White),
            ) { Text(loc(L10nKey.commonSave), fontWeight = FontWeight.SemiBold) }
        }
    }

    if (showDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = startMonth.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { startMonth = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().withDayOfMonth(1) }
                    showDate = false
                }) { Text(loc(L10nKey.commonSave)) }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text(loc(L10nKey.commonCancel)) } },
        ) { DatePicker(state = state) }
    }
}
