import XCTest
@testable import KontivaCore

/// The accumulated balance is derived from elapsed months × the monthly amount
/// (plus any starting balance), using exact integer Rappen — never floating point.
final class SavingsGoalTests: XCTestCase {

    private let cal = Calendar.swiss
    private func month(_ y: Int, _ m: Int) -> Date {
        cal.date(from: DateComponents(year: y, month: m, day: 1))!
    }

    func testContributionCountIsInclusiveOfStartAndCurrentMonth() {
        let g = SavingsGoal(name: "Auto", monthlyContribution: Money.parse("500.00"),
                            startDate: month(2026, 1))
        XCTAssertEqual(g.monthsContributed(asOf: month(2026, 1)), 1)  // first month counts
        XCTAssertEqual(g.monthsContributed(asOf: month(2026, 6)), 6)  // Jan…Jun
        XCTAssertEqual(g.monthsContributed(asOf: month(2026, 12)), 12)
    }

    func testFutureStartHasNoContributions() {
        let g = SavingsGoal(name: "Ferien", monthlyContribution: Money.parse("200.00"),
                            startDate: month(2026, 9))
        XCTAssertEqual(g.monthsContributed(asOf: month(2026, 6)), 0)
        XCTAssertEqual(g.accumulated(asOf: month(2026, 6)), .zero)
    }

    func testAccumulatedAddsStartingBalanceAndContributions() {
        // 1'000 starting + 500 × 6 months = 4'000.00, exact.
        let g = SavingsGoal(name: "Säule 3a", category: .retirement,
                            monthlyContribution: Money.parse("500.00"),
                            startingBalance: Money.parse("1'000.00")!,
                            startDate: month(2026, 1))
        XCTAssertEqual(g.accumulated(asOf: month(2026, 6)), Money.parse("4'000.00"))
    }

    func testProgressPercentUsesAccumulatedVsTarget() {
        let g = SavingsGoal(name: "Auto", target: Money.parse("10'000.00")!,
                            monthlyContribution: Money.parse("500.00"),
                            startDate: month(2026, 1))
        // 500 × 6 = 3'000 of 10'000 → 30%.
        XCTAssertEqual(g.progressPercent(asOf: month(2026, 6)), 30)
    }

    func testMonthlyContributionsReduceAvailableBalance() {
        let income = [Income(label: "Lohn", monthlyNet: Money.parse("6'000.00")!)]
        let savings = [SavingsGoal(name: "Auto", monthlyContribution: Money.parse("500.00"),
                                   startDate: month(2026, 1)),
                       SavingsGoal(name: "3a", monthlyContribution: Money.parse("500.00"),
                                   startDate: month(2026, 1))]
        let a = AvailabilityEngine.compute(incomes: income, fixedCosts: [], variableBudgets: [],
                                           bills: [], savingsGoals: savings, asOf: month(2026, 6))
        XCTAssertEqual(a.plannedSavings, Money.parse("1'000.00"))
        XCTAssertEqual(a.available, Money.parse("5'000.00"))   // 6'000 − 1'000 savings
    }
}
