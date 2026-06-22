import SwiftUI
import AppKit
import KontivaCore

/// The application root: a branded `NavigationSplitView`, gated by the lock screen.
/// The `AppModel` is owned by the app (so the menu commands and the Settings
/// window share it) and injected via the environment.
public struct RootView: View {
    @EnvironmentObject private var model: AppModel
    @EnvironmentObject private var loc: Localizer
    @State private var activityMonitor: Any?
    @State private var showExport = false
    @State private var savedToast = false
    // Periodic idle check (the decision itself uses the configured interval).
    private let idleTimer = Timer.publish(every: 15, on: .main, in: .common).autoconnect()

    public init() {}

    public var body: some View {
        Group {
            if model.onboardingActive {
                OnboardingView()
            } else {
                switch model.lockState {
                case .needsSetup, .locked:
                    LockView()
                case .unlocked:
                    mainSplit
                }
            }
        }
        .preferredColorScheme(colorScheme)
        .tint(KontivaTheme.accent)   // system controls follow the chosen theme
        // Mirror the layout for right-to-left scripts (Arabic, Urdu, Pashto).
        // `loc` republishes on language change, so this updates live.
        .environment(\.layoutDirection, loc.language.isRTL ? .rightToLeft : .leftToRight)
        .frame(minWidth: 920, minHeight: 600)
        .onReceive(idleTimer) { _ in Task { await model.checkAutoLock() } }
        .onAppear(perform: startActivityMonitor)
        .onDisappear(perform: stopActivityMonitor)
        // ⌘E export works from any section.
        .onChange(of: model.exportSignal) {
            if model.lockState == .unlocked { showExport = true }
        }
        .sheet(isPresented: $showExport) {
            ReportExportSheet().environmentObject(model).environmentObject(model.localizer)
        }
        // Brief "saved" confirmation after any entry is saved.
        .onChange(of: model.saveSignal) {
            withAnimation(.snappy) { savedToast = true }
            Task { try? await Task.sleep(for: .seconds(1.6)); withAnimation(.snappy) { savedToast = false } }
        }
        .overlay(alignment: .bottom) {
            if savedToast {
                HStack(spacing: KontivaTheme.Space.xs) {
                    Image(systemName: "checkmark.circle.fill").foregroundStyle(KontivaTheme.positive)
                    Text(model.localizer.localization.string(.commonSaved))
                }
                .font(.callout.weight(.medium))
                .padding(.horizontal, KontivaTheme.Space.md)
                .padding(.vertical, KontivaTheme.Space.sm)
                .background(.regularMaterial, in: Capsule())
                .overlay(Capsule().strokeBorder(KontivaTheme.softBorder.opacity(0.5), lineWidth: 1))
                .shadow(color: .black.opacity(0.12), radius: 12, y: 4)
                .padding(.bottom, KontivaTheme.Space.xl)
                .transition(.move(edge: .bottom).combined(with: .opacity))
            }
        }
    }

    /// Reset the idle timer on any mouse/keyboard activity within the app.
    private func startActivityMonitor() {
        guard activityMonitor == nil else { return }
        let mask: NSEvent.EventTypeMask = [.mouseMoved, .leftMouseDown, .rightMouseDown,
                                           .keyDown, .scrollWheel]
        activityMonitor = NSEvent.addLocalMonitorForEvents(matching: mask) { event in
            model.noteActivity()
            return event
        }
    }

    private func stopActivityMonitor() {
        if let monitor = activityMonitor {
            NSEvent.removeMonitor(monitor)
            activityMonitor = nil
        }
    }

    private var colorScheme: ColorScheme? {
        switch model.settings.appearance {
        case .system: return nil
        case .light:  return .light
        case .dark:   return .dark
        }
    }

    private var mainSplit: some View {
        NavigationSplitView {
            Sidebar()
                .environment(\.colorScheme, .dark)   // legible light text on charcoal
                .navigationSplitViewColumnWidth(min: 264, ideal: 300, max: 360)
        } detail: {
            DetailContainer()   // follows the system appearance (light/dark)
        }
        .navigationTitle("")
    }
}

/// The Kontiva-branded sidebar: charcoal with subtle depth and a red selection
/// pill (brand-forward, rather than the flat system highlight).
struct Sidebar: View {
    @EnvironmentObject private var model: AppModel
    @EnvironmentObject private var loc: Localizer

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            brandHeader
            Divider().opacity(0.25)

            VStack(spacing: 2) {
                ForEach(AppSection.allCases) { section in
                    SidebarRow(systemImage: section.systemImage,
                               title: loc(section.titleKey),
                               selected: model.selection == section) {
                        model.selection = section
                    }
                }
            }
            .padding(.horizontal, KontivaTheme.Space.xs + 2)
            .padding(.vertical, KontivaTheme.Space.sm)

            Spacer(minLength: 0)
            Divider().opacity(0.25)
            if let household = model.household, !household.name.isEmpty {
                profileChip(household)
                Divider().opacity(0.25)
            }
            lockFooter
        }
        .background(
            LinearGradient(colors: [Color(hex: 0x18222D), KontivaTheme.charcoal],
                           startPoint: .top, endPoint: .bottom)
        )
    }

    private func profileChip(_ household: Household) -> some View {
        HStack(spacing: KontivaTheme.Space.sm) {
            ProfileAvatar(name: household.avatarName, size: 30)
            VStack(alignment: .leading, spacing: 0) {
                Text(household.name)
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(KontivaTheme.offWhite)
                    .lineLimit(1)
                if let canton = household.canton {
                    Text("\(canton.name) (\(canton.abbreviation))")
                        .font(.caption2)
                        .foregroundStyle(KontivaTheme.offWhite.opacity(0.6))
                        .lineLimit(1)
                }
            }
            Spacer(minLength: 0)
        }
        .padding(.horizontal, KontivaTheme.Space.md)
        .padding(.vertical, KontivaTheme.Space.sm)
    }

    private var brandHeader: some View {
        HStack(spacing: 0) {
            // Transparent wordmark (mark + text), white on the dark sidebar.
            // Width-driven so it fills the sidebar as large as it fits.
            Group {
                if let image = BrandAsset.wordmarkDark {
                    image.resizable().interpolation(.high).scaledToFit()
                } else {
                    BrandWordmark(height: 66)
                }
            }
            .frame(maxWidth: 280, alignment: .leading)
            .accessibilityLabel("Kontiva")
            Spacer(minLength: 0)
        }
        .padding(.horizontal, KontivaTheme.Space.sm)
        .padding(.top, KontivaTheme.Space.lg)
        .padding(.bottom, KontivaTheme.Space.md)
    }

    private var lockFooter: some View {
        HStack(spacing: KontivaTheme.Space.md) {
            // Opens the native Settings window (⌘,).
            SettingsLink {
                footerLabel(loc(.settingsTitle), systemImage: "gearshape")
            }
            .buttonStyle(.plain)
            .help(loc(.settingsTitle))

            Spacer(minLength: 0)

            Button { Task { await model.lock() } } label: {
                footerLabel(loc(.lockTitle), systemImage: "lock")
            }
            .buttonStyle(.plain)
            .help(loc(.lockTitle))
        }
        .padding(.horizontal, KontivaTheme.Space.md)
        .padding(.vertical, KontivaTheme.Space.sm)
    }

    private func footerLabel(_ title: String, systemImage: String) -> some View {
        Label(title, systemImage: systemImage)
            .font(.system(size: 12, weight: .medium))
            .foregroundStyle(KontivaTheme.offWhite.opacity(0.85))
    }
}

/// One sidebar nav item: a red rounded pill when selected, a faint highlight on
/// hover, dimmed when idle.
private struct SidebarRow: View {
    let systemImage: String
    let title: String
    let selected: Bool
    let action: () -> Void
    @State private var hovered = false

    var body: some View {
        Button(action: action) {
            HStack(spacing: KontivaTheme.Space.sm) {
                Image(systemName: systemImage)
                    .font(.system(size: 13, weight: .medium))
                    .frame(width: 20)
                Text(title)
                    .font(.system(size: 13, weight: selected ? .semibold : .regular))
                Spacer(minLength: 0)
            }
            .foregroundStyle(selected ? KontivaTheme.offWhite
                                      : KontivaTheme.offWhite.opacity(hovered ? 0.95 : 0.7))
            .padding(.horizontal, KontivaTheme.Space.sm)
            .padding(.vertical, 7)
            .background(
                RoundedRectangle(cornerRadius: 7, style: .continuous)
                    .fill(selected ? AnyShapeStyle(KontivaTheme.accent)
                                   : AnyShapeStyle(hovered ? KontivaTheme.offWhite.opacity(0.08) : Color.clear))
            )
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .onHover { hovered = $0 }
        .animation(.easeOut(duration: 0.12), value: hovered)
        .animation(.easeOut(duration: 0.15), value: selected)
    }
}

/// Routes the selected section to its screen, with a gentle cross-fade between
/// sections.
struct DetailContainer: View {
    @EnvironmentObject private var model: AppModel

    var body: some View {
        ZStack {
            switch model.selection {
            case .overview:  OverviewView()
            case .insights:  InsightsView()
            case .planning:  PlanningView()
            case .sparen:    SparenView()
            case .bills:     BillsView()
            case .schulden:  SchuldenView()
            case .report:    ReportProblemView()
            }
        }
        .id(model.selection)
        .transition(.opacity)
        .animation(.easeInOut(duration: 0.18), value: model.selection)
        .background(KontivaTheme.pageGradient.ignoresSafeArea())
    }
}
