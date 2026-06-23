package ch.kontiva.android.core

import ch.kontiva.android.core.l10n.L10nKey
import kotlinx.serialization.Serializable
import java.util.UUID

/** How a 13th salary is treated (1:1 with iOS). */
@Serializable
enum class ThirteenthSalaryModel { SEPARATE, AVERAGED_MONTHLY }

@Serializable
data class Income(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val monthlyNet: Money,
    val thirteenthAmount: Money? = null,
    val thirteenthModel: ThirteenthSalaryModel = ThirteenthSalaryModel.SEPARATE,
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
    // Limited standing-order window (startMonth + installments) comes with the
    // engine-polish stage; for now every fixed cost is open-ended.
)

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
