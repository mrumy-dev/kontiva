import SwiftUI
import KontivaCore

/// Month/year stepper (◀ Juni 2026 ▶) bound to `AppModel.selectedMonth`. Changing
/// it recomputes every month-aware view (available balance, bill classification,
/// insights). A "Heute" button jumps back to the current month when away from it.
struct MonthSelector: View {
    @EnvironmentObject private var model: AppModel
    @EnvironmentObject private var loc: Localizer

    private var monthLabel: String {
        let formatter = DateFormatter()
        formatter.locale = loc.language.locale
        formatter.calendar = .swiss
        formatter.setLocalizedDateFormatFromTemplate("LLLL yyyy")
        return formatter.string(from: model.selectedMonth)
    }

    var body: some View {
        HStack(spacing: KontivaTheme.Space.xs) {
            stepperButton("chevron.left") { withAnimation(.snappy) { model.shiftMonth(by: -1) } }

            Text(monthLabel)
                .font(.system(size: 14, weight: .semibold))
                .monospacedDigit()
                .contentTransition(.numericText())
                .foregroundStyle(KontivaTheme.textPrimary)
                .frame(minWidth: 124)
                .multilineTextAlignment(.center)

            stepperButton("chevron.right") { withAnimation(.snappy) { model.shiftMonth(by: 1) } }

            if !model.isCurrentMonth {
                Button(loc(.monthToday)) { withAnimation(.snappy) { model.goToCurrentMonth() } }
                    .buttonStyle(.plain)
                    .font(.caption.weight(.medium))
                    .foregroundStyle(KontivaTheme.accent)
                    .padding(.leading, KontivaTheme.Space.xxs)
            }
        }
        .padding(.horizontal, KontivaTheme.Space.sm)
        .padding(.vertical, KontivaTheme.Space.xs)
        .background(
            Capsule().fill(KontivaTheme.cardSurface)
                .overlay(Capsule().strokeBorder(KontivaTheme.softBorder.opacity(0.6), lineWidth: 1))
        )
        .fixedSize() // never compress (keeps the "Heute" button from clipping)
    }

    private func stepperButton(_ symbol: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: symbol)
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(KontivaTheme.textSecondary)
                .frame(width: 22, height: 22)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}
