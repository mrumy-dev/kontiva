package ch.kontiva.android.core

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.util.UUID

/** User-set status of a bill. */
@Serializable
enum class BillStatus { OPEN, PAID }

/**
 * A one-off bill (Rechnung) with a due date. Recurring payments belong in Monthly
 * Planning, not here. 1:1 with the iOS `OneOffBill`.
 */
@Serializable
data class OneOffBill(
    val id: String = UUID.randomUUID().toString(),
    val provider: String,
    val amount: Money,
    @Serializable(with = LocalDateSerializer::class) val dueDate: LocalDate,
    val status: BillStatus = BillStatus.OPEN,
    val notes: String? = null,
)

/** Derived state of a bill relative to the current month (1:1 with iOS). */
enum class BillState(val countsAgainstCurrentMonth: Boolean) {
    PAID(false),
    OVERDUE(true),
    DUE_THIS_MONTH(true),
    FUTURE(false),
}

object BillClassifier {

    fun state(bill: OneOffBill, today: LocalDate = LocalDate.now()): BillState {
        if (bill.status == BillStatus.PAID) return BillState.PAID
        val monthStart = today.withDayOfMonth(1)
        val nextMonthStart = monthStart.plusMonths(1)
        return when {
            bill.dueDate.isBefore(monthStart) -> BillState.OVERDUE
            bill.dueDate.isBefore(nextMonthStart) -> BillState.DUE_THIS_MONTH
            else -> BillState.FUTURE
        }
    }

    fun amount(target: BillState, bills: List<OneOffBill>, today: LocalDate = LocalDate.now()): Money =
        bills.filter { state(it, today) == target }.map { it.amount }.total()
}
