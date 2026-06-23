package ch.kontiva.android.core

/** The transparent monthly breakdown shown on the Overview (1:1 with iOS). */
data class MonthlyAvailability(
    val netIncomeThisMonth: Money,
    val thirteenthShownSeparately: Money?,
    val recurringFixedCosts: Money,
    val plannedVariableBudgets: Money,
    val openBillsDueThisMonth: Money,
    val overdueOpenBills: Money,
    val plannedSavings: Money,
    val available: Money,
) {
    val totalCommitted: Money
        get() = recurringFixedCosts + plannedVariableBudgets + openBillsDueThisMonth +
            overdueOpenBills + plannedSavings
}

/**
 * Available this month = net income − fixed − variable − bills(due+overdue) − savings.
 * Bills/savings are passed in as totals (their tabs compute them); until those tabs
 * exist they default to zero. 1:1 with the iOS AvailabilityEngine.
 */
object AvailabilityEngine {

    fun netIncomeThisMonth(income: Income): Money = when (income.thirteenthModel) {
        ThirteenthSalaryModel.SEPARATE -> income.monthlyNet
        ThirteenthSalaryModel.AVERAGED_MONTHLY ->
            income.monthlyNet + (income.thirteenthAmount?.divided(12) ?: Money.zero)
    }

    fun netIncomeThisMonth(incomes: List<Income>): Money = incomes.map(::netIncomeThisMonth).total()

    fun thirteenthShownSeparately(incomes: List<Income>): Money? {
        val separate = incomes
            .filter { it.thirteenthModel == ThirteenthSalaryModel.SEPARATE }
            .mapNotNull { it.thirteenthAmount }
        if (separate.isEmpty()) return null
        val total = separate.total()
        return if (total.isZero) null else total
    }

    fun compute(
        incomes: List<Income>,
        fixedCosts: List<RecurringFixedExpense>,
        variableBudgets: List<VariableMonthlyBudget>,
        plannedSavings: Money = Money.zero,
        openBillsDueThisMonth: Money = Money.zero,
        overdueOpenBills: Money = Money.zero,
    ): MonthlyAvailability {
        val netIncome = netIncomeThisMonth(incomes)
        val fixed = fixedCosts.map { it.monthlyAmount }.total()
        val variable = variableBudgets.map { it.plannedAmount }.total()
        val available = netIncome - fixed - variable - openBillsDueThisMonth - overdueOpenBills - plannedSavings
        return MonthlyAvailability(
            netIncomeThisMonth = netIncome,
            thirteenthShownSeparately = thirteenthShownSeparately(incomes),
            recurringFixedCosts = fixed,
            plannedVariableBudgets = variable,
            openBillsDueThisMonth = openBillsDueThisMonth,
            overdueOpenBills = overdueOpenBills,
            plannedSavings = plannedSavings,
            available = available,
        )
    }

    /** Full computation including bills (due + overdue) and savings contributions. */
    fun compute(
        incomes: List<Income>,
        fixedCosts: List<RecurringFixedExpense>,
        variableBudgets: List<VariableMonthlyBudget>,
        bills: List<OneOffBill>,
        savingsGoals: List<SavingsGoal>,
        today: java.time.LocalDate = java.time.LocalDate.now(),
    ): MonthlyAvailability = compute(
        incomes = incomes,
        fixedCosts = fixedCosts,
        variableBudgets = variableBudgets,
        plannedSavings = savingsGoals.mapNotNull { it.monthlyContribution }.total(),
        openBillsDueThisMonth = BillClassifier.amount(BillState.DUE_THIS_MONTH, bills, today),
        overdueOpenBills = BillClassifier.amount(BillState.OVERDUE, bills, today),
    )
}
