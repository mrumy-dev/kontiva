import XCTest
@testable import KontivaCore

/// A limited standing order (e.g. 6 tax instalments) only burdens the budget during
/// its window, then drops out automatically.
final class StandingOrderTests: XCTestCase {

    private let cal = Calendar.swiss
    private func month(_ y: Int, _ m: Int) -> Date {
        cal.date(from: DateComponents(year: y, month: m, day: 1))!
    }

    private func taxOrder() -> RecurringFixedExpense {
        // 6 × CHF 600 from June 2026.
        RecurringFixedExpense(name: "Steuern", monthlyAmount: Money(rappen: 60_000), category: .other,
                              startMonth: month(2026, 6), installments: 6)
    }

    func testActiveOnlyWithinWindow() {
        let order = taxOrder()
        XCTAssertFalse(order.isActive(in: month(2026, 5)))  // before start
        XCTAssertTrue(order.isActive(in: month(2026, 6)))   // instalment 1
        XCTAssertTrue(order.isActive(in: month(2026, 11)))  // instalment 6
        XCTAssertFalse(order.isActive(in: month(2026, 12))) // window over
        XCTAssertFalse(order.isActive(in: month(2027, 6)))  // next year, nothing
    }

    func testInstalmentNumbering() {
        let order = taxOrder()
        XCTAssertEqual(order.installmentNumber(in: month(2026, 6)), 1)
        XCTAssertEqual(order.installmentNumber(in: month(2026, 9)), 4)
        XCTAssertNil(order.installmentNumber(in: month(2026, 12)))
    }

    func testOpenEndedCostIsAlwaysActive() {
        let rent = RecurringFixedExpense(name: "Miete", monthlyAmount: Money(rappen: 195_000), category: .rent)
        XCTAssertFalse(rent.isLimited)
        XCTAssertTrue(rent.isActive(in: month(2030, 1)))
    }

    func testAvailabilityDropsTheOrderAfterItsWindow() {
        let income = [Income(label: "Lohn", monthlyNet: Money.parse("6'000.00")!)]
        let costs = [taxOrder()]  // 600/month, Jun–Nov 2026
        let inWindow = AvailabilityEngine.compute(incomes: income, fixedCosts: costs,
                                                  variableBudgets: [], bills: [], asOf: month(2026, 7))
        XCTAssertEqual(inWindow.recurringFixedCosts, Money.parse("600.00"))
        XCTAssertEqual(inWindow.available, Money.parse("5'400.00"))

        let afterWindow = AvailabilityEngine.compute(incomes: income, fixedCosts: costs,
                                                     variableBudgets: [], bills: [], asOf: month(2026, 12))
        XCTAssertEqual(afterWindow.recurringFixedCosts, .zero)       // dropped out
        XCTAssertEqual(afterWindow.available, Money.parse("6'000.00"))
    }
}
