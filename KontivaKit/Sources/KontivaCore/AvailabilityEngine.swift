import Foundation

/// The transparent breakdown shown on the Overview dashboard. Every line is an
/// exact `Money` value so the UI can display the calculation openly:
///
/// ```
/// Available this month =
///     Net income this month
///   − Recurring fixed costs
///   − Planned variable budgets
///   − Bills due this month (paid or unpaid — paying doesn't free up money)
///   − Overdue open bills
///   − Planned savings contributions   ("pay yourself first")
/// ```
///
/// Monthly savings contributions are treated as a committed outflow: money you set
/// aside each month is no longer freely available to spend, so it is subtracted
/// from the available balance just like a fixed cost.
public struct MonthlyAvailability: Equatable, Sendable {
    public let netIncomeThisMonth: Money
    /// The 13th-salary instalment, shown separately when the model is `.separate`.
    public let thirteenthShownSeparately: Money?
    public let recurringFixedCosts: Money
    public let plannedVariableBudgets: Money
    public let billsDueThisMonth: Money
    public let overdueOpenBills: Money
    public let plannedSavings: Money
    public let available: Money

    /// Sum of everything subtracted from net income.
    public var totalCommitted: Money {
        recurringFixedCosts + plannedVariableBudgets + billsDueThisMonth
            + overdueOpenBills + plannedSavings
    }
}

public enum AvailabilityEngine {

    /// Compute net income for the month from one income source, honouring the
    /// 13th-salary model. With `.separate` (default) the 13th is NOT added.
    public static func netIncomeThisMonth(_ income: Income, asOf month: Date = Date(),
                                          calendar: Calendar = .swiss) -> Money {
        var total = income.monthlyNet
        let m = calendar.component(.month, from: month) // 1…12
        if let th = income.thirteenthAmount, !th.isZero {
            switch income.thirteenthModel {
            case .separate:        break                          // excluded from monthly
            case .averagedMonthly: total = total + th.divided(by: 12)
            case .december:        if m == 12 { total = total + th }
            case .november:        if m == 11 { total = total + th }
            case .splitNovDec:
                let twelfth = th.divided(by: 12)
                if m == 11 { total = total + (th - twelfth) }     // 11/12
                else if m == 12 { total = total + twelfth }       // 1/12
            }
        }
        // Bonuses (Sonderzahlungen) that land in this month.
        for b in income.bonuses where b.month == m { total = total + b.amount }
        return total
    }

    /// Net income across multiple income sources, for the given month.
    public static func netIncomeThisMonth(_ incomes: [Income], asOf month: Date = Date(),
                                          calendar: Calendar = .swiss) -> Money {
        incomes.map { netIncomeThisMonth($0, asOf: month, calendar: calendar) }.total()
    }

    /// The 13th instalment to display separately (only when its model is `.separate`).
    public static func thirteenthShownSeparately(_ incomes: [Income]) -> Money? {
        let separate = incomes
            .filter { $0.thirteenthModel == .separate }
            .compactMap(\.thirteenthAmount)
        guard !separate.isEmpty else { return nil }
        let total = separate.total()
        return total.isZero ? nil : total
    }

    /// Full dashboard computation.
    public static func compute(
        incomes: [Income],
        fixedCosts: [RecurringFixedExpense],
        variableBudgets: [VariableMonthlyBudget],
        bills: [OneOffBill],
        savingsGoals: [SavingsGoal] = [],
        asOf reference: Date = Date(),
        calendar: Calendar = .swiss
    ) -> MonthlyAvailability {

        let netIncome = netIncomeThisMonth(incomes, asOf: reference, calendar: calendar)
        // Only fixed costs active in this month count — a limited standing order
        // (e.g. 6 tax instalments) drops out once its window ends.
        let fixed = fixedCosts
            .filter { $0.isActive(in: reference, calendar: calendar) }
            .map(\.monthlyAmount).total()
        let variable = variableBudgets.map(\.plannedAmount).total()
        let dueThisMonth = BillClassifier.amountDueInMonth(bills,
                                                 asOf: reference, calendar: calendar)
        let overdue = BillClassifier.amount(in: .overdue, bills: bills,
                                            asOf: reference, calendar: calendar)
        // Savings only cost from their start month onward (a future plan is .zero now).
        let savings = savingsGoals
            .filter { $0.contributesIn(reference, calendar: calendar) }
            .compactMap(\.monthlyContribution).total()

        let available = netIncome - fixed - variable - dueThisMonth - overdue - savings

        return MonthlyAvailability(
            netIncomeThisMonth: netIncome,
            thirteenthShownSeparately: thirteenthShownSeparately(incomes),
            recurringFixedCosts: fixed,
            plannedVariableBudgets: variable,
            billsDueThisMonth: dueThisMonth,
            overdueOpenBills: overdue,
            plannedSavings: savings,
            available: available
        )
    }
}
