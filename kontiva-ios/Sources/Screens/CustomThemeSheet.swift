import SwiftUI
import KontivaCore

/// Build-your-own theme: pick any colour(s) with the native colour picker + a style,
/// with a live preview. Mirrors the Android CustomThemeSheet.
struct CustomThemeSheet: View {
    @EnvironmentObject private var model: AppModel
    @EnvironmentObject private var loc: Localizer
    @Environment(\.dismiss) private var dismiss

    @State private var primary: Color = KontivaTheme.accent
    @State private var secondary: Color = KontivaTheme.accentSecondary
    @State private var style: ThemeStyle = .gradient

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    RoundedRectangle(cornerRadius: KontivaTheme.Radius.card, style: .continuous)
                        .fill(previewGradient(style))
                        .frame(height: 64)
                }
                Section {
                    HStack(spacing: KontivaTheme.Space.md) {
                        ForEach(ThemeStyle.allCases, id: \.self) { styleDot($0) }
                        Spacer()
                    }
                    ColorPicker(selection: $primary, supportsOpacity: false) {
                        Image(systemName: "drop.fill").foregroundStyle(primary)
                    }
                    if style == .dual {
                        ColorPicker(selection: $secondary, supportsOpacity: false) {
                            Image(systemName: "drop.fill").foregroundStyle(secondary)
                        }
                    }
                }
            }
            .navigationTitle(loc(.themeCustom))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button(loc(.commonCancel)) { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button(loc(.commonSave)) {
                        model.applyCustomTheme(primary.hexString, style: style,
                                               secondaryHex: style == .dual ? secondary.hexString : nil)
                        dismiss()
                    }.fontWeight(.semibold)
                }
            }
            .onAppear {
                primary = Color.fromHex(model.settings.customAccent) ?? KontivaTheme.accent
                secondary = Color.fromHex(model.settings.customAccentSecondary) ?? KontivaTheme.accentSecondary
                style = model.settings.themeStyle == .solid ? .gradient : model.settings.themeStyle
            }
        }
    }

    private func previewGradient(_ s: ThemeStyle) -> LinearGradient {
        let colors: [Color]
        switch s {
        case .solid:    colors = [primary, primary]
        case .gradient: colors = [primary, primary.blended(with: .white, fraction: 0.5)]
        case .dual:     colors = [primary, secondary]
        }
        return LinearGradient(colors: colors, startPoint: .leading, endPoint: .trailing)
    }

    private func styleDot(_ s: ThemeStyle) -> some View {
        Button { withAnimation(.snappy) { style = s } } label: {
            Circle()
                .fill(previewGradient(s))
                .frame(width: 34, height: 34)
                .overlay(Circle().strokeBorder(.black.opacity(0.08), lineWidth: 0.5))
                .padding(4)
                .overlay {
                    Circle().strokeBorder(KontivaTheme.accent, lineWidth: 2.5)
                        .opacity(style == s ? 1 : 0).scaleEffect(style == s ? 1 : 0.72)
                }
                .animation(.snappy(duration: 0.22), value: style)
        }
        .buttonStyle(.plain)
    }
}
