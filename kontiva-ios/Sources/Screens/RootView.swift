import SwiftUI

/// Top-level gate: first run → onboarding, existing vault → lock, unlocked → app.
struct RootView: View {
    @EnvironmentObject private var model: AppModel
    @EnvironmentObject private var loc: Localizer

    var body: some View {
        ZStack {
            KontivaTheme.pageBackground.ignoresSafeArea()
            switch model.lockState {
            case .needsSetup: OnboardingView()
            case .locked:     LockView()
            case .unlocked:   MainTabView()
            }
        }
        // Mirror the whole UI for right-to-left scripts (Arabic, Urdu, Pashto).
        // `loc` republishes on language change, so this updates live.
        .environment(\.layoutDirection, loc.language.isRTL ? .rightToLeft : .leftToRight)
        // Rebuild the tree fresh when crossing the LTR↔RTL boundary. Toggling
        // layoutDirection on a live NavigationStack otherwise leaves the active
        // tab horizontally mirrored until relaunch; a new identity avoids that.
        .id(loc.language.isRTL)
    }
}
