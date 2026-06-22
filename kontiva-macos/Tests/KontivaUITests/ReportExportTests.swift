import XCTest
import AppKit
import KontivaCore
@testable import KontivaUI

/// The PDF report is built by rendering SwiftUI pages off-screen. These tests
/// prove the pipeline produces a valid, multi-page PDF for a realistic budget,
/// and refuses to export when there is nothing to show.
final class ReportExportTests: XCTestCase {

    private func chf(_ francs: Int64) -> Money { Money(rappen: francs * 100) }

    private func sampleInput() -> ReportInput {
        let cal = Calendar.swiss
        let month = cal.startOfMonth(for: Date())
        let incomes = [Income(label: "Lohn", monthlyNet: chf(6_800), thirteenthAmount: chf(6_800))]
        let fixedCosts = [
            RecurringFixedExpense(name: "Miete", monthlyAmount: chf(1_950), category: .rent),
            RecurringFixedExpense(name: "Krankenkasse", monthlyAmount: chf(420), category: .insurance),
        ]
        let variable = [VariableMonthlyBudget(name: "Lebensmittel", plannedAmount: chf(850), category: .groceries)]
        let savings = [SavingsGoal(name: "Notgroschen", category: .emergency, target: chf(15_000),
                                   monthlyContribution: chf(400), startingBalance: chf(9_200),
                                   startDate: cal.date(byAdding: .month, value: -10, to: month)!)]
        let bills = [OneOffBill(provider: "Zahnarzt", amount: chf(380), dueDate: month)]
        let availability = AvailabilityEngine.compute(
            incomes: incomes, fixedCosts: fixedCosts, variableBudgets: variable, bills: bills, asOf: month)
        let insights = InsightEngine.analyze(
            incomes: incomes, fixedCosts: fixedCosts, variableBudgets: variable,
            bills: bills, savingsGoals: savings, availability: availability, asOf: month)
        return ReportInput(
            loc: Localization(language: .deCH), locale: AppLanguage.deCH.locale, month: month,
            availability: availability, incomes: incomes, fixedCosts: fixedCosts,
            variableBudgets: variable, savingsGoals: savings, bills: bills, insights: insights,
            household: Household(name: "Muster", canton: Canton(name: "Zürich", abbreviation: "ZH")))
    }

    @MainActor
    func testReportProducesValidMultiPagePDF() {
        _ = NSApplication.shared
        guard let data = ReportBuilder.makePDF(sampleInput()) else {
            return XCTFail("report should be produced for a non-empty budget")
        }
        // Valid PDF header + non-trivial content.
        XCTAssertTrue(data.starts(with: Array("%PDF".utf8)))
        XCTAssertGreaterThan(data.count, 5_000)

        // Cover + summary + planning + variable + savings + bills + insights = 7 pages.
        let doc = CGPDFDocument(CGDataProvider(data: data as CFData)!)
        XCTAssertEqual(doc?.numberOfPages, 7)
    }

    @MainActor
    func testEmptyBudgetExportsNothing() {
        _ = NSApplication.shared
        let month = Calendar.swiss.startOfMonth(for: Date())
        let empty = ReportInput(
            loc: Localization(language: .en), locale: AppLanguage.en.locale, month: month,
            availability: AvailabilityEngine.compute(incomes: [], fixedCosts: [],
                                                     variableBudgets: [], bills: [], asOf: month),
            incomes: [], fixedCosts: [], variableBudgets: [], savingsGoals: [],
            bills: [], insights: [], household: nil)
        XCTAssertNil(ReportBuilder.makePDF(empty))
    }
}
