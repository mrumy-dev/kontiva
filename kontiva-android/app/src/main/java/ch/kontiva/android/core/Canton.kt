package ch.kontiva.android.core

import kotlinx.serialization.Serializable

/** A Swiss canton (name + abbreviation). 1:1 with the iOS Canton list. */
@Serializable
data class Canton(val name: String, val abbreviation: String) {
    companion object {
        val all: List<Canton> = listOf(
            Canton("Aargau", "AG"),
            Canton("Appenzell Ausserrhoden", "AR"),
            Canton("Appenzell Innerrhoden", "AI"),
            Canton("Basel-Landschaft", "BL"),
            Canton("Basel-Stadt", "BS"),
            Canton("Bern", "BE"),
            Canton("Freiburg", "FR"),
            Canton("Genf", "GE"),
            Canton("Glarus", "GL"),
            Canton("Graubünden", "GR"),
            Canton("Jura", "JU"),
            Canton("Luzern", "LU"),
            Canton("Neuenburg", "NE"),
            Canton("Nidwalden", "NW"),
            Canton("Obwalden", "OW"),
            Canton("Schaffhausen", "SH"),
            Canton("Schwyz", "SZ"),
            Canton("Solothurn", "SO"),
            Canton("St. Gallen", "SG"),
            Canton("Tessin", "TI"),
            Canton("Thurgau", "TG"),
            Canton("Uri", "UR"),
            Canton("Waadt", "VD"),
            Canton("Wallis", "VS"),
            Canton("Zug", "ZG"),
            Canton("Zürich", "ZH"),
        )

        fun byAbbreviation(abbr: String?): Canton? = all.firstOrNull { it.abbreviation == abbr }
    }
}
