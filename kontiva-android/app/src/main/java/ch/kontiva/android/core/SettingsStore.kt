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
    )

    fun save(settings: AppSettings) {
        prefs.edit()
            .putString(KEY_LANGUAGE, settings.language.code)
            .putString(KEY_APPEARANCE, settings.appearance.name)
            .putString(KEY_ACCENT, settings.accent.name)
            .apply()
    }

    /** Records that the language default was resolved, so we only auto-pick once. */
    val hasStoredLanguage: Boolean get() = prefs.contains(KEY_LANGUAGE)

    private companion object {
        const val KEY_LANGUAGE = "language"
        const val KEY_APPEARANCE = "appearance"
        const val KEY_ACCENT = "accent"
    }
}

private inline fun <reified T : Enum<T>> String?.toEnum(default: T): T =
    this?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default
