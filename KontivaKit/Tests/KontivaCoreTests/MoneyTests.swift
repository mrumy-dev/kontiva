import XCTest
@testable import KontivaCore

final class MoneyTests: XCTestCase {

    // MARK: - Storage is integer Rappen, never floating point

    /// These additions would lose precision with Double (0.1 + 0.2 != 0.3).
    /// With Int64 Rappen they are exact and associativity holds bit-for-bit.
    func testNoFloatingPointDriftOnAddition() {
        let a = Money.parse("0.10")!
        let b = Money.parse("0.20")!
        let c = Money.parse("0.30")!
        XCTAssertEqual(a + b, c)
        XCTAssertEqual((a + b).rappen, 30)

        // Ten 0.10 increments must equal exactly 1.00.
        let ten = Array(repeating: a, count: 10).total()
        XCTAssertEqual(ten, Money.parse("1.00"))
        XCTAssertEqual(ten.rappen, 100)
    }

    func testAdditionIsAssociativeUnlikeFloat() {
        let x = Money.parse("0.10")!
        let y = Money.parse("0.20")!
        let z = Money.parse("0.30")!
        XCTAssertEqual((x + y) + z, x + (y + z))
        XCTAssertEqual(((x + y) + z).rappen, 60)
    }

    /// A value whose exact cents matter at large magnitude is preserved exactly.
    func testLargeValueExactToTheRappen() {
        let m = Money.parse("12'345'678.91")!
        XCTAssertEqual(m.rappen, 1_234_567_891)
        XCTAssertEqual(m.formattedCHF(), "CHF 12'345'678.91")
    }

    func testComponents() {
        let m = Money(rappen: 123_45)
        XCTAssertEqual(m.wholeFrancs, 123)
        XCTAssertEqual(m.rappenComponent, 45)
        let neg = Money(rappen: -123_45)
        XCTAssertEqual(neg.wholeFrancs, -123)
        XCTAssertEqual(neg.rappenComponent, 45)
    }

    // MARK: - Parsing (no Double anywhere in the path)

    func testParsingSwissFormats() {
        XCTAssertEqual(Money.parse("1'234.50")?.rappen, 123_450)
        XCTAssertEqual(Money.parse("CHF 1'234.50")?.rappen, 123_450)
        XCTAssertEqual(Money.parse("1234,50")?.rappen, 123_450)   // comma decimal
        XCTAssertEqual(Money.parse("1 234.50")?.rappen, 123_450)  // space grouping
        XCTAssertEqual(Money.parse("-50")?.rappen, -5_000)
        XCTAssertEqual(Money.parse("0.05")?.rappen, 5)
        XCTAssertEqual(Money.parse("5")?.rappen, 500)
        XCTAssertEqual(Money.parse(".5")?.rappen, 50)
        XCTAssertEqual(Money.parse("100.")?.rappen, 10_000)
    }

    func testParsingRejectsMalformedInput() {
        XCTAssertNil(Money.parse(""))
        XCTAssertNil(Money.parse("abc"))
        XCTAssertNil(Money.parse("1.234"))       // 3 fractional digits → reject, no silent rounding
        XCTAssertNil(Money.parse("12.3.4"))      // multiple decimals after grouping removal
        XCTAssertNil(Money.parse("1.2x"))
    }

    func testParseFormatRoundTrip() {
        for raw in ["0.00", "0.05", "1'000.00", "1'234'567.89", "9.99"] {
            let parsed = Money.parse(raw)
            XCTAssertNotNil(parsed, raw)
            XCTAssertEqual(parsed?.formattedCHF(showSymbol: false), raw)
        }
    }

    // MARK: - Formatting

    func testFormattingSwissStyle() {
        XCTAssertEqual(Money(rappen: 0).formattedCHF(), "CHF 0.00")
        XCTAssertEqual(Money(rappen: 5).formattedCHF(), "CHF 0.05")
        XCTAssertEqual(Money(rappen: 123_450).formattedCHF(), "CHF 1'234.50")
        XCTAssertEqual(Money(rappen: -5_000).formattedCHF(), "-CHF 50.00")
        XCTAssertEqual(Money(rappen: 1_000_000_00).formattedCHF(), "CHF 1'000'000.00")
        XCTAssertEqual(Money(rappen: 999).formattedCHF(showSymbol: false), "9.99")
    }

    // MARK: - Arithmetic helpers

    func testScaledAndDivided() {
        let unit = Money.parse("19.95")!
        XCTAssertEqual(unit.scaled(by: 3).rappen, 5_985)
        XCTAssertEqual(Money(rappen: 1_200).divided(by: 12).rappen, 100)
    }

    func testDividedDistributingConservesEveryRappen() {
        let m = Money(rappen: 100)  // CHF 1.00 split three ways
        let shares = m.dividedDistributing(into: 3)
        XCTAssertEqual(shares.map(\.rappen), [34, 33, 33])
        XCTAssertEqual(shares.total(), m)  // nothing lost or created
    }

    func testInitFromFrancsAndRappen() {
        XCTAssertEqual(Money(francs: 12, rappen: 5).rappen, 1_205)
        XCTAssertEqual(Money(francs: -12, rappen: 5).rappen, -1_205)
    }
}
