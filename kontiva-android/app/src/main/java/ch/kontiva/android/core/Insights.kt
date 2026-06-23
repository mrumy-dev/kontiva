package ch.kontiva.android.core

import ch.kontiva.android.core.l10n.L10nKey
import java.time.LocalDate

enum class InsightSeverity(val priority: Int) { WARNING(0), TIP(1), INFO(2), POSITIVE(3) }

/** A single budget insight with its numbers; the UI turns it into a localized card. */
sealed class Insight(val severity: InsightSeverity, val titleKey: L10nKey) {
    data class Overspending(val deficit: Money) : Insight(InsightSeverity.WARNING, L10nKey.insightOverspending)
    data class TightBudget(val available: Money, val pct: Int) : Insight(InsightSeverity.TIP, L10nKey.insightTightBudget)
    data class HealthySurplus(val available: Money, val pct: Int) : Insight(InsightSeverity.POSITIVE, L10nKey.insightHealthySurplus)
    data class HighFixedBurden(val total: Money, val pct: Int) : Insight(InsightSeverity.TIP, L10nKey.insightHighFixed)
    data class HighHousing(val amount: Money, val pct: Int) : Insight(InsightSeverity.TIP, L10nKey.insightHighHousing)
    data class LargestFixedCost(val name: String, val amount: Money, val pctOfFixed: Int) : Insight(InsightSeverity.INFO, L10nKey.insightLargestFixed)
    data class LargestVariable(val name: String, val amount: Money) : Insight(InsightSeverity.INFO, L10nKey.insightLargestVariable)
    data class OverdueBills(val count: Int, val total: Money) : Insight(InsightSeverity.WARNING, L10nKey.insightOverdue)
    data object NoSavings : Insight(InsightSeverity.TIP, L10nKey.insightNoSavings)
    data class GoodSavingsRate(val monthly: Money, val pct: Int) : Insight(InsightSeverity.POSITIVE, L10nKey.insightGoodSavings)
    data object AllHealthy : Insight(InsightSeverity.POSITIVE, L10nKey.insightAllHealthy)
}

/** Rule-based analysis of this month's plan (Swiss thresholds). 1:1 with iOS. */
object InsightEngine {
    fun analyze(
        fixedCosts: List<RecurringFixedExpense>,
        variableBudgets: List<VariableMonthlyBudget>,
        bills: List<OneOffBill>,
        savingsGoals: List<SavingsGoal>,
        availability: MonthlyAvailability,
        today: LocalDate = LocalDate.now(),
    ): List<Insight> {
        val out = mutableListOf<Insight>()
        val income = availability.netIncomeThisMonth
        val available = availability.available

        if (available.isNegative) {
            out.add(Insight.Overspending(-available))
        } else if (income.isPositive) {
            val pct = available.percentOf(income)
            if (pct < 10) out.add(Insight.TightBudget(available, pct))
            else if (pct >= 20) out.add(Insight.HealthySurplus(available, pct))
        }

        val totalFixed = fixedCosts.map { it.monthlyAmount }.total()
        if (income.isPositive && totalFixed.isPositive) {
            val pct = totalFixed.percentOf(income)
            if (pct >= 50) out.add(Insight.HighFixedBurden(totalFixed, pct))
        }

        val rent = fixedCosts.filter { it.category == FixedExpenseCategory.RENT }.map { it.monthlyAmount }.total()
        if (rent.isPositive && income.isPositive) {
            val pct = rent.percentOf(income)
            if (pct >= 33) out.add(Insight.HighHousing(rent, pct))
        }

        if (fixedCosts.size >= 2) {
            val largest = fixedCosts.maxByOrNull { it.monthlyAmount.rappen }
            if (largest != null && largest.monthlyAmount.isPositive) {
                out.add(Insight.LargestFixedCost(largest.name, largest.monthlyAmount, largest.monthlyAmount.percentOf(totalFixed)))
            }
        }
        if (variableBudgets.size >= 2) {
            val largest = variableBudgets.maxByOrNull { it.plannedAmount.rappen }
            if (largest != null && largest.plannedAmount.isPositive) out.add(Insight.LargestVariable(largest.name, largest.plannedAmount))
        }

        val overdue = bills.filter { BillClassifier.state(it, today) == BillState.OVERDUE }
        if (overdue.isNotEmpty()) out.add(Insight.OverdueBills(overdue.size, overdue.map { it.amount }.total()))

        val monthlySavings = savingsGoals.mapNotNull { it.monthlyContribution }.total()
        if (income.isPositive) {
            if (monthlySavings.isZero && available.isPositive && !available.isZero) out.add(Insight.NoSavings)
            else if (monthlySavings.isPositive) {
                val pct = monthlySavings.percentOf(income)
                if (pct >= 10) out.add(Insight.GoodSavingsRate(monthlySavings, pct))
            }
        }

        if (out.isEmpty()) out.add(Insight.AllHealthy)
        return out.sortedWith(compareBy { it.severity.priority }) // stable
    }
}
