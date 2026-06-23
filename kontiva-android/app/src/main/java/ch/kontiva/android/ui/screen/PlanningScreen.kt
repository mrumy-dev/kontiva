package ch.kontiva.android.ui.screen

import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.kontiva.android.core.FixedExpenseCategory
import ch.kontiva.android.core.VariableBudgetCategory
import ch.kontiva.android.core.RecurringFixedExpense
import ch.kontiva.android.core.l10n.L10nKey
import ch.kontiva.android.core.l10n.LocalLocalizer
import ch.kontiva.android.core.l10n.Localizer
import ch.kontiva.android.ui.KontivaViewModel
import ch.kontiva.android.ui.pressScale
import ch.kontiva.android.ui.theme.KontivaTheme
import ch.kontiva.android.ui.theme.icon

private enum class Sheet { INCOME, FIXED, VARIABLE }

@Composable
fun PlanningScreen(vm: KontivaViewModel) {
    val loc = LocalLocalizer.current
    val colors = KontivaTheme.colors
    val d = vm.dataset
    val avail = vm.availability
    var sheet by remember { mutableStateOf<Sheet?>(null) }
    var editId by remember { mutableStateOf<String?>(null) }
    var initName by remember { mutableStateOf("") }
    var initAmt by remember { mutableStateOf("") }
    var initFixedCat by remember { mutableStateOf<FixedExpenseCategory?>(null) }
    var initVarCat by remember { mutableStateOf<VariableBudgetCategory?>(null) }
    var init13th by remember { mutableStateOf("") }
    var init13thModel by remember { mutableStateOf(ch.kontiva.android.core.ThirteenthSalaryModel.SEPARATE) }
    var initLimited by remember { mutableStateOf(false) }
    var initStartMonth by remember { mutableStateOf<java.time.LocalDate?>(null) }
    var initInstallments by remember { mutableStateOf<Int?>(null) }

    fun startAdd(k: Sheet) {
        editId = null; initName = ""; initAmt = ""; initFixedCat = null; initVarCat = null; init13th = ""
        init13thModel = ch.kontiva.android.core.ThirteenthSalaryModel.SEPARATE
        initLimited = false; initStartMonth = null; initInstallments = null; sheet = k
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(KontivaTheme.spaceLg),
        verticalArrangement = Arrangement.spacedBy(KontivaTheme.spaceMd),
    ) {
        item { MonthHeader(loc(L10nKey.planningTitle), vm) }
        item {
            SummaryCard(
                label = loc(L10nKey.planningBalance),
                value = avail.available.formattedCHF(),
                income = avail.netIncomeThisMonth.formattedCHF(),
                fixed = avail.recurringFixedCosts.formattedCHF(),
                variable = avail.plannedVariableBudgets.formattedCHF(),
            )
        }
        item {
            SectionCard(
                icon = Icons.Rounded.AttachMoney,
                title = loc(L10nKey.planningIncome),
                count = d.incomes.size,
                total = avail.netIncomeThisMonth.formattedCHF(),
                onAdd = { startAdd(Sheet.INCOME) },
            ) {
                d.incomes.forEach { e ->
                    EntryRow(
                        Icons.Rounded.AttachMoney, e.label, e.thirteenthAmount?.let { "+13. ${it.formattedCHF()}" }, e.monthlyNet.formattedCHF(),
                        onClick = {
                            editId = e.id; initName = e.label; initAmt = e.monthlyNet.formattedCHF(false)
                            init13th = e.thirteenthAmount?.formattedCHF(false) ?: ""; init13thModel = e.thirteenthModel; sheet = Sheet.INCOME
                        },
                        onDelete = { vm.deleteIncome(e.id) },
                    )
                }
            }
        }
        item {
            SectionCard(
                icon = Icons.Rounded.SwapHoriz,
                title = loc(L10nKey.planningFixed),
                count = d.fixedCosts.size,
                total = avail.recurringFixedCosts.formattedCHF(),
                explainer = loc(L10nKey.planningFixedExplainer),
                onAdd = { startAdd(Sheet.FIXED) },
            ) {
                d.fixedCosts.forEach { e ->
                    EntryRow(
                        e.category.icon(), e.name, fixedSubtitle(e, loc), e.monthlyAmount.formattedCHF(),
                        onClick = {
                            editId = e.id; initName = e.name; initAmt = e.monthlyAmount.formattedCHF(false); initFixedCat = e.category
                            initLimited = e.isLimited; initStartMonth = e.startMonth; initInstallments = e.installments; sheet = Sheet.FIXED
                        },
                        onDelete = { vm.deleteFixedCost(e.id) },
                    )
                }
            }
        }
        item {
            SectionCard(
                icon = Icons.Rounded.Tune,
                title = loc(L10nKey.planningVariable),
                count = d.variableBudgets.size,
                total = avail.plannedVariableBudgets.formattedCHF(),
                explainer = loc(L10nKey.planningVariableExplainer),
                onAdd = { startAdd(Sheet.VARIABLE) },
            ) {
                d.variableBudgets.forEach { e ->
                    EntryRow(
                        e.category.icon(), e.name, loc(e.category.labelKey), e.plannedAmount.formattedCHF(),
                        onClick = { editId = e.id; initName = e.name; initAmt = e.plannedAmount.formattedCHF(false); initVarCat = e.category; sheet = Sheet.VARIABLE },
                        onDelete = { vm.deleteVariableBudget(e.id) },
                    )
                }
            }
        }
    }

    when (sheet) {
        Sheet.INCOME -> IncomeSheet(
            initialName = initName, initialAmount = initAmt, initialThirteenth = init13th, initialModel = init13thModel,
            onDismiss = { sheet = null },
            onSave = { name, amount, thirteenth, model ->
                editId?.let { vm.updateIncome(it, name, amount, thirteenth, model) } ?: vm.addIncome(name, amount, thirteenth, model); sheet = null
            },
        )
        Sheet.FIXED -> FixedCostSheet(
            categories = FixedExpenseCategory.entries,
            initialName = initName, initialAmount = initAmt, initialCategory = initFixedCat,
            initialLimited = initLimited, initialStartMonth = initStartMonth, initialInstallments = initInstallments,
            onDismiss = { sheet = null },
            onSave = { name, amount, cat, startMonth, installments ->
                editId?.let { vm.updateFixedCost(it, name, amount, cat, startMonth, installments) }
                    ?: vm.addFixedCost(name, amount, cat, startMonth, installments)
                sheet = null
            },
        )
        Sheet.VARIABLE -> EntrySheet(
            title = loc(L10nKey.planningVariable),
            categories = VariableBudgetCategory.entries,
            categoryLabel = { loc(it.labelKey) },
            categoryIcon = { it.icon() },
            initialName = initName, initialAmount = initAmt, initialCategory = initVarCat,
            onDismiss = { sheet = null },
            onSave = { name, amount, cat ->
                val c = cat ?: VariableBudgetCategory.OTHER
                editId?.let { vm.updateVariableBudget(it, name, amount, c) } ?: vm.addVariableBudget(name, amount, c); sheet = null
            },
        )
        null -> Unit
    }
}

@Composable
private fun SummaryCard(label: String, value: String, income: String, fixed: String, variable: String) {
    val colors = KontivaTheme.colors
    Surface(shape = RoundedCornerShape(KontivaTheme.radiusCard), color = colors.cardSurface, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(KontivaTheme.spaceMd)) {
            Text(label.uppercase(), fontSize = 11.sp, color = colors.textTertiary)
            AnimatedAmount(value, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
            Spacer(Modifier.size(KontivaTheme.spaceSm))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MiniStat(KontivaTheme.colors.positive, income)
                MiniStat(colors.textSecondary, fixed)
                MiniStat(colors.textSecondary, variable)
            }
        }
    }
}

@Composable
private fun MiniStat(color: Color, value: String) {
    Text(value, fontSize = 13.sp, color = color, fontWeight = FontWeight.Medium)
}

@Composable
private fun SectionCard(
    icon: ImageVector,
    title: String,
    count: Int,
    total: String,
    explainer: String? = null,
    onAdd: () -> Unit,
    content: @Composable () -> Unit,
) {
    val loc = LocalLocalizer.current
    val colors = KontivaTheme.colors
    Surface(shape = RoundedCornerShape(KontivaTheme.radiusCard), color = colors.cardSurface, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(KontivaTheme.spaceMd).animateContentSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = KontivaTheme.accent, modifier = Modifier.size(22.dp))
                Spacer(Modifier.size(KontivaTheme.spaceXs))
                Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                Spacer(Modifier.weight(1f))
                if (count > 0) AnimatedAmount(total, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
            }
            content()
            Spacer(Modifier.size(KontivaTheme.spaceXs))
            Row(
                Modifier.fillMaxWidth().background(KontivaTheme.accent.copy(alpha = 0.08f), RoundedCornerShape(KontivaTheme.radiusControl))
                    .clickable(onClick = onAdd)
                    .padding(vertical = KontivaTheme.spaceSm),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, tint = KontivaTheme.accent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(KontivaTheme.spaceXxs))
                Text(loc(L10nKey.commonAdd), color = KontivaTheme.accent, fontWeight = FontWeight.Medium)
            }
            if (explainer != null) {
                Spacer(Modifier.size(KontivaTheme.spaceXs))
                Text(explainer, fontSize = 12.sp, color = colors.textTertiary)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EntryRow(icon: ImageVector, name: String, subtitle: String?, amount: String, onClick: () -> Unit, onDelete: () -> Unit) {
    val colors = KontivaTheme.colors
    var menu by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier
                .fillMaxWidth()
                .pressScale()
                .combinedClickable(onClick = onClick, onLongClick = { menu = true })
                .padding(vertical = KontivaTheme.spaceSm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(30.dp).background(KontivaTheme.accent.copy(alpha = 0.12f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = KontivaTheme.accent, modifier = Modifier.size(17.dp))
            }
            Spacer(Modifier.size(KontivaTheme.spaceSm))
            Column(Modifier.weight(1f)) {
                Text(name, fontSize = 15.sp, color = colors.textPrimary)
                if (subtitle != null) Text(subtitle, fontSize = 12.sp, color = colors.textTertiary)
            }
            Text(amount, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
        }
        RowActionsMenu(menu, { menu = false }, onClick, onDelete)
    }
}

/** Fixed-cost row subtitle: category, plus the standing-order window when limited
 *  (e.g. "Leasing · Dauerauftrag · 6× Juni 2026"). 1:1 with iOS fixedSubtitle. */
private fun fixedSubtitle(e: RecurringFixedExpense, loc: Localizer): String {
    val cat = loc(e.category.labelKey)
    val start = e.startMonth ?: return cat
    val count = e.installments ?: return cat
    if (!e.isLimited) return cat
    val fmt = java.time.format.DateTimeFormatter.ofPattern("LLLL yyyy", loc.language.locale)
    return "$cat · ${loc(L10nKey.planningStandingOrder)} · ${count}× ${start.format(fmt).replaceFirstChar { it.uppercase() }}"
}
