import SwiftUI
import AppKit
import KontivaCore

// MARK: - Form building blocks

/// A labelled form row. Inside a `Form().formStyle(.columns)` this renders as a
/// native aligned label/control pair (macOS form look).
struct FormField<Content: View>: View {
    let label: String
    @ViewBuilder var content: Content
    var body: some View {
        LabeledContent(label) { content }
    }
}

/// Standard add/edit sheet chrome: a title, a native columns `Form`, and a
/// Cancel / Save bar (Return saves, Esc cancels).
struct FormScaffold<Content: View>: View {
    @EnvironmentObject private var loc: Localizer
    let title: String
    let canSave: Bool
    let onCancel: () -> Void
    let onSave: () -> Void
    @ViewBuilder var content: Content

    var body: some View {
        VStack(spacing: 0) {
            Text(title).font(.headline)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, KontivaTheme.Space.lg)
                .padding(.top, KontivaTheme.Space.lg)
                .padding(.bottom, KontivaTheme.Space.xs)

            Form { content }
                .formStyle(.columns)
                .padding(.horizontal, KontivaTheme.Space.lg)
                .padding(.vertical, KontivaTheme.Space.sm)

            Divider()

            HStack {
                Button(loc(.commonCancel), action: onCancel)
                    .keyboardShortcut(.cancelAction)
                Spacer()
                Button(loc(.commonSave), action: onSave)
                    .keyboardShortcut(.defaultAction)
                    .buttonStyle(.borderedProminent).tint(KontivaTheme.accent)
                    .disabled(!canSave)
                    // Resign the focused text field as the cursor enters Save, so
                    // the click fires the button (macOS first-responder two-click bug).
                    .onHover { if $0 { NSApp.keyWindow?.makeFirstResponder(nil) } }
            }
            .padding(KontivaTheme.Space.md)
        }
        .frame(width: 460)
    }
}

/// Prefill helper: format a Money value for an editable text field (no symbol).
func moneyEditString(_ money: Money?) -> String {
    guard let money else { return "" }
    return money.formattedCHF(showSymbol: false)
}

/// A tidy date field that matches the rounded text fields: a chip showing the
/// formatted date which opens a graphical calendar in a popover. (The default
/// macOS stepper date picker looks out of place in these forms.)
struct FormDateField: View {
    @EnvironmentObject private var loc: Localizer
    @Binding var date: Date
    /// Show "Juni 2026" instead of a full day-precise date (for month-based fields).
    var monthYear: Bool = false
    @State private var open = false

    private var label: String {
        let f = DateFormatter()
        f.locale = loc.language.locale
        f.calendar = .swiss
        if monthYear { f.setLocalizedDateFormatFromTemplate("MMMM yyyy") } else { f.dateStyle = .medium }
        return f.string(from: date)
    }

    var body: some View {
        Button { open.toggle() } label: {
            HStack(spacing: KontivaTheme.Space.xs) {
                Image(systemName: "calendar").font(.system(size: 13))
                    .foregroundStyle(KontivaTheme.textTertiary)
                Text(label).foregroundStyle(KontivaTheme.textPrimary)
                Spacer(minLength: 0)
                Image(systemName: "chevron.up.chevron.down").font(.system(size: 10))
                    .foregroundStyle(KontivaTheme.textTertiary)
            }
            .padding(.horizontal, 10).padding(.vertical, 7)
            .background(RoundedRectangle(cornerRadius: KontivaTheme.Radius.control)
                .fill(Color(nsColor: .textBackgroundColor)))
            .overlay(RoundedRectangle(cornerRadius: KontivaTheme.Radius.control)
                .strokeBorder(KontivaTheme.softBorder, lineWidth: 1))
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .popover(isPresented: $open, arrowEdge: .bottom) {
            DatePicker("", selection: $date, displayedComponents: .date)
                .datePickerStyle(.graphical)
                .labelsHidden()
                .environment(\.locale, loc.language.locale)
                .padding(KontivaTheme.Space.md)
                .frame(width: 300)
        }
    }
}

// MARK: - Income

struct IncomeFormSheet: View {
    @EnvironmentObject private var model: AppModel
    @EnvironmentObject private var loc: Localizer
    @Environment(\.dismiss) private var dismiss

    let existing: Income?
    @State private var label: String
    @State private var amount: String
    @State private var thirteenthOn: Bool
    @State private var thirteenth: String
    @State private var thModel: ThirteenthSalaryModel

    init(existing: Income?) {
        self.existing = existing
        _label = State(initialValue: existing?.label ?? "")
        _amount = State(initialValue: moneyEditString(existing?.monthlyNet))
        _thirteenthOn = State(initialValue: existing?.thirteenthAmount != nil)
        _thirteenth = State(initialValue: moneyEditString(existing?.thirteenthAmount))
        _thModel = State(initialValue: existing?.thirteenthModel ?? .separate)
    }

    private var parsedNet: Money? { Money.parse(amount) }
    private var canSave: Bool { !label.isEmpty && parsedNet != nil && (!thirteenthOn || Money.parse(thirteenth) != nil) }

    var body: some View {
        FormScaffold(title: loc(existing == nil ? .commonAdd : .commonEdit),
                     canSave: canSave, onCancel: { dismiss() }, onSave: save) {
            FormField(label: loc(.formName)) {
                TextField("", text: $label, prompt: Text(loc(.planningIncome))).textFieldStyle(.roundedBorder)
            }
            FormField(label: loc(.formAmount)) {
                TextField("", text: $amount, prompt: Text(verbatim: "0.00")).textFieldStyle(.roundedBorder)
            }
            Toggle(isOn: $thirteenthOn) { Text(loc(.formThirteenthAmount)) }
            if thirteenthOn {
                TextField("", text: $thirteenth, prompt: Text(verbatim: "0.00")).textFieldStyle(.roundedBorder)
                Picker(loc(.formThirteenthModel), selection: $thModel) {
                    Text(loc(.thirteenthModelSeparate)).tag(ThirteenthSalaryModel.separate)
                    Text(loc(.thirteenthModelAveraged)).tag(ThirteenthSalaryModel.averagedMonthly)
                }.pickerStyle(.radioGroup)
            }
        }
    }

    private func save() {
        guard let net = parsedNet else { return }
        let income = Income(id: existing?.id ?? UUID(), label: label, monthlyNet: net,
                            thirteenthAmount: thirteenthOn ? Money.parse(thirteenth) : nil,
                            thirteenthModel: thModel)
        Task { await model.upsertIncome(income); dismiss() }
    }
}

// MARK: - Recurring fixed expense

struct FixedExpenseFormSheet: View {
    @EnvironmentObject private var model: AppModel
    @EnvironmentObject private var loc: Localizer
    @Environment(\.dismiss) private var dismiss

    let existing: RecurringFixedExpense?
    @State private var name: String
    @State private var amount: String
    @State private var category: FixedExpenseCategory
    @State private var limited: Bool
    @State private var startMonth: Date
    @State private var installments: Int

    init(existing: RecurringFixedExpense?) {
        self.existing = existing
        _name = State(initialValue: existing?.name ?? "")
        _amount = State(initialValue: moneyEditString(existing?.monthlyAmount))
        _category = State(initialValue: existing?.category ?? .other)
        _limited = State(initialValue: existing?.isLimited ?? false)
        _startMonth = State(initialValue: existing?.startMonth ?? Date())
        _installments = State(initialValue: existing?.installments ?? 6)
    }

    private var parsed: Money? { Money.parse(amount) }
    private var canSave: Bool { !name.isEmpty && parsed != nil && (!limited || installments >= 1) }

    var body: some View {
        FormScaffold(title: loc(existing == nil ? .commonAdd : .commonEdit),
                     canSave: canSave, onCancel: { dismiss() }, onSave: save) {
            FormField(label: loc(.formName)) {
                TextField("", text: $name, prompt: Text(loc(.planningFixed))).textFieldStyle(.roundedBorder)
            }
            FormField(label: loc(.formAmount)) {
                TextField("", text: $amount, prompt: Text(verbatim: "0.00")).textFieldStyle(.roundedBorder)
            }
            FormField(label: loc(.formCategory)) {
                Picker("", selection: $category) {
                    ForEach(FixedExpenseCategory.allCases, id: \.self) { c in
                        Label(c.localizedName(loc.localization), systemImage: c.systemImage).tag(c)
                    }
                }.labelsHidden()
            }

            Divider()
            Toggle(isOn: $limited) { Text(loc(.formLimitedDuration)) }
            Text(loc(.formLimitedDurationHint))
                .font(.caption2).foregroundStyle(KontivaTheme.textTertiary)
                .fixedSize(horizontal: false, vertical: true)
            if limited {
                FormField(label: loc(.formStartMonth)) {
                    FormDateField(date: $startMonth, monthYear: true)
                }
                FormField(label: loc(.formInstallments)) {
                    Stepper(value: $installments, in: 1...120) {
                        Text("\(installments)").monospacedDigit()
                    }
                }
            }
        }
    }

    private func save() {
        guard let amt = parsed else { return }
        let item = RecurringFixedExpense(id: existing?.id ?? UUID(), name: name,
                                         monthlyAmount: amt, category: category,
                                         startMonth: limited ? startMonth : nil,
                                         installments: limited ? installments : nil)
        Task { await model.upsertFixedCost(item); dismiss() }
    }
}

// MARK: - Variable budget

struct VariableBudgetFormSheet: View {
    @EnvironmentObject private var model: AppModel
    @EnvironmentObject private var loc: Localizer
    @Environment(\.dismiss) private var dismiss

    let existing: VariableMonthlyBudget?
    @State private var name: String
    @State private var amount: String
    @State private var category: VariableBudgetCategory

    init(existing: VariableMonthlyBudget?) {
        self.existing = existing
        _name = State(initialValue: existing?.name ?? "")
        _amount = State(initialValue: moneyEditString(existing?.plannedAmount))
        _category = State(initialValue: existing?.category ?? .other)
    }

    private var parsed: Money? { Money.parse(amount) }
    private var canSave: Bool { !name.isEmpty && parsed != nil }

    var body: some View {
        FormScaffold(title: loc(existing == nil ? .commonAdd : .commonEdit),
                     canSave: canSave, onCancel: { dismiss() }, onSave: save) {
            FormField(label: loc(.formName)) {
                TextField("", text: $name, prompt: Text(loc(.planningVariable))).textFieldStyle(.roundedBorder)
            }
            FormField(label: loc(.formAmount)) {
                TextField("", text: $amount, prompt: Text(verbatim: "0.00")).textFieldStyle(.roundedBorder)
            }
            FormField(label: loc(.formCategory)) {
                Picker("", selection: $category) {
                    ForEach(VariableBudgetCategory.allCases, id: \.self) { c in
                        Label(c.localizedName(loc.localization), systemImage: c.systemImage).tag(c)
                    }
                }.labelsHidden()
            }
        }
    }

    private func save() {
        guard let amt = parsed else { return }
        let item = VariableMonthlyBudget(id: existing?.id ?? UUID(), name: name,
                                         plannedAmount: amt, category: category)
        Task { await model.upsertVariableBudget(item); dismiss() }
    }
}

// MARK: - Savings goal

struct SavingsGoalFormSheet: View {
    @EnvironmentObject private var model: AppModel
    @EnvironmentObject private var loc: Localizer
    @Environment(\.dismiss) private var dismiss

    let existing: SavingsGoal?
    @State private var name: String
    @State private var category: SavingsCategory
    @State private var monthly: String
    @State private var startDate: Date
    @State private var startingBalance: String
    @State private var target: String

    init(existing: SavingsGoal?) {
        self.existing = existing
        _name = State(initialValue: existing?.name ?? "")
        _category = State(initialValue: existing?.category ?? .other)
        _monthly = State(initialValue: moneyEditString(existing?.monthlyContribution))
        _startDate = State(initialValue: existing?.startDate ?? Date())
        _startingBalance = State(initialValue: moneyEditString(existing?.startingBalance))
        _target = State(initialValue: existing.map { $0.hasTarget ? moneyEditString($0.target) : "" } ?? "")
    }

    private var parsedMonthly: Money? { Money.parse(monthly) }
    private var canSave: Bool {
        !name.isEmpty && parsedMonthly != nil
            && (target.isEmpty || Money.parse(target) != nil)
            && (startingBalance.isEmpty || Money.parse(startingBalance) != nil)
    }

    var body: some View {
        FormScaffold(title: loc(existing == nil ? .commonAdd : .commonEdit),
                     canSave: canSave, onCancel: { dismiss() }, onSave: save) {
            FormField(label: loc(.formName)) {
                TextField("", text: $name, prompt: Text(loc(.planningSavings))).textFieldStyle(.roundedBorder)
            }
            FormField(label: loc(.formCategory)) {
                Picker("", selection: $category) {
                    ForEach(SavingsCategory.allCases) { c in
                        Label(c.localizedName(loc.localization), systemImage: c.systemImage).tag(c)
                    }
                }.labelsHidden()
            }
            FormField(label: loc(.formMonthlyContribution)) {
                TextField("", text: $monthly, prompt: Text(verbatim: "0.00")).textFieldStyle(.roundedBorder)
            }
            FormField(label: loc(.formStartDate)) {
                FormDateField(date: $startDate, monthYear: true)
            }
            FormField(label: loc(.formStartingBalance)) {
                TextField("", text: $startingBalance, prompt: Text(verbatim: "0.00")).textFieldStyle(.roundedBorder)
            }
            FormField(label: loc(.formTarget)) {
                TextField("", text: $target, prompt: Text(verbatim: "0.00")).textFieldStyle(.roundedBorder)
            }
        }
    }

    private func save() {
        guard let monthlyAmount = parsedMonthly else { return }
        let item = SavingsGoal(id: existing?.id ?? UUID(), name: name, category: category,
                               target: Money.parse(target) ?? .zero,
                               monthlyContribution: monthlyAmount,
                               startingBalance: Money.parse(startingBalance) ?? .zero,
                               startDate: startDate)
        Task { await model.upsertSavingsGoal(item); dismiss() }
    }
}

// MARK: - One-off bill

struct BillFormSheet: View {
    @EnvironmentObject private var model: AppModel
    @EnvironmentObject private var loc: Localizer
    @Environment(\.dismiss) private var dismiss

    let existing: OneOffBill?
    @State private var provider: String
    @State private var amount: String
    @State private var dueDate: Date
    @State private var status: BillStatus
    @State private var notes: String

    init(existing: OneOffBill?) {
        self.existing = existing
        _provider = State(initialValue: existing?.provider ?? "")
        _amount = State(initialValue: moneyEditString(existing?.amount))
        _dueDate = State(initialValue: existing?.dueDate ?? Date())
        _status = State(initialValue: existing?.status ?? .open)
        _notes = State(initialValue: existing?.notes ?? "")
    }

    private var parsed: Money? { Money.parse(amount) }
    private var canSave: Bool { !provider.isEmpty && parsed != nil }

    var body: some View {
        FormScaffold(title: loc(existing == nil ? .commonAdd : .commonEdit),
                     canSave: canSave, onCancel: { dismiss() }, onSave: save) {
            FormField(label: loc(.billsProvider)) {
                TextField("", text: $provider, prompt: Text(loc(.billsProvider))).textFieldStyle(.roundedBorder)
            }
            FormField(label: loc(.formAmount)) {
                TextField("", text: $amount, prompt: Text(verbatim: "0.00")).textFieldStyle(.roundedBorder)
            }
            FormField(label: loc(.billsDueDate)) {
                FormDateField(date: $dueDate)
            }
            Picker(loc(.formStatus), selection: $status) {
                Text(loc(.billsStatusOpen)).tag(BillStatus.open)
                Text(loc(.billsStatusPaid)).tag(BillStatus.paid)
            }.pickerStyle(.segmented)
            FormField(label: loc(.formNotes)) {
                TextField("", text: $notes, axis: .vertical).textFieldStyle(.roundedBorder).lineLimit(2...4)
            }
        }
    }

    private func save() {
        guard let amt = parsed else { return }
        let bill = OneOffBill(id: existing?.id ?? UUID(), provider: provider, amount: amt,
                              dueDate: dueDate, status: status,
                              notes: notes.isEmpty ? nil : notes)
        Task { await model.upsertBill(bill); dismiss() }
    }
}

// MARK: - Debt (Schuld)

struct DebtFormSheet: View {
    @EnvironmentObject private var model: AppModel
    @EnvironmentObject private var loc: Localizer
    @Environment(\.dismiss) private var dismiss

    let existing: DebtItem?
    @State private var creditor: String
    @State private var amount: String
    @State private var type: DebtType
    @State private var hasDate: Bool
    @State private var date: Date
    @State private var reference: String
    @State private var notes: String

    init(existing: DebtItem?) {
        self.existing = existing
        _creditor = State(initialValue: existing?.creditor ?? "")
        _amount = State(initialValue: moneyEditString(existing?.amount))
        _type = State(initialValue: existing?.type ?? .openClaim)
        _hasDate = State(initialValue: existing?.date != nil)
        _date = State(initialValue: existing?.date ?? Date())
        _reference = State(initialValue: existing?.reference ?? "")
        _notes = State(initialValue: existing?.notes ?? "")
    }

    private var parsed: Money? { Money.parse(amount) }
    private var canSave: Bool { !creditor.isEmpty && parsed != nil }

    var body: some View {
        FormScaffold(title: loc(existing == nil ? .commonAdd : .commonEdit),
                     canSave: canSave, onCancel: { dismiss() }, onSave: save) {
            FormField(label: loc(.debtCreditor)) {
                TextField("", text: $creditor, prompt: Text(loc(.debtCreditor))).textFieldStyle(.roundedBorder)
            }
            FormField(label: loc(.formAmount)) {
                TextField("", text: $amount, prompt: Text(verbatim: "0.00")).textFieldStyle(.roundedBorder)
            }
            FormField(label: loc(.debtType)) {
                Picker("", selection: $type) {
                    ForEach(DebtType.allCases) { t in
                        Label(t.localizedName(loc.localization), systemImage: t.systemImage).tag(t)
                    }
                }.labelsHidden()
            }
            Toggle(isOn: $hasDate) { Text(loc(.debtDate)) }
            if hasDate {
                FormField(label: loc(.debtDate)) {
                    FormDateField(date: $date)
                }
            }
            FormField(label: loc(.debtReference)) {
                TextField("", text: $reference).textFieldStyle(.roundedBorder)
            }
            FormField(label: loc(.formNotes)) {
                TextField("", text: $notes, axis: .vertical).textFieldStyle(.roundedBorder).lineLimit(2...4)
            }
        }
    }

    private func save() {
        guard let amt = parsed else { return }
        let debt = DebtItem(id: existing?.id ?? UUID(), creditor: creditor, amount: amt,
                            type: type, date: hasDate ? date : nil,
                            reference: reference.isEmpty ? nil : reference,
                            notes: notes.isEmpty ? nil : notes)
        Task { await model.upsertDebt(debt); dismiss() }
    }
}
