import Foundation

/// An amount of Swiss money, stored exactly as integer **Rappen** (1 CHF = 100 Rp).
///
/// Money is **never** represented as a floating-point number anywhere in Kontiva.
/// `Double`/`Float` lose precision (e.g. `0.1 + 0.2 != 0.3`), which is
/// unacceptable for salaries, bills, and tax figures. All storage and arithmetic
/// use `Int64` Rappen; CHF strings are produced for *presentation only*
/// (see `Money+Format`) and the formatter never routes through `Double`.
public struct Money: Equatable, Hashable, Comparable, Codable, Sendable {

    /// The amount in Rappen. May be negative.
    public let rappen: Int64

    public init(rappen: Int64) {
        self.rappen = rappen
    }

    /// Construct from whole francs and a 0–99 Rappen remainder.
    /// - Note: integers only; there is intentionally no `Double` initializer.
    public init(francs: Int64, rappen: Int64) {
        precondition((0...99).contains(rappen), "rappen component must be 0...99")
        let sign: Int64 = francs < 0 ? -1 : 1
        self.rappen = francs * 100 + sign * rappen
    }

    public static let zero = Money(rappen: 0)

    public var isZero: Bool { rappen == 0 }
    public var isNegative: Bool { rappen < 0 }
    public var isPositive: Bool { rappen > 0 }

    /// Whole-franc part (truncated toward zero).
    public var wholeFrancs: Int64 { rappen / 100 }
    /// 0–99 Rappen part, always non-negative.
    public var rappenComponent: Int64 { abs(rappen % 100) }

    // MARK: - Comparable

    public static func < (lhs: Money, rhs: Money) -> Bool {
        lhs.rappen < rhs.rappen
    }

    // MARK: - Checked arithmetic
    //
    // Money + Money and Money − Money are valid. Multiplying money by money is
    // meaningless and is deliberately NOT provided. Scaling by an integer factor
    // (e.g. quantity) is provided; dividing (e.g. averaging a 13th salary over
    // 12 months) truncates toward zero and is explicit about it.

    public static func + (lhs: Money, rhs: Money) -> Money {
        let (sum, overflow) = lhs.rappen.addingReportingOverflow(rhs.rappen)
        precondition(!overflow, "Money addition overflowed Int64 Rappen")
        return Money(rappen: sum)
    }

    public static func - (lhs: Money, rhs: Money) -> Money {
        let (diff, overflow) = lhs.rappen.subtractingReportingOverflow(rhs.rappen)
        precondition(!overflow, "Money subtraction overflowed Int64 Rappen")
        return Money(rappen: diff)
    }

    public static prefix func - (value: Money) -> Money {
        Money(rappen: -value.rappen)
    }

    public static func += (lhs: inout Money, rhs: Money) { lhs = lhs + rhs }
    public static func -= (lhs: inout Money, rhs: Money) { lhs = lhs - rhs }

    /// Multiply by an integer count (e.g. 3 × a unit price). Stays exact.
    public func scaled(by factor: Int64) -> Money {
        let (product, overflow) = rappen.multipliedReportingOverflow(by: factor)
        precondition(!overflow, "Money scaling overflowed Int64 Rappen")
        return Money(rappen: product)
    }

    /// This amount as a whole-number percentage of `base`, using integer math
    /// only (no floating point). Returns 0 if `base` is zero.
    public func percent(of base: Money) -> Int {
        guard base.rappen != 0 else { return 0 }
        let (scaled, overflow) = rappen.multipliedReportingOverflow(by: 100)
        if overflow { return Int((rappen / base.rappen) * 100) } // extreme values
        return Int(scaled / base.rappen)
    }

    /// Integer division of an amount into `parts` equal shares, truncating toward
    /// zero. Used for explicit cases like averaging a 13th salary over 12 months.
    /// Any remainder is discarded — callers that must conserve every Rappen
    /// should use `dividedDistributing(into:)` instead.
    public func divided(by parts: Int64) -> Money {
        precondition(parts != 0, "cannot divide Money by zero")
        return Money(rappen: rappen / parts)
    }

    /// Split into `parts` shares whose sum equals the original exactly. Early
    /// shares absorb the remainder Rappen, so no money is created or lost.
    public func dividedDistributing(into parts: Int64) -> [Money] {
        precondition(parts > 0, "parts must be > 0")
        let base = rappen / parts
        let remainder = rappen % parts
        let extra = remainder >= 0 ? Int64(1) : Int64(-1)
        let remCount = abs(remainder)
        return (0..<parts).map { index in
            let share = base + (index < remCount ? extra : 0)
            return Money(rappen: share)
        }
    }
}

public extension Sequence where Element == Money {
    /// Exact sum of money values. No floating point involved.
    func total() -> Money {
        reduce(Money.zero, +)
    }
}
