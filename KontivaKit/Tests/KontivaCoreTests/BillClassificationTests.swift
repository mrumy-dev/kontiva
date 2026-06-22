import XCTest
@testable import KontivaCore

final class BillClassificationTests: XCTestCase {

    private let calendar = Calendar.swiss

    /// Fixed reference date for deterministic tests: 15 June 2026.
    private func reference() -> Date {
        calendar.date(from: DateComponents(year: 2026, month: 6, day: 15))!
    }

    private func date(_ y: Int, _ m: Int, _ d: Int) -> Date {
        calendar.date(from: DateComponents(year: y, month: m, day: d))!
    }

    private func bill(due: Date, status: BillStatus, amount: Int64 = 10_000) -> OneOffBill {
        OneOffBill(provider: "Test", amount: Money(rappen: amount), dueDate: due, status: status)
    }

    // MARK: - Each of the four states

    func testPaidDoesNotCount() {
        let b = bill(due: date(2026, 6, 10), status: .paid)
        let state = BillClassifier.state(of: b, asOf: reference(), calendar: calendar)
        XCTAssertEqual(state, .paid)
        XCTAssertFalse(state.countsAgainstCurrentMonth)
    }

    func testOpenDueThisMonthCounts() {
        let b = bill(due: date(2026, 6, 20), status: .open)
        let state = BillClassifier.state(of: b, asOf: reference(), calendar: calendar)
        XCTAssertEqual(state, .dueThisMonth)
        XCTAssertTrue(state.countsAgainstCurrentMonth)
    }

    func testOpenOverdueCounts() {
        // Due last month, still open.
        let b = bill(due: date(2026, 5, 28), status: .open)
        let state = BillClassifier.state(of: b, asOf: reference(), calendar: calendar)
        XCTAssertEqual(state, .overdue)
        XCTAssertTrue(state.countsAgainstCurrentMonth)
    }

    func testOpenFutureDoesNotCount() {
        let b = bill(due: date(2026, 7, 5), status: .open)
        let state = BillClassifier.state(of: b, asOf: reference(), calendar: calendar)
        XCTAssertEqual(state, .future)
        XCTAssertFalse(state.countsAgainstCurrentMonth)
    }

    // MARK: - Boundaries

    func testFirstDayOfMonthIsDueThisMonth() {
        let b = bill(due: date(2026, 6, 1), status: .open)
        XCTAssertEqual(BillClassifier.state(of: b, asOf: reference(), calendar: calendar), .dueThisMonth)
    }

    func testLastDayOfMonthIsDueThisMonth() {
        let b = bill(due: date(2026, 6, 30), status: .open)
        XCTAssertEqual(BillClassifier.state(of: b, asOf: reference(), calendar: calendar), .dueThisMonth)
    }

    func testPaidFutureBillStillPaid() {
        let b = bill(due: date(2026, 12, 1), status: .paid)
        XCTAssertEqual(BillClassifier.state(of: b, asOf: reference(), calendar: calendar), .paid)
    }

    // MARK: - Aggregation against the available balance

    func testOnlyOverdueAndDueThisMonthAffectBalance() {
        let bills = [
            bill(due: date(2026, 5, 28), status: .open,  amount: 100_00), // overdue → counts
            bill(due: date(2026, 6, 20), status: .open,  amount: 200_00), // due this month → counts
            bill(due: date(2026, 7, 5),  status: .open,  amount: 400_00), // future → excluded
            bill(due: date(2026, 6, 2),  status: .paid,  amount: 800_00), // paid → excluded
        ]
        let affecting = BillClassifier.amountAffectingCurrentMonth(bills, asOf: reference(), calendar: calendar)
        XCTAssertEqual(affecting, Money(rappen: 300_00)) // 100 + 200 only

        XCTAssertEqual(BillClassifier.amount(in: .overdue, bills: bills, asOf: reference(), calendar: calendar),
                       Money(rappen: 100_00))
        XCTAssertEqual(BillClassifier.amount(in: .dueThisMonth, bills: bills, asOf: reference(), calendar: calendar),
                       Money(rappen: 200_00))
        XCTAssertEqual(BillClassifier.amount(in: .future, bills: bills, asOf: reference(), calendar: calendar),
                       Money(rappen: 400_00))
        XCTAssertEqual(BillClassifier.amount(in: .paid, bills: bills, asOf: reference(), calendar: calendar),
                       Money(rappen: 800_00))
    }
}
