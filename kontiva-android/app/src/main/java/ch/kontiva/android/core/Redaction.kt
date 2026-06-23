package ch.kontiva.android.core

/**
 * Local redaction for the user-reviewed bug-report composer. Sensitive-looking
 * patterns are replaced with a neutral placeholder BEFORE the user reviews the
 * final text. Nothing is ever sent automatically; the user copies it themselves.
 * Conservative by design — errs toward redacting. 1:1 with the iOS Redactor.
 */
object Redactor {
    const val PLACEHOLDER = "[entfernt]"

    // Order matters: more specific patterns first.
    private val rules: List<Regex> = listOf(
        // Email addresses.
        Regex("""[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}"""),
        // Swiss IBAN (CH + 2 check + 17 alnum), tolerant of spaces.
        Regex("""\bCH(?:\s?[0-9A-Z]){19}\b""", RegexOption.IGNORE_CASE),
        // Swiss AHV/AVS number: 756.XXXX.XXXX.XX
        Regex("""\b756[.\s]?\d{4}[.\s]?\d{4}[.\s]?\d{2}\b"""),
        // Long digit runs (card-like / account-like), 11+ digits possibly grouped.
        Regex("""\b(?:\d[ \-]?){11,}\b"""),
        // Phone numbers in +41 / 0xx forms.
        Regex("""(?:\+41|0)(?:[ \-]?\d){8,12}\b"""),
    )

    /** Return a redacted copy of [text]. */
    fun redact(text: String): String {
        var result = text
        for (rule in rules) result = rule.replace(result) { PLACEHOLDER }
        return result
    }

    /** True if the text still appears to contain a redactable pattern. */
    fun containsSensitivePattern(text: String): Boolean = rules.any { it.containsMatchIn(text) }
}

/**
 * The structured content of a local bug report. Only non-sensitive metadata is
 * auto-added; no screenshots, logs, database, or vault files are ever attached.
 * 1:1 with the iOS BugReport (the composed text is intentionally German, matching iOS).
 */
data class BugReport(
    val summary: String = "",
    val expectedBehavior: String = "",
    val actualBehavior: String = "",
    val reproductionSteps: String = "",
    val appVersion: String,
    val systemVersion: String,
    val appLanguage: String,
    val selectedArea: String,
) {
    /** Compose the final, redacted report text for the user to review and copy. */
    fun composeRedacted(): String =
        """
        Kontiva — Problembericht
        ========================

        Bereich: $selectedArea

        Zusammenfassung:
        ${Redactor.redact(summary)}

        Erwartetes Verhalten:
        ${Redactor.redact(expectedBehavior)}

        Tatsächliches Verhalten:
        ${Redactor.redact(actualBehavior)}

        Schritte zur Reproduktion:
        ${Redactor.redact(reproductionSteps)}

        --- Technische Angaben (automatisch, nicht sensibel) ---
        App-Version: $appVersion
        System-Version: $systemVersion
        Sprache: $appLanguage
        """.trimIndent()
}
