package ch.kontiva.android.core

import ch.kontiva.android.core.l10n.L10nKey
import kotlinx.serialization.Serializable

/** Light / dark / follow-system. */
@Serializable
enum class AppAppearance { SYSTEM, LIGHT, DARK }

/**
 * User-selectable accent colour. Recolours interactive/brand UI; danger semantics
 * (negative balances, overdue, errors) always stay red. 1:1 with iOS `AccentTheme`.
 * The actual Compose Color lives in the UI layer (see KontivaTheme.accentColor).
 */
@Serializable
enum class AccentTheme(val labelKey: L10nKey) {
    SWISS_RED(L10nKey.themeSwissRed), // brand default
    ORANGE(L10nKey.themeOrange),
    SAND(L10nKey.themeSand),
    GREEN(L10nKey.themeGreen),
    TEAL(L10nKey.themeTeal),
    BLUE(L10nKey.themeBlue),
    PURPLE(L10nKey.themePurple),
    PINK(L10nKey.themePink),
}

/** Idle time before the app re-locks. IMMEDIATE locks the moment you leave the app
 *  or the phone screen turns off. */
@Serializable
enum class AutoLockInterval(val seconds: Long?, val displayLabel: String) {
    IMMEDIATE(0, "Sofort"),
    ONE_MINUTE(60, "1 Min."),
    FIVE_MINUTES(5 * 60, "5 Min."),
    FIFTEEN_MINUTES(15 * 60, "15 Min."),
    NEVER(null, "—"),
}

/** How the Sparen list is ordered. 1:1 with iOS `SavingsSort`. */
@Serializable
enum class SavingsSort(val labelKey: L10nKey) {
    START_MONTH(L10nKey.sparenSortStartMonth),
    MONTHLY(L10nKey.formMonthlyContribution),
    ACCUMULATED(L10nKey.sparenAccumulatedTotal),
    CATEGORY(L10nKey.formCategory),
    NAME(L10nKey.formName);

    /** Order [goals] by this criterion (accumulated is as-of [month]). */
    fun apply(goals: List<SavingsGoal>, month: java.time.LocalDate): List<SavingsGoal> = when (this) {
        START_MONTH -> goals.sortedBy { it.startDate }
        MONTHLY -> goals.sortedByDescending { it.monthlyContribution?.rappen ?: 0L }
        ACCUMULATED -> goals.sortedByDescending { it.accumulated(month).rappen }
        CATEGORY -> goals.sortedBy { it.category.ordinal }
        NAME -> goals.sortedBy { it.name.lowercase() }
    }
}

/** How the Rechnungen list is ordered within each status section. 1:1 with iOS `BillSort`. */
@Serializable
enum class BillSort(val labelKey: L10nKey) {
    DUE_DATE(L10nKey.billsDueDate),
    AMOUNT(L10nKey.formAmount),
    PROVIDER(L10nKey.billsProvider);

    /** Order one section's [bills] by this criterion. */
    fun apply(bills: List<OneOffBill>): List<OneOffBill> = when (this) {
        DUE_DATE -> bills.sortedBy { it.dueDate }
        AMOUNT -> bills.sortedByDescending { it.amount.rappen }
        PROVIDER -> bills.sortedBy { it.provider.lowercase() }
    }
}

/** Non-secret application settings. */
@Serializable
data class AppSettings(
    val language: AppLanguage = AppLanguage.deCH,
    val appearance: AppAppearance = AppAppearance.SYSTEM,
    val accent: AccentTheme = AccentTheme.SWISS_RED,
    val savingsSort: SavingsSort = SavingsSort.START_MONTH,
    val billSort: BillSort = BillSort.DUE_DATE,
)

/**
 * Security configuration. Contains NO secrets — only parameters and state.
 * Real key material lives in the security layer, only ever wrapped.
 */
@Serializable
data class SecuritySettings(
    val autoLock: AutoLockInterval = AutoLockInterval.IMMEDIATE,
    val hasPassphrase: Boolean = false,
    val recoveryWarningAcknowledged: Boolean = false,
)
