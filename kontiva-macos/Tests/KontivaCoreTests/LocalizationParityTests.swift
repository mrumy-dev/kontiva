import XCTest
@testable import KontivaCore

final class LocalizationParityTests: XCTestCase {

    /// Every language must define a value for every key — exact key parity.
    func testEveryLanguageCoversEveryKey() {
        for language in AppLanguage.allCases {
            let table = Localization.tables[language]
            XCTAssertNotNil(table, "Missing table for \(language.rawValue)")
            let keys = Set(table!.keys)
            let expected = Set(L10nKey.allCases)
            let missing = expected.subtracting(keys)
            XCTAssertTrue(missing.isEmpty,
                          "\(language.rawValue) is missing keys: \(missing.map(\.rawValue).sorted())")
        }
    }

    /// No language may contain a key outside the canonical set.
    func testNoExtraKeys() {
        let expected = Set(L10nKey.allCases)
        for language in AppLanguage.allCases {
            let keys = Set(Localization.tables[language]!.keys)
            let extra = keys.subtracting(expected)
            XCTAssertTrue(extra.isEmpty, "\(language.rawValue) has unexpected keys")
        }
    }

    /// No translation may be empty or whitespace-only.
    func testNoEmptyTranslations() {
        for language in AppLanguage.allCases {
            for (key, value) in Localization.tables[language]! {
                XCTAssertFalse(value.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty,
                               "Empty value for \(key.rawValue) in \(language.rawValue)")
            }
        }
    }

    func testLookupFallsBackToGermanNeverToRawKey() {
        let loc = Localization(language: .frCH)
        XCTAssertEqual(loc.string(.navOverview), "Aperçu")
        // de-CH is always complete, so lookups never return a raw key string.
        for key in L10nKey.allCases {
            XCTAssertNotEqual(loc.string(key), key.rawValue, "Returned raw key for \(key.rawValue)")
        }
    }

    func testLanguageRoster() {
        // 35 languages: 5 Swiss-tier + 19 European + 11 Asian.
        XCTAssertEqual(AppLanguage.allCases.count, 35)
        XCTAssertEqual(AppLanguage.deCH, AppLanguage(rawValue: "de-CH"))
        // Every language must have a complete table (no German fallback at the
        // dictionary level — every case is wired).
        for language in AppLanguage.allCases {
            XCTAssertNotNil(Localization.tables[language],
                            "No table wired for \(language.rawValue)")
        }
    }

    /// The selector is grouped Swiss → European → Asian, and the Swiss tier is
    /// exactly the four national languages plus English.
    func testGrouping() {
        let swiss = AppLanguage.allCases.filter { $0.group == .swiss }
        XCTAssertEqual(swiss, [.deCH, .frCH, .itCH, .rm, .en])

        let asian = AppLanguage.allCases.filter { $0.group == .asian }
        XCTAssertEqual(Set(asian), [.ar, .zhHans, .ja, .ko, .vi, .th, .hi, .ta, .si, .ur, .ps])

        // Declaration order keeps the groups contiguous and in region order, so
        // a flat allCases picker already reads Swiss → European → Asian.
        let groupsInOrder = AppLanguage.allCases.map(\.group)
        XCTAssertEqual(groupsInOrder, groupsInOrder.sorted { $0.rawValue < $1.rawValue })
    }

    /// Right-to-left scripts are flagged so the UI can mirror its layout.
    func testRTLLanguages() {
        let rtl = AppLanguage.allCases.filter(\.isRTL)
        XCTAssertEqual(Set(rtl), [.ar, .ur, .ps])
    }
}
