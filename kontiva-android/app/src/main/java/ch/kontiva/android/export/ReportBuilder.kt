package ch.kontiva.android.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import ch.kontiva.android.core.MonthlyAvailability
import ch.kontiva.android.core.l10n.L10nKey
import ch.kontiva.android.core.l10n.Localizer
import ch.kontiva.android.persistence.AppDataset
import java.io.File
import java.io.FileOutputStream

/** Generates a one-page A4 PDF of the month's plan and returns a shareable Uri. */
object ReportBuilder {

    fun makePdf(
        context: Context,
        dataset: AppDataset,
        availability: MonthlyAvailability,
        monthLabel: String,
        loc: Localizer,
    ): File {
        val doc = PdfDocument()
        val info = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 @ 72dpi
        val page = doc.startPage(info)
        val c = page.canvas

        val body = Paint().apply { color = Color.rgb(0x12, 0x1A, 0x22); textSize = 11f; isAntiAlias = true }
        val muted = Paint(body).apply { color = Color.rgb(0x6E, 0x6E, 0x73) }
        val h1 = Paint(body).apply { textSize = 24f; isFakeBoldText = true }
        val h2 = Paint(body).apply { textSize = 13f; isFakeBoldText = true }
        val red = Paint(body).apply { color = Color.rgb(0xE1, 0x1D, 0x2E); isFakeBoldText = true }
        val rightBody = Paint(body).apply { textAlign = Paint.Align.RIGHT }
        val left = 48f
        val right = 547f
        var y = 64f

        c.drawText("Kontiva", left, y, h1)
        c.drawText(monthLabel, right, y, rightBody.apply { textSize = 13f })
        y += 18f
        dataset.household?.name?.let { c.drawText(it, left, y, muted); y += 6f }
        y += 18f
        c.drawLine(left, y, right, y, muted); y += 24f

        // Headline
        c.drawText(loc(L10nKey.overviewAvailableThisMonth), left, y, h2)
        c.drawText(availability.available.formattedCHF(), right, y, Paint(red).apply { textAlign = Paint.Align.RIGHT; textSize = 13f })
        y += 28f

        fun line(label: String, value: String, bold: Boolean = false) {
            c.drawText(label, left, y, if (bold) h2 else body)
            c.drawText(value, right, y, Paint(if (bold) h2 else body).apply { textAlign = Paint.Align.RIGHT })
            y += 20f
        }

        line(loc(L10nKey.overviewNetIncome), availability.netIncomeThisMonth.formattedCHF())
        line(loc(L10nKey.overviewRecurringFixed), "− ${availability.recurringFixedCosts.formattedCHF()}")
        line(loc(L10nKey.overviewPlannedVariable), "− ${availability.plannedVariableBudgets.formattedCHF()}")
        line(loc(L10nKey.overviewBillsDueThisMonth), "− ${(availability.billsDueThisMonth + availability.overdueOpenBills).formattedCHF()}")
        line(loc(L10nKey.overviewPlannedSavings), "− ${availability.plannedSavings.formattedCHF()}")
        y += 4f
        c.drawLine(left, y, right, y, muted); y += 22f
        line(loc(L10nKey.overviewAvailableThisMonth), availability.available.formattedCHF(), bold = true)
        y += 16f

        fun section(title: String, rows: List<Pair<String, String>>) {
            if (rows.isEmpty()) return
            c.drawText(title, left, y, h2); y += 18f
            rows.forEach { (l, v) ->
                c.drawText(l, left + 8f, y, body)
                c.drawText(v, right, y, rightBody.apply { textSize = 11f })
                y += 17f
            }
            y += 12f
        }

        section(loc(L10nKey.planningIncome), dataset.incomes.map { it.label to it.monthlyNet.formattedCHF() })
        section(loc(L10nKey.planningFixed), dataset.fixedCosts.map { it.name to it.monthlyAmount.formattedCHF() })
        section(loc(L10nKey.planningVariable), dataset.variableBudgets.map { it.name to it.plannedAmount.formattedCHF() })

        c.drawText("AES-256-GCM · local-first · ${loc(L10nKey.appTagline)}", left, 810f, muted.apply { textSize = 9f })

        doc.finishPage(page)
        val file = File(context.cacheDir, "kontiva-report.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }

    fun shareUri(context: Context, file: File) =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
