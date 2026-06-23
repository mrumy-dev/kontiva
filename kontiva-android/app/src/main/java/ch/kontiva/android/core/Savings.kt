package ch.kontiva.android.core

import ch.kontiva.android.core.l10n.L10nKey
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

/** What a savings pot is for — order = picker order, labels 1:1 with iOS. */
@Serializable
enum class SavingsCategory(val labelKey: L10nKey) {
    EMERGENCY(L10nKey.savingsCatEmergency),
    RETIREMENT(L10nKey.savingsCatRetirement),
    HOME(L10nKey.savingsCatHome),
    CAR(L10nKey.savingsCatCar),
    VACATION(L10nKey.savingsCatVacation),
    WEDDING(L10nKey.savingsCatWedding),
    FAMILY(L10nKey.savingsCatFamily),
    EDUCATION(L10nKey.savingsCatEducation),
    RENOVATION(L10nKey.savingsCatRenovation),
    ELECTRONICS(L10nKey.savingsCatElectronics),
    TAXES(L10nKey.savingsCatTaxes),
    INVESTMENT(L10nKey.savingsCatInvestment),
    HEALTH(L10nKey.savingsCatHealth),
    GIFT(L10nKey.savingsCatGift),
    OTHER(L10nKey.savingsCatOther),
}

/**
 * A recurring savings pot. The accumulated balance is *derived* — startingBalance
 * plus one monthlyContribution for every month since startDate — never stored as a
 * running total, all in exact Rappen. 1:1 with the iOS `SavingsGoal`.
 */
@Serializable
data class SavingsGoal(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: SavingsCategory = SavingsCategory.OTHER,
    val target: Money = Money.zero,
    val monthlyContribution: Money? = null,
    val startingBalance: Money = Money.zero,
    @Serializable(with = LocalDateSerializer::class) val startDate: LocalDate = LocalDate.now(),
) {
    val hasTarget: Boolean get() = target.isPositive

    /** Contributions made from startDate's month up to & including [today]'s month. */
    fun monthsContributed(today: LocalDate = LocalDate.now()): Int {
        val start = startDate.withDayOfMonth(1)
        val current = today.withDayOfMonth(1)
        if (current.isBefore(start)) return 0
        return ChronoUnit.MONTHS.between(start, current).toInt() + 1
    }

    fun accumulated(today: LocalDate = LocalDate.now()): Money =
        startingBalance + (monthlyContribution ?: Money.zero).scaled(monthsContributed(today).toLong())

    fun progressPercent(today: LocalDate = LocalDate.now()): Int =
        if (hasTarget) accumulated(today).percentOf(target) else 0
}
