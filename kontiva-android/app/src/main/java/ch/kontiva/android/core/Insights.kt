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
    data class ExtraIncomeThisMonth(val amount: Money) : Insight(InsightSeverity.POSITIVE, L10nKey.insightExtraIncome)
    data class SavingsGoalProgress(val name: String, val pct: Int, val saved: Money, val target: Money) : Insight(InsightSeverity.POSITIVE, L10nKey.insightGoalProgress)
    data class BillsDueSoon(val count: Int, val total: Money) : Insight(InsightSeverity.TIP, L10nKey.insightBillsDueSoon)
    data object GettingStarted : Insight(InsightSeverity.TIP, L10nKey.insightGettingStarted)
    data object AllHealthy : Insight(InsightSeverity.POSITIVE, L10nKey.insightAllHealthy)
}

/** Rule-based analysis of this month's plan (Swiss thresholds). 1:1 with iOS. */
object InsightEngine {
    fun analyze(
        incomes: List<Income>,
        fixedCosts: List<RecurringFixedExpense>,
        variableBudgets: List<VariableMonthlyBudget>,
        bills: List<OneOffBill>,
        savingsGoals: List<SavingsGoal>,
        availability: MonthlyAvailability,
        today: LocalDate = LocalDate.now(),
    ): List<Insight> {
        val out = mutableListOf<Insight>()
        // Only standing orders active this month feed the burden/largest-cost rules.
        val fixedCosts = fixedCosts.filter { it.isActive(today) }
        val income = availability.netIncomeThisMonth
        val available = availability.available

        // Brand-new plan: nudge the first useful step instead of a hollow "all good".
        if (incomes.isEmpty()) out.add(Insight.GettingStarted)

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

        // Unpaid bills still due this month — a gentle nudge before they go overdue.
        val dueSoon = bills.filter {
            it.status == BillStatus.OPEN && BillClassifier.state(it, today) != BillState.OVERDUE &&
                it.dueDate.year == today.year && it.dueDate.monthValue == today.monthValue
        }
        if (dueSoon.isNotEmpty()) out.add(Insight.BillsDueSoon(dueSoon.size, dueSoon.map { it.amount }.total()))

        // Extra income landing this month (13th-salary slice + bonuses) — encouraging.
        val extraIncome = incomes.map { AvailabilityEngine.netIncomeThisMonth(it, today) - it.monthlyNet }.total()
        if (extraIncome.isPositive) out.add(Insight.ExtraIncomeThisMonth(extraIncome))

        // A savings goal in reach — celebrate the momentum.
        savingsGoals.filter { it.hasTarget && it.contributesIn(today) }
            .map { it to it.progressPercent(today) }
            .filter { it.second >= 25 }
            .maxByOrNull { it.second }
            ?.let { (g, pct) -> out.add(Insight.SavingsGoalProgress(g.name, pct.coerceIn(0, 100), g.accumulated(today), g.target)) }

        if (out.isEmpty()) out.add(Insight.AllHealthy)
        return out.sortedWith(compareBy { it.severity.priority }) // stable
    }
}
