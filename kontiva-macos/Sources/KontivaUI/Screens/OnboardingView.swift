import SwiftUI
import KontivaCore

/// A warm, guided first-run flow shown only on the very first launch (no vault
/// yet). Three calm steps on the lock-screen gradient:
///   1. Welcome — what Kontiva is, and why it's private.
///   2. Code — choose and confirm a 6-digit unlock code (no recovery).
///   3. Profile — an optional name + avatar, stored locally.
/// The vault is created at the end of step 2, but the flow stays on screen
/// (`model.onboardingActive`) until the profile step finishes.
struct OnboardingView: View {
    @EnvironmentObject private var model: AppModel
    @EnvironmentObject private var loc: Localizer

    @State private var step = 0
    @State private var pin = ""
    @State private var firstPin = ""
    @State private var confirming = false
    @State private var pinError = false
    @State private var pinAttempts = 0
    @State private var profileName = ""
    @State private var avatarName: String? = nil
    @FocusState private var nameFocused: Bool

    private let length = 6

    var body: some View {
        ZStack {
            LinearGradient(colors: [KontivaTheme.lockBackgroundTop, KontivaTheme.lockBackgroundBottom],
                           startPoint: .top, endPoint: .bottom)
                .ignoresSafeArea()

            VStack(spacing: KontivaTheme.Space.xl) {
                Spacer(minLength: 0)

                Group {
                    switch step {
                    case 0:  welcomeStep
                    case 1:  codeStep
                    default: profileStep
                    }
                }
                .transition(.asymmetric(
                    insertion: .move(edge: .trailing).combined(with: .opacity),
                    removal: .move(edge: .leading).combined(with: .opacity)))

                stepDots

                Spacer(minLength: 0)
            }
            .frame(maxWidth: 460)
            .padding(KontivaTheme.Space.xxl)
            .animation(.snappy(duration: 0.35), value: step)
        }
    }

    // MARK: Step 1 — welcome

    private var welcomeStep: some View {
        VStack(spacing: KontivaTheme.Space.lg) {
            ZStack {
                Circle().fill(KontivaTheme.accent.opacity(0.12)).frame(width: 156, height: 156)
                BrandIcon(size: 112)
            }

            VStack(spacing: KontivaTheme.Space.xs) {
                Text(loc(.lockWelcomeSetup))
                    .font(.system(size: 30, weight: .semibold))
                    .tracking(0.3)
                    .foregroundStyle(KontivaTheme.textPrimary)
                Text(loc(.onboardingIntroBody))
                    .font(.callout)
                    .foregroundStyle(KontivaTheme.textSecondary)
                    .multilineTextAlignment(.center)
                    .fixedSize(horizontal: false, vertical: true)
            }

            VStack(alignment: .leading, spacing: KontivaTheme.Space.sm) {
                feature("lock.shield", loc(.onboardingFeaturePrivate))
                feature("key.fill", loc(.onboardingFeatureSecure))
                feature("checkmark.seal", loc(.onboardingFeatureMoney))
            }
            .padding(.vertical, KontivaTheme.Space.xs)

            Button { advance(to: 1) } label: {
                Text(loc(.onboardingStart)).fontWeight(.semibold)
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(KontivaPrimaryButtonStyle())
            .keyboardShortcut(.defaultAction)
        }
        .frame(maxWidth: 380)
    }

    private func feature(_ symbol: String, _ text: String) -> some View {
        HStack(spacing: KontivaTheme.Space.sm) {
            Image(systemName: symbol)
                .font(.system(size: 15, weight: .medium))
                .foregroundStyle(KontivaTheme.accent)
                .frame(width: 22)
            Text(text)
                .font(.subheadline)
                .foregroundStyle(KontivaTheme.textSecondary)
            Spacer(minLength: 0)
        }
    }

    // MARK: Step 2 — choose / confirm code

    private var codeStep: some View {
        VStack(spacing: KontivaTheme.Space.md) {
            stepTitle(confirming ? "Code bestätigen" : "Code festlegen")

            VStack(spacing: KontivaTheme.Space.sm) {
                Text(confirming ? "Geben Sie den Code zur Bestätigung erneut ein."
                                : "Wählen Sie einen 6-stelligen Code zum Entsperren.")
                    .font(.caption).foregroundStyle(KontivaTheme.textSecondary)
                    .multilineTextAlignment(.center)
                    .fixedSize(horizontal: false, vertical: true)

                PinField(pin: $pin, length: length, error: pinError, onComplete: codeComplete)
                    .modifier(Shake(animatableData: CGFloat(pinAttempts)))
                    .padding(.vertical, KontivaTheme.Space.xs)

                Text("Codes stimmen nicht überein.")
                    .font(.caption).foregroundStyle(KontivaTheme.swissRed)
                    .opacity(pinError ? 1 : 0)

                Label(loc(.lockRecoveryWarning), systemImage: "exclamationmark.triangle")
                    .font(.caption2).foregroundStyle(KontivaTheme.swissRed)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .panel()
        }
        .frame(maxWidth: 380)
    }

    private func codeComplete() {
        if !confirming {
            firstPin = pin
            pin = ""
            withAnimation(.snappy(duration: 0.3)) { confirming = true }
            return
        }
        if pin == firstPin {
            model.recoveryAcknowledged = true
            let code = pin
            Task {
                await model.completeSetup(passphrase: code)
                if model.lockState == .unlocked {
                    pin = ""; firstPin = ""
                    advance(to: 2)
                }
            }
        } else {
            pinError = true
            withAnimation(.linear(duration: 0.4)) { pinAttempts += 1 }
            Task {
                try? await Task.sleep(for: .milliseconds(550))
                pin = ""; firstPin = ""; pinError = false
                withAnimation(.snappy(duration: 0.3)) { confirming = false }
            }
        }
    }

    // MARK: Step 3 — profile

    private var profileStep: some View {
        VStack(spacing: KontivaTheme.Space.md) {
            VStack(spacing: KontivaTheme.Space.xs) {
                Text(loc(.onboardingProfileTitle))
                    .font(.system(size: 26, weight: .semibold))
                    .foregroundStyle(KontivaTheme.textPrimary)
                Text(loc(.onboardingProfileBody))
                    .font(.callout).foregroundStyle(KontivaTheme.textSecondary)
                    .multilineTextAlignment(.center)
                    .fixedSize(horizontal: false, vertical: true)
            }

            VStack(spacing: KontivaTheme.Space.md) {
                ProfileAvatar(name: avatarName, size: 84)
                    .shadow(color: .black.opacity(0.12), radius: 10, y: 4)

                TextField(loc(.profileName), text: $profileName)
                    .textFieldStyle(.roundedBorder)
                    .focused($nameFocused)
                    .frame(maxWidth: 260)

                avatarStrip
            }
            .panel()

            HStack(spacing: KontivaTheme.Space.md) {
                Button(loc(.onboardingSkip)) { finish(saveProfile: false) }
                    .buttonStyle(.plain)
                    .foregroundStyle(KontivaTheme.textSecondary)
                Spacer(minLength: 0)
                Button { finish(saveProfile: true) } label: {
                    HStack(spacing: KontivaTheme.Space.xs) {
                        if model.isWorking { ProgressView().controlSize(.small).tint(.white) }
                        Text(loc(.commonDone)).fontWeight(.semibold)
                    }
                    .frame(minWidth: 120)
                }
                .buttonStyle(KontivaPrimaryButtonStyle())
                .keyboardShortcut(.defaultAction)
                .disabled(model.isWorking)
            }
            .frame(maxWidth: 360)
        }
        .frame(maxWidth: 400)
    }

    private var avatarStrip: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: KontivaTheme.Space.sm) {
                ForEach(ProfileIcons.all, id: \.self) { name in
                    Button { avatarName = name } label: {
                        ProfileAvatar(name: name, size: 48)
                            .overlay(
                                RoundedRectangle(cornerRadius: 48 * 0.24, style: .continuous)
                                    .strokeBorder(avatarName == name ? KontivaTheme.accent : Color.clear,
                                                  lineWidth: 3))
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal, 2).padding(.vertical, 3)
        }
        .frame(maxWidth: 320)
    }

    private func finish(saveProfile: Bool) {
        let name = profileName.trimmingCharacters(in: .whitespacesAndNewlines)
        if saveProfile && (!name.isEmpty || avatarName != nil) {
            Task {
                await model.updateProfile(name: name, avatarName: avatarName, canton: nil)
                model.finishOnboarding()
            }
        } else {
            model.finishOnboarding()
        }
    }

    // MARK: Shared chrome

    private func stepTitle(_ text: String) -> some View {
        Text(text)
            .font(.system(size: 26, weight: .semibold))
            .foregroundStyle(KontivaTheme.textPrimary)
    }

    private var stepDots: some View {
        HStack(spacing: KontivaTheme.Space.xs) {
            ForEach(0..<3, id: \.self) { i in
                Capsule()
                    .fill(i == step ? KontivaTheme.accent : KontivaTheme.textTertiary.opacity(0.3))
                    .frame(width: i == step ? 22 : 7, height: 7)
                    .animation(.snappy, value: step)
            }
        }
    }

    private func advance(to next: Int) {
        withAnimation(.snappy(duration: 0.35)) { step = next }
    }
}

/// A soft card surface used by the onboarding steps (matches the lock panel).
private extension View {
    func panel() -> some View {
        self
            .padding(KontivaTheme.Space.lg)
            .frame(maxWidth: 360)
            .background(
                RoundedRectangle(cornerRadius: KontivaTheme.Radius.card, style: .continuous)
                    .fill(KontivaTheme.cardSurface))
            .overlay(
                RoundedRectangle(cornerRadius: KontivaTheme.Radius.card, style: .continuous)
                    .strokeBorder(KontivaTheme.softBorder.opacity(0.6), lineWidth: 1))
            .shadow(color: .black.opacity(0.12), radius: 18, y: 8)
    }
}
