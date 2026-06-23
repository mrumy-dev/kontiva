package ch.kontiva.android.core

import ch.kontiva.android.core.l10n.L10nKey
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.util.UUID

/** Swiss SchKG debt-enforcement stages + private debts. severityRank = most pressing first. */
@Serializable
enum class DebtType(val labelKey: L10nKey, val severityRank: Int) {
    PFAENDUNG(L10nKey.debtTypePfaendung, 0),
    BETREIBUNG(L10nKey.debtTypeBetreibung, 1),
    OPEN_CLAIM(L10nKey.debtTypeOpenClaim, 2),
    VERLUSTSCHEIN(L10nKey.debtTypeVerlustschein, 3),
    OTHER(L10nKey.debtTypeOther, 4),
}

/** A single recorded debt (principal only — an organiser, not a legal tool). 1:1 with iOS. */
@Serializable
data class DebtItem(
    val id: String = UUID.randomUUID().toString(),
    val creditor: String,
    val amount: Money,
    val type: DebtType = DebtType.OPEN_CLAIM,
    @Serializable(with = LocalDateSerializer::class) val date: LocalDate? = null,
    val reference: String? = null,
    val notes: String? = null,
)
