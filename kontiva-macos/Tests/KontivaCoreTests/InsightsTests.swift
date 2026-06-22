import XCTest
@testable import KontivaCore

final class InsightsTests: XCTestCase {

    private let calendar = Calendar.swiss
    private func date(_ y: Int, _ m: Int, _ d: Int) -> Date {
        calendar.date(from: DateComponents(year: y, month: m, day: d))!
    }
    private func ref() -> Date { date(2026, 6, 15) }

    private func analyze(income: [Income] = [],
                         fixed: [RecurringFixedExpense] = [],
                         variable: [VariableMonthlyBudget] = [],
                         bills: [OneOffBill] = [],
                         savings: [SavingsGoal] = []) -> [Insight] {
        let a = AvailabilityEngine.compute(incomes: income, fixedCosts: fixed,
                                           variableBudgets: variable, bills: bills,
                                           asOf: ref(), calendar: calendar)
        return InsightEngine.analyze(incomes: income, fixedCosts: fixed, variableBudgets: variable,
                                     bills: bills, savingsGoals: savings, availability: a,
                                     asOf: ref(), calendar: calendar)
    }

    // MARK: - Money.percent (integer only)

    func testPercentIsIntegerMath() {
        XCTAssertEqual(Money.parse("1'850.00")!.percent(of: Money.parse("6'500.00")!), 28) // 28.46 → 28
        XCTAssertEqual(Money(rappen: 5_000).percent(of: Money(rappen: 10_000)), 50)
        XCTAssertEqual(Money(rappen: 100).percent(of: .zero), 0) // guard
    }

    // MARK: - Rules

    func testOverspendingDetected() {
        let insights = analyze(income: [Income(label: "L", monthlyNet: Money.parse("3'000.00")!)],
                               fixed: [RecurringFixedExpense(name: "Miete", monthlyAmount: Money.parse("3'500.00")!, category: .rent)])
        XCTAssertTrue(insights.contains { if case .overspending(let d) = $0 { return d == Money.parse("500.00") }; return false })
        // Warnings sort first.
        XCTAssertEqual(insights.first?.severity, .warning)
    }

    func testHealthySurplusAndSavings() {
        let insights = analyze(
            income: [Income(label: "L", monthlyNet: Money.parse("6'500.00")!)],
            fixed: [RecurringFixedExpense(name: "Misc", monthlyAmount: Money.parse("1'000.00")!)],
            savings: [SavingsGoal(name: "Spar", target: Money.parse("10'000.00")!,
                                  monthlyContribution: Money.parse("800.00")!)])
        XCTAssertTrue(insights.contains { if case .healthySurplus = $0 { return true }; return false })
        XCTAssertTrue(insights.contains { if case .goodSavingsRate = $0 { return true }; return false })
    }

    func testHighHousingRatio() {
        let insights = analyze(
            income: [Income(label: "L", monthlyNet: Money.parse("5'000.00")!)],
            fixed: [RecurringFixedExpense(name: "Miete", monthlyAmount: Money.parse("1'800.00")!, category: .rent)])
        // 1800/5000 = 36% ≥ 33 → highHousing
        XCTAssertTrue(insights.contains { if case .highHousing(_, let pct) = $0 { return pct == 36 }; return false })
    }

    func testOverdueBillsWarning() {
        let bill = OneOffBill(provider: "X", amount: Money.parse("200.00")!,
                              dueDate: date(2026, 5, 20), status: .open) // overdue vs June
        let insights = analyze(income: [Income(label: "L", monthlyNet: Money.parse("6'000.00")!)],
                               bills: [bill])
        XCTAssertTrue(insights.contains { if case .overdueBills(let c, let t) = $0 { return c == 1 && t == Money.parse("200.00") }; return false })
    }

    func testNoSavingsTip() {
        let insights = analyze(income: [Income(label: "L", monthlyNet: Money.parse("6'000.00")!)],
                               fixed: [RecurringFixedExpense(name: "Misc", monthlyAmount: Money.parse("1'000.00")!)])
        XCTAssertTrue(insights.contains { if case .noSavings = $0 { return true }; return false })
    }

    func testLargestFixedNeedsTwoItems() {
        let one = analyze(income: [Income(label: "L", monthlyNet: Money.parse("6'000.00")!)],
                          fixed: [RecurringFixedExpense(name: "Miete", monthlyAmount: Money.parse("1'800.00")!)])
        XCTAssertFalse(one.contains { if case .largestFixedCost = $0 { return true }; return false })

        let two = analyze(income: [Income(label: "L", monthlyNet: Money.parse("6'000.00")!)],
                          fixed: [RecurringFixedExpense(name: "Miete", monthlyAmount: Money.parse("1'800.00")!),
                                  RecurringFixedExpense(name: "KK", monthlyAmount: Money.parse("340.00")!)])
        XCTAssertTrue(two.contains { if case .largestFixedCost(let n, _, _) = $0 { return n == "Miete" }; return false })
    }

    func testEmptyBudgetIsHealthyFallback() {
        let insights = analyze()
        XCTAssertEqual(insights, [.allHealthy])
    }
}
