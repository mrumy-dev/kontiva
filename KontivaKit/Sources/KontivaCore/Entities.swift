import Foundation

// MARK: - Canton

/// A Swiss canton stored by **name and abbreviation only**.
/// No coats of arms, flags, crests, or emblems anywhere in Kontiva.
public struct Canton: Equatable, Hashable, Codable, Sendable, Identifiable {
    public var id: String { abbreviation }
    public let name: String          // e.g. "Zürich"
    public let abbreviation: String  // e.g. "ZH"

    public init(name: String, abbreviation: String) {
        self.name = name
        self.abbreviation = abbreviation
    }

    /// The 26 Swiss cantons (name + abbreviation only).
    public static let all: [Canton] = [
        .init(name: "Aargau", abbreviation: "AG"),
        .init(name: "Appenzell Ausserrhoden", abbreviation: "AR"),
        .init(name: "Appenzell Innerrhoden", abbreviation: "AI"),
        .init(name: "Basel-Landschaft", abbreviation: "BL"),
        .init(name: "Basel-Stadt", abbreviation: "BS"),
        .init(name: "Bern", abbreviation: "BE"),
        .init(name: "Freiburg", abbreviation: "FR"),
        .init(name: "Genf", abbreviation: "GE"),
        .init(name: "Glarus", abbreviation: "GL"),
        .init(name: "Graubünden", abbreviation: "GR"),
        .init(name: "Jura", abbreviation: "JU"),
        .init(name: "Luzern", abbreviation: "LU"),
        .init(name: "Neuenburg", abbreviation: "NE"),
        .init(name: "Nidwalden", abbreviation: "NW"),
        .init(name: "Obwalden", abbreviation: "OW"),
        .init(name: "Schaffhausen", abbreviation: "SH"),
        .init(name: "Schwyz", abbreviation: "SZ"),
        .init(name: "Solothurn", abbreviation: "SO"),
        .init(name: "St. Gallen", abbreviation: "SG"),
        .init(name: "Tessin", abbreviation: "TI"),
        .init(name: "Thurgau", abbreviation: "TG"),
        .init(name: "Uri", abbreviation: "UR"),
        .init(name: "Waadt", abbreviation: "VD"),
        .init(name: "Wallis", abbreviation: "VS"),
        .init(name: "Zug", abbreviation: "ZG"),
        .init(name: "Zürich", abbreviation: "ZH"),
    ]
}

// MARK: - Household / Profile

public struct Household: Equatable, Codable, Sendable, Identifiable {
    public let id: UUID
    public var name: String
    public var canton: Canton?
    public var adults: Int
    public var children: Int
    /// The chosen local profile picture — the bundled icon's file name (no
    /// extension, e.g. "human-08-premium"), or nil for the default avatar.
    public var avatarName: String?

    public init(id: UUID = UUID(), name: String, canton: Canton? = nil,
                adults: Int = 1, children: Int = 0, avatarName: String? = nil) {
        self.id = id
        self.name = name
        self.canton = canton
        self.adults = adults
        self.children = children
        self.avatarName = avatarName
    }
}

// MARK: - Income

/// How a 13th-month salary is treated. Default is `.separate` — it is **never**
/// silently averaged into monthly income.
public enum ThirteenthSalaryModel: String, Codable, Sendable, CaseIterable {
    /// Shown on its own; excluded from monthly net income.
    case separate
    /// Explicitly spread as `amount / 12` across each month.
    case averagedMonthly
}

public struct Income: Equatable, Codable, Sendable, Identifiable {
    public let id: UUID
    public var label: String
    /// Net monthly income (after deductions), exact Rappen.
    public var monthlyNet: Money
    /// Amount of one 13th-salary instalment (typically one extra month), or nil.
    public var thirteenthAmount: Money?
    public var thirteenthModel: ThirteenthSalaryModel

    public init(id: UUID = UUID(), label: String, monthlyNet: Money,
                thirteenthAmount: Money? = nil,
                thirteenthModel: ThirteenthSalaryModel = .separate) {
        self.id = id
        self.label = label
        self.monthlyNet = monthlyNet
        self.thirteenthAmount = thirteenthAmount
        self.thirteenthModel = thirteenthModel
    }

    public var hasThirteenth: Bool {
        if let amount = thirteenthAmount { return !amount.isZero }
        return false
    }
}

// MARK: - Recurring fixed costs (Monthly Planning, NOT Bills)

public enum FixedExpenseCategory: String, Codable, Sendable, CaseIterable {
    // Existing raw values kept stable so already-stored data still decodes;
    // new categories are appended. Order here drives the picker order.
    case rent
    case mortgage
    case healthInsurance
    case insurance
    case utilities
    case telecom
    case subscription
    case serafe
    case leasing
    case publicTransport
    case childcare
    case education
    case membership
    case alimony
    case taxes
    case other
}

public struct RecurringFixedExpense: Equatable, Codable, Sendable, Identifiable {
    public let id: UUID
    public var name: String
    public var monthlyAmount: Money
    public var category: FixedExpenseCategory
    /// A limited-duration standing order (Dauerauftrag): if both are set, this cost
    /// applies only for `installments` consecutive months starting in `startMonth`'s
    /// month — e.g. 6 tax instalments from June. nil = open-ended (counts every
    /// month, like rent).
    public var startMonth: Date?
    public var installments: Int?

    public init(id: UUID = UUID(), name: String, monthlyAmount: Money,
                category: FixedExpenseCategory = .other,
                startMonth: Date? = nil, installments: Int? = nil) {
        self.id = id
        self.name = name
        self.monthlyAmount = monthlyAmount
        self.category = category
        self.startMonth = startMonth
        self.installments = installments
    }

    /// Whether this is a time-limited standing order rather than an open-ended cost.
    public var isLimited: Bool { startMonth != nil && (installments ?? 0) > 0 }

    /// Whether the cost applies in the given month. Open-ended costs always apply;
    /// limited ones only within their instalment window.
    public func isActive(in month: Date, calendar: Calendar = .swiss) -> Bool {
        guard let start = startMonth, let count = installments, count > 0 else { return true }
        let startMonthStart = calendar.startOfMonth(for: start)
        let m = calendar.startOfMonth(for: month)
        guard m >= startMonthStart else { return false }
        let elapsed = calendar.dateComponents([.month], from: startMonthStart, to: m).month ?? 0
        return elapsed < count
    }

    /// For a limited order: which instalment number (1…count) the given month is,
    /// or nil if the month falls outside the window.
    public func installmentNumber(in month: Date, calendar: Calendar = .swiss) -> Int? {
        guard isLimited, isActive(in: month, calendar: calendar),
              let start = startMonth else { return nil }
        let startMonthStart = calendar.startOfMonth(for: start)
        let m = calendar.startOfMonth(for: month)
        return (calendar.dateComponents([.month], from: startMonthStart, to: m).month ?? 0) + 1
    }
}

// MARK: - Planned variable monthly budgets

public enum VariableBudgetCategory: String, Codable, Sendable, CaseIterable {
    // Existing raw values kept stable; new categories appended. Order = picker order.
    case groceries
    case dining
    case household
    case clothing
    case personal
    case health
    case fuel
    case transport
    case leisure
    case entertainment
    case children
    case pets
    case gifts
    case travel
    case education
    case charity
    case other
}

public struct VariableMonthlyBudget: Equatable, Codable, Sendable, Identifiable {
    public let id: UUID
    public var name: String
    public var plannedAmount: Money
    public var category: VariableBudgetCategory

    public init(id: UUID = UUID(), name: String, plannedAmount: Money,
                category: VariableBudgetCategory = .other) {
        self.id = id
        self.name = name
        self.plannedAmount = plannedAmount
        self.category = category
    }
}

// MARK: - Savings

/// What a savings pot is for. Drives the icon and label in the Sparen view.
public enum SavingsCategory: String, Codable, Sendable, CaseIterable, Identifiable {
    // Existing raw values kept stable so saved goals still decode; new ones
    // appended. Order here drives the picker order (most common goals first).
    case emergency
    case retirement
    case home
    case car
    case vacation
    case wedding
    case family
    case education
    case renovation
    case electronics
    case taxes
    case investment
    case health
    case gift
    case other
    public var id: String { rawValue }
}

/// A recurring savings pot: a monthly contribution toward something (car, holiday,
/// Säule 3a …), optionally with a target amount.
///
/// The **accumulated balance** is *derived*, not stored as a running total: it is
/// `startingBalance` (whatever was already in the pot when tracking began) plus one
/// `monthlyContribution` for every month from `startDate` up to the month being
/// viewed. So the app can answer "how much is already in there?" purely from how
/// long the plan has been running — all in integer Rappen, never floating point.
public struct SavingsGoal: Equatable, Codable, Sendable, Identifiable {
    public let id: UUID
    public var name: String
    public var category: SavingsCategory
    /// Optional goal amount. Zero means open-ended (e.g. Säule 3a / Vorsorge).
    public var target: Money
    public var monthlyContribution: Money?
    /// Amount already in the pot when tracking began (often zero).
    public var startingBalance: Money
    /// The month the first contribution was made.
    public var startDate: Date

    public init(id: UUID = UUID(), name: String, category: SavingsCategory = .other,
                target: Money = .zero, monthlyContribution: Money? = nil,
                startingBalance: Money = .zero, startDate: Date = Date()) {
        self.id = id
        self.name = name
        self.category = category
        self.target = target
        self.monthlyContribution = monthlyContribution
        self.startingBalance = startingBalance
        self.startDate = startDate
    }

    public var hasTarget: Bool { target.isPositive }

    /// Whether a contribution is actually made in `month` — true only from the start
    /// month onward (a plan starting next year doesn't cost anything now).
    public func contributesIn(_ month: Date, calendar: Calendar = .swiss) -> Bool {
        calendar.startOfMonth(for: month) >= calendar.startOfMonth(for: startDate)
    }

    /// Number of monthly contributions made from `startDate`'s month up to and
    /// including `reference`'s month. Zero if `reference` is before the start month.
    public func monthsContributed(asOf reference: Date = Date(), calendar: Calendar = .swiss) -> Int {
        let start = calendar.startOfMonth(for: startDate)
        let current = calendar.startOfMonth(for: reference)
        guard current >= start else { return 0 }
        let months = calendar.dateComponents([.month], from: start, to: current).month ?? 0
        return months + 1
    }

    /// Accumulated balance as of `reference`: starting balance plus one monthly
    /// contribution for every month since the start. Exact integer arithmetic.
    public func accumulated(asOf reference: Date = Date(), calendar: Calendar = .swiss) -> Money {
        let months = Int64(monthsContributed(asOf: reference, calendar: calendar))
        let contributed = (monthlyContribution ?? .zero).scaled(by: months)
        return startingBalance + contributed
    }

    /// Progress toward the target (0–100) based on the accumulated balance. Returns
    /// 0 when there is no target.
    public func progressPercent(asOf reference: Date = Date(), calendar: Calendar = .swiss) -> Int {
        guard hasTarget else { return 0 }
        return accumulated(asOf: reference, calendar: calendar).percent(of: target)
    }
}
