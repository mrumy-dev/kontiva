import SwiftUI
import KontivaCore

/// Einstellungen — a single grouped iOS settings screen (the desktop uses a tabbed
/// window). Pushed inside the "Mehr" tab's navigation stack. Themes, backup/restore
/// and the avatar picker are follow-ups (backup/restore needs the iOS file pickers).
struct SettingsView: View {
    @EnvironmentObject private var model: AppModel
    @EnvironmentObject private var loc: Localizer

    @State private var profileName = ""
    @State private var canton: Canton?
    @State private var avatarName: String?
    @State private var showAvatarPicker = false
    @State private var showDeleteConfirm = false
    @State private var showChangePassphrase = false
    @State private var showBackup = false
    @State private var showRestore = false

    var body: some View {
        Form {
            profileSection
            languageSection
            themeSection
            securitySection
            dataSection
            dangerSection
            aboutSection
        }
        .tint(KontivaTheme.accent)
        .navigationTitle(loc(.navSettings))
        .navigationBarTitleDisplayMode(.inline)
        .onAppear(perform: loadProfile)
        .sheet(isPresented: $showAvatarPicker) {
            AvatarPickerSheet(selected: $avatarName).environmentObject(loc)
        }
        .onChange(of: avatarName) { saveProfile() }
        .sheet(isPresented: $showChangePassphrase) {
            ChangePassphraseSheet().environmentObject(model).environmentObject(loc)
        }
        .sheet(isPresented: $showBackup) {
            BackupSheet().environmentObject(model).environmentObject(loc)
        }
        .sheet(isPresented: $showRestore) {
            RestoreSheet().environmentObject(model).environmentObject(loc)
        }
        .confirmationDialog(loc(.settingsDeleteAll), isPresented: $showDeleteConfirm, titleVisibility: .visible) {
            Button(loc(.settingsDeleteAll), role: .destructive) { Task { await model.deleteAllLocalData() } }
            Button(loc(.commonCancel), role: .cancel) { }
        } message: {
            Text(loc(.lockRecoveryWarning))
        }
    }

    private var profileSection: some View {
        Section(loc(.settingsProfile)) {
            Button { showAvatarPicker = true } label: {
                HStack(spacing: KontivaTheme.Space.md) {
                    ProfileAvatar(name: avatarName, size: 52)
                    Text(profileName.isEmpty ? loc(.pdfDefaultHousehold) : profileName)
                        .font(.headline).foregroundStyle(KontivaTheme.textPrimary)
                    Spacer(minLength: 0)
                    Image(systemName: "pencil.circle").foregroundStyle(KontivaTheme.textTertiary)
                }
            }
            .buttonStyle(.plain)
            TextField(loc(.profileName), text: $profileName)
            Picker(loc(.settingsCanton), selection: $canton) {
                Text("—").tag(Canton?.none)
                ForEach(Canton.all) { c in
                    Text("\(c.name) (\(c.abbreviation))").tag(Canton?.some(c))
                }
            }
            Button(loc(.commonSave), action: saveProfile)
                .disabled(profileName.isEmpty)
        }
    }

    private var languageSection: some View {
        Section(loc(.settingsLanguage)) {
            Picker(loc(.settingsLanguage), selection: Binding(
                get: { model.settings.language },
                set: { model.setLanguage($0) })) {
                ForEach(AppLanguage.allCases) { Text($0.displayName).tag($0) }
            }
        }
    }

    private var themeSection: some View {
        Section {
            // Curated presets (gradient / 2-colour) — pick by look.
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: KontivaTheme.Space.xs) {
                    ForEach(themePresets) { presetSwatch($0) }
                }
                .padding(.vertical, KontivaTheme.Space.xs)
            }
            // Solid accent colours.
            HStack(spacing: 0) {
                ForEach(AccentTheme.allCases) { theme in
                    accentSwatch(theme).frame(maxWidth: .infinity)
                }
            }
            .padding(.vertical, KontivaTheme.Space.sm)
        } header: {
            Text(loc(.settingsTheme))
        }
    }

    private struct ThemePreset: Identifiable {
        let primary: AccentTheme, style: ThemeStyle, secondary: AccentTheme
        var id: String { "\(primary.rawValue)-\(style.rawValue)-\(secondary.rawValue)" }
    }
    private let themePresets: [ThemePreset] = [
        .init(primary: .swissRed, style: .solid, secondary: .swissRed),
        .init(primary: .blue, style: .dual, secondary: .teal),
        .init(primary: .orange, style: .dual, secondary: .pink),
        .init(primary: .green, style: .dual, secondary: .teal),
        .init(primary: .purple, style: .dual, secondary: .pink),
        .init(primary: .teal, style: .dual, secondary: .blue),
        .init(primary: .pink, style: .dual, secondary: .purple),
        .init(primary: .blue, style: .gradient, secondary: .blue),
        .init(primary: .purple, style: .gradient, secondary: .purple),
        .init(primary: .sand, style: .gradient, secondary: .sand),
    ]

    private func presetSwatch(_ p: ThemePreset) -> some View {
        let selected = model.settings.accent == p.primary && model.settings.themeStyle == p.style
            && model.settings.accentSecondary == p.secondary
        let colors: [Color]
        switch p.style {
        case .solid:    colors = [p.primary.color, p.primary.color]
        case .gradient: colors = [p.primary.color, p.primary.color.blended(with: .white, fraction: 0.5)]
        case .dual:     colors = [p.primary.color, p.secondary.color]
        }
        return Button { model.applyTheme(p.primary, style: p.style, secondary: p.secondary) } label: {
            Circle()
                .fill(LinearGradient(colors: colors, startPoint: .topLeading, endPoint: .bottomTrailing))
                .frame(width: 30, height: 30)
                .overlay(Circle().strokeBorder(.black.opacity(0.08), lineWidth: 0.5))
                .padding(5)
                .overlay {
                    Circle().strokeBorder(p.primary.color, lineWidth: 2.5)
                        .opacity(selected ? 1 : 0).scaleEffect(selected ? 1 : 0.72)
                }
                .contentShape(Circle())
                .animation(.snappy(duration: 0.22), value: selected)
        }
        .buttonStyle(.plain)
    }

    /// One accent swatch, iOS-style: a filled dot that, when selected, sits
    /// inside a same-colour ring with a clear gap (no checkmark).
    private func accentSwatch(_ theme: AccentTheme) -> some View {
        let selected = model.settings.accent == theme && model.settings.themeStyle == .solid
        return Button { model.setAccent(theme) } label: {
            Circle()
                .fill(theme.color)
                .frame(width: 28, height: 28)
                .overlay(Circle().strokeBorder(.black.opacity(0.08), lineWidth: 0.5)) // edge on white
                .shadow(color: theme.color.opacity(selected ? 0.45 : 0), radius: 5, y: 1)
                .padding(5) // gap for the selection ring
                .overlay {
                    Circle()
                        .strokeBorder(theme.color, lineWidth: 2.5)
                        .opacity(selected ? 1 : 0)
                        .scaleEffect(selected ? 1 : 0.72)
                }
                .contentShape(Circle())
                .animation(.snappy(duration: 0.22), value: selected)
        }
        .buttonStyle(.plain)
        .accessibilityLabel(loc(theme.labelKey))
        .accessibilityAddTraits(selected ? [.isSelected] : [])
    }

    private var securitySection: some View {
        Section {
            if model.biometricKind.isAvailable {
                Toggle(isOn: Binding(
                    get: { model.biometricEnabled },
                    set: { on in if on { _ = model.enableBiometric() } else { model.disableBiometric() } })) {
                    Label(model.biometricKind.label, systemImage: model.biometricKind.icon)
                }
            }
            Picker(loc(.settingsAutoLock), selection: Binding(
                get: { model.autoLock },
                set: { v in Task { await model.setAutoLock(v) } })) {
                ForEach(AutoLockInterval.allCases, id: \.self) { Text($0.displayLabel).tag($0) }
            }
            Button(loc(.settingsChangePassphrase)) { showChangePassphrase = true }
        } header: {
            Text(loc(.settingsSecurity))
        } footer: {
            Label(loc(.securityNote), systemImage: "lock.fill")
                .font(.caption).foregroundStyle(KontivaTheme.positive)
        }
    }

    private var dataSection: some View {
        Section {
            Button { showBackup = true } label: { Label(loc(.settingsBackup), systemImage: "arrow.up.doc") }
            Button { showRestore = true } label: { Label(loc(.settingsRestore), systemImage: "arrow.down.doc") }
            Button(action: exportPDF) { Label(loc(.exportReport), systemImage: "doc.richtext") }
        } header: {
            Text(loc(.settingsData))
        } footer: {
            Text(loc(.backupSavedHint))
        }
    }

    private func exportPDF() {
        guard let data = ReportBuilder.makePDF(model: model) else { return }
        let url = FileManager.default.temporaryDirectory
            .appendingPathComponent(ReportBuilder.suggestedFilename(month: model.selectedMonth))
        try? data.write(to: url, options: [.atomic])
        Share.present([url])
    }

    private var dangerSection: some View {
        Section {
            Button(role: .destructive) { showDeleteConfirm = true } label: {
                Label(loc(.settingsDeleteAll), systemImage: "trash")
            }
        } header: {
            Text(loc(.settingsDangerZone))
        } footer: {
            Text(loc(.lockRecoveryWarning))
        }
    }

    private var aboutSection: some View {
        Section {
            Text(loc(.appTagline)).foregroundStyle(KontivaTheme.textSecondary)
            Label("local-first · offline-first · no telemetry · no network", systemImage: "lock.shield")
                .font(.caption).foregroundStyle(KontivaTheme.positive)
            Label("AES-256-GCM · PBKDF2", systemImage: "key.fill")
                .font(.caption).foregroundStyle(KontivaTheme.textTertiary)
        } footer: {
            Text("Kontiva \(AppInfo.version)")
        }
    }

    private func loadProfile() {
        guard let h = model.household else { return }
        profileName = h.name
        canton = h.canton
        avatarName = h.avatarName
    }

    private func saveProfile() {
        Task { await model.updateProfile(name: profileName, avatarName: avatarName, canton: canton) }
    }
}

/// Change the passphrase. The store re-wraps the same master key, so existing data
/// stays readable; biometrics are kept in sync by the model.
private struct ChangePassphraseSheet: View {
    @EnvironmentObject private var model: AppModel
    @EnvironmentObject private var loc: Localizer
    @Environment(\.dismiss) private var dismiss

    @State private var oldPass = ""
    @State private var newPass = ""
    @State private var failed = false

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    SecureField(loc(.lockEnterPassphrase), text: $oldPass)
                    SecureField(loc(.settingsChangePassphrase), text: $newPass)
                }
                if failed {
                    Text(loc(.lockWrongPassphrase)).font(.caption).foregroundStyle(KontivaTheme.swissRed)
                }
            }
            .navigationTitle(loc(.settingsChangePassphrase))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button(loc(.commonCancel)) { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button(loc(.commonSave), action: submit)
                        .fontWeight(.semibold)
                        .disabled(oldPass.isEmpty || newPass.isEmpty || model.isWorking)
                }
            }
        }
    }

    private func submit() {
        Task {
            let ok = await model.changePassphrase(old: oldPass, new: newPass)
            if ok { dismiss() } else { failed = true }
        }
    }
}
