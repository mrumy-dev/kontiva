package ch.kontiva.android.core.l10n

import androidx.compose.runtime.staticCompositionLocalOf
import ch.kontiva.android.core.AppLanguage

/**
 * Assembles the per-language tables (generated from the iOS Swift source by
 * tools/transpile_l10n.py) into one lookup. 1:1 with iOS `Localization.tables`.
 */
object Localization {
    val tables: Map<AppLanguage, Map<L10nKey, String>> = mapOf(
        AppLanguage.deCH to LOC_deCH,
        AppLanguage.frCH to LOC_frCH,
        AppLanguage.itCH to LOC_itCH,
        AppLanguage.rm to LOC_rm,
        AppLanguage.en to LOC_en,
        AppLanguage.es to LOC_es,
        AppLanguage.ptPT to LOC_ptPT,
        AppLanguage.ptBR to LOC_ptBR,
        AppLanguage.nl to LOC_nl,
        AppLanguage.da to LOC_da,
        AppLanguage.nb to LOC_nb,
        AppLanguage.sv to LOC_sv,
        AppLanguage.fi to LOC_fi,
        AppLanguage.pl to LOC_pl,
        AppLanguage.ro to LOC_ro,
        AppLanguage.hu to LOC_hu,
        AppLanguage.sq to LOC_sq,
        AppLanguage.sr to LOC_sr,
        AppLanguage.hr to LOC_hr,
        AppLanguage.bs to LOC_bs,
        AppLanguage.mk to LOC_mk,
        AppLanguage.tr to LOC_tr,
        AppLanguage.ru to LOC_ru,
        AppLanguage.uk to LOC_uk,
        AppLanguage.ar to LOC_ar,
        AppLanguage.zhHans to LOC_zhHans,
        AppLanguage.ja to LOC_ja,
        AppLanguage.ko to LOC_ko,
        AppLanguage.vi to LOC_vi,
        AppLanguage.th to LOC_th,
        AppLanguage.hi to LOC_hi,
        AppLanguage.ta to LOC_ta,
        AppLanguage.si to LOC_si,
        AppLanguage.ur to LOC_ur,
        AppLanguage.ps to LOC_ps,
    )
}

/**
 * Resolves keys for one language, falling back to de-CH then the raw key —
 * exactly like the iOS `Localizer`.
 */
class Localizer(val language: AppLanguage) {
    private val table: Map<L10nKey, String> =
        Localization.tables[language] ?: Localization.tables.getValue(AppLanguage.deCH)

    fun string(key: L10nKey): String =
        table[key] ?: Localization.tables[AppLanguage.deCH]?.get(key) ?: key.name

    operator fun invoke(key: L10nKey): String = string(key)
}

/** The active localizer, provided high in the Compose tree so any screen can read it. */
val LocalLocalizer = staticCompositionLocalOf { Localizer(AppLanguage.deCH) }
