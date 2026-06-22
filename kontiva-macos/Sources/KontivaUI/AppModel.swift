import SwiftUI
import KontivaCore
import KontivaSecurity
import KontivaPersistence

/// Navigation sections (the sidebar).
public enum AppSection: String, CaseIterable, Identifiable, Hashable {
    public var id: String { rawValue }
    // Settings is not here — it lives in the native ⌘, Settings window.
    case overview, insights, planning, sparen, bills, schulden, report

    public var titleKey: L10nKey {
        switch self {
        case .overview:  return .navOverview
        case .insights:  return .navInsights
        case .planning:  return .navPlanning
        case .sparen:    return .navSparen
        case .bills:     return .navBills
        case .schulden:  return .navSchulden
        case .report:    return .navReport
        }
    }

    public var systemImage: String {
        switch self {
        case .overview:  return "square.grid.2x2"
        case .insights:  return "lightbulb"
        case .planning:  return "calendar"
        case .sparen:    return "banknote"
        case .bills:     return "doc.text"
        case .schulden:  return "creditcard"
        case .report:    return "exclamationmark.bubble"
        }
    }
}

/// Lock gate state.
public enum LockState: Equatable {
    case needsSetup   // first run — no vault on disk yet
    case locked       // vault exists, awaiting passphrase
    case unlocked
}

@MainActor
public final class AppModel: ObservableObject {

    // Settings & language.
    @Published public var settings = AppSettings()
    public let localizer: Localizer

    // Lock gate, backed by the encrypted store (KontivaSecurity + KontivaPersistence).
    @Published public var lockState: LockState
    @Published public var recoveryAcknowledged = false
    @Published public var isWorking = false

    /// The decrypted dataset, mirrored on the main actor for synchronous UI reads.
    /// Source of truth is the encrypted store; refreshed after every store op.
    @Published public private(set) var dataset = AppDataset.empty

    // Selected sidebar section.
    @Published public var selection: AppSection = .overview

    // Menu-command signals: ⌘N (add in the current section) and ⌘E (export) bump
    // these; the active screen / root observes them. (Menu actions can't reach a
    // screen's local sheet state directly.)
    @Published public var newItemSignal = 0
    @Published public var exportSignal = 0
    /// Bumped after any entry is saved — the root shows a brief confirmation toast.
    @Published public var saveSignal = 0
    /// True for one render right after unlocking — the dashboard counts its figures
    /// up from zero once.
    @Published public var justUnlocked = false

    // The month being viewed/planned. Recurring income/costs apply to every month;
    // bills are placed by their due date. Changing this re-classifies bills and
    // recomputes the available balance + insights relative to the chosen month.
    @Published public var selectedMonth: Date = Calendar.swiss.startOfMonth(for: Date())

    public func shiftMonth(by months: Int) {
        if let shifted = Calendar.swiss.date(byAdding: .month, value: months, to: selectedMonth) {
            selectedMonth = Calendar.swiss.startOfMonth(for: shifted)
        }
    }
    public func goToCurrentMonth() {
        selectedMonth = Calendar.swiss.startOfMonth(for: Date())
    }
    public var isCurrentMonth: Bool {
        Calendar.swiss.isDate(selectedMonth, equalTo: Date(), toGranularity: .month)
    }

    // Auto-lock: idle interval (persisted in the dataset) + last activity timestamp.
    @Published public var autoLock: AutoLockInterval = .fiveMinutes
    private var lastActivity = Date()

    /// Whether the unlock code is stored behind biometrics (Touch ID).
    @Published public private(set) var biometricEnabled = BiometricVault.hasStored
    var biometricKind: BiometricKind { Biometrics.kind }
    /// Held only while unlocked, to enrol biometrics without re-prompting; cleared on lock.
    private var sessionPassphrase: String?

    private let store: EncryptedStore

    private static let languageDefaultsKey = "kontiva.ui.language"
    private static let accentDefaultsKey = "kontiva.ui.accent"

    public init(language: AppLanguage = .deCH) {
        // The UI language is a non-sensitive preference, stored in UserDefaults so
        // it persists across launches and applies to the lock screen (before the
        // encrypted store is unlocked).
        let saved = UserDefaults.standard.string(forKey: Self.languageDefaultsKey)
        let initial = saved.flatMap(AppLanguage.init(rawValue:)) ?? language
        self.localizer = Localizer(language: initial)
        let location = (try? StoreLocation.applicationSupport())
            ?? StoreLocation(directory: FileManager.default.temporaryDirectory
                .appendingPathComponent("Kontiva", isDirectory: true))
        let store = EncryptedStore(location: location)
        self.store = store
        self.lockState = store.hasExistingVault() ? .locked : .needsSetup
        // All stored properties initialized; now safe to mutate.
        self.settings.language = initial
        // Accent theme is also a non-sensitive UI preference (UserDefaults), so it
        // applies on the lock screen and persists across launches.
        let savedAccent = UserDefaults.standard.string(forKey: Self.accentDefaultsKey)
            .flatMap(AccentTheme.init(rawValue:)) ?? .swissRed
        self.settings.accent = savedAccent
        KontivaTheme.accent = savedAccent.color
        // First ever run → guided onboarding instead of a bare setup prompt.
        self.onboardingActive = !store.hasExistingVault()
    }

    // MARK: Accent theme

    /// Change the accent colour. Applies immediately across the app and persists.
    public func setAccent(_ accent: AccentTheme) {
        guard accent != settings.accent else { return }
        settings.accent = accent
        KontivaTheme.accent = accent.color
        UserDefaults.standard.set(accent.rawValue, forKey: Self.accentDefaultsKey)
    }

    // MARK: Backup reminder (data-loss safety net)

    private static let backupReminderKey = "kontiva.ui.backupReminderDismissed"
    @Published public var backupReminderDismissed =
        UserDefaults.standard.bool(forKey: AppModel.backupReminderKey)

    /// Dismiss the "make a backup" nudge for good (the user made a backup, or chose not to).
    public func dismissBackupReminder() {
        backupReminderDismissed = true
        UserDefaults.standard.set(true, forKey: Self.backupReminderKey)
    }

    public var hasFinancialData: Bool { dataset.hasFinancialData }

    /// Show the backup nudge once the user has real data and hasn't acted on it —
    /// there is no passphrase recovery, so a backup is the only safety net.
    public var shouldShowBackupReminder: Bool {
        lockState == .unlocked && hasFinancialData && !backupReminderDismissed
    }

    /// Guided first-run flow is active (welcome → passphrase → profile). Stays true
    /// even after the vault unlocks, so the flow can finish before the main UI shows.
    @Published public var onboardingActive = false

    /// Finish onboarding and drop into the app.
    public func finishOnboarding() { onboardingActive = false }

    // MARK: Language

    public func setLanguage(_ language: AppLanguage) {
        guard language != localizer.language else { return } // ignore no-op writes
        settings.language = language
        localizer.setLanguage(language)
        UserDefaults.standard.set(language.rawValue, forKey: Self.languageDefaultsKey)
    }

    // MARK: Lock gate — real key wrapping + encrypted persistence
    //
    // The KDF and file I/O run inside the EncryptedStore actor (off the main
    // thread). The decrypted key and dataset live in the store only while
    // unlocked and are dropped on lock.

    public func completeSetup(passphrase: String) async {
        guard recoveryAcknowledged, !passphrase.isEmpty else { return }
        isWorking = true
        defer { isWorking = false }
        do {
            try await store.createVault(passphrase: passphrase)
            sessionPassphrase = passphrase
            await refreshDataset()
            lockState = .unlocked
        } catch {
            // e.g. a vault already exists; stay on the gate.
        }
    }

    /// Returns true on success, false on wrong passphrase (AES-GCM auth failure).
    @discardableResult
    public func unlock(passphrase: String) async -> Bool {
        isWorking = true
        defer { isWorking = false }
        do {
            try await store.unlock(passphrase: passphrase)
            sessionPassphrase = passphrase
            await refreshDataset()
            justUnlocked = true
            lockState = .unlocked
            return true
        } catch {
            return false
        }
    }

    public func lock() async {
        await store.lock()
        sessionPassphrase = nil
        dataset = .empty
        lockState = store.hasExistingVault() ? .locked : .needsSetup
    }

    // MARK: Biometric unlock (Touch ID)

    /// Store the current code behind biometrics. Requires being unlocked.
    @discardableResult
    public func enableBiometric() -> Bool {
        guard let code = sessionPassphrase else { return false }
        let ok = BiometricVault.store(code: code)
        biometricEnabled = BiometricVault.hasStored
        return ok
    }

    public func disableBiometric() {
        BiometricVault.delete()
        biometricEnabled = false
    }

    /// Prompt biometrics and unlock with the stored code. Returns success.
    @discardableResult
    public func unlockWithBiometrics() async -> Bool {
        guard biometricEnabled, biometricKind.isAvailable else { return false }
        guard let code = await BiometricVault.retrieve(reason: localizer.string(.lockTitle)) else { return false }
        let ok = await unlock(passphrase: code)
        if !ok { disableBiometric() }   // stored code no longer valid → clear it
        return ok
    }

    // MARK: Auto-lock (idle timer)

    /// Record user activity (mouse/keyboard) to reset the idle timer.
    public func noteActivity() { lastActivity = Date() }

    /// Lock if the app has been idle past the configured interval. Called by a
    /// periodic timer in the UI; the decision uses the tested `AutoLock` logic.
    public func checkAutoLock(now: Date = Date()) async {
        guard lockState == .unlocked else { return }
        if AutoLock.shouldLock(lastActivity: lastActivity, now: now, interval: autoLock) {
            await lock()
        }
    }

    /// Change and persist the auto-lock interval.
    public func setAutoLock(_ interval: AutoLockInterval) async {
        autoLock = interval
        await mutate { $0.securitySettings.autoLock = interval }
    }

    /// Re-wrap the master key under a new passphrase. False if the old is wrong.
    @discardableResult
    public func changePassphrase(old: String, new: String) async -> Bool {
        isWorking = true
        defer { isWorking = false }
        do { try await store.changePassphrase(old: old, new: new); return true }
        catch { return false }
    }

    /// Danger zone: erase all local data and return to first-run setup.
    public func deleteAllLocalData() async {
        try? await store.deleteAllData()
        disableBiometric()
        sessionPassphrase = nil
        dataset = .empty
        recoveryAcknowledged = false
        lockState = .needsSetup
        onboardingActive = true   // route back through the guided first-run flow
    }

    private func refreshDataset() async {
        dataset = (try? await store.snapshot()) ?? .empty
        autoLock = dataset.securitySettings.autoLock
        noteActivity() // a refresh implies interaction
    }

    // MARK: CRUD (Phase 4) — every change is persisted encrypted via the store.

    private func mutate(_ block: @Sendable @escaping (inout AppDataset) -> Void) async {
        guard lockState == .unlocked else { return }
        try? await store.mutate(block)
        await refreshDataset()
    }

    public func upsertIncome(_ income: Income) async {
        await mutate { ds in
            if let i = ds.incomes.firstIndex(where: { $0.id == income.id }) { ds.incomes[i] = income }
            else { ds.incomes.append(income) }
        }
        saveSignal &+= 1
    }
    public func deleteIncome(_ id: UUID) async { await mutate { $0.incomes.removeAll { $0.id == id } } }

    public func upsertFixedCost(_ item: RecurringFixedExpense) async {
        await mutate { ds in
            if let i = ds.fixedCosts.firstIndex(where: { $0.id == item.id }) { ds.fixedCosts[i] = item }
            else { ds.fixedCosts.append(item) }
        }
        saveSignal &+= 1
    }
    public func deleteFixedCost(_ id: UUID) async { await mutate { $0.fixedCosts.removeAll { $0.id == id } } }

    public func upsertVariableBudget(_ item: VariableMonthlyBudget) async {
        await mutate { ds in
            if let i = ds.variableBudgets.firstIndex(where: { $0.id == item.id }) { ds.variableBudgets[i] = item }
            else { ds.variableBudgets.append(item) }
        }
        saveSignal &+= 1
    }
    public func deleteVariableBudget(_ id: UUID) async { await mutate { $0.variableBudgets.removeAll { $0.id == id } } }

    public func upsertSavingsGoal(_ item: SavingsGoal) async {
        await mutate { ds in
            if let i = ds.savingsGoals.firstIndex(where: { $0.id == item.id }) { ds.savingsGoals[i] = item }
            else { ds.savingsGoals.append(item) }
        }
        saveSignal &+= 1
    }
    public func deleteSavingsGoal(_ id: UUID) async { await mutate { $0.savingsGoals.removeAll { $0.id == id } } }

    public func upsertBill(_ bill: OneOffBill) async {
        await mutate { ds in
            if let i = ds.bills.firstIndex(where: { $0.id == bill.id }) { ds.bills[i] = bill }
            else { ds.bills.append(bill) }
        }
        saveSignal &+= 1
    }
    public func deleteBill(_ id: UUID) async { await mutate { $0.bills.removeAll { $0.id == id } } }

    // MARK: Debts (Schulden)

    public func upsertDebt(_ debt: DebtItem) async {
        await mutate { ds in
            if let i = ds.debts.firstIndex(where: { $0.id == debt.id }) { ds.debts[i] = debt }
            else { ds.debts.append(debt) }
        }
        saveSignal &+= 1
    }
    public func deleteDebt(_ id: UUID) async { await mutate { $0.debts.removeAll { $0.id == id } } }

    public var debts: [DebtItem] { dataset.debts }

    /// Open bills already overdue as of today — these are debts in all but name and
    /// flow into the Schulden view automatically (managed back in Rechnungen).
    public var overdueBills: [OneOffBill] {
        dataset.bills.filter { BillClassifier.state(of: $0, asOf: Date()) == .overdue }
    }
    public var totalOverdueBills: Money { overdueBills.map(\.amount).total() }
    public var totalManualDebt: Money { dataset.debts.map(\.amount).total() }
    /// Everything owed: overdue bills + recorded formal/other debts.
    public var totalDebt: Money { totalOverdueBills + totalManualDebt }
    public var hasAnyDebt: Bool { !overdueBills.isEmpty || !dataset.debts.isEmpty }

    // MARK: Reordering (drag-to-reorder in the lists; display order only — the
    // available-balance maths is order-independent)

    public func moveIncomes(from: IndexSet, to: Int) async {
        await mutate { $0.incomes.move(fromOffsets: from, toOffset: to) }
    }
    public func moveFixedCosts(from: IndexSet, to: Int) async {
        await mutate { $0.fixedCosts.move(fromOffsets: from, toOffset: to) }
    }
    public func moveVariableBudgets(from: IndexSet, to: Int) async {
        await mutate { $0.variableBudgets.move(fromOffsets: from, toOffset: to) }
    }
    public func moveSavingsGoals(from: IndexSet, to: Int) async {
        await mutate { $0.savingsGoals.move(fromOffsets: from, toOffset: to) }
    }

    // MARK: Local profile

    /// The local household profile (name, picture, canton), if set.
    public var household: Household? { dataset.household }

    /// Create or update the local profile. Stored only on this device.
    public func updateProfile(name: String, avatarName: String?, canton: Canton?) async {
        await mutate { ds in
            var h = ds.household ?? Household(name: name)
            h.name = name
            h.avatarName = avatarName
            h.canton = canton
            ds.household = h
        }
    }

    // MARK: Backup & restore (Phase 5)

    /// Produce a portable encrypted backup blob, or nil on failure.
    public func makeBackupData(passphrase: String) async -> Data? {
        guard lockState == .unlocked else { return nil }
        isWorking = true; defer { isWorking = false }
        return try? await store.makeBackup(backupPassphrase: passphrase, appVersion: AppInfo.version)
    }

    /// Validate a backup and return its preview (counts/date), or nil if the
    /// passphrase is wrong or the file is invalid.
    public func previewBackup(data: Data, passphrase: String) async -> BackupPreview? {
        isWorking = true; defer { isWorking = false }
        return try? await store.previewBackup(data: data, backupPassphrase: passphrase)
    }

    /// Guarded restore — caller must have confirmed the destructive replace.
    @discardableResult
    public func restoreFromBackup(data: Data, passphrase: String) async -> Bool {
        guard lockState == .unlocked else { return false }
        isWorking = true; defer { isWorking = false }
        do {
            try await store.restoreBackup(data: data, backupPassphrase: passphrase)
            await refreshDataset()
            return true
        } catch {
            return false
        }
    }

    // MARK: Derived data for the UI (reads from the cached dataset)

    public var incomes: [Income] { dataset.incomes }
    public var fixedCosts: [RecurringFixedExpense] { dataset.fixedCosts }
    public var variableBudgets: [VariableMonthlyBudget] { dataset.variableBudgets }
    public var savingsGoals: [SavingsGoal] { dataset.savingsGoals }
    public var bills: [OneOffBill] { dataset.bills }

    public var hasAnyPlanningData: Bool {
        !incomes.isEmpty || !fixedCosts.isEmpty || !variableBudgets.isEmpty
    }

    public var availability: MonthlyAvailability {
        AvailabilityEngine.compute(
            incomes: incomes, fixedCosts: fixedCosts,
            variableBudgets: variableBudgets, bills: bills,
            savingsGoals: savingsGoals, asOf: selectedMonth)
    }

    /// The available balance and total accumulated savings for each of the last
    /// `months` months up to the selected month — computed from the current plan
    /// (bills, standing orders and savings accumulation genuinely vary by date).
    public func trend(months: Int = 6) -> [(month: Date, available: Money, savings: Money)] {
        let cal = Calendar.swiss
        return (0..<months).reversed().compactMap { offset in
            guard let shifted = cal.date(byAdding: .month, value: -offset, to: selectedMonth) else { return nil }
            let m = cal.startOfMonth(for: shifted)
            let a = AvailabilityEngine.compute(
                incomes: incomes, fixedCosts: fixedCosts, variableBudgets: variableBudgets,
                bills: bills, savingsGoals: savingsGoals, asOf: m)
            let saved = savingsGoals.map { $0.accumulated(asOf: m) }.total()
            return (m, a.available, saved)
        }
    }

    /// Total committed to savings each month (the sum of all monthly contributions).
    public var totalMonthlySavings: Money {
        savingsGoals.compactMap(\.monthlyContribution).total()
    }

    /// Total accumulated across all savings pots, as of the selected month.
    public var totalAccumulatedSavings: Money {
        savingsGoals.map { $0.accumulated(asOf: selectedMonth) }.total()
    }

    public var insights: [Insight] {
        InsightEngine.analyze(
            incomes: incomes, fixedCosts: fixedCosts, variableBudgets: variableBudgets,
            bills: bills, savingsGoals: savingsGoals, availability: availability,
            asOf: selectedMonth)
    }
}
