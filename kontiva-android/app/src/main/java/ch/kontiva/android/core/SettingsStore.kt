package ch.kontiva.android.core

import android.content.Context

/**
 * Non-secret settings persisted outside the encrypted vault (language, appearance,
 * accent) so they apply on the lock screen before unlock — mirrors the iOS use of
 * UserDefaults. Contains no personal or financial data.
 */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("kontiva.settings", Context.MODE_PRIVATE)

    fun load(deviceDefault: AppLanguage): AppSettings = AppSettings(
        language = AppLanguage.fromCode(prefs.getString(KEY_LANGUAGE, null)) ?: deviceDefault,
        appearance = prefs.getString(KEY_APPEARANCE, null).toEnum(AppAppearance.SYSTEM),
        accent = prefs.getString(KEY_ACCENT, null).toEnum(AccentTheme.SWISS_RED),
        themeStyle = prefs.getString(KEY_THEME_STYLE, null).toEnum(ThemeStyle.SOLID),
        accentSecondary = prefs.getString(KEY_ACCENT_SECONDARY, null).toEnum(AccentTheme.SWISS_RED),
        customAccent = prefs.getString(KEY_CUSTOM_ACCENT, null),
        customAccentSecondary = prefs.getString(KEY_CUSTOM_ACCENT_SECONDARY, null),
    )

    fun save(settings: AppSettings) {
        prefs.edit()
            .putString(KEY_LANGUAGE, settings.language.code)
            .putString(KEY_APPEARANCE, settings.appearance.name)
            .putString(KEY_ACCENT, settings.accent.name)
            .putString(KEY_THEME_STYLE, settings.themeStyle.name)
            .putString(KEY_ACCENT_SECONDARY, settings.accentSecondary.name)
            .putString(KEY_CUSTOM_ACCENT, settings.customAccent)
            .putString(KEY_CUSTOM_ACCENT_SECONDARY, settings.customAccentSecondary)
            .apply()
    }

    /** Records that the language default was resolved, so we only auto-pick once. */
    val hasStoredLanguage: Boolean get() = prefs.contains(KEY_LANGUAGE)

    private companion object {
        const val KEY_LANGUAGE = "language"
        const val KEY_APPEARANCE = "appearance"
        const val KEY_ACCENT = "accent"
        const val KEY_THEME_STYLE = "themeStyle"
        const val KEY_ACCENT_SECONDARY = "accentSecondary"
        const val KEY_CUSTOM_ACCENT = "customAccent"
        const val KEY_CUSTOM_ACCENT_SECONDARY = "customAccentSecondary"
    }
}

private inline fun <reified T : Enum<T>> String?.toEnum(default: T): T =
    this?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default
