import XCTest
@testable import KontivaCore

final class DebtTests: XCTestCase {

    private func chf(_ f: Int64) -> Money { Money(rappen: f * 100) }

    func testSeverityOrdering() {
        // Most pressing first: Pfändung → Betreibung → offene Forderung → Verlustschein → other.
        let ordered = DebtType.allCases.sorted { $0.severityRank < $1.severityRank }
        XCTAssertEqual(ordered, [.pfaendung, .betreibung, .openClaim, .verlustschein, .other])
        XCTAssertLessThan(DebtType.pfaendung.severityRank, DebtType.betreibung.severityRank)
        XCTAssertLessThan(DebtType.betreibung.severityRank, DebtType.verlustschein.severityRank)
    }

    func testDebtTotalsAreExactRappen() {
        let debts = [
            DebtItem(creditor: "Swisscom", amount: chf(1_980), type: .betreibung),
            DebtItem(creditor: "Steueramt", amount: chf(600), type: .verlustschein),
            DebtItem(creditor: "Privat", amount: Money(rappen: 4_55), type: .other), // 4.55
        ]
        let total = debts.map(\.amount).total()
        XCTAssertEqual(total, Money(rappen: 198_000 + 60_000 + 455))
        XCTAssertEqual(total.formattedCHF(showSymbol: false), "2'584.55")
    }

    func testDefaultsAndOptionalFields() {
        let d = DebtItem(creditor: "X", amount: chf(100))
        XCTAssertEqual(d.type, .openClaim)   // default
        XCTAssertNil(d.date)
        XCTAssertNil(d.reference)
        XCTAssertNil(d.notes)
    }

    func testCodableRoundTrip() throws {
        let d = DebtItem(creditor: "Swisscom AG", amount: chf(1_980), type: .betreibung,
                         date: Date(timeIntervalSince1970: 1_700_000_000),
                         reference: "BN 2026-4471", notes: "note")
        let data = try JSONEncoder().encode(d)
        let back = try JSONDecoder().decode(DebtItem.self, from: data)
        XCTAssertEqual(d, back)
    }

    func testRawValuesStableForStoredData() {
        // These strings live in saved vaults — they must not change.
        XCTAssertEqual(DebtType.openClaim.rawValue, "openClaim")
        XCTAssertEqual(DebtType.betreibung.rawValue, "betreibung")
        XCTAssertEqual(DebtType.pfaendung.rawValue, "pfaendung")
        XCTAssertEqual(DebtType.verlustschein.rawValue, "verlustschein")
        XCTAssertEqual(DebtType.other.rawValue, "other")
    }
}
