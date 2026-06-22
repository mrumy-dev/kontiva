import SwiftUI
import KontivaUI
import KontivaCore

/// Kontiva — native macOS app entry point.
///
/// Local-first, offline-first. No networking, no telemetry, no analytics, no
/// payment logic. This is an early development build — not beta-ready, not V1.
@main
struct KontivaApp: App {
    // Owned at the app level so the menu commands and the Settings window share the
    // same instance as the main window.
    @StateObject private var model = AppModel()

    var body: some Scene {
        WindowGroup("Kontiva") {
            RootView()
                .environmentObject(model)
                .environmentObject(model.localizer)
        }
        .windowStyle(.titleBar)
        .windowToolbarStyle(.unified)
        .defaultSize(width: 1180, height: 760)
        .commands {
            KontivaCommands(model: model)
        }

        // Native Settings window (Kontiva ▸ Settings…, ⌘,).
        Settings {
            SettingsWindow()
                .environmentObject(model)
                .environmentObject(model.localizer)
        }
    }
}
