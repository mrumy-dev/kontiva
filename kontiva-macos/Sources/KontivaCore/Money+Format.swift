import Foundation

/// Swiss CHF presentation for `Money`. **Presentation only** — these functions
/// turn an exact `Int64` Rappen value into a display string and never route the
/// amount through `Double`. Grouping is done by integer string manipulation.
public extension Money {

    /// Swiss-style CHF string, e.g. `CHF 1'234.50`, `-CHF 50.00`, `CHF 0.05`.
    ///
    /// Switzerland uses an apostrophe (`'`) as the thousands separator and a
    /// period (`.`) as the decimal separator for CHF across all UI languages,
    /// so the same convention is used for de-CH / fr-CH / it-CH / en.
    func formattedCHF(showSymbol: Bool = true) -> String {
        let negative = rappen < 0
        let magnitude = UInt64(rappen.magnitude)
        let francs = magnitude / 100
        let rp = magnitude % 100

        let grouped = Self.groupThousands(francs)
        // %02llu pads the Rappen component; operates on the integer, no float.
        let rappenString = String(format: "%02llu", rp)

        var result = "\(grouped).\(rappenString)"
        if showSymbol { result = "CHF \(result)" }
        if negative { result = "-\(result)" }
        return result
    }

    /// Group a non-negative integer with Swiss apostrophes: `1234567` → `1'234'567`.
    static func groupThousands(_ value: UInt64) -> String {
        let digits = String(value)
        guard digits.count > 3 else { return digits }

        var result = ""
        var count = 0
        for char in digits.reversed() {
            if count != 0 && count % 3 == 0 {
                result.append("'")
            }
            result.append(char)
            count += 1
        }
        return String(result.reversed())
    }
}
