import SwiftUI
import AppKit
import KontivaCore

/// The native macOS Settings window (Kontiva ▸ Settings…, ⌘,): a tabbed set of
/// grouped forms.
public struct SettingsWindow: View {
    @EnvironmentObject private var model: AppModel
    @EnvironmentObject private var loc: Localizer

    public init() {}

    @State private var profileName = ""
    @State private var avatarName: String? = nil
    @State private var canton: Canton? = nil
    @State private var showAvatarPicker = false
    @State private var showDeleteConfirm = false
    @State private var showChangePassphrase = false
    @State private var showBackup = false
    @State private var showRestore = false
    @State private var showExport = false

    private var locked: Bool { model.lockState != .unlocked }

    public var body: some View {
        TabView {
            profileTab.tabItem { Label(loc(.settingsProfile), systemImage: "person.crop.circle") }
            generalTab.tabItem { Label(loc(.settingsLanguage), systemImage: "globe") }
            securityTab.tabItem { Label(loc(.settingsSecurity), systemImage: "lock") }
            dataTab.tabItem { Label(loc(.settingsData), systemImage: "externaldrive") }
            privacyTab.tabItem { Label(loc(.settingsPrivacy), systemImage: "hand.raised") }
        }
        .frame(width: 500, height: 420)
        .tint(KontivaTheme.accent)
        .environment(\.layoutDirection, loc.language.isRTL ? .rightToLeft : .leftToRight)
        .onAppear(perform: loadProfile)
        .sheet(isPresented: $showAvatarPicker) {
            AvatarPickerSheet(selected: $avatarName).environmentObject(loc)
        }
        .sheet(isPresented: $showExport) {
            ReportExportSheet().environmentObject(model).environmentObject(loc)
        }
        .sheet(isPresented: $showChangePassphrase) {
            ChangePassphraseSheet().environmentObject(model).environmentObject(loc)
        }
        .sheet(isPresented: $showBackup) {
            BackupExportSheet().environmentObject(model).environmentObject(loc)
        }
        .sheet(isPresented: $showRestore) {
            RestoreSheet().environmentObject(model).environmentObject(loc)
        }
        .confirmationDialog(loc(.settingsDeleteAll), isPresented: $showDeleteConfirm, titleVisibility: .visible) {
            Button(loc(.settingsDeleteAll), role: .destructive) {
                Task { await model.deleteAllLocalData() }
            }
            Button(loc(.commonCancel), role: .cancel) { }
        } message: {
            Text(loc(.lockRecoveryWarning))
        }
    }

    private var profileTab: some View {
        Form {
            Section {
                HStack(spacing: KontivaTheme.Space.md) {
                    Button { showAvatarPicker = true } label: {
                        ZStack(alignment: .bottomTrailing) {
                            ProfileAvatar(name: avatarName, size: 56)
                            Image(systemName: "pencil.circle.fill")
                                .font(.system(size: 16))
                                .foregroundStyle(KontivaTheme.accent)
                                .background(Circle().fill(KontivaTheme.cardSurface))
                        }
                    }
                    .buttonStyle(.plain).help(loc(.profileChoosePicture))
                    Text(profileName.isEmpty ? loc(.pdfDefaultHousehold) : profileName).font(.headline)
                    Spacer(minLength: 0)
                }
            }
            Section {
                TextField(loc(.profileName), text: $profileName).onSubmit(saveProfile)
                // Canton stored by name + abbreviation only (no emblem).
                Picker(loc(.settingsCanton), selection: $canton) {
                    Text("—").tag(Canton?.none)
                    ForEach(Canton.all) { c in
                        Text("\(c.name) (\(c.abbreviation))").tag(Canton?.some(c))
                    }
                }
            }
            Section {
                Button(loc(.commonSave), action: saveProfile)
                    .disabled(locked || profileName.isEmpty)
                Text(loc(.profileLocalNote)).font(.caption).foregroundStyle(KontivaTheme.textTertiary)
            }
        }
        .formStyle(.grouped)
    }

    private var generalTab: some View {
        Form {
            Picker(loc(.settingsLanguage), selection: Binding(
                get: { model.settings.language }, set: { model.setLanguage($0) })) {
                ForEach(AppLanguage.allCases) { Text($0.displayName).tag($0) }
            }
            Section(loc(.settingsTheme)) {
                HStack(spacing: KontivaTheme.Space.sm) {
                    ForEach(AccentTheme.allCases) { accentSwatch($0) }
                    Spacer(minLength: 0)
                }
                .padding(.vertical, KontivaTheme.Space.xxs)
                Text(loc(model.settings.accent.labelKey))
                    .font(.caption).foregroundStyle(KontivaTheme.textSecondary)
            }
        }
        .formStyle(.grouped)
    }

    /// A round accent swatch; the selected one shows a ring + checkmark.
    private func accentSwatch(_ theme: AccentTheme) -> some View {
        let selected = model.settings.accent == theme
        return Button { model.setAccent(theme) } label: {
            ZStack {
                Circle().fill(theme.color).frame(width: 30, height: 30)
                    .overlay(Circle().strokeBorder(.white.opacity(0.45), lineWidth: 1))
                if selected {
                    Image(systemName: "checkmark")
                        .font(.system(size: 12, weight: .bold))
                        .foregroundStyle(.white)
                }
            }
            .padding(4)
            .overlay(Circle().strokeBorder(selected ? theme.color : Color.clear, lineWidth: 2))
            .contentShape(Circle())
        }
        .buttonStyle(.plain)
        .help(loc(theme.labelKey))
        .accessibilityLabel(loc(theme.labelKey))
    }

    private var securityTab: some View {
        Form {
            Section {
                if model.biometricKind.isAvailable {
                    Toggle(isOn: Binding(
                        get: { model.biometricEnabled },
                        set: { on in if on { _ = model.enableBiometric() } else { model.disableBiometric() } })) {
                        Label(model.biometricKind.label, systemImage: model.biometricKind.icon)
                    }
                    .disabled(locked)
                }
                Picker(loc(.settingsAutoLock), selection: Binding(
                    get: { model.autoLock },
                    set: { v in Task { await model.setAutoLock(v) } })) {
                    ForEach(AutoLockInterval.allCases, id: \.self) { Text($0.displayLabel).tag($0) }
                }
                .disabled(locked)
            }
            Section {
                Button(loc(.settingsChangePassphrase)) { showChangePassphrase = true }.disabled(locked)
                Label(loc(.securityNote), systemImage: "lock.fill")
                    .font(.caption).foregroundStyle(KontivaTheme.positive)
            }
        }
        .formStyle(.grouped)
    }

    private var dataTab: some View {
        Form {
            Section(loc(.settingsBackup)) {
                Button(loc(.settingsBackup)) { showBackup = true }.disabled(locked)
                Button(loc(.settingsRestore)) { showRestore = true }.disabled(locked)
                Text(loc(.backupSavedHint)).font(.caption).foregroundStyle(KontivaTheme.textTertiary)
            }
            Section(loc(.exportReport)) {
                Button(loc(.exportReport)) { showExport = true }.disabled(locked)
                Text(loc(.pdfUnencryptedNote)).font(.caption).foregroundStyle(KontivaTheme.textTertiary)
            }
            Section(loc(.settingsDangerZone)) {
                Button(role: .destructive) { showDeleteConfirm = true } label: {
                    Label(loc(.settingsDeleteAll), systemImage: "trash")
                }
                .disabled(locked)
                Text(loc(.lockRecoveryWarning)).font(.caption).foregroundStyle(KontivaTheme.textTertiary)
            }
        }
        .formStyle(.grouped)
    }

    private var privacyTab: some View {
        Form {
            Section {
                Text(loc(.appTagline)).foregroundStyle(KontivaTheme.textSecondary)
                Label("local-first · offline-first · no telemetry · no network",
                      systemImage: "lock.shield")
                    .font(.caption).foregroundStyle(KontivaTheme.positive)
            }
        }
        .formStyle(.grouped)
    }

    private func loadProfile() {
        guard let h = model.household else { return }
        profileName = h.name
        avatarName = h.avatarName
        canton = h.canton
    }

    private func saveProfile() {
        Task { await model.updateProfile(name: profileName, avatarName: avatarName, canton: canton) }
    }
}

/// Sheet for changing the passphrase. The backend re-wraps the same master key,
/// so existing encrypted data stays readable.
private struct ChangePassphraseSheet: View {
    @EnvironmentObject private var model: AppModel
    @EnvironmentObject private var loc: Localizer
    @Environment(\.dismiss) private var dismiss

    @State private var oldPass = ""
    @State private var newPass = ""
    @State private var failed = false

    var body: some View {
        VStack(alignment: .leading, spacing: KontivaTheme.Space.md) {
            Text(loc(.settingsChangePassphrase)).font(.headline)
            SecureField(loc(.lockEnterPassphrase), text: $oldPass)
                .textFieldStyle(.roundedBorder)
            SecureField("Neue Passphrase", text: $newPass)
                .textFieldStyle(.roundedBorder)
                .onSubmit(submit)
            if failed {
                Text(loc(.lockWrongPassphrase)).font(.caption).foregroundStyle(KontivaTheme.swissRed)
            }
            HStack {
                Button(loc(.commonCancel)) { dismiss() }
                Spacer()
                Button(loc(.commonSave), action: submit)
                    .buttonStyle(.borderedProminent).tint(KontivaTheme.accent)
                    .disabled(oldPass.isEmpty || newPass.isEmpty || model.isWorking)
                    .onHover { if $0 { NSApp.keyWindow?.makeFirstResponder(nil) } }
            }
        }
        .padding(KontivaTheme.Space.lg)
        .frame(width: 360)
    }

    private func submit() {
        guard !oldPass.isEmpty, !newPass.isEmpty else { return }
        Task {
            let ok = await model.changePassphrase(old: oldPass, new: newPass)
            if ok { dismiss() } else { failed = true }
        }
    }
}

/// Pick a bundled profile picture from a tidy grid.
struct AvatarPickerSheet: View {
    @EnvironmentObject private var loc: Localizer
    @Environment(\.dismiss) private var dismiss
    @Binding var selected: String?

    private let columns = Array(repeating: GridItem(.fixed(64), spacing: KontivaTheme.Space.md), count: 4)

    var body: some View {
        VStack(alignment: .leading, spacing: KontivaTheme.Space.md) {
            Text(loc(.profileChoosePicture)).font(.headline)
            LazyVGrid(columns: columns, spacing: KontivaTheme.Space.md) {
                ForEach(ProfileIcons.all, id: \.self) { name in
                    Button { selected = name; dismiss() } label: {
                        ProfileAvatar(name: name, size: 64)
                            .overlay(
                                RoundedRectangle(cornerRadius: 64 * 0.24, style: .continuous)
                                    .strokeBorder(selected == name ? KontivaTheme.accent : Color.clear,
                                                  lineWidth: 3)
                            )
                    }
                    .buttonStyle(.plain)
                }
            }
            HStack {
                Button(loc(.profileNoPicture)) { selected = nil; dismiss() }
                Spacer()
                Button(loc(.commonCancel)) { dismiss() }
            }
        }
        .padding(KontivaTheme.Space.lg)
        .frame(width: 408)
    }
}
