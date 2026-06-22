import Foundation

/// How prominent an insight is. Drives ordering, colour, and icon in the UI.
public enum InsightSeverity: String, Sendable, Equatable {
    case warning   // needs attention (overspending, overdue bills)
    case tip       // an opportunity to improve
    case info      // a neutral observation about where money goes
    case positive  // something going well

    var priority: Int {
        switch self {
        case .warning: return 0
        case .tip:     return 1
        case .info:    return 2
        case .positive:return 3
        }
    }
}

/// A single budget insight. Pure data with the numbers already computed — the UI
/// turns each case into a localized title + detail (see `KontivaUI`).
public enum Insight: Equatable, Sendable, Identifiable {
    case overspending(deficit: Money)
    case tightBudget(available: Money, pct: Int)
    case healthySurplus(available: Money, pct: Int)
    case highFixedBurden(total: Money, pct: Int)
    case highHousing(amount: Money, pct: Int)
    case largestFixedCost(name: String, amount: Money, pctOfFixed: Int)
    case largestVariable(name: String, amount: Money)
    case overdueBills(count: Int, total: Money)
    case noSavings
    case goodSavingsRate(monthly: Money, pct: Int)
    case allHealthy

    public var id: String {
        switch self {
        case .overspending:    return "overspending"
        case .tightBudget:     return "tightBudget"
        case .healthySurplus:  return "healthySurplus"
        case .highFixedBurden: return "highFixedBurden"
        case .highHousing:     return "highHousing"
        case .largestFixedCost:return "largestFixedCost"
        case .largestVariable: return "largestVariable"
        case .overdueBills:    return "overdueBills"
        case .noSavings:       return "noSavings"
        case .goodSavingsRate: return "goodSavingsRate"
        case .allHealthy:      return "allHealthy"
        }
    }

    public var severity: InsightSeverity {
        switch self {
        case .overspending, .overdueBills:       return .warning
        case .highFixedBurden, .highHousing,
             .noSavings, .tightBudget:           return .tip
        case .largestFixedCost, .largestVariable:return .info
        case .healthySurplus, .goodSavingsRate,
             .allHealthy:                        return .positive
        }
    }
}

/// Rule-based analysis of the current budget snapshot. Swiss-flavoured thresholds
/// (e.g. housing ≤ ~⅓ of income, save ≥ ~10%). No history is required; this
/// describes *this month's plan*. Month-over-month comparison is a future addition
/// that needs stored history.
public enum InsightEngine {

    public static func analyze(
        incomes: [Income],
        fixedCosts: [RecurringFixedExpense],
        variableBudgets: [VariableMonthlyBudget],
        bills: [OneOffBill],
        savingsGoals: [SavingsGoal],
        availability: MonthlyAvailability,
        asOf reference: Date = Date(),
        calendar: Calendar = .swiss
    ) -> [Insight] {

        var insights: [Insight] = []
        let income = availability.netIncomeThisMonth
        let available = availability.available

        // Available balance health.
        if available.isNegative {
            insights.append(.overspending(deficit: Money(rappen: -available.rappen)))
        } else if income.isPositive {
            let pct = available.percent(of: income)
            if pct < 10 {
                insights.append(.tightBudget(available: available, pct: pct))
            } else if pct >= 20 {
                insights.append(.healthySurplus(available: available, pct: pct))
            }
        }

        // Only fixed costs active this month are considered (a finished standing
        // order no longer burdens the budget).
        let activeFixed = fixedCosts.filter { $0.isActive(in: reference, calendar: calendar) }

        // Fixed-cost burden.
        let totalFixed = activeFixed.map(\.monthlyAmount).total()
        if income.isPositive && totalFixed.isPositive {
            let pct = totalFixed.percent(of: income)
            if pct >= 50 { insights.append(.highFixedBurden(total: totalFixed, pct: pct)) }
        }

        // Housing ratio (rent-category fixed costs vs income; Swiss guideline ~⅓).
        let rent = activeFixed.filter { $0.category == .rent }.map(\.monthlyAmount).total()
        if rent.isPositive && income.isPositive {
            let pct = rent.percent(of: income)
            if pct >= 33 { insights.append(.highHousing(amount: rent, pct: pct)) }
        }

        // Largest fixed cost (only worth noting when there are several).
        if activeFixed.count >= 2,
           let largest = activeFixed.max(by: { $0.monthlyAmount < $1.monthlyAmount }),
           largest.monthlyAmount.isPositive {
            let pctOfFixed = largest.monthlyAmount.percent(of: totalFixed)
            insights.append(.largestFixedCost(name: largest.name, amount: largest.monthlyAmount,
                                              pctOfFixed: pctOfFixed))
        }

        // Largest variable budget.
        if variableBudgets.count >= 2,
           let largest = variableBudgets.max(by: { $0.plannedAmount < $1.plannedAmount }),
           largest.plannedAmount.isPositive {
            insights.append(.largestVariable(name: largest.name, amount: largest.plannedAmount))
        }

        // Overdue bills.
        let overdue = bills.filter {
            BillClassifier.state(of: $0, asOf: reference, calendar: calendar) == .overdue
        }
        if !overdue.isEmpty {
            insights.append(.overdueBills(count: overdue.count, total: overdue.map(\.amount).total()))
        }

        // Savings.
        let monthlySavings = savingsGoals.compactMap(\.monthlyContribution).total()
        if income.isPositive {
            if monthlySavings.isZero && available.isPositive && !available.isZero {
                insights.append(.noSavings)
            } else if monthlySavings.isPositive {
                let pct = monthlySavings.percent(of: income)
                if pct >= 10 { insights.append(.goodSavingsRate(monthly: monthlySavings, pct: pct)) }
            }
        }

        if insights.isEmpty { insights.append(.allHealthy) }

        // Most important first; stable within a severity.
        return insights.enumerated()
            .sorted { ($0.element.severity.priority, $0.offset) < ($1.element.severity.priority, $1.offset) }
            .map(\.element)
    }
}
