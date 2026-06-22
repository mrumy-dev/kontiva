package ch.kontiva.android.core

import ch.kontiva.android.core.l10n.L10nKey

/** Light / dark / follow-system. */
enum class AppAppearance { SYSTEM, LIGHT, DARK }

/**
 * User-selectable accent colour. Recolours interactive/brand UI; danger semantics
 * (negative balances, overdue, errors) always stay red. 1:1 with iOS `AccentTheme`.
 * The actual Compose Color lives in the UI layer (see KontivaTheme.accentColor).
 */
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

/** Idle time before the app re-locks. */
enum class AutoLockInterval(val seconds: Long?, val displayLabel: String) {
    ONE_MINUTE(60, "1 Min."),
    FIVE_MINUTES(5 * 60, "5 Min."),
    FIFTEEN_MINUTES(15 * 60, "15 Min."),
    NEVER(null, "—"),
}

/** Non-secret application settings. */
data class AppSettings(
    val language: AppLanguage = AppLanguage.deCH,
    val appearance: AppAppearance = AppAppearance.SYSTEM,
    val accent: AccentTheme = AccentTheme.SWISS_RED,
)

/**
 * Security configuration. Contains NO secrets — only parameters and state.
 * Real key material lives in the security layer, only ever wrapped.
 */
data class SecuritySettings(
    val autoLock: AutoLockInterval = AutoLockInterval.FIVE_MINUTES,
    val hasPassphrase: Boolean = false,
    val recoveryWarningAcknowledged: Boolean = false,
)
