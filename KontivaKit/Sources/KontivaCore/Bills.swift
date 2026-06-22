import Foundation

/// A one-off bill (Rechnung): a single obligation with a due date.
///
/// Recurring payments (rent, insurance, subscriptions, leasing) do **not** belong
/// here — they are recurring fixed costs in Monthly Planning. Bills are one-off.
public struct OneOffBill: Equatable, Codable, Sendable, Identifiable {
    public let id: UUID
    public var provider: String
    public var amount: Money
    public var dueDate: Date
    public var status: BillStatus
    public var notes: String?

    public init(id: UUID = UUID(), provider: String, amount: Money, dueDate: Date,
                status: BillStatus = .open, notes: String? = nil) {
        self.id = id
        self.provider = provider
        self.amount = amount
        self.dueDate = dueDate
        self.status = status
        self.notes = notes
    }
}

/// User-set status of a bill.
public enum BillStatus: String, Codable, Sendable, CaseIterable {
    case open
    case paid
}

/// Derived state of a bill relative to a reference month. This is what decides
/// whether the bill counts against the current month's available balance.
public enum BillState: String, Sendable, CaseIterable {
    /// Paid — does not count.
    case paid
    /// Open and due before the current month — counts (it is owed and late).
    case overdue
    /// Open and due within the current month — counts.
    case dueThisMonth
    /// Open and due in a future month — does not count this month.
    case future

    /// Whether a bill in this state reduces the **current month's** available balance.
    public var countsAgainstCurrentMonth: Bool {
        switch self {
        case .overdue, .dueThisMonth: return true
        case .paid, .future:          return false
        }
    }
}

public enum BillClassifier {

    /// Classify a bill relative to `asOf` (defaults to now), using `calendar`.
    ///
    /// Rules:
    /// - paid              → `.paid`            (never counts)
    /// - open + due < this month start → `.overdue`      (counts)
    /// - open + due within this month  → `.dueThisMonth`  (counts)
    /// - open + due in a later month   → `.future`        (does not count)
    public static func state(of bill: OneOffBill,
                             asOf reference: Date = Date(),
                             calendar: Calendar = .swiss) -> BillState {
        if bill.status == .paid { return .paid }

        guard let interval = calendar.dateInterval(of: .month, for: reference) else {
            // Extremely defensive fallback; should not happen for a Gregorian calendar.
            return bill.dueDate < reference ? .overdue : .dueThisMonth
        }
        let monthStart = interval.start  // inclusive
        let monthEnd = interval.end      // exclusive (first instant of next month)

        if bill.dueDate < monthStart {
            return .overdue
        } else if bill.dueDate < monthEnd {
            return .dueThisMonth
        } else {
            return .future
        }
    }

    /// Total of all bills that count against the current month's available balance.
    public static func amountAffectingCurrentMonth(_ bills: [OneOffBill],
                                                   asOf reference: Date = Date(),
                                                   calendar: Calendar = .swiss) -> Money {
        bills
            .filter { state(of: $0, asOf: reference, calendar: calendar).countsAgainstCurrentMonth }
            .map(\.amount)
            .total()
    }

    /// Total of bills in a specific state (e.g. just the overdue ones).
    public static func amount(in target: BillState,
                              bills: [OneOffBill],
                              asOf reference: Date = Date(),
                              calendar: Calendar = .swiss) -> Money {
        bills
            .filter { state(of: $0, asOf: reference, calendar: calendar) == target }
            .map(\.amount)
            .total()
    }
}

public extension Calendar {
    /// Gregorian calendar in the Swiss locale — used consistently for month math.
    static var swiss: Calendar {
        var calendar = Calendar(identifier: .gregorian)
        calendar.locale = Locale(identifier: "de_CH")
        calendar.firstWeekday = 2 // Monday
        return calendar
    }

    /// First instant of the month containing `date` — the canonical reference for
    /// a "selected month" view.
    func startOfMonth(for date: Date) -> Date {
        self.date(from: dateComponents([.year, .month], from: date)) ?? date
    }
}
