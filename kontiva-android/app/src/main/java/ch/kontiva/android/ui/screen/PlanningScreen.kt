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
import ch.kontiva.android.core.l10n.L10nKey
import ch.kontiva.android.core.l10n.LocalLocalizer
import ch.kontiva.android.ui.KontivaViewModel
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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(KontivaTheme.spaceLg),
        verticalArrangement = Arrangement.spacedBy(KontivaTheme.spaceMd),
    ) {
        item {
            Text(loc(L10nKey.planningTitle), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
        }
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
                onAdd = { sheet = Sheet.INCOME },
            ) {
                d.incomes.forEach { e ->
                    EntryRow(Icons.Rounded.AttachMoney, e.label, null, e.monthlyNet.formattedCHF()) { vm.deleteIncome(e.id) }
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
                onAdd = { sheet = Sheet.FIXED },
            ) {
                d.fixedCosts.forEach { e ->
                    EntryRow(e.category.icon(), e.name, loc(e.category.labelKey), e.monthlyAmount.formattedCHF()) { vm.deleteFixedCost(e.id) }
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
                onAdd = { sheet = Sheet.VARIABLE },
            ) {
                d.variableBudgets.forEach { e ->
                    EntryRow(e.category.icon(), e.name, loc(e.category.labelKey), e.plannedAmount.formattedCHF()) { vm.deleteVariableBudget(e.id) }
                }
            }
        }
    }

    when (sheet) {
        Sheet.INCOME -> EntrySheet<Unit>(
            title = loc(L10nKey.planningIncome),
            categories = null,
            categoryLabel = { "" },
            onDismiss = { sheet = null },
            onSave = { name, amount, _ -> vm.addIncome(name, amount); sheet = null },
        )
        Sheet.FIXED -> EntrySheet(
            title = loc(L10nKey.planningFixed),
            categories = FixedExpenseCategory.entries,
            categoryLabel = { loc(it.labelKey) },
            onDismiss = { sheet = null },
            onSave = { name, amount, cat -> vm.addFixedCost(name, amount, cat ?: FixedExpenseCategory.OTHER); sheet = null },
        )
        Sheet.VARIABLE -> EntrySheet(
            title = loc(L10nKey.planningVariable),
            categories = VariableBudgetCategory.entries,
            categoryLabel = { loc(it.labelKey) },
            onDismiss = { sheet = null },
            onSave = { name, amount, cat -> vm.addVariableBudget(name, amount, cat ?: VariableBudgetCategory.OTHER); sheet = null },
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
            Text(value, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
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
        Column(Modifier.padding(KontivaTheme.spaceMd)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = KontivaTheme.accent, modifier = Modifier.size(22.dp))
                Spacer(Modifier.size(KontivaTheme.spaceXs))
                Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                Spacer(Modifier.weight(1f))
                if (count > 0) Text(total, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
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
private fun EntryRow(icon: ImageVector, name: String, subtitle: String?, amount: String, onDelete: () -> Unit) {
    val colors = KontivaTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = {}, onLongClick = onDelete)
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
}
