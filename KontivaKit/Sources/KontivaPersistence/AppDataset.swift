import Foundation
import KontivaCore

/// The entire application dataset, serialized as one Codable aggregate. The whole
/// thing is AES-256-GCM sealed before it ever touches disk (see `EncryptedStore`).
/// A household's data is small, so an in-memory model with a single encrypted
/// blob is simple, fast, and easy to reason about.
public struct AppDataset: Codable, Equatable, Sendable {
    public static let currentSchemaVersion = 1

    public var schemaVersion: Int
    public var household: Household?
    public var incomes: [Income]
    public var fixedCosts: [RecurringFixedExpense]
    public var variableBudgets: [VariableMonthlyBudget]
    public var savingsGoals: [SavingsGoal]
    public var bills: [OneOffBill]
    public var debts: [DebtItem]
    public var securitySettings: SecuritySettings
    public var appSettings: AppSettings

    public init(schemaVersion: Int = AppDataset.currentSchemaVersion,
                household: Household? = nil,
                incomes: [Income] = [],
                fixedCosts: [RecurringFixedExpense] = [],
                variableBudgets: [VariableMonthlyBudget] = [],
                savingsGoals: [SavingsGoal] = [],
                bills: [OneOffBill] = [],
                debts: [DebtItem] = [],
                securitySettings: SecuritySettings = SecuritySettings(),
                appSettings: AppSettings = AppSettings()) {
        self.schemaVersion = schemaVersion
        self.household = household
        self.incomes = incomes
        self.fixedCosts = fixedCosts
        self.variableBudgets = variableBudgets
        self.savingsGoals = savingsGoals
        self.bills = bills
        self.debts = debts
        self.securitySettings = securitySettings
        self.appSettings = appSettings
    }

    /// Tolerant decoder: every field is optional-on-read so adding new fields (like
    /// `debts`) never breaks an older on-disk store written before the field existed.
    public init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        schemaVersion   = try c.decodeIfPresent(Int.self, forKey: .schemaVersion) ?? AppDataset.currentSchemaVersion
        household       = try c.decodeIfPresent(Household.self, forKey: .household)
        incomes         = try c.decodeIfPresent([Income].self, forKey: .incomes) ?? []
        fixedCosts      = try c.decodeIfPresent([RecurringFixedExpense].self, forKey: .fixedCosts) ?? []
        variableBudgets = try c.decodeIfPresent([VariableMonthlyBudget].self, forKey: .variableBudgets) ?? []
        savingsGoals    = try c.decodeIfPresent([SavingsGoal].self, forKey: .savingsGoals) ?? []
        bills           = try c.decodeIfPresent([OneOffBill].self, forKey: .bills) ?? []
        debts           = try c.decodeIfPresent([DebtItem].self, forKey: .debts) ?? []
        securitySettings = try c.decodeIfPresent(SecuritySettings.self, forKey: .securitySettings) ?? SecuritySettings()
        appSettings     = try c.decodeIfPresent(AppSettings.self, forKey: .appSettings) ?? AppSettings()
    }

    /// A fresh empty dataset.
    public static var empty: AppDataset { AppDataset() }

    public var hasFinancialData: Bool {
        !incomes.isEmpty || !fixedCosts.isEmpty || !variableBudgets.isEmpty || !bills.isEmpty
    }
}
