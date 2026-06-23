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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.kontiva.android.core.Insight
import ch.kontiva.android.core.InsightSeverity
import ch.kontiva.android.core.l10n.L10nKey
import ch.kontiva.android.core.l10n.LocalLocalizer
import ch.kontiva.android.core.l10n.Localizer
import ch.kontiva.android.ui.KontivaViewModel
import ch.kontiva.android.ui.theme.KontivaTheme

@Composable
fun InsightsScreen(vm: KontivaViewModel, onBack: () -> Unit) {
    val loc = LocalLocalizer.current
    val colors = KontivaTheme.colors
    val insights = vm.insights

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(KontivaTheme.spaceLg),
        verticalArrangement = Arrangement.spacedBy(KontivaTheme.spaceMd),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "back", tint = colors.textPrimary) }
                Text(loc(L10nKey.navInsights), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
            }
        }
        item { Text(loc(L10nKey.insightsSubtitle), fontSize = 14.sp, color = colors.textSecondary) }
        items@ for (insight in insights) {
            item { InsightCard(insight, loc) }
        }
    }
}

@Composable
private fun InsightCard(insight: Insight, loc: Localizer) {
    val colors = KontivaTheme.colors
    val tint = when (insight.severity) {
        InsightSeverity.WARNING -> colors.swissRed
        InsightSeverity.TIP -> colors.warning
        InsightSeverity.INFO -> colors.chartFixed
        InsightSeverity.POSITIVE -> colors.positive
    }
    val icon: ImageVector = when (insight.severity) {
        InsightSeverity.WARNING -> Icons.Rounded.Warning
        InsightSeverity.TIP -> Icons.Rounded.Lightbulb
        InsightSeverity.INFO -> Icons.Rounded.Info
        InsightSeverity.POSITIVE -> Icons.Rounded.CheckCircle
    }
    Surface(shape = RoundedCornerShape(KontivaTheme.radiusCard), color = colors.cardSurface, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(KontivaTheme.spaceMd), verticalAlignment = Alignment.Top) {
            Box(Modifier.size(34.dp).clip(CircleShape).background(tint.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.size(KontivaTheme.spaceSm))
            Column(Modifier.weight(1f)) {
                Text(loc(insight.titleKey), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                val detail = detailFor(insight, loc)
                if (detail.isNotEmpty()) Text(detail, fontSize = 13.sp, color = colors.textSecondary)
            }
        }
    }
}

private fun detailFor(insight: Insight, loc: Localizer): String = when (insight) {
    is Insight.Overspending -> insight.deficit.formattedCHF()
    is Insight.TightBudget -> "${insight.available.formattedCHF()} · ${insight.pct}% ${loc(L10nKey.fragOfNetIncome)}"
    is Insight.HealthySurplus -> "${insight.available.formattedCHF()} · ${insight.pct}% ${loc(L10nKey.fragOfNetIncome)}"
    is Insight.HighFixedBurden -> "${insight.total.formattedCHF()} · ${insight.pct}% ${loc(L10nKey.fragOfNetIncome)}"
    is Insight.HighHousing -> "${loc(L10nKey.insightHousingHint)} (${insight.pct}%)"
    is Insight.LargestFixedCost -> "${insight.name} · ${insight.amount.formattedCHF()} · ${insight.pctOfFixed}% ${loc(L10nKey.fragOfFixedCosts)}"
    is Insight.LargestVariable -> "${insight.name} · ${insight.amount.formattedCHF()}"
    is Insight.OverdueBills -> "${insight.count} · ${insight.total.formattedCHF()}"
    Insight.NoSavings -> loc(L10nKey.insightNoSavingsDetail)
    is Insight.GoodSavingsRate -> "${insight.monthly.formattedCHF()} · ${insight.pct}% ${loc(L10nKey.fragOfNetIncome)}"
    is Insight.ExtraIncomeThisMonth -> "+${insight.amount.formattedCHF()}"
    is Insight.SavingsGoalProgress -> "${insight.name} · ${insight.saved.formattedCHF()} / ${insight.target.formattedCHF()} · ${insight.pct}%"
    is Insight.BillsDueSoon -> "${insight.count} · ${insight.total.formattedCHF()}"
    Insight.GettingStarted -> loc(L10nKey.insightGettingStartedDetail)
    Insight.AllHealthy -> loc(L10nKey.insightAllHealthyDetail)
}
