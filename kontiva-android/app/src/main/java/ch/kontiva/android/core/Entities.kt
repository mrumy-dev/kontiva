package ch.kontiva.android.core

import ch.kontiva.android.core.l10n.L10nKey
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

/** When the 13th salary actually lands — the Swiss patterns (1:1 with iOS). */
@Serializable
enum class ThirteenthSalaryModel {
    SEPARATE,          // shown on its own, excluded from monthly available
    AVERAGED_MONTHLY,  // amount / 12 every month
    DECEMBER,          // full in December
    NOVEMBER,          // full in November
    SPLIT_NOV_DEC,     // 11/12 in November, 1/12 in December
    HALF_YEARLY,       // half in June, half in December
}

/** An irregular extra payment (Sonderzahlung) that lands in one month each year. */
@Serializable
data class Bonus(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val amount: Money,
    val month: Int, // 1 = January … 12 = December
)

@Serializable
data class Income(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val monthlyNet: Money,
    val thirteenthAmount: Money? = null,
    val thirteenthModel: ThirteenthSalaryModel = ThirteenthSalaryModel.DECEMBER,
    val bonuses: List<Bonus> = emptyList(),
) {
    val hasThirteenth: Boolean get() = thirteenthAmount?.isZero == false
}

/** Recurring fixed-cost categories — order = picker order, labels 1:1 with iOS. */
@Serializable
enum class FixedExpenseCategory(val labelKey: L10nKey) {
    RENT(L10nKey.catRent),
    MORTGAGE(L10nKey.catMortgage),
    HEALTH_INSURANCE(L10nKey.catHealthInsurance),
    INSURANCE(L10nKey.catInsurance),
    UTILITIES(L10nKey.catUtilities),
    TELECOM(L10nKey.catTelecom),
    SUBSCRIPTION(L10nKey.catSubscription),
    SERAFE(L10nKey.catSerafe),
    LEASING(L10nKey.catLeasing),
    PUBLIC_TRANSPORT(L10nKey.catPublicTransport),
    CHILDCARE(L10nKey.catChildcare),
    EDUCATION(L10nKey.catEducation),
    MEMBERSHIP(L10nKey.catMembership),
    ALIMONY(L10nKey.catAlimony),
    TAXES(L10nKey.catTaxes),
    OTHER(L10nKey.catOther),
}

@Serializable
data class RecurringFixedExpense(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val monthlyAmount: Money,
    val category: FixedExpenseCategory = FixedExpenseCategory.OTHER,
    // Limited standing-order window: open-ended unless both are set (1:1 with iOS).
    @Serializable(with = LocalDateSerializer::class) val startMonth: LocalDate? = null,
    val installments: Int? = null,
) {
    /** A finite standing order (leasing, a loan, a fixed-term plan). */
    val isLimited: Boolean get() = startMonth != null && (installments ?: 0) > 0

    /** Whether the cost applies in [month]. Open-ended costs always apply; limited
     *  ones only within their instalment window. */
    fun isActive(month: LocalDate): Boolean {
        val start = startMonth ?: return true
        val count = installments ?: return true
        if (count <= 0) return true
        val startM = start.withDayOfMonth(1)
        val m = month.withDayOfMonth(1)
        if (m.isBefore(startM)) return false
        return ChronoUnit.MONTHS.between(startM, m) < count
    }

    /** For a limited order: which instalment (1…count) [month] is, or null if outside. */
    fun installmentNumber(month: LocalDate): Int? {
        if (!isLimited || !isActive(month)) return null
        val start = startMonth ?: return null
        return ChronoUnit.MONTHS.between(start.withDayOfMonth(1), month.withDayOfMonth(1)).toInt() + 1
    }
}

/** Planned variable-budget categories — order = picker order, labels 1:1 with iOS. */
@Serializable
enum class VariableBudgetCategory(val labelKey: L10nKey) {
    GROCERIES(L10nKey.catGroceries),
    DINING(L10nKey.catDining),
    HOUSEHOLD(L10nKey.catHousehold),
    CLOTHING(L10nKey.catClothing),
    PERSONAL(L10nKey.catPersonal),
    HEALTH(L10nKey.catHealth),
    FUEL(L10nKey.catFuel),
    TRANSPORT(L10nKey.catTransport),
    LEISURE(L10nKey.catLeisure),
    ENTERTAINMENT(L10nKey.catEntertainment),
    CHILDREN(L10nKey.catChildren),
    PETS(L10nKey.catPets),
    GIFTS(L10nKey.catGifts),
    TRAVEL(L10nKey.catTravel),
    EDUCATION(L10nKey.catEducation),
    CHARITY(L10nKey.catCharity),
    OTHER(L10nKey.catOther),
}

@Serializable
data class VariableMonthlyBudget(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val plannedAmount: Money,
    val category: VariableBudgetCategory = VariableBudgetCategory.OTHER,
)
