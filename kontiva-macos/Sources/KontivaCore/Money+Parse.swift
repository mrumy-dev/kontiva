import Foundation

public extension Money {

    /// Parse user input into exact `Int64` Rappen **without using `Double`**.
    ///
    /// Accepts Swiss-style input liberally:
    /// - optional `CHF` prefix / suffix
    /// - apostrophe, space, or non-breaking space as thousands separators
    /// - either `.` or `,` as the decimal separator
    /// - optional leading `-`
    /// - 0, 1, or 2 fractional digits
    ///
    /// Returns `nil` for malformed input (e.g. more than 2 fractional digits,
    /// stray letters, multiple decimal separators). Strict-by-design: silently
    /// rounding money is worse than rejecting bad input.
    static func parse(_ raw: String) -> Money? {
        var s = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !s.isEmpty else { return nil }

        // Strip currency markers (case-insensitive), and the CHF symbol forms.
        for token in ["CHF", "chf", "Chf", "Fr.", "fr.", "SFr."] {
            s = s.replacingOccurrences(of: token, with: "")
        }
        s = s.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !s.isEmpty else { return nil }

        // Sign.
        var negative = false
        if s.hasPrefix("-") {
            negative = true
            s.removeFirst()
        } else if s.hasPrefix("+") {
            s.removeFirst()
        }

        // Remove thousands separators: apostrophe, regular space, NBSP, narrow NBSP.
        for sep in ["'", "\u{2019}", " ", "\u{00A0}", "\u{202F}", "\u{2009}"] {
            s = s.replacingOccurrences(of: sep, with: "")
        }
        guard !s.isEmpty else { return nil }

        // Determine the decimal separator: the last '.' or ',' in the string.
        let lastDot = s.lastIndex(of: ".")
        let lastComma = s.lastIndex(of: ",")
        let decimalIndex: String.Index?
        switch (lastDot, lastComma) {
        case let (d?, c?): decimalIndex = d > c ? d : c
        case let (d?, nil): decimalIndex = d
        case let (nil, c?): decimalIndex = c
        case (nil, nil): decimalIndex = nil
        }

        let integerPart: String
        let fractionPart: String
        if let idx = decimalIndex {
            integerPart = String(s[s.startIndex..<idx])
            fractionPart = String(s[s.index(after: idx)...])
        } else {
            integerPart = s
            fractionPart = ""
        }

        // After removing separators, only digits may remain in each part.
        let intDigits = integerPart.isEmpty ? "0" : integerPart
        guard intDigits.allSatisfy(\.isNumber) else { return nil }
        guard fractionPart.allSatisfy(\.isNumber) else { return nil }
        guard fractionPart.count <= 2 else { return nil } // no silent rounding

        guard let francs = Int64(intDigits) else { return nil }

        // Pad fraction to exactly two digits: "" → "00", "5" → "50", "05" → "05".
        let paddedFraction = fractionPart.padding(toLength: 2, withPad: "0", startingAt: 0)
        guard let rp = Int64(paddedFraction) else { return nil }

        let (scaled, overflow1) = francs.multipliedReportingOverflow(by: 100)
        guard !overflow1 else { return nil }
        let (total, overflow2) = scaled.addingReportingOverflow(rp)
        guard !overflow2 else { return nil }

        return Money(rappen: negative ? -total : total)
    }
}
