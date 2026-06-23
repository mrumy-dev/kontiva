package ch.kontiva.android.core

/** The transparent monthly breakdown shown on the Overview (1:1 with iOS). */
data class MonthlyAvailability(
    val netIncomeThisMonth: Money,
    val thirteenthShownSeparately: Money?,
    val recurringFixedCosts: Money,
    val plannedVariableBudgets: Money,
    val billsDueThisMonth: Money,
    val overdueOpenBills: Money,
    val plannedSavings: Money,
    val available: Money,
) {
    val totalCommitted: Money
        get() = recurringFixedCosts + plannedVariableBudgets + billsDueThisMonth +
            overdueOpenBills + plannedSavings
}

/**
 * Available this month = net income − fixed − variable − bills(due+overdue) − savings.
 * Bills/savings are passed in as totals (their tabs compute them); until those tabs
 * exist they default to zero. 1:1 with the iOS AvailabilityEngine.
 */
object AvailabilityEngine {

    /** Net income landing in [month]: base + the 13th-salary slice for this month +
     *  any bonuses (Sonderzahlungen) paid this month. */
    fun netIncomeThisMonth(income: Income, month: java.time.LocalDate): Money {
        var total = income.monthlyNet
        val m = month.monthValue // 1…12
        val th = income.thirteenthAmount
        if (th != null && !th.isZero) {
            total += when (income.thirteenthModel) {
                ThirteenthSalaryModel.SEPARATE -> Money.zero
                ThirteenthSalaryModel.AVERAGED_MONTHLY -> th.divided(12)
                ThirteenthSalaryModel.DECEMBER -> if (m == 12) th else Money.zero
                ThirteenthSalaryModel.NOVEMBER -> if (m == 11) th else Money.zero
                ThirteenthSalaryModel.SPLIT_NOV_DEC -> {
                    val twelfth = th.divided(12)
                    when (m) { 11 -> th - twelfth; 12 -> twelfth; else -> Money.zero }
                }
            }
        }
        total += income.bonuses.filter { it.month == m }.map { it.amount }.total()
        return total
    }

    fun netIncomeThisMonth(incomes: List<Income>, month: java.time.LocalDate): Money =
        incomes.map { netIncomeThisMonth(it, month) }.total()

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
        billsDueThisMonth: Money = Money.zero,
        overdueOpenBills: Money = Money.zero,
        month: java.time.LocalDate = java.time.LocalDate.now(),
    ): MonthlyAvailability {
        val netIncome = netIncomeThisMonth(incomes, month)
        val fixed = fixedCosts.map { it.monthlyAmount }.total()
        val variable = variableBudgets.map { it.plannedAmount }.total()
        val available = netIncome - fixed - variable - billsDueThisMonth - overdueOpenBills - plannedSavings
        return MonthlyAvailability(
            netIncomeThisMonth = netIncome,
            thirteenthShownSeparately = thirteenthShownSeparately(incomes),
            recurringFixedCosts = fixed,
            plannedVariableBudgets = variable,
            billsDueThisMonth = billsDueThisMonth,
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
        // Limited standing orders only count inside their instalment window.
        fixedCosts = fixedCosts.filter { it.isActive(today) },
        variableBudgets = variableBudgets,
        // Savings only cost from their start month onward (a future plan is CHF 0 now).
        plannedSavings = savingsGoals.filter { it.contributesIn(today) }.mapNotNull { it.monthlyContribution }.total(),
        // All bills due this month count, paid or not — paying one doesn't free up money.
        billsDueThisMonth = BillClassifier.amountDueInMonth(bills, today),
        overdueOpenBills = BillClassifier.amount(BillState.OVERDUE, bills, today),
        month = today,
    )
}
