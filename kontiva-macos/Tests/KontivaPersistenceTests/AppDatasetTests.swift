import XCTest
import KontivaCore
@testable import KontivaPersistence

/// Guards the tolerant `AppDataset` decoder: adding new fields (like `debts`) must
/// never break loading a store/backup written before the field existed. Every key
/// is decoded with `decodeIfPresent`, so missing keys fall back to sensible defaults.
final class AppDatasetTests: XCTestCase {

    private func chf(_ f: Int64) -> Money { Money(rappen: f * 100) }

    private func decode(_ json: String) throws -> AppDataset {
        try JSONDecoder().decode(AppDataset.self, from: Data(json.utf8))
    }

    func testEmptyObjectDecodesToEmptyDataset() throws {
        let ds = try decode("{}")
        XCTAssertEqual(ds.schemaVersion, AppDataset.currentSchemaVersion)
        XCTAssertTrue(ds.incomes.isEmpty)
        XCTAssertTrue(ds.debts.isEmpty)
        XCTAssertNil(ds.household)
    }

    /// The real regression: an OLD store written before `debts` existed must still
    /// load. Encode a real dataset, strip the "debts" key, and decode — proving the
    /// format matches exactly and a missing key defaults to [].
    func testOldStoreWithoutDebtsKeyStillLoads() throws {
        var original = AppDataset.empty
        original.incomes = [Income(label: "Lohn", monthlyNet: chf(6_500))]
        original.bills = [OneOffBill(provider: "Zahnarzt", amount: chf(380), dueDate: Date())]

        let data = try JSONEncoder().encode(original)
        var obj = try XCTUnwrap(JSONSerialization.jsonObject(with: data) as? [String: Any])
        XCTAssertNotNil(obj["debts"], "debts should have been encoded in the first place")
        obj.removeValue(forKey: "debts")   // mimic a store written before the field existed
        let stripped = try JSONSerialization.data(withJSONObject: obj)

        let ds = try JSONDecoder().decode(AppDataset.self, from: stripped)
        XCTAssertEqual(ds.incomes.first?.label, "Lohn")
        XCTAssertEqual(ds.bills.count, 1)
        XCTAssertTrue(ds.debts.isEmpty)    // missing key → empty, no throw
    }

    func testUnknownKeysAreIgnored() throws {
        // A future/older field we don't know about must not break decoding.
        let ds = try decode(#"{"debts": [], "someFutureKey": 123, "taxItems": []}"#)
        XCTAssertTrue(ds.debts.isEmpty)
    }

    func testRoundTripPreservesDebts() throws {
        var ds = AppDataset.empty
        ds.incomes = [Income(label: "Lohn", monthlyNet: chf(6_500))]
        ds.debts = [
            DebtItem(creditor: "Swisscom", amount: chf(1_980), type: .betreibung, reference: "BN-1"),
            DebtItem(creditor: "Steueramt", amount: chf(600), type: .verlustschein),
        ]
        let data = try JSONEncoder().encode(ds)
        let back = try JSONDecoder().decode(AppDataset.self, from: data)
        XCTAssertEqual(back, ds)
        XCTAssertEqual(back.debts.count, 2)
        XCTAssertEqual(back.debts.map(\.amount).total(), chf(2_580))
    }
}
