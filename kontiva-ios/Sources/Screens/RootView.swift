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
            // Rebuild the tab tree fresh when crossing the LTR↔RTL boundary:
            // toggling layoutDirection on a live NavigationStack otherwise leaves
            // the active tab mirrored. Scoped to the unlocked app so it doesn't
            // reset the onboarding/lock flow when the language changes.
            case .unlocked:   MainTabView().id(loc.language.isRTL)
            }
        }
        // Mirror the whole UI for right-to-left scripts (Arabic, Urdu, Pashto).
        // `loc` republishes on language change, so this updates live.
        .environment(\.layoutDirection, loc.language.isRTL ? .rightToLeft : .leftToRight)
    }
}
