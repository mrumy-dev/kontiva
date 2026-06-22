import Foundation

/// A debt the household owes. Overdue *bills* flow in automatically from
/// `OneOffBill` (see the Schulden view) — `DebtItem` is for the formal Swiss debt-
/// enforcement stages and other debts the user records by hand.
///
/// The stages mirror the Swiss SchKG process (Bundesgesetz über Schuldbetreibung
/// und Konkurs):
///   openClaim     – an outstanding claim / reminder, not yet in enforcement.
///   betreibung    – a Betreibung is running; the debtor received a Zahlungsbefehl.
///   pfaendung     – a Pfändung (seizure of wages/assets) has been ordered.
///   verlustschein – a Verlustschein (certificate of loss) was issued.
///   other         – a private loan or any other debt.
public enum DebtType: String, Codable, Sendable, CaseIterable, Identifiable {
    case openClaim
    case betreibung
    case pfaendung
    case verlustschein
    case other

    public var id: String { rawValue }

    /// Rough urgency ordering (most pressing first) for display + guidance.
    public var severityRank: Int {
        switch self {
        case .pfaendung:     return 0
        case .betreibung:    return 1
        case .openClaim:     return 2
        case .verlustschein: return 3
        case .other:         return 4
        }
    }
}

/// A single recorded debt. No interest modelling — Kontiva tracks the principal the
/// user enters; this is an organiser and an information aid, not a legal tool.
public struct DebtItem: Equatable, Codable, Sendable, Identifiable {
    public let id: UUID
    public var creditor: String          // Gläubiger
    public var amount: Money
    public var type: DebtType
    /// The relevant date (e.g. when the Zahlungsbefehl was served, or the
    /// Verlustschein issued). Optional — the user may not know it.
    public var date: Date?
    /// Reference number (Betreibungs-Nr. / Aktenzeichen), if any.
    public var reference: String?
    public var notes: String?

    public init(id: UUID = UUID(), creditor: String, amount: Money,
                type: DebtType = .openClaim, date: Date? = nil,
                reference: String? = nil, notes: String? = nil) {
        self.id = id
        self.creditor = creditor
        self.amount = amount
        self.type = type
        self.date = date
        self.reference = reference
        self.notes = notes
    }
}
