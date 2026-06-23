package ch.kontiva.android.ui.screen

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.kontiva.android.core.Money
import java.time.format.DateTimeFormatter
import ch.kontiva.android.core.SavingsCategory
import ch.kontiva.android.core.SavingsGoal
import ch.kontiva.android.core.SavingsSort
import ch.kontiva.android.core.total
import ch.kontiva.android.core.l10n.L10nKey
import ch.kontiva.android.core.l10n.LocalLocalizer
import ch.kontiva.android.ui.KontivaViewModel
import ch.kontiva.android.ui.theme.KontivaTheme
import ch.kontiva.android.ui.theme.icon

@Composable
fun SavingsScreen(vm: KontivaViewModel) {
    val loc = LocalLocalizer.current
    val colors = KontivaTheme.colors
    val goals = vm.settings.savingsSort.apply(vm.dataset.savingsGoals, vm.selectedMonth)
    var showSheet by remember { mutableStateOf(false) }
    var editGoal by remember { mutableStateOf<SavingsGoal?>(null) }
    var sortMenu by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(KontivaTheme.spaceLg),
        verticalArrangement = Arrangement.spacedBy(KontivaTheme.spaceMd),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(loc(L10nKey.navSparen), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                Spacer(Modifier.weight(1f))
                MonthSelector(vm)
                IconButton(onClick = { editGoal = null; showSheet = true }) { Icon(Icons.Rounded.Add, contentDescription = null, tint = KontivaTheme.accent) }
            }
        }
        if (goals.isEmpty()) {
            item { EmptyCard(loc(L10nKey.sparenEmpty)) }
            return@LazyColumn
        }
        item {
            val accumulated = goals.map { it.accumulated(vm.selectedMonth) }.total()
            val monthly = goals.filter { it.contributesIn(vm.selectedMonth) }.mapNotNull { it.monthlyContribution }.total()
            Surface(shape = RoundedCornerShape(KontivaTheme.radiusCard), color = colors.cardSurface, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(KontivaTheme.spaceMd)) {
                    Text(loc(L10nKey.sparenAccumulatedTotal).uppercase(), fontSize = 11.sp, color = colors.textTertiary)
                    Text(accumulated.formattedCHF(), fontSize = 26.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    Spacer(Modifier.size(KontivaTheme.spaceSm))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(loc(L10nKey.sparenMonthlyTotal), fontSize = 10.sp, color = colors.textTertiary)
                            Text(monthly.formattedCHF(), fontSize = 13.sp, color = KontivaTheme.accent, fontWeight = FontWeight.Medium)
                        }
                        Column {
                            Text(loc(L10nKey.sparenGoalsLabel), fontSize = 10.sp, color = colors.textTertiary)
                            Text("${goals.size}", fontSize = 13.sp, color = colors.textPrimary, fontWeight = FontWeight.Medium)
                        }
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
                        Text(loc(vm.settings.savingsSort.labelKey), fontSize = 13.sp, color = KontivaTheme.accent, fontWeight = FontWeight.Medium)
                        Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, tint = KontivaTheme.accent)
                    }
                    DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                        SavingsSort.entries.forEach { s ->
                            DropdownMenuItem(text = { Text(loc(s.labelKey)) }, onClick = { vm.setSavingsSort(s); sortMenu = false })
                        }
                    }
                }
            }
        }
        for (g in goals) {
            item { GoalCard(g, vm.selectedMonth, onClick = { editGoal = g; showSheet = true }, onDelete = { vm.deleteSavingsGoal(g.id) }) }
        }
    }

    if (showSheet) {
        val g = editGoal
        SavingsSheet(
            onDismiss = { showSheet = false },
            onSave = { name, cat, monthly, starting, target, startDate ->
                if (g != null) vm.updateSavingsGoal(g.id, name, cat, monthly, starting, target, startDate)
                else vm.addSavingsGoal(name, cat, monthly, starting, target, startDate)
                showSheet = false
            },
            initialName = g?.name ?: "",
            initialCategory = g?.category ?: SavingsCategory.EMERGENCY,
            initialMonthly = g?.monthlyContribution?.formattedCHF(false) ?: "",
            initialStarting = g?.startingBalance?.takeIf { !it.isZero }?.formattedCHF(false) ?: "",
            initialTarget = g?.target?.takeIf { !it.isZero }?.formattedCHF(false) ?: "",
            initialStartDate = g?.startDate ?: java.time.LocalDate.now(),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GoalCard(g: SavingsGoal, month: java.time.LocalDate, onClick: () -> Unit, onDelete: () -> Unit) {
    val loc = LocalLocalizer.current
    val colors = KontivaTheme.colors
    val accumulated = g.accumulated(month)
    val months = g.monthsContributed(month)
    val monthFmt = DateTimeFormatter.ofPattern("LLLL yyyy", loc.language.locale)
    Surface(
        shape = RoundedCornerShape(KontivaTheme.radiusCard),
        color = colors.cardSurface,
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onDelete),
    ) {
        Column(Modifier.padding(KontivaTheme.spaceMd), verticalArrangement = Arrangement.spacedBy(KontivaTheme.spaceMd)) {
            // Header: icon, name + category, monthly contribution.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(38.dp).clip(CircleShape).background(KontivaTheme.accent.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                    Icon(g.category.icon(), contentDescription = null, tint = KontivaTheme.accent, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.size(KontivaTheme.spaceSm))
                Column(Modifier.weight(1f)) {
                    Text(g.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                    Text(loc(g.category.labelKey), fontSize = 12.sp, color = colors.textTertiary)
                }
                Spacer(Modifier.size(KontivaTheme.spaceSm))
                Column(horizontalAlignment = Alignment.End) {
                    Text((g.monthlyContribution ?: Money.zero).formattedCHF(), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                    Text(loc(L10nKey.sparenPerMonth), fontSize = 10.sp, color = colors.textTertiary)
                }
            }
            HorizontalDivider(color = colors.softBorder.copy(alpha = 0.4f))
            // Accumulated balance (shown for every goal, target or not) + progress ring.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(loc(L10nKey.sparenAccumulatedTotal), fontSize = 11.sp, color = colors.textSecondary)
                    Text(
                        "$months ${loc(L10nKey.sparenContributions)} · ${loc(L10nKey.sparenSince)} ${g.startDate.format(monthFmt).replaceFirstChar { it.uppercase() }}",
                        fontSize = 10.sp, color = colors.textTertiary,
                    )
                }
                Spacer(Modifier.size(KontivaTheme.spaceSm))
                Column(horizontalAlignment = Alignment.End) {
                    Text(accumulated.formattedCHF(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.positive)
                    if (g.hasTarget) {
                        Text("${loc(L10nKey.formTarget)}: ${g.target.formattedCHF()}", fontSize = 10.sp, color = colors.textTertiary)
                    } else {
                        Text(loc(L10nKey.sparenOpenEnded), fontSize = 10.sp, color = colors.textTertiary)
                    }
                }
                if (g.hasTarget) {
                    Spacer(Modifier.size(KontivaTheme.spaceSm))
                    ProgressRing(g.progressPercent(month))
                }
            }
        }
    }
}

@Composable
private fun ProgressRing(percent: Int, size: Dp = 48.dp) {
    val colors = KontivaTheme.colors
    val accent = KontivaTheme.accent
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val sw = this.size.minDimension * 0.12f
            val inset = sw / 2
            drawCircle(color = colors.softBorder.copy(alpha = 0.5f), radius = (this.size.minDimension - sw) / 2, style = Stroke(sw))
            drawArc(
                color = accent, startAngle = -90f,
                sweepAngle = 360f * (percent.coerceIn(0, 100) / 100f), useCenter = false,
                topLeft = Offset(inset, inset), size = Size(this.size.width - sw, this.size.height - sw),
                style = Stroke(sw, cap = StrokeCap.Round),
            )
        }
        Text("$percent%", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SavingsSheet(
    onDismiss: () -> Unit,
    onSave: (String, SavingsCategory, Money?, Money, Money, java.time.LocalDate) -> Unit,
    initialName: String = "",
    initialCategory: SavingsCategory = SavingsCategory.EMERGENCY,
    initialMonthly: String = "",
    initialStarting: String = "",
    initialTarget: String = "",
    initialStartDate: java.time.LocalDate = java.time.LocalDate.now(),
) {
    val loc = LocalLocalizer.current
    val colors = KontivaTheme.colors
    var name by remember { mutableStateOf(initialName) }
    var category by remember { mutableStateOf(initialCategory) }
    var menuOpen by remember { mutableStateOf(false) }
    var monthlyText by remember { mutableStateOf(initialMonthly) }
    var startingText by remember { mutableStateOf(initialStarting) }
    var targetText by remember { mutableStateOf(initialTarget) }
    var startDate by remember { mutableStateOf(initialStartDate) }
    val canSave = name.isNotBlank()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.cardSurface,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = KontivaTheme.spaceLg)
                .padding(bottom = KontivaTheme.spaceLg)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(KontivaTheme.spaceMd),
        ) {
            Text(loc(L10nKey.navSparen), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
            OutlinedTextField(name, { name = it }, label = { Text(loc(L10nKey.formName)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth().clickable { menuOpen = true }.padding(vertical = KontivaTheme.spaceSm), verticalAlignment = Alignment.CenterVertically) {
                Text(loc(L10nKey.formCategory), color = colors.textSecondary)
                Spacer(Modifier.weight(1f))
                Text(loc(category.labelKey), color = KontivaTheme.accent, fontWeight = FontWeight.Medium)
                Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, tint = colors.textTertiary)
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    SavingsCategory.entries.forEach { c ->
                        DropdownMenuItem(text = { Text(loc(c.labelKey)) }, onClick = { category = c; menuOpen = false })
                    }
                }
            }
            MoneyField(loc(L10nKey.formMonthlyContribution), monthlyText) { monthlyText = it }
            DatePickerRow(loc(L10nKey.formStartDate), startDate) { startDate = it }
            MoneyField(loc(L10nKey.formStartingBalance), startingText) { startingText = it }
            MoneyField(loc(L10nKey.formTarget), targetText) { targetText = it }
            Button(
                onClick = {
                    if (canSave) onSave(
                        name.trim(), category,
                        Money.parse(monthlyText), Money.parse(startingText) ?: Money.zero, Money.parse(targetText) ?: Money.zero, startDate,
                    )
                },
                enabled = canSave, modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(KontivaTheme.radiusControl),
                colors = ButtonDefaults.buttonColors(containerColor = KontivaTheme.accent, contentColor = androidx.compose.ui.graphics.Color.White),
            ) { Text(loc(L10nKey.commonSave), fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
private fun MoneyField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value, onChange, label = { Text(label) }, prefix = { Text("CHF ") }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(),
    )
}
