import SwiftUI
import AppKit
import KontivaCore
import KontivaPersistence

/// Create a portable encrypted backup protected by a separate backup passphrase.
struct BackupExportSheet: View {
    @EnvironmentObject private var model: AppModel
    @EnvironmentObject private var loc: Localizer
    @Environment(\.dismiss) private var dismiss

    @State private var passphrase = ""
    @State private var error: String?

    var body: some View {
        VStack(alignment: .leading, spacing: KontivaTheme.Space.md) {
            Text(loc(.backupCreateTitle)).font(.headline)
            Text(loc(.backupHint)).font(.caption).foregroundStyle(KontivaTheme.textSecondary)
                .fixedSize(horizontal: false, vertical: true)
            SecureField(loc(.backupPassphrase), text: $passphrase).textFieldStyle(.roundedBorder)
            if let error { Text(error).font(.caption).foregroundStyle(KontivaTheme.swissRed) }
            Text(loc(.backupSavedHint)).font(.caption2).foregroundStyle(KontivaTheme.textTertiary)
                .fixedSize(horizontal: false, vertical: true)
            HStack {
                Button(loc(.commonCancel)) { dismiss() }
                Spacer()
                Button(loc(.commonSave), action: export)
                    .buttonStyle(.borderedProminent).tint(KontivaTheme.accent)
                    .disabled(passphrase.isEmpty || model.isWorking)
                    .onHover { if $0 { NSApp.keyWindow?.makeFirstResponder(nil) } }
            }
        }
        .padding(KontivaTheme.Space.lg).frame(width: 440)
    }

    private func export() {
        let pass = passphrase
        Task {
            guard let data = await model.makeBackupData(passphrase: pass) else {
                error = loc(.backupInvalid); return
            }
            let panel = NSSavePanel()
            let df = DateFormatter(); df.dateFormat = "yyyy-MM-dd"
            panel.nameFieldStringValue = "kontiva-\(df.string(from: Date())).kontivabackup"
            panel.canCreateDirectories = true
            if panel.runModal() == .OK, let url = panel.url {
                try? data.write(to: url, options: [.atomic])
                dismiss()
            }
        }
    }
}

/// Guarded restore: pick a backup file, preview its contents, then confirm the
/// destructive replace.
struct RestoreSheet: View {
    @EnvironmentObject private var model: AppModel
    @EnvironmentObject private var loc: Localizer
    @Environment(\.dismiss) private var dismiss

    @State private var fileURL: URL?
    @State private var fileData: Data?
    @State private var passphrase = ""
    @State private var preview: BackupPreview?
    @State private var error: String?

    var body: some View {
        VStack(alignment: .leading, spacing: KontivaTheme.Space.md) {
            Text(loc(.settingsRestore)).font(.headline)

            Button {
                pickFile()
            } label: {
                Label(fileURL?.lastPathComponent ?? loc(.settingsRestore), systemImage: "doc")
            }

            SecureField(loc(.backupPassphrase), text: $passphrase).textFieldStyle(.roundedBorder)

            Button(loc(.restorePreview), action: loadPreview)
                .disabled(fileData == nil || passphrase.isEmpty || model.isWorking)
                .onHover { if $0 { NSApp.keyWindow?.makeFirstResponder(nil) } }

            if let preview {
                Divider()
                VStack(alignment: .leading, spacing: KontivaTheme.Space.xxs) {
                    Text(SwissDate.medium(preview.createdAt, locale: loc.language.locale))
                        .font(.caption).foregroundStyle(KontivaTheme.textSecondary)
                    Text(summary(preview)).font(.callout).foregroundStyle(KontivaTheme.textPrimary)
                }
                Text(loc(.restoreWarning)).font(.caption).foregroundStyle(KontivaTheme.swissRed)
                    .fixedSize(horizontal: false, vertical: true)
                Button(role: .destructive, action: doRestore) {
                    Label(loc(.restoreConfirm), systemImage: "exclamationmark.triangle.fill")
                }
                .buttonStyle(.borderedProminent).tint(KontivaTheme.swissRed)
                .disabled(model.isWorking)
            }

            if let error { Text(error).font(.caption).foregroundStyle(KontivaTheme.swissRed) }

            HStack { Spacer(); Button(loc(.commonCancel)) { dismiss() } }
        }
        .padding(KontivaTheme.Space.lg).frame(width: 460)
    }

    private func summary(_ p: BackupPreview) -> String {
        func n(_ key: String) -> Int { p.counts[key] ?? 0 }
        return "\(loc(.planningIncome)): \(n("incomes")) · \(loc(.planningFixed)): \(n("fixedCosts")) · \(loc(.billsTitle)): \(n("bills"))"
    }

    private func pickFile() {
        let panel = NSOpenPanel()
        panel.allowsMultipleSelection = false
        panel.canChooseDirectories = false
        if panel.runModal() == .OK, let url = panel.url {
            let scoped = url.startAccessingSecurityScopedResource()
            let data = try? Data(contentsOf: url)
            if scoped { url.stopAccessingSecurityScopedResource() }
            fileURL = url
            fileData = data
            preview = nil
            error = data == nil ? loc(.backupInvalid) : nil
        }
    }

    private func loadPreview() {
        guard let data = fileData else { return }
        let pass = passphrase
        Task {
            if let p = await model.previewBackup(data: data, passphrase: pass) {
                preview = p; error = nil
            } else {
                preview = nil; error = loc(.backupInvalid)
            }
        }
    }

    private func doRestore() {
        guard let data = fileData else { return }
        let pass = passphrase
        Task {
            let ok = await model.restoreFromBackup(data: data, passphrase: pass)
            if ok { dismiss() } else { error = loc(.backupInvalid) }
        }
    }
}
