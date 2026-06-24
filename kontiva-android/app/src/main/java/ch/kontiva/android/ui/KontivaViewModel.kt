package ch.kontiva.android.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ch.kontiva.android.core.AccentTheme
import ch.kontiva.android.core.ThemeStyle
import ch.kontiva.android.core.AppLanguage
import ch.kontiva.android.core.AppSettings
import ch.kontiva.android.core.AutoLockInterval
import ch.kontiva.android.core.AvailabilityEngine
import ch.kontiva.android.core.BillSort
import ch.kontiva.android.core.BillStatus
import ch.kontiva.android.core.Bonus
import ch.kontiva.android.core.Canton
import ch.kontiva.android.core.DebtItem
import ch.kontiva.android.core.DebtType
import ch.kontiva.android.core.Insight
import ch.kontiva.android.core.InsightEngine
import ch.kontiva.android.core.FixedExpenseCategory
import ch.kontiva.android.core.Income
import ch.kontiva.android.core.Money
import ch.kontiva.android.core.MonthlyAvailability
import ch.kontiva.android.core.OneOffBill
import ch.kontiva.android.core.RecurringFixedExpense
import ch.kontiva.android.core.SavingsCategory
import ch.kontiva.android.core.SavingsGoal
import ch.kontiva.android.core.SavingsSort
import ch.kontiva.android.core.SettingsStore
import ch.kontiva.android.core.ThirteenthSalaryModel
import ch.kontiva.android.core.VariableBudgetCategory
import ch.kontiva.android.core.VariableMonthlyBudget
import java.time.LocalDate
import ch.kontiva.android.persistence.AppDataset
import ch.kontiva.android.persistence.EncryptedStore
import ch.kontiva.android.persistence.Household
import ch.kontiva.android.persistence.StoreLocation
import ch.kontiva.android.security.BiometricAuth
import ch.kontiva.android.security.BiometricVault
import ch.kontiva.android.security.WrongPassphraseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

enum class AppPhase { ONBOARDING, LOCKED, UNLOCKED }

/**
 * App state machine (1:1 with the iOS AppModel): needs-setup → locked → unlocked.
 * Owns the encrypted store and the non-secret settings. The expensive KDF runs on
 * Dispatchers.Default so the UI stays responsive.
 */
class KontivaViewModel(app: Application) : AndroidViewModel(app) {
    private val store = EncryptedStore(StoreLocation(app.filesDir))
    private val settingsStore = SettingsStore(app)

    var phase by mutableStateOf(
        if (store.hasExistingVault()) AppPhase.LOCKED else AppPhase.ONBOARDING,
    )
        private set

    var settings: AppSettings by mutableStateOf(
        settingsStore.load(deviceDefault = AppLanguage.bestForDevice(deviceLocales())),
    )
        private set

    var household by mutableStateOf<Household?>(null)
        private set

    /** The decrypted dataset while unlocked; drives the planning + overview screens. */
    var dataset by mutableStateOf(AppDataset.empty)
        private set

    var busy by mutableStateOf(false)
        private set
    var unlockFailed by mutableStateOf(false)
        private set

    /** Held only while unlocked, to enrol biometrics without re-prompting; cleared on lock. */
    private var sessionPassphrase: String? = null

    /** Whether a passphrase is stored behind biometrics (mirrors iOS biometricEnabled). */
    var biometricEnabled by mutableStateOf(BiometricVault.hasStored(app))
        private set

    /** Whether the device offers a usable biometric (fingerprint/face). */
    val biometricAvailable: Boolean get() = BiometricAuth.isAvailable(getApplication())

    /** One-shot: offer to enable biometric unlock right after onboarding finishes. */
    var offerBiometricSetup by mutableStateOf(false)
        private set
    fun dismissBiometricOffer() { offerBiometricSetup = false }

    /** The month the whole app is viewing (first of month). Drives availability,
     *  bill classification, savings accumulation, and insights. */
    var selectedMonth by mutableStateOf(LocalDate.now().withDayOfMonth(1))
        private set

    fun previousMonth() { selectedMonth = selectedMonth.minusMonths(1) }
    fun nextMonth() { selectedMonth = selectedMonth.plusMonths(1) }
    fun goToToday() { selectedMonth = LocalDate.now().withDayOfMonth(1) }
    val isCurrentMonth: Boolean get() = selectedMonth == LocalDate.now().withDayOfMonth(1)

    // Auto-lock: lock the vault when the app has been backgrounded longer than the
    // chosen interval (NEVER disables it). Driven by the Activity lifecycle.
    private var backgroundedAt: Long? = null

    /** Set before launching an in-app system activity (file picker, share sheet) so the
     *  immediate auto-lock doesn't fire when that activity backgrounds us and returns. */
    private var suppressAutoLockOnce = false
    fun suppressNextAutoLock() { suppressAutoLockOnce = true }

    fun onAppBackgrounded() {
        if (phase == AppPhase.UNLOCKED) backgroundedAt = System.currentTimeMillis()
    }

    fun onAppForegrounded() {
        val since = backgroundedAt ?: return
        backgroundedAt = null
        if (suppressAutoLockOnce) { suppressAutoLockOnce = false; return }
        val interval = dataset.securitySettings.autoLock.seconds ?: return
        if ((System.currentTimeMillis() - since) / 1000 >= interval) lock()
    }

    init {
        // Persist the resolved default so the lock screen reads the same language.
        settingsStore.save(settings)
    }

    fun setLanguage(language: AppLanguage) = applySettings(settings.copy(language = language))
    /** Pick a solid accent (resets to the single-colour style, clears any custom colours). */
    fun setAccent(accent: AccentTheme) =
        applySettings(settings.copy(accent = accent, themeStyle = ThemeStyle.SOLID, accentSecondary = accent,
            customAccent = null, customAccentSecondary = null))
    /** Apply a curated preset: primary accent + style + secondary (clears custom colours). */
    fun applyTheme(primary: AccentTheme, style: ThemeStyle, secondary: AccentTheme) =
        applySettings(settings.copy(accent = primary, themeStyle = style, accentSecondary = secondary,
            customAccent = null, customAccentSecondary = null))
    /** Apply a fully custom theme: any primary (+ second) hex colour and a style. */
    fun applyCustomTheme(primaryHex: String, style: ThemeStyle, secondaryHex: String?) =
        applySettings(settings.copy(themeStyle = style, customAccent = primaryHex, customAccentSecondary = secondaryHex))
    fun setSavingsSort(sort: SavingsSort) = applySettings(settings.copy(savingsSort = sort))
    fun setBillSort(sort: BillSort) = applySettings(settings.copy(billSort = sort))

    private fun applySettings(updated: AppSettings) {
        settings = updated
        settingsStore.save(updated)
        if (store.isUnlocked) runCatching { store.mutate { it.copy(appSettings = updated) }; dataset = store.snapshot() }
    }

    fun updateProfile(name: String, canton: Canton?, avatar: String? = null) {
        edit {
            it.copy(
                household = if (name.isBlank()) null
                else (it.household?.copy(name = name, canton = canton, avatarName = avatar) ?: Household(name = name, canton = canton, avatarName = avatar)),
            )
        }
        household = dataset.household
    }

    fun setAutoLock(interval: AutoLockInterval) = edit {
        it.copy(securitySettings = it.securitySettings.copy(autoLock = interval))
    }

    /** Re-wrap the master key under a new passphrase; reports success on the main thread. */
    fun changePassphrase(old: String, new: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.Default) {
                try { store.changePassphrase(old, new); true } catch (_: Exception) { false }
            }
            if (ok) {
                sessionPassphrase = new
                if (biometricEnabled) BiometricVault.store(getApplication(), new) // keep biometrics in sync
            }
            onResult(ok)
        }
    }

    /** Danger zone: wipe the vault and return to onboarding. */
    fun deleteAllData() {
        store.deleteAllData()
        disableBiometric()
        sessionPassphrase = null
        household = null
        dataset = AppDataset.empty
        // A fresh start returns to the brand default — the theme is per-vault.
        applySettings(settings.copy(accent = AccentTheme.SWISS_RED, themeStyle = ThemeStyle.SOLID,
            accentSecondary = AccentTheme.SWISS_RED, customAccent = null, customAccentSecondary = null))
        phase = AppPhase.ONBOARDING
    }

    /** Encrypted backup blob under a separate backup passphrase. */
    fun makeBackup(passphrase: String): ByteArray = store.makeBackup(passphrase, "0.0.1")

    /** Restore from a backup blob; returns false on a wrong passphrase / corrupt file. */
    fun restoreBackup(data: ByteArray, passphrase: String): Boolean = try {
        store.restoreBackup(data, passphrase)
        dataset = store.snapshot()
        household = dataset.household
        true
    } catch (_: Exception) {
        false
    }

    /** Finish onboarding: create the encrypted vault, store profile + settings, unlock. */
    fun completeSetup(passphrase: String, name: String, avatar: String?) {
        if (busy) return
        busy = true
        viewModelScope.launch {
            val hh = if (name.isNotBlank()) Household(name = name, avatarName = avatar) else null
            withContext(Dispatchers.Default) {
                store.createVault(passphrase, AppDataset(household = hh, appSettings = settings))
            }
            sessionPassphrase = passphrase
            household = hh
            dataset = store.snapshot()
            busy = false
            offerBiometricSetup = biometricAvailable && !biometricEnabled
            phase = AppPhase.UNLOCKED
        }
    }

    /** Unlock an existing vault; flips [unlockFailed] on a wrong code. */
    fun unlock(passphrase: String) {
        if (busy) return
        busy = true
        unlockFailed = false
        viewModelScope.launch {
            val ok = withContext(Dispatchers.Default) {
                try {
                    store.unlock(passphrase); true
                } catch (_: WrongPassphraseException) {
                    false
                }
            }
            if (ok) {
                sessionPassphrase = passphrase
                val snap = store.snapshot()
                household = snap.household
                dataset = snap
                settings = snap.appSettings
                settingsStore.save(settings)
                phase = AppPhase.UNLOCKED
            } else {
                unlockFailed = true
            }
            busy = false
        }
    }

    fun clearUnlockError() {
        unlockFailed = false
    }

    // Planning ----------------------------------------------------------------

    /** The live monthly breakdown (income − fixed − variable − bills − savings). */
    val availability: MonthlyAvailability
        get() = AvailabilityEngine.compute(
            dataset.incomes, dataset.fixedCosts, dataset.variableBudgets, dataset.bills, dataset.savingsGoals, selectedMonth,
        )

    private fun edit(block: (AppDataset) -> AppDataset) {
        runCatching {
            store.mutate(block)
            dataset = store.snapshot()
        }
    }

    fun addIncome(label: String, amount: Money, thirteenth: Money? = null, thirteenthModel: ThirteenthSalaryModel = ThirteenthSalaryModel.DECEMBER, bonuses: List<Bonus> = emptyList()) =
        edit { it.copy(incomes = it.incomes + Income(label = label, monthlyNet = amount, thirteenthAmount = thirteenth, thirteenthModel = thirteenthModel, bonuses = bonuses)) }

    fun addFixedCost(name: String, amount: Money, category: FixedExpenseCategory, startMonth: LocalDate? = null, installments: Int? = null) =
        edit { it.copy(fixedCosts = it.fixedCosts + RecurringFixedExpense(name = name, monthlyAmount = amount, category = category, startMonth = startMonth, installments = installments)) }

    fun addVariableBudget(name: String, amount: Money, category: VariableBudgetCategory) =
        edit { it.copy(variableBudgets = it.variableBudgets + VariableMonthlyBudget(name = name, plannedAmount = amount, category = category)) }

    fun deleteIncome(id: String) = edit { it.copy(incomes = it.incomes.filterNot { e -> e.id == id }) }
    fun deleteFixedCost(id: String) = edit { it.copy(fixedCosts = it.fixedCosts.filterNot { e -> e.id == id }) }
    fun deleteVariableBudget(id: String) = edit { it.copy(variableBudgets = it.variableBudgets.filterNot { e -> e.id == id }) }

    fun addBill(provider: String, amount: Money, dueDate: LocalDate, paid: Boolean, notes: String? = null) = edit {
        it.copy(bills = it.bills + OneOffBill(provider = provider, amount = amount, dueDate = dueDate, status = if (paid) BillStatus.PAID else BillStatus.OPEN, notes = notes))
    }

    fun toggleBillPaid(id: String) = edit { ds ->
        ds.copy(bills = ds.bills.map { if (it.id == id) it.copy(status = if (it.status == BillStatus.PAID) BillStatus.OPEN else BillStatus.PAID) else it })
    }

    fun deleteBill(id: String) = edit { it.copy(bills = it.bills.filterNot { e -> e.id == id }) }

    fun addSavingsGoal(name: String, category: SavingsCategory, monthly: Money?, startingBalance: Money, target: Money, startDate: LocalDate = LocalDate.now()) = edit {
        it.copy(savingsGoals = it.savingsGoals + SavingsGoal(name = name, category = category, monthlyContribution = monthly, startingBalance = startingBalance, target = target, startDate = startDate))
    }

    fun deleteSavingsGoal(id: String) = edit { it.copy(savingsGoals = it.savingsGoals.filterNot { e -> e.id == id }) }

    /** Mark a reached goal done: freeze its balance and stop it reducing "available". */
    fun completeSavingsGoal(id: String) = edit { ds ->
        val done = LocalDate.now().withDayOfMonth(1)
        ds.copy(savingsGoals = ds.savingsGoals.map { if (it.id == id) it.copy(completedDate = done) else it })
    }
    /** Resume contributing to a previously completed goal. */
    fun reopenSavingsGoal(id: String) = edit { ds ->
        ds.copy(savingsGoals = ds.savingsGoals.map { if (it.id == id) it.copy(completedDate = null) else it })
    }

    fun addDebt(creditor: String, amount: Money, type: DebtType, date: LocalDate? = null, reference: String? = null, notes: String? = null) = edit {
        it.copy(debts = it.debts + DebtItem(creditor = creditor, amount = amount, type = type, date = date, reference = reference, notes = notes))
    }

    fun deleteDebt(id: String) = edit { it.copy(debts = it.debts.filterNot { e -> e.id == id }) }

    // Edits ----------------------------------------------------------------
    fun updateIncome(id: String, label: String, amount: Money, thirteenth: Money?, thirteenthModel: ThirteenthSalaryModel = ThirteenthSalaryModel.DECEMBER, bonuses: List<Bonus> = emptyList()) =
        edit { ds -> ds.copy(incomes = ds.incomes.map { if (it.id == id) it.copy(label = label, monthlyNet = amount, thirteenthAmount = thirteenth, thirteenthModel = thirteenthModel, bonuses = bonuses) else it }) }

    fun updateFixedCost(id: String, name: String, amount: Money, category: FixedExpenseCategory, startMonth: LocalDate? = null, installments: Int? = null) =
        edit { ds -> ds.copy(fixedCosts = ds.fixedCosts.map { if (it.id == id) it.copy(name = name, monthlyAmount = amount, category = category, startMonth = startMonth, installments = installments) else it }) }

    fun updateVariableBudget(id: String, name: String, amount: Money, category: VariableBudgetCategory) =
        edit { ds -> ds.copy(variableBudgets = ds.variableBudgets.map { if (it.id == id) it.copy(name = name, plannedAmount = amount, category = category) else it }) }

    fun updateBill(id: String, provider: String, amount: Money, dueDate: LocalDate, paid: Boolean, notes: String? = null) =
        edit { ds -> ds.copy(bills = ds.bills.map { if (it.id == id) it.copy(provider = provider, amount = amount, dueDate = dueDate, status = if (paid) BillStatus.PAID else BillStatus.OPEN, notes = notes) else it }) }

    fun updateSavingsGoal(id: String, name: String, category: SavingsCategory, monthly: Money?, starting: Money, target: Money, startDate: LocalDate = LocalDate.now()) =
        edit { ds -> ds.copy(savingsGoals = ds.savingsGoals.map { if (it.id == id) it.copy(name = name, category = category, monthlyContribution = monthly, startingBalance = starting, target = target, startDate = startDate) else it }) }

    fun updateDebt(id: String, creditor: String, amount: Money, type: DebtType, date: LocalDate? = null, reference: String? = null, notes: String? = null) =
        edit { ds -> ds.copy(debts = ds.debts.map { if (it.id == id) it.copy(creditor = creditor, amount = amount, type = type, date = date, reference = reference, notes = notes) else it }) }

    /** Rule-based insights about this month's plan. */
    val insights: List<Insight>
        get() = InsightEngine.analyze(dataset.incomes, dataset.fixedCosts, dataset.variableBudgets, dataset.bills, dataset.savingsGoals, availability, selectedMonth)

    fun lock() {
        store.lock()
        sessionPassphrase = null
        household = null
        phase = AppPhase.LOCKED
    }

    // Biometric unlock (fingerprint/face) — 1:1 with the iOS BiometricVault flow.

    /** Store the current passphrase behind biometrics. Requires being unlocked. */
    fun enableBiometric(): Boolean {
        val pass = sessionPassphrase ?: return false
        val ok = BiometricVault.store(getApplication(), pass)
        biometricEnabled = BiometricVault.hasStored(getApplication())
        return ok
    }

    /** Forget the biometric-stored passphrase. */
    fun disableBiometric() {
        BiometricVault.clear(getApplication())
        biometricEnabled = false
    }

    /** The passphrase stored behind biometrics — call only after a successful prompt. */
    fun biometricPassphrase(): String? = BiometricVault.retrieve(getApplication())

    private fun deviceLocales(): List<Locale> {
        val list = getApplication<Application>().resources.configuration.locales
        return (0 until list.size()).map { list[it] }.ifEmpty { listOf(Locale.getDefault()) }
    }
}
