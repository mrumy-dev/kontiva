import Foundation

/// Local redaction for the user-reviewed bug-report composer. Sensitive-looking
/// patterns are replaced with a neutral placeholder **before** the user reviews
/// the final text. Nothing is ever sent automatically; the user copies the
/// reviewed text themselves.
///
/// Conservative by design: it errs toward redacting. It does not guarantee that
/// every sensitive token is caught — it reduces accidental leakage in a report
/// the user still reviews by hand.
public enum Redactor {

    public static let placeholder = "[entfernt]"

    private struct Rule {
        let pattern: String
        let options: NSRegularExpression.Options
    }

    // Order matters: more specific patterns first.
    private static let rules: [Rule] = [
        // Email addresses.
        .init(pattern: #"[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}"#, options: []),
        // Swiss IBAN (CH + 2 check + 17 alnum), tolerant of spaces.
        .init(pattern: #"\bCH(?:\s?[0-9A-Z]){19}\b"#, options: [.caseInsensitive]),
        // Swiss AHV/AVS number: 756.XXXX.XXXX.XX
        .init(pattern: #"\b756[.\s]?\d{4}[.\s]?\d{4}[.\s]?\d{2}\b"#, options: []),
        // Long digit runs (card-like / account-like), 11+ digits possibly grouped.
        .init(pattern: #"\b(?:\d[ \-]?){11,}\b"#, options: []),
        // Phone numbers in +41 / 0xx forms.
        .init(pattern: #"(?:\+41|0)(?:[ \-]?\d){8,12}\b"#, options: []),
    ]

    private static let compiled: [NSRegularExpression] = rules.compactMap {
        try? NSRegularExpression(pattern: $0.pattern, options: $0.options)
    }

    /// Return a redacted copy of `text`.
    public static func redact(_ text: String) -> String {
        var result = text
        for regex in compiled {
            let range = NSRange(result.startIndex..., in: result)
            result = regex.stringByReplacingMatches(
                in: result, options: [], range: range, withTemplate: placeholder)
        }
        return result
    }

    /// True if the text still appears to contain a redactable pattern.
    public static func containsSensitivePattern(_ text: String) -> Bool {
        compiled.contains { regex in
            let range = NSRange(text.startIndex..., in: text)
            return regex.firstMatch(in: text, options: [], range: range) != nil
        }
    }
}

/// The structured content of a local bug report. Only non-sensitive metadata is
/// auto-added; no screenshots, documents, logs, database, or vault files are ever
/// attached automatically.
public struct BugReport: Equatable, Sendable {
    public var summary: String
    public var expectedBehavior: String
    public var actualBehavior: String
    public var reproductionSteps: String

    // Auto-added, non-sensitive metadata.
    public var appVersion: String
    public var macOSVersion: String
    public var appLanguage: String
    public var selectedArea: String

    public init(summary: String = "", expectedBehavior: String = "",
                actualBehavior: String = "", reproductionSteps: String = "",
                appVersion: String, macOSVersion: String,
                appLanguage: String, selectedArea: String) {
        self.summary = summary
        self.expectedBehavior = expectedBehavior
        self.actualBehavior = actualBehavior
        self.reproductionSteps = reproductionSteps
        self.appVersion = appVersion
        self.macOSVersion = macOSVersion
        self.appLanguage = appLanguage
        self.selectedArea = selectedArea
    }

    /// Compose the final, redacted report text for the user to review and copy.
    public func composeRedacted() -> String {
        """
        Kontiva — Problembericht
        ========================

        Bereich: \(selectedArea)

        Zusammenfassung:
        \(Redactor.redact(summary))

        Erwartetes Verhalten:
        \(Redactor.redact(expectedBehavior))

        Tatsächliches Verhalten:
        \(Redactor.redact(actualBehavior))

        Schritte zur Reproduktion:
        \(Redactor.redact(reproductionSteps))

        --- Technische Angaben (automatisch, nicht sensibel) ---
        App-Version: \(appVersion)
        System-Version: \(macOSVersion)
        Sprache: \(appLanguage)
        """
    }
}
