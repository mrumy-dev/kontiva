package ch.kontiva.android.core

import kotlinx.serialization.Serializable
import java.util.Locale

/** Region grouping for the language picker (Swiss first, then European, then Asian). */
enum class LanguageGroup { SWISS, EUROPEAN, ASIAN }

/**
 * Supported UI languages — a 1:1 port of the iOS `AppLanguage`. de-CH is the default.
 * Declaration order = picker order. The enum constant names match the generated
 * `LOC_<name>` tables in [ch.kontiva.android.core.l10n].
 */
@Serializable
enum class AppLanguage(
    /** Persistence id; identical to the iOS rawValue. */
    val code: String,
    /** Endonym shown in the picker. */
    val displayName: String,
    /** BCP-47 tag used for date/number formatting (Swiss conventions where relevant). */
    val localeTag: String,
    val group: LanguageGroup,
    val isRtl: Boolean = false,
) {
    // Swiss (official national languages + English)
    deCH("de-CH", "Deutsch (Schweiz)", "de-CH", LanguageGroup.SWISS),
    frCH("fr-CH", "Français (Suisse)", "fr-CH", LanguageGroup.SWISS),
    itCH("it-CH", "Italiano (Svizzera)", "it-CH", LanguageGroup.SWISS),
    rm("rm", "Rumantsch", "rm-CH", LanguageGroup.SWISS),
    en("en", "English", "en-CH", LanguageGroup.SWISS), // English UI, Swiss conventions

    // European
    es("es", "Español", "es-ES", LanguageGroup.EUROPEAN),
    ptPT("pt-PT", "Português", "pt-PT", LanguageGroup.EUROPEAN),
    ptBR("pt-BR", "Português (Brasil)", "pt-BR", LanguageGroup.EUROPEAN),
    nl("nl", "Nederlands", "nl-NL", LanguageGroup.EUROPEAN),
    da("da", "Dansk", "da-DK", LanguageGroup.EUROPEAN),
    nb("nb", "Norsk", "nb-NO", LanguageGroup.EUROPEAN),
    sv("sv", "Svenska", "sv-SE", LanguageGroup.EUROPEAN),
    fi("fi", "Suomi", "fi-FI", LanguageGroup.EUROPEAN),
    pl("pl", "Polski", "pl-PL", LanguageGroup.EUROPEAN),
    ro("ro", "Română", "ro-RO", LanguageGroup.EUROPEAN),
    hu("hu", "Magyar", "hu-HU", LanguageGroup.EUROPEAN),
    sq("sq", "Shqip", "sq-AL", LanguageGroup.EUROPEAN),
    sr("sr", "Srpski", "sr-Latn-RS", LanguageGroup.EUROPEAN),
    hr("hr", "Hrvatski", "hr-HR", LanguageGroup.EUROPEAN),
    bs("bs", "Bosanski", "bs-BA", LanguageGroup.EUROPEAN),
    mk("mk", "Македонски", "mk-MK", LanguageGroup.EUROPEAN),
    tr("tr", "Türkçe", "tr-TR", LanguageGroup.EUROPEAN),
    ru("ru", "Русский", "ru-RU", LanguageGroup.EUROPEAN),
    uk("uk", "Українська", "uk-UA", LanguageGroup.EUROPEAN),

    // Asian & Middle East
    ar("ar", "العربية", "ar", LanguageGroup.ASIAN, isRtl = true),
    zhHans("zh-Hans", "简体中文", "zh-Hans", LanguageGroup.ASIAN),
    ja("ja", "日本語", "ja-JP", LanguageGroup.ASIAN),
    ko("ko", "한국어", "ko-KR", LanguageGroup.ASIAN),
    vi("vi", "Tiếng Việt", "vi-VN", LanguageGroup.ASIAN),
    th("th", "ไทย", "th-TH", LanguageGroup.ASIAN),
    hi("hi", "हिन्दी", "hi-IN", LanguageGroup.ASIAN),
    ta("ta", "தமிழ்", "ta-IN", LanguageGroup.ASIAN),
    si("si", "සිංහල", "si-LK", LanguageGroup.ASIAN),
    ur("ur", "اردو", "ur-PK", LanguageGroup.ASIAN, isRtl = true),
    ps("ps", "پښتو", "ps-AF", LanguageGroup.ASIAN, isRtl = true);

    val locale: Locale get() = Locale.forLanguageTag(localeTag)

    companion object {
        fun fromCode(code: String?): AppLanguage? = entries.firstOrNull { it.code == code }

        /**
         * Best-matching supported language for the device, walking the user's preferred
         * locales. Falls back to Swiss German (the app's home market). 1:1 with iOS.
         */
        fun bestForDevice(preferred: List<Locale>): AppLanguage {
            for (locale in preferred) {
                val match = matching(locale.language, locale.country.ifBlank { null })
                if (match != null) return match
            }
            return deCH
        }

        private fun matching(code: String, region: String?): AppLanguage? = when (code) {
            "de" -> deCH
            "fr" -> frCH
            "it" -> itCH
            "rm" -> rm
            "en" -> en
            "es" -> es
            "pt" -> if (region == "BR") ptBR else ptPT
            "nl" -> nl
            "da" -> da
            "nb", "nn", "no" -> nb
            "sv" -> sv
            "fi" -> fi
            "pl" -> pl
            "ro" -> ro
            "hu" -> hu
            "sq" -> sq
            "sr" -> sr
            "hr" -> hr
            "bs" -> bs
            "mk" -> mk
            "tr" -> tr
            "ru" -> ru
            "uk" -> uk
            "ar" -> ar
            "zh" -> zhHans // only Simplified is bundled
            "ja" -> ja
            "ko" -> ko
            "vi" -> vi
            "th" -> th
            "hi" -> hi
            "ta" -> ta
            "si" -> si
            "ur" -> ur
            "ps" -> ps
            else -> null
        }
    }
}
