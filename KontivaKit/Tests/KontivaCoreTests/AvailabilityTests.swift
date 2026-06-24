import XCTest
@testable import KontivaCore

final class AvailabilityTests: XCTestCase {

    private let calendar = Calendar.swiss
    private func reference() -> Date {
        calendar.date(from: DateComponents(year: 2026, month: 6, day: 15))!
    }
    private func date(_ y: Int, _ m: Int, _ d: Int) -> Date {
        calendar.date(from: DateComponents(year: y, month: m, day: d))!
    }

    // MARK: - The available-this-month formula

    func testAvailableFormula() {
        let income = Income(label: "Lohn", monthlyNet: Money.parse("6'500.00")!)
        let fixed = [
            RecurringFixedExpense(name: "Miete", monthlyAmount: Money.parse("1'800.00")!),
            RecurringFixedExpense(name: "Krankenkasse", monthlyAmount: Money.parse("350.00")!),
        ]
        let variable = [
            VariableMonthlyBudget(name: "Lebensmittel", plannedAmount: Money.parse("600.00")!),
        ]
        let bills = [
            OneOffBill(provider: "Zahnarzt", amount: Money.parse("300.00")!,
                       dueDate: date(2026, 6, 20), status: .open),   // due this month
            OneOffBill(provider: "Garage", amount: Money.parse("250.00")!,
                       dueDate: date(2026, 5, 30), status: .open),   // overdue
            OneOffBill(provider: "Spende", amount: Money.parse("100.00")!,
                       dueDate: date(2026, 8, 1), status: .open),    // future → excluded
            OneOffBill(provider: "Internet", amount: Money.parse("80.00")!,
                       dueDate: date(2026, 6, 5), status: .paid),    // paid but due THIS month → still counts
        ]

        let result = AvailabilityEngine.compute(
            incomes: [income], fixedCosts: fixed, variableBudgets: variable,
            bills: bills, asOf: reference(), calendar: calendar)

        // Bills due this month = Zahnarzt 300 + Internet 80 (paid, but the money is gone
        // either way) = 380.  6500 − 2150 − 600 − 380 − 250 = 3120.
        XCTAssertEqual(result.netIncomeThisMonth, Money.parse("6'500.00"))
        XCTAssertEqual(result.recurringFixedCosts, Money.parse("2'150.00"))
        XCTAssertEqual(result.plannedVariableBudgets, Money.parse("600.00"))
        XCTAssertEqual(result.billsDueThisMonth, Money.parse("380.00"))
        XCTAssertEqual(result.overdueOpenBills, Money.parse("250.00"))
        XCTAssertEqual(result.available, Money.parse("3'120.00"))
    }

    // MARK: - Regression: paying a bill must not free up money

    func testPayingABillDoesNotChangeAvailable() {
        let income = Income(label: "Lohn", monthlyNet: Money.parse("5'000.00")!)
        let openBill = OneOffBill(provider: "Stadtkasse", amount: Money.parse("500.00")!,
                                  dueDate: date(2026, 6, 20), status: .open)
        let paidBill = OneOffBill(id: openBill.id, provider: "Stadtkasse", amount: Money.parse("500.00")!,
                                  dueDate: date(2026, 6, 20), status: .paid)

        let whenOpen = AvailabilityEngine.compute(
            incomes: [income], fixedCosts: [], variableBudgets: [], bills: [openBill],
            asOf: reference(), calendar: calendar)
        let whenPaid = AvailabilityEngine.compute(
            incomes: [income], fixedCosts: [], variableBudgets: [], bills: [paidBill],
            asOf: reference(), calendar: calendar)

        XCTAssertEqual(whenOpen.available, Money.parse("4'500.00"))
        XCTAssertEqual(whenPaid.available, whenOpen.available, "paying a bill must NOT increase available money")
        XCTAssertEqual(whenPaid.billsDueThisMonth, Money.parse("500.00"))
    }

    // MARK: - Regression: future savings don't cost anything until they start

    func testFutureSavingsDoNotReduceAvailableUntilTheyStart() {
        let income = Income(label: "Lohn", monthlyNet: Money.parse("5'000.00")!)
        let goal = SavingsGoal(name: "Auto", monthlyContribution: Money.parse("1'000.00")!,
                               startDate: date(2026, 9, 1))   // starts September

        // Viewing June → not started → no deduction.
        let june = AvailabilityEngine.compute(
            incomes: [income], fixedCosts: [], variableBudgets: [], bills: [],
            savingsGoals: [goal], asOf: date(2026, 6, 15), calendar: calendar)
        XCTAssertEqual(june.plannedSavings, .zero)
        XCTAssertEqual(june.available, Money.parse("5'000.00"))

        // Viewing September → started → counts.
        let sept = AvailabilityEngine.compute(
            incomes: [income], fixedCosts: [], variableBudgets: [], bills: [],
            savingsGoals: [goal], asOf: date(2026, 9, 15), calendar: calendar)
        XCTAssertEqual(sept.plannedSavings, Money.parse("1'000.00"))
        XCTAssertEqual(sept.available, Money.parse("4'000.00"))
    }

    // MARK: - Reached/completed savings goals

    func testCompletedGoalStopsReducingAvailable() {
        let income = Income(label: "Lohn", monthlyNet: Money.parse("5'000.00")!)
        let goal = SavingsGoal(name: "Notgroschen", target: Money.parse("2'000.00")!,
                               monthlyContribution: Money.parse("500.00")!,
                               startDate: date(2026, 1, 1), completedDate: date(2026, 6, 1))
        // June is the completion month → it still counts that month.
        let june = AvailabilityEngine.compute(incomes: [income], fixedCosts: [], variableBudgets: [], bills: [],
                                              savingsGoals: [goal], asOf: date(2026, 6, 15), calendar: calendar)
        XCTAssertEqual(june.plannedSavings, Money.parse("500.00"))
        // July → done; no longer reduces available.
        let july = AvailabilityEngine.compute(incomes: [income], fixedCosts: [], variableBudgets: [], bills: [],
                                              savingsGoals: [goal], asOf: date(2026, 7, 15), calendar: calendar)
        XCTAssertEqual(july.plannedSavings, .zero)
        XCTAssertEqual(july.available, Money.parse("5'000.00"))
    }

    func testCompletedGoalBalanceFreezes() {
        let goal = SavingsGoal(name: "Auto", target: Money.parse("6'000.00")!,
                               monthlyContribution: Money.parse("1'000.00")!,
                               startDate: date(2026, 1, 1), completedDate: date(2026, 6, 1))
        // Jan…June inclusive = 6 × 1000 = 6000, then frozen.
        XCTAssertEqual(goal.accumulated(asOf: date(2026, 6, 15)), Money.parse("6'000.00"))
        XCTAssertEqual(goal.accumulated(asOf: date(2026, 12, 15)), Money.parse("6'000.00"))
        XCTAssertTrue(goal.isCompleted)
        XCTAssertTrue(goal.isReached(asOf: date(2026, 12, 15)))
    }

    // MARK: - 12 vs 13 salary

    func testThirteenthSeparateByDefaultIsNotAddedToMonthly() {
        let income = Income(label: "Lohn", monthlyNet: Money.parse("6'000.00")!,
                            thirteenthAmount: Money.parse("6'000.00")!,
                            thirteenthModel: .separate)

        XCTAssertEqual(AvailabilityEngine.netIncomeThisMonth(income), Money.parse("6'000.00"))
        XCTAssertEqual(AvailabilityEngine.thirteenthShownSeparately([income]), Money.parse("6'000.00"))
    }

    func testThirteenthAveragedAddsOneTwelfth() {
        let income = Income(label: "Lohn", monthlyNet: Money.parse("6'000.00")!,
                            thirteenthAmount: Money.parse("6'000.00")!,
                            thirteenthModel: .averagedMonthly)

        // 6000 + 6000/12 = 6000 + 500 = 6500
        XCTAssertEqual(AvailabilityEngine.netIncomeThisMonth(income), Money.parse("6'500.00"))
        // Averaged income is not shown separately.
        XCTAssertNil(AvailabilityEngine.thirteenthShownSeparately([income]))
    }

    // MARK: - 13th payout schedule + bonuses (month-aware)

    func testThirteenthPaidInDecemberOnlyCountsInDecember() {
        let income = Income(label: "Lohn", monthlyNet: Money.parse("6'000.00")!,
                            thirteenthAmount: Money.parse("6'000.00")!, thirteenthModel: .december)
        XCTAssertEqual(AvailabilityEngine.netIncomeThisMonth(income, asOf: date(2026, 6, 15)), Money.parse("6'000.00"))
        XCTAssertEqual(AvailabilityEngine.netIncomeThisMonth(income, asOf: date(2026, 12, 15)), Money.parse("12'000.00"))
    }

    func testThirteenthSplitNovDec() {
        let income = Income(label: "Lohn", monthlyNet: Money.parse("6'000.00")!,
                            thirteenthAmount: Money.parse("6'000.00")!, thirteenthModel: .splitNovDec)
        // 11/12 (= 5500) in November, 1/12 (= 500) in December.
        XCTAssertEqual(AvailabilityEngine.netIncomeThisMonth(income, asOf: date(2026, 11, 15)), Money.parse("11'500.00"))
        XCTAssertEqual(AvailabilityEngine.netIncomeThisMonth(income, asOf: date(2026, 12, 15)), Money.parse("6'500.00"))
        XCTAssertEqual(AvailabilityEngine.netIncomeThisMonth(income, asOf: date(2026, 7, 15)), Money.parse("6'000.00"))
    }

    func testBonusLandsOnlyInItsMonth() {
        let income = Income(label: "Lohn", monthlyNet: Money.parse("6'000.00")!,
                            bonuses: [Bonus(label: "Jahresbonus", amount: Money.parse("3'000.00")!, month: 3)])
        XCTAssertEqual(AvailabilityEngine.netIncomeThisMonth(income, asOf: date(2026, 3, 15)), Money.parse("9'000.00"))
        XCTAssertEqual(AvailabilityEngine.netIncomeThisMonth(income, asOf: date(2026, 4, 15)), Money.parse("6'000.00"))
    }

    func testNoThirteenthMeansNothingExtra() {
        let income = Income(label: "Lohn", monthlyNet: Money.parse("6'000.00")!)
        XCTAssertEqual(AvailabilityEngine.netIncomeThisMonth(income), Money.parse("6'000.00"))
        XCTAssertNil(AvailabilityEngine.thirteenthShownSeparately([income]))
    }

    func testMultipleIncomeSources() {
        let a = Income(label: "Lohn A", monthlyNet: Money.parse("4'000.00")!)
        let b = Income(label: "Lohn B", monthlyNet: Money.parse("2'500.00")!)
        XCTAssertEqual(AvailabilityEngine.netIncomeThisMonth([a, b]), Money.parse("6'500.00"))
    }

    func testNegativeAvailableWhenCommitmentsExceedIncome() {
        let income = Income(label: "Lohn", monthlyNet: Money.parse("3'000.00")!)
        let fixed = [RecurringFixedExpense(name: "Miete", monthlyAmount: Money.parse("3'500.00")!)]
        let result = AvailabilityEngine.compute(
            incomes: [income], fixedCosts: fixed, variableBudgets: [], bills: [],
            asOf: reference(), calendar: calendar)
        XCTAssertEqual(result.available, Money.parse("-500.00"))
        XCTAssertTrue(result.available.isNegative)
    }
}
