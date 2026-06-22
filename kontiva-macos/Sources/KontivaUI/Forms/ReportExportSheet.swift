import SwiftUI
import AppKit
import UniformTypeIdentifiers
import KontivaCore

/// Export the whole budget as a polished, multi-page PDF report. The report is a
/// human-readable document (unlike the encrypted backup), so the sheet says so
/// plainly before the user saves it to a location they choose.
struct ReportExportSheet: View {
    @EnvironmentObject private var model: AppModel
    @EnvironmentObject private var loc: Localizer
    @Environment(\.dismiss) private var dismiss

    @State private var working = false
    @State private var error: String?

    var body: some View {
        VStack(alignment: .leading, spacing: KontivaTheme.Space.md) {
            Text(loc(.exportReport)).font(.headline)

            Text(loc(.pdfSummary) + " · " + ReportBuilder.monthLabel(model.selectedMonth,
                                                                        locale: loc.language.locale))
                .font(.callout).foregroundStyle(KontivaTheme.textSecondary)

            Label(loc(.pdfUnencryptedNote), systemImage: "doc.text.magnifyingglass")
                .font(.caption).foregroundStyle(KontivaTheme.textSecondary)
                .fixedSize(horizontal: false, vertical: true)

            if let error {
                Text(error).font(.caption).foregroundStyle(KontivaTheme.swissRed)
            }

            HStack {
                Button(loc(.commonCancel)) { dismiss() }
                Spacer()
                Button(loc(.exportReport), action: export)
                    .buttonStyle(.borderedProminent).tint(KontivaTheme.accent)
                    .disabled(working)
                    .onHover { if $0 { NSApp.keyWindow?.makeFirstResponder(nil) } }
            }
        }
        .padding(KontivaTheme.Space.lg)
        .frame(width: 440)
    }

    private func export() {
        working = true
        defer { working = false }
        guard let data = ReportBuilder.makePDF(model: model) else {
            error = loc(.pdfNoData); return
        }
        let panel = NSSavePanel()
        panel.allowedContentTypes = [.pdf]
        panel.nameFieldStringValue = ReportBuilder.suggestedFilename(month: model.selectedMonth)
        panel.canCreateDirectories = true
        if panel.runModal() == .OK, let url = panel.url {
            try? data.write(to: url, options: [.atomic])
            dismiss()
        }
    }
}
