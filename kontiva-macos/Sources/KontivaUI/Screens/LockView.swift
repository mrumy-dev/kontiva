import SwiftUI
import KontivaCore

/// The returning-user unlock screen: a generous wordmark, "Willkommen zurück",
/// and a 6-digit code typed on the keyboard (dots fill as you type). Wrong code →
/// red dots + shake. First-run setup goes through `OnboardingView` instead.
struct LockView: View {
    @EnvironmentObject private var model: AppModel
    @EnvironmentObject private var loc: Localizer

    @State private var pin = ""
    @State private var wrong = false
    @State private var attempts = 0
    @State private var triedBiometric = false
    private let length = 6

    private var canUseBiometrics: Bool { model.biometricEnabled && model.biometricKind.isAvailable }

    var body: some View {
        ZStack {
            LinearGradient(colors: [KontivaTheme.lockBackgroundTop, KontivaTheme.lockBackgroundBottom],
                           startPoint: .top, endPoint: .bottom)
                .ignoresSafeArea()

            VStack(spacing: KontivaTheme.Space.lg) {
                Spacer(minLength: 0)

                BrandWordmark(height: 46)

                Text(loc(.lockWelcomeBack))
                    .font(.system(size: 20, weight: .medium))
                    .foregroundStyle(KontivaTheme.textSecondary)

                PinField(pin: $pin, length: length, error: wrong, onComplete: submit)
                    .modifier(Shake(animatableData: CGFloat(attempts)))
                    .padding(.top, KontivaTheme.Space.md)

                Text(loc(.lockWrongPassphrase))
                    .font(.caption).foregroundStyle(KontivaTheme.swissRed)
                    .opacity(wrong ? 1 : 0)

                if canUseBiometrics {
                    Button { unlockWithBiometrics() } label: {
                        Label("Mit \(model.biometricKind.label) entsperren", systemImage: model.biometricKind.icon)
                            .font(.subheadline.weight(.medium))
                    }
                    .buttonStyle(.plain)
                    .foregroundStyle(KontivaTheme.accent)
                    .padding(.top, KontivaTheme.Space.xs)
                }

                Spacer(minLength: 0)

                Label("AES-256-GCM", systemImage: "lock.fill")
                    .font(.caption2).foregroundStyle(KontivaTheme.textTertiary)
            }
            .padding(KontivaTheme.Space.xxl)
            .task {
                if canUseBiometrics, !triedBiometric {
                    triedBiometric = true
                    unlockWithBiometrics()
                }
            }
        }
    }

    private func unlockWithBiometrics() {
        Task { _ = await model.unlockWithBiometrics() }
    }

    private func submit() {
        let entered = pin
        Task {
            let ok = await model.unlock(passphrase: entered)
            if ok {
                pin = ""
            } else {
                wrong = true
                withAnimation(.linear(duration: 0.4)) { attempts += 1 }
                try? await Task.sleep(for: .milliseconds(500))
                pin = ""
            }
        }
    }
}
