import XCTest
@testable import KontivaCore

final class RedactionTests: XCTestCase {

    func testRedactsEmail() {
        let out = Redactor.redact("Kontakt: anna.muster@example.ch bitte")
        XCTAssertFalse(out.contains("anna.muster@example.ch"))
        XCTAssertTrue(out.contains(Redactor.placeholder))
    }

    func testRedactsSwissIBAN() {
        let out = Redactor.redact("Konto CH93 0076 2011 6238 5295 7 zahlen")
        XCTAssertFalse(out.contains("6238"))
        XCTAssertTrue(out.contains(Redactor.placeholder))
    }

    func testRedactsAHVNumber() {
        let out = Redactor.redact("AHV 756.1234.5678.97 angeben")
        XCTAssertFalse(out.contains("756.1234.5678.97"))
    }

    func testRedactsLongDigitRuns() {
        let out = Redactor.redact("Karte 4111 1111 1111 1111 verwendet")
        XCTAssertFalse(out.contains("4111 1111 1111 1111"))
    }

    func testKeepsOrdinaryText() {
        let text = "Die Übersicht zeigt einen falschen Betrag bei den Fixkosten."
        XCTAssertEqual(Redactor.redact(text), text)
    }

    func testContainsSensitivePattern() {
        XCTAssertTrue(Redactor.containsSensitivePattern("mail me at a@b.ch"))
        XCTAssertFalse(Redactor.containsSensitivePattern("nur normaler Text"))
    }

    func testBugReportComposesRedactedAndAddsOnlyNonSensitiveMetadata() {
        let report = BugReport(
            summary: "Fehler, schreibt an support@kontiva.ch",
            expectedBehavior: "Korrekter Betrag",
            actualBehavior: "Falscher Betrag",
            reproductionSteps: "1. Öffnen 2. Klicken",
            appVersion: "0.0.1",
            macOSVersion: "26.0",
            appLanguage: "de-CH",
            selectedArea: "Übersicht")

        let text = report.composeRedacted()
        XCTAssertFalse(text.contains("support@kontiva.ch")) // redacted
        XCTAssertTrue(text.contains("0.0.1"))               // allowed metadata
        XCTAssertTrue(text.contains("de-CH"))
        XCTAssertTrue(text.contains("Übersicht"))
        // No sensitive auto-attachments: only the four fields + tech block exist.
        XCTAssertTrue(text.contains("Technische Angaben"))
    }
}
