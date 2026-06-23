package ch.kontiva.android.ui.screen

import androidx.compose.foundation.background
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.kontiva.android.core.Money
import ch.kontiva.android.core.MonthlyAvailability
import ch.kontiva.android.core.l10n.L10nKey
import ch.kontiva.android.core.l10n.LocalLocalizer
import ch.kontiva.android.ui.KontivaViewModel
import ch.kontiva.android.ui.appearStagger
import ch.kontiva.android.ui.theme.KontivaTheme

@Composable
fun OverviewScreen(vm: KontivaViewModel) {
    val loc = LocalLocalizer.current
    val colors = KontivaTheme.colors
    val a = vm.availability
    val hasData = vm.dataset.incomes.isNotEmpty() || vm.dataset.fixedCosts.isNotEmpty() ||
        vm.dataset.variableBudgets.isNotEmpty()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(KontivaTheme.spaceLg),
        verticalArrangement = Arrangement.spacedBy(KontivaTheme.spaceMd),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(loc(L10nKey.navOverview), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                Spacer(Modifier.weight(1f))
                MonthSelector(vm)
            }
        }

        if (!hasData) {
            item {
                Surface(shape = RoundedCornerShape(KontivaTheme.radiusCard), color = colors.cardSurface, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        loc(L10nKey.overviewEmpty),
                        color = colors.textSecondary,
                        modifier = Modifier.padding(KontivaTheme.spaceLg),
                    )
                }
            }
            return@LazyColumn
        }

        item { Box(Modifier.fillMaxWidth().appearStagger(0)) { AvailableCard(a) } }
        item { Box(Modifier.fillMaxWidth().appearStagger(1)) { BreakdownCard(a) } }
    }
}

@Composable
private fun AvailableCard(a: MonthlyAvailability) {
    val loc = LocalLocalizer.current
    val colors = KontivaTheme.colors
    val statusKey = when {
        a.available.isNegative -> L10nKey.overviewStatusNegative
        a.available.percentOf(a.netIncomeThisMonth) < 10 -> L10nKey.overviewStatusTight
        else -> L10nKey.overviewStatusGood
    }
    val statusColor = when (statusKey) {
        L10nKey.overviewStatusNegative -> colors.swissRed
        L10nKey.overviewStatusTight -> colors.warning
        else -> colors.positive
    }
    Surface(shape = RoundedCornerShape(KontivaTheme.radiusCard), color = colors.cardSurface, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(KontivaTheme.spaceMd)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(loc(L10nKey.overviewAvailableThisMonth).uppercase(), fontSize = 11.sp, color = colors.textTertiary)
                Spacer(Modifier.weight(1f))
                Box(Modifier.background(statusColor.copy(alpha = 0.14f), RoundedCornerShape(20.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                    Text(loc(statusKey), fontSize = 11.sp, color = statusColor, fontWeight = FontWeight.Medium)
                }
            }
            AnimatedAmount(
                a.available.formattedCHF(),
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = if (a.available.isNegative) colors.swissRed else colors.textPrimary,
            )
        }
    }
}

@Composable
private fun BreakdownCard(a: MonthlyAvailability) {
    val loc = LocalLocalizer.current
    val colors = KontivaTheme.colors
    val avail = if (a.available.isPositive) a.available else Money.zero
    val rows = listOf(
        Triple(colors.chartFixed, loc(L10nKey.overviewRecurringFixed), a.recurringFixedCosts),
        Triple(colors.chartVariable, loc(L10nKey.overviewPlannedVariable), a.plannedVariableBudgets),
        Triple(colors.chartBills, loc(L10nKey.overviewBillsDueThisMonth), a.billsDueThisMonth + a.overdueOpenBills),
        Triple(colors.chartSavings, loc(L10nKey.overviewPlannedSavings), a.plannedSavings),
        Triple(colors.chartAvailable, loc(L10nKey.overviewAvailableThisMonth), avail),
    )
    val pct = avail.percentOf(a.netIncomeThisMonth)

    Surface(shape = RoundedCornerShape(KontivaTheme.radiusCard), color = colors.cardSurface, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(KontivaTheme.spaceMd), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.fillMaxWidth()) {
                Text("$pct% ${loc(L10nKey.overviewAllocationOf)}", fontSize = 12.sp, color = colors.textTertiary)
            }
            Spacer(Modifier.size(KontivaTheme.spaceMd))
            Donut(
                segments = rows.map { DonutSegment(it.first, it.third.rappen.coerceAtLeast(0)) },
                centerTop = loc(L10nKey.overviewAvailableThisMonth).uppercase(),
                centerValue = avail.formattedCHF(),
                centerBottom = "$pct%",
            )
            Spacer(Modifier.size(KontivaTheme.spaceMd))
            rows.forEach { (color, label, amount) ->
                LegendRow(color, label, amount.formattedCHF(), amount.percentOf(a.netIncomeThisMonth))
            }
        }
    }
}

@Composable
private fun LegendRow(color: Color, label: String, amount: String, percent: Int) {
    val colors = KontivaTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(vertical = KontivaTheme.spaceXs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Spacer(Modifier.size(KontivaTheme.spaceSm))
        Text(
            label, fontSize = 14.sp, color = colors.textPrimary,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(end = KontivaTheme.spaceXs),
        )
        Text(amount, fontSize = 14.sp, color = colors.textPrimary)
        Spacer(Modifier.size(KontivaTheme.spaceXs))
        Box(Modifier.background(colors.softBorder.copy(alpha = 0.5f), RoundedCornerShape(6.dp)).padding(horizontal = 5.dp, vertical = 1.dp)) {
            Text("$percent%", fontSize = 11.sp, color = colors.textSecondary)
        }
    }
}
