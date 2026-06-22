import SwiftUI
import KontivaCore

struct OverviewView: View {
    @EnvironmentObject private var model: AppModel
    @EnvironmentObject private var loc: Localizer

    @State private var showExport = false
    @State private var showBackupSheet = false
    @State private var revealed = true

    private var a: MonthlyAvailability { model.availability }
    private var hasData: Bool { model.hasAnyPlanningData || !model.bills.isEmpty }

    /// Zero until revealed, so figures count up once after unlocking.
    private func reveal(_ money: Money) -> Money { revealed ? money : .zero }

    private func startCountUp() {
        guard model.justUnlocked else { return }
        model.justUnlocked = false
        revealed = false
        DispatchQueue.main.async {
            withAnimation(.snappy(duration: 0.8)) { revealed = true }
        }
    }

    private let columns = [GridItem(.adaptive(minimum: 240), spacing: KontivaTheme.Space.md)]

    var body: some View {
        ScreenScroll {
            if !model.hasAnyPlanningData && model.bills.isEmpty {
                KontivaCard {
                    EmptyState(systemImage: "square.grid.2x2",
                               title: loc(.overviewTitle),
                               message: loc(.overviewEmpty),
                               actionTitle: loc(.overviewAddCta)) { model.selection = .planning }
                }
            } else {
                if model.shouldShowBackupReminder { backupBanner }
                headlineCard
                chartCard
                metricsGrid
                trendCard
                breakdownCard
                securityFooter
            }
        }
        .animation(.snappy, value: a)
        .onAppear(perform: startCountUp)
        .navigationTitle(loc(.overviewTitle))
        .navigationSubtitle(loc(.appTagline))
        .toolbar {
            ToolbarItemGroup(placement: .primaryAction) {
                MonthSelector()
                Button { showExport = true } label: {
                    Label(loc(.exportReport), systemImage: "square.and.arrow.up")
                }
                .disabled(!hasData)
                .help(loc(.exportReport))
            }
        }
        .sheet(isPresented: $showExport) {
            ReportExportSheet().environmentObject(model).environmentObject(loc)
        }
        .sheet(isPresented: $showBackupSheet) {
            BackupExportSheet().environmentObject(model).environmentObject(loc)
        }
    }

    /// One-time nudge to make an encrypted backup — there is no passphrase recovery.
    private var backupBanner: some View {
        KontivaCard {
            HStack(spacing: KontivaTheme.Space.md) {
                Image(systemName: "exclamationmark.shield.fill")
                    .font(.system(size: 22)).foregroundStyle(KontivaTheme.warning)
                Text(loc(.backupNudgeText))
                    .font(.callout).foregroundStyle(KontivaTheme.textSecondary)
                    .fixedSize(horizontal: false, vertical: true)
                Spacer(minLength: KontivaTheme.Space.sm)
                Button(loc(.settingsBackup)) {
                    model.dismissBackupReminder()
                    showBackupSheet = true
                }
                .buttonStyle(.borderedProminent).tint(KontivaTheme.accent)
                Button { model.dismissBackupReminder() } label: {
                    Image(systemName: "xmark").font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(KontivaTheme.textTertiary)
                }
                .buttonStyle(.plain).help(loc(.commonClose))
            }
        }
    }

    // A donut visualising where this month's net income goes.
    @ViewBuilder private var chartCard: some View {
        if a.netIncomeThisMonth.isPositive || a.totalCommitted.isPositive {
            KontivaCard {
                VStack(alignment: .leading, spacing: KontivaTheme.Space.md) {
                    CardTitle(loc(.chartSpendingTitle))
                    SpendingDonut(availability: a, loc: loc.localization)
                }
            }
        }
    }

    // The single most important number, with its calculation made explicit.
    private var trend: [(month: Date, available: Money, savings: Money)] { model.trend() }

    /// A quick qualitative read of the month, shown as a pill on the hero.
    private var status: (text: String, color: Color)? {
        guard hasData else { return nil }
        if a.available.isNegative { return (loc(.overviewStatusNegative), KontivaTheme.swissRed) }
        if a.netIncomeThisMonth.isPositive && a.available.percent(of: a.netIncomeThisMonth) < 12 {
            return (loc(.overviewStatusTight), KontivaTheme.warning)
        }
        return (loc(.overviewStatusGood), KontivaTheme.positive)
    }

    private var headlineCard: some View {
        KontivaCard {
            VStack(alignment: .leading, spacing: KontivaTheme.Space.sm) {
                HStack {
                    CardTitle(loc(.overviewAvailableThisMonth))
                    Spacer(minLength: KontivaTheme.Space.sm)
                    if let status { StatusPill(text: status.text, color: status.color) }
                }
                HStack(alignment: .center) {
                    Text(reveal(a.available).formattedCHF())
                        .font(.system(size: 42, weight: .bold))
                        .monospacedDigit()
                        .contentTransition(.numericText())
                        .foregroundStyle(a.available.isNegative ? KontivaTheme.swissRed : KontivaTheme.textPrimary)
                        .lineLimit(1)
                        .minimumScaleFactor(0.5)
                    Spacer(minLength: KontivaTheme.Space.md)
                    Sparkline(values: trend.map { Double($0.available.rappen) / 100 },
                              color: a.available.isNegative ? KontivaTheme.swissRed : KontivaTheme.chartAvailable,
                              width: 96, height: 34)
                }
            }
        }
    }

    /// A quiet trust signal at the foot of the dashboard.
    private var securityFooter: some View {
        HStack(spacing: KontivaTheme.Space.xxs) {
            Image(systemName: "lock.fill")
            Text("AES-256-GCM")
        }
        .font(.caption2)
        .foregroundStyle(KontivaTheme.textTertiary)
        .frame(maxWidth: .infinity, alignment: .center)
        .padding(.top, KontivaTheme.Space.xs)
    }

    // A real 6-month trend of the available balance, computed from the current plan.
    private var trendCard: some View {
        KontivaCard {
            VStack(alignment: .leading, spacing: KontivaTheme.Space.md) {
                CardTitle(loc(.overviewTrendTitle))
                TrendChart(points: trend.map { .init(month: $0.month, value: $0.available) },
                           locale: loc.language.locale,
                           color: a.available.isNegative ? KontivaTheme.swissRed : KontivaTheme.chartAvailable)
            }
        }
    }

    // Transparent line-by-line breakdown — collapsed by default so the dashboard
    // leads with the hero + donut + tiles, not three views of the same numbers.
    private var breakdownCard: some View {
        KontivaCard {
            DisclosureGroup {
                breakdownRows.padding(.top, KontivaTheme.Space.sm)
            } label: {
                Text(loc(.overviewShowCalculation))
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(KontivaTheme.textSecondary)
            }
            .tint(KontivaTheme.textSecondary)
        }
    }

    private var breakdownRows: some View {
        VStack(alignment: .leading, spacing: KontivaTheme.Space.sm) {
            Text(loc(.overviewFormulaExplainer))
                .font(.caption)
                .foregroundStyle(KontivaTheme.textTertiary)
                .fixedSize(horizontal: false, vertical: true)
                .padding(.bottom, KontivaTheme.Space.xxs)
            MoneyRow(label: loc(.overviewNetIncome), amount: a.netIncomeThisMonth, emphasised: true)
                if let thirteenth = a.thirteenthShownSeparately {
                    HStack {
                        Text(loc(.overviewThirteenthSeparate))
                            .font(.caption)
                            .foregroundStyle(KontivaTheme.textTertiary)
                        Spacer()
                        Text(thirteenth.formattedCHF())
                            .font(.caption).monospacedDigit()
                            .foregroundStyle(KontivaTheme.textTertiary)
                    }
                }
                Divider()
                MoneyRow(label: loc(.overviewRecurringFixed), amount: a.recurringFixedCosts, subtractive: true)
                MoneyRow(label: loc(.overviewPlannedVariable), amount: a.plannedVariableBudgets, subtractive: true)
                MoneyRow(label: loc(.overviewBillsDueThisMonth), amount: a.openBillsDueThisMonth, subtractive: true)
                MoneyRow(label: loc(.overviewOverdueBills), amount: a.overdueOpenBills, subtractive: true)
                if a.plannedSavings.isPositive {
                    MoneyRow(label: loc(.overviewPlannedSavings), amount: a.plannedSavings, subtractive: true)
                }
                Divider()
                MoneyRow(label: loc(.overviewAvailableThisMonth), amount: a.available, emphasised: true)
            }
        }

    private var metricsGrid: some View {
        LazyVGrid(columns: columns, spacing: KontivaTheme.Space.md) {
            MetricTile(title: loc(.planningIncome),
                       value: reveal(a.netIncomeThisMonth).formattedCHF(),
                       icon: "arrow.down.circle.fill", iconColor: KontivaTheme.positive,
                       valueColor: KontivaTheme.positive)
            MetricTile(title: loc(.planningFixed),
                       value: reveal(a.recurringFixedCosts).formattedCHF(),
                       icon: "repeat.circle.fill")
            MetricTile(title: loc(.planningVariable),
                       value: reveal(a.plannedVariableBudgets).formattedCHF(),
                       icon: "slider.horizontal.3")
            if !model.savingsGoals.isEmpty {
                MetricTile(title: loc(.navSparen),
                           value: model.totalMonthlySavings.formattedCHF(),
                           icon: "banknote.fill", iconColor: KontivaTheme.positive,
                           caption: "\(loc(.sparenAccumulatedTotal)): \(model.totalAccumulatedSavings.formattedCHF())")
            }
            MetricTile(title: loc(.navBills),
                       value: reveal(a.openBillsDueThisMonth).formattedCHF(),
                       icon: "doc.text.fill")
            if model.hasAnyDebt {
                MetricTile(title: loc(.navSchulden),
                           value: model.totalDebt.formattedCHF(),
                           icon: "creditcard", iconColor: KontivaTheme.swissRed,
                           valueColor: KontivaTheme.swissRed,
                           caption: model.totalOverdueBills.isZero ? nil
                                : "\(loc(.schuldenOverdueBills)): \(model.totalOverdueBills.formattedCHF())")
            }
        }
    }
}
