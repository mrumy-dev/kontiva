import SwiftUI
import KontivaCore

/// First-run flow: welcome → language → profile → create a numeric unlock code.
/// (The code is the vault secret — AES-256-GCM / PBKDF2 under the hood. Face ID can
/// be turned on afterwards in Einstellungen.)
struct OnboardingView: View {
    @EnvironmentObject private var model: AppModel
    @EnvironmentObject private var loc: Localizer

    private enum Step { case welcome, language, profile, choose, confirm }
    @State private var step: Step = .welcome
    @State private var pin = ""
    @State private var firstPin = ""
    @State private var attempts = 0
    @State private var mismatch = false
    @State private var profileName = ""
    @State private var avatarName: String? = nil
    @State private var showAvatarPicker = false

    private let length = 6

    var body: some View {
        ZStack {
            KontivaTheme.pageGradient.ignoresSafeArea()
            Group {
                switch step {
                case .welcome:  welcomeHero
                case .language: languageStep
                case .profile:  profileStep
                case .choose:   pinStep(title: loc(.pinSetTitle), subtitle: loc(.pinRequirementNote), error: false)
                case .confirm:  pinStep(title: loc(.pinConfirmTitle), subtitle: nil, error: mismatch)
                }
            }
            .transition(.asymmetric(insertion: .move(edge: .trailing).combined(with: .opacity),
                                    removal: .move(edge: .leading).combined(with: .opacity)))
        }
        .animation(.snappy(duration: 0.35), value: step)
        .sheet(isPresented: $showAvatarPicker) {
            AvatarPickerSheet(selected: $avatarName).environmentObject(loc)
        }
    }

    // MARK: Step 1 — welcome

    private var welcomeHero: some View {
        VStack(spacing: 0) {
            Spacer(minLength: KontivaTheme.Space.lg)

            WordmarkHero()

            Text(loc(.onboardingIntroBody))
                .font(.callout).foregroundStyle(KontivaTheme.textSecondary)
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)
                .padding(.top, KontivaTheme.Space.md)
                .padding(.horizontal, KontivaTheme.Space.sm)

            VStack(spacing: 0) {
                cue("lock.shield.fill", loc(.onboardingFeaturePrivate))
                Divider().background(KontivaTheme.softBorder.opacity(0.4)).padding(.leading, 58)
                cue("mountain.2.fill", loc(.onboardingFeatureSecure))
                Divider().background(KontivaTheme.softBorder.opacity(0.4)).padding(.leading, 58)
                cue("chart.pie.fill", loc(.onboardingFeatureMoney))
            }
            .background(RoundedRectangle(cornerRadius: KontivaTheme.Radius.card, style: .continuous)
                .fill(KontivaTheme.cardSurface))
            .overlay(RoundedRectangle(cornerRadius: KontivaTheme.Radius.card, style: .continuous)
                .strokeBorder(KontivaTheme.softBorder.opacity(0.5), lineWidth: 1))
            .shadow(color: KontivaTheme.charcoal.opacity(0.05), radius: 12, y: 4)
            .padding(.top, KontivaTheme.Space.xl)

            Spacer(minLength: KontivaTheme.Space.lg)

            primaryButton(loc(.onboardingStart)) { advance(to: .language) }

            securityFooter.padding(.top, KontivaTheme.Space.md)
        }
        .padding(.horizontal, KontivaTheme.Space.xl)
        .padding(.vertical, KontivaTheme.Space.lg)
    }

    private func cue(_ symbol: String, _ text: String) -> some View {
        HStack(spacing: KontivaTheme.Space.sm) {
            KontivaIconTile(symbol, size: 34)
            Text(text).font(.subheadline).foregroundStyle(KontivaTheme.textPrimary)
            Spacer(minLength: 0)
        }
        .padding(KontivaTheme.Space.md)
    }

    // MARK: Step 2 — language

    private var languageStep: some View {
        VStack(spacing: 0) {
            stepBar(back: .welcome)

            Text(loc(.settingsLanguage))
                .font(.title2.weight(.bold)).foregroundStyle(KontivaTheme.textPrimary)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.top, KontivaTheme.Space.xs)

            ScrollView {
                VStack(spacing: 0) {
                    ForEach(Array(AppLanguage.allCases.enumerated()), id: \.element) { i, lang in
                        languageRow(lang)
                        if i < AppLanguage.allCases.count - 1 {
                            Divider().padding(.leading, KontivaTheme.Space.md)
                        }
                    }
                }
                .background(RoundedRectangle(cornerRadius: KontivaTheme.Radius.card, style: .continuous)
                    .fill(KontivaTheme.cardSurface))
                .overlay(RoundedRectangle(cornerRadius: KontivaTheme.Radius.card, style: .continuous)
                    .strokeBorder(KontivaTheme.softBorder.opacity(0.5), lineWidth: 1))
                .padding(.top, KontivaTheme.Space.md)
            }
            .scrollIndicators(.hidden)

            primaryButton(loc(.commonNext)) { advance(to: .profile) }
                .padding(.top, KontivaTheme.Space.sm)
        }
        .padding(.horizontal, KontivaTheme.Space.lg)
        .padding(.bottom, KontivaTheme.Space.lg)
    }

    private func languageRow(_ lang: AppLanguage) -> some View {
        Button {
            withAnimation(.snappy) { model.setLanguage(lang) }
        } label: {
            HStack {
                Text(lang.displayName)
                    .font(.body).foregroundStyle(KontivaTheme.textPrimary)
                Spacer(minLength: 0)
                if lang == loc.language {
                    Image(systemName: "checkmark")
                        .font(.body.weight(.semibold)).foregroundStyle(KontivaTheme.accent)
                        .transition(.scale.combined(with: .opacity))
                }
            }
            .padding(.horizontal, KontivaTheme.Space.md)
            .padding(.vertical, 13)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    // MARK: Step 3 — profile

    private var profileStep: some View {
        VStack(spacing: 0) {
            stepBar(back: .language)
            Spacer(minLength: KontivaTheme.Space.sm)

            Text(loc(.onboardingProfileTitle))
                .font(.title2.weight(.bold)).foregroundStyle(KontivaTheme.textPrimary)
                .multilineTextAlignment(.center)

            Text(loc(.onboardingProfileBody))
                .font(.callout).foregroundStyle(KontivaTheme.textSecondary)
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)
                .padding(.top, KontivaTheme.Space.xs)
                .padding(.horizontal, KontivaTheme.Space.sm)

            Button { showAvatarPicker = true } label: {
                ZStack(alignment: .bottomTrailing) {
                    ProfileAvatar(name: avatarName, size: 108)
                        .shadow(color: KontivaTheme.charcoal.opacity(0.12), radius: 10, y: 5)
                    Circle().fill(KontivaTheme.accent)
                        .frame(width: 34, height: 34)
                        .overlay(Image(systemName: "pencil")
                            .font(.system(size: 14, weight: .semibold)).foregroundStyle(.white))
                        .shadow(color: .black.opacity(0.18), radius: 3, y: 1)
                }
            }
            .buttonStyle(.plain)
            .padding(.top, KontivaTheme.Space.xl)

            TextField(loc(.profileName), text: $profileName)
                .textInputAutocapitalization(.words)
                .submitLabel(.done)
                .font(.body)
                .multilineTextAlignment(.center)
                .padding(KontivaTheme.Space.md)
                .background(RoundedRectangle(cornerRadius: KontivaTheme.Radius.card, style: .continuous)
                    .fill(KontivaTheme.cardSurface))
                .overlay(RoundedRectangle(cornerRadius: KontivaTheme.Radius.card, style: .continuous)
                    .strokeBorder(KontivaTheme.softBorder.opacity(0.5), lineWidth: 1))
                .padding(.top, KontivaTheme.Space.lg)

            Spacer(minLength: KontivaTheme.Space.lg)

            primaryButton(loc(.commonNext)) { advance(to: .choose) }

            Button(loc(.onboardingSkip)) {
                profileName = ""; avatarName = nil
                advance(to: .choose)
            }
            .font(.subheadline).tint(KontivaTheme.textSecondary)
            .padding(.top, KontivaTheme.Space.sm)
        }
        .padding(.horizontal, KontivaTheme.Space.lg)
        .padding(.bottom, KontivaTheme.Space.lg)
    }

    // MARK: Steps 4 & 5 — choose / confirm code

    private func pinStep(title: String, subtitle: String?, error: Bool) -> some View {
        VStack(spacing: 0) {
            Spacer(minLength: KontivaTheme.Space.lg)
            Image("Wordmark").resizable().scaledToFit().frame(maxWidth: 180).frame(height: 40)

            Text(title)
                .font(.title3.weight(.medium)).foregroundStyle(KontivaTheme.textPrimary)
                .padding(.top, KontivaTheme.Space.md)

            if let subtitle {
                Text(subtitle).font(.caption).foregroundStyle(KontivaTheme.textTertiary)
                    .padding(.top, KontivaTheme.Space.xxs)
            }

            PinDots(count: length, filled: pin.count, error: error)
                .modifier(Shake(animatableData: CGFloat(attempts)))
                .padding(.top, KontivaTheme.Space.xl)

            Text(loc(.pinMismatch))
                .font(.caption).foregroundStyle(KontivaTheme.swissRed)
                .opacity(error ? 1 : 0)
                .padding(.top, KontivaTheme.Space.sm)

            Spacer(minLength: KontivaTheme.Space.lg)

            PinKeypad(onDigit: append, onDelete: deleteLast).disabled(model.isWorking)

            Spacer(minLength: KontivaTheme.Space.md)
            securityFooter.padding(.bottom, KontivaTheme.Space.md)
        }
        .padding(.horizontal, KontivaTheme.Space.lg)
    }

    // MARK: Shared chrome

    private func stepBar(back: Step) -> some View {
        HStack {
            Button { advance(to: back) } label: {
                Image(systemName: "chevron.left").font(.title3.weight(.semibold))
            }
            .tint(KontivaTheme.textSecondary)
            Spacer()
        }
        .padding(.top, KontivaTheme.Space.sm)
    }

    private func primaryButton(_ title: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title).fontWeight(.semibold).frame(maxWidth: .infinity)
        }
        .buttonStyle(.borderedProminent).tint(KontivaTheme.accent).controlSize(.large)
    }

    private var securityFooter: some View {
        HStack(spacing: 5) { Image(systemName: "lock.fill"); Text("AES-256-GCM") }
            .font(.caption2).foregroundStyle(KontivaTheme.textTertiary)
    }

    // MARK: Entry logic

    private func append(_ digit: Int) {
        guard pin.count < length, !model.isWorking else { return }
        mismatch = false
        pin.append(String(digit))
        if pin.count == length { complete() }
    }

    private func deleteLast() {
        guard !pin.isEmpty else { return }
        pin.removeLast()
        mismatch = false
    }

    private func complete() {
        switch step {
        case .welcome, .language, .profile:
            break
        case .choose:
            firstPin = pin
            pin = ""
            advance(to: .confirm)
        case .confirm:
            if pin == firstPin {
                Haptics.success()
                let code = pin
                let name = profileName.trimmingCharacters(in: .whitespacesAndNewlines)
                let avatar = avatarName
                Task {
                    await model.setUp(passphrase: code)             // creates the vault → unlocks
                    if !name.isEmpty || avatar != nil {             // persist the chosen profile
                        await model.updateProfile(name: name, avatarName: avatar, canton: nil)
                    }
                }
            } else {
                Haptics.error()
                mismatch = true
                withAnimation(.linear(duration: 0.4)) { attempts += 1 }
                Task {
                    try? await Task.sleep(for: .milliseconds(550))
                    pin = ""; firstPin = ""
                    advance(to: .choose)
                }
            }
        }
    }

    private func advance(to next: Step) {
        withAnimation(.snappy(duration: 0.35)) { step = next }
    }
}

/// The brand wordmark as the welcome hero: large and edge-to-edge, revealed with
/// a left-to-right wipe on appear so it "expands" across the screen.
private struct WordmarkHero: View {
    @State private var reveal: CGFloat = 0
    @State private var settle = false

    var body: some View {
        Image("Wordmark")
            .resizable()
            .scaledToFit()
            .frame(maxWidth: .infinity)
            .frame(maxHeight: 88)
            .mask(alignment: .leading) {
                GeometryReader { geo in
                    Rectangle().frame(width: max(0, geo.size.width * reveal))
                }
            }
            .scaleEffect(settle ? 1 : 0.98)
            .onAppear {
                withAnimation(.easeOut(duration: 0.85).delay(0.15)) { reveal = 1 }
                withAnimation(.spring(response: 0.6, dampingFraction: 0.7).delay(0.15)) { settle = true }
            }
            .accessibilityLabel("Kontiva")
    }
}
