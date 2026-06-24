import SwiftUI
import KontivaCore

/// iOS design tokens — the same calm palette as the desktop app, resolved via
/// UIColor so colours adapt to light/dark on iOS.
/// Danger semantics (negative balances, overdue, errors) always use `swissRed`.
enum KontivaTheme {

    // Fixed brand / danger constants.
    static let swissRed = Color(hex: 0xE11D2E)
    static let charcoal = Color(hex: 0x121A22)
    static let offWhite = Color(hex: 0xF6F7F8)

    /// The current accent colour. A mutable token (like the desktop): `AppModel`
    /// sets it from the chosen `AccentTheme`, and because every screen observes the
    /// model, the whole UI re-reads this on a theme change. Only ever touched on the
    /// main thread (set via the `@MainActor` model, read in views).
    nonisolated(unsafe) static var accent = AccentTheme.swissRed.color

    // Adaptive semantic colours.
    static let pageBackground = Color.adaptive(light: 0xF8F7F4, dark: 0x0F151B)
    static let cardSurface    = Color.adaptive(light: 0xFFFFFF, dark: 0x1A222B)
    static let textPrimary    = Color.adaptive(light: 0x121A22, dark: 0xF2F4F6)
    static let textSecondary  = Color.adaptive(light: 0x5A6672, dark: 0x9BA7B3)
    static let textTertiary   = Color.adaptive(light: 0x8A95A0, dark: 0x707C88)
    static let softBorder     = Color.adaptive(light: 0xDCE1E5, dark: 0x2A333D)
    static let positive       = Color.adaptive(light: 0x1F7A4D, dark: 0x44C088)
    static let warning        = Color.adaptive(light: 0xB26A00, dark: 0xE0A042)

    /// Accent-tinted page background — a soft wash so the whole app (not just the
    /// symbols) takes on the chosen colour. Reads the live `accent`, so it re-tints
    /// when the theme changes.
    static var pageGradient: LinearGradient {
        let bg = pageBackground
        return LinearGradient(colors: [bg.blended(with: accent, fraction: 0.18),
                                       bg,
                                       bg.blended(with: accent, fraction: 0.06)],
                              startPoint: .top, endPoint: .bottom)
    }

    // Chart palette (calm, brand-aligned; red reserved for bills/overdraw).
    static let chartFixed     = Color.adaptive(light: 0x3E5C76, dark: 0x6E94B4)
    static let chartVariable  = Color.adaptive(light: 0x8AA0B0, dark: 0xAEC0CD)
    static let chartBills     = swissRed
    static let chartSavings   = Color.adaptive(light: 0x6A4C93, dark: 0x9B7FC4)
    static let chartAvailable = Color.adaptive(light: 0x1F7A4D, dark: 0x44C088)

    enum Space {
        static let xxs: CGFloat = 4
        static let xs: CGFloat = 8
        static let sm: CGFloat = 12
        static let md: CGFloat = 16
        static let lg: CGFloat = 24
        static let xl: CGFloat = 32
        static let xxl: CGFloat = 48
    }

    enum Radius {
        static let card: CGFloat = 16
        static let control: CGFloat = 10
        static let tile: CGFloat = 14
    }
}

extension Color {
    init(hex: UInt32) {
        self.init(.sRGB,
                  red: Double((hex >> 16) & 0xFF) / 255,
                  green: Double((hex >> 8) & 0xFF) / 255,
                  blue: Double(hex & 0xFF) / 255,
                  opacity: 1)
    }

    /// A colour that resolves to `light` or `dark` based on the iOS trait collection.
    static func adaptive(light: UInt32, dark: UInt32) -> Color {
        Color(UIColor { traits in
            UIColor(hex: traits.userInterfaceStyle == .dark ? dark : light)
        })
    }

    /// An opaque blend of this colour toward `other` by `fraction` (0–1), resolved
    /// per trait collection so it stays correct in light and dark.
    func blended(with other: Color, fraction: Double) -> Color {
        let f = CGFloat(max(0, min(1, fraction)))
        return Color(UIColor { traits in
            let a = UIColor(self).resolvedColor(with: traits)
            let b = UIColor(other).resolvedColor(with: traits)
            var ar: CGFloat = 0, ag: CGFloat = 0, ab: CGFloat = 0, aa: CGFloat = 0
            var br: CGFloat = 0, bg: CGFloat = 0, bb: CGFloat = 0, ba: CGFloat = 0
            a.getRed(&ar, green: &ag, blue: &ab, alpha: &aa)
            b.getRed(&br, green: &bg, blue: &bb, alpha: &ba)
            return UIColor(red: ar + (br - ar) * f, green: ag + (bg - ag) * f,
                           blue: ab + (bb - ab) * f, alpha: 1)
        })
    }
}

private extension UIColor {
    convenience init(hex: UInt32) {
        self.init(red: CGFloat((hex >> 16) & 0xFF) / 255,
                  green: CGFloat((hex >> 8) & 0xFF) / 255,
                  blue: CGFloat(hex & 0xFF) / 255,
                  alpha: 1)
    }
}

extension AccentTheme {
    /// The adaptive accent colour for this theme, tuned for both light and dark.
    var color: Color {
        switch self {
        case .swissRed: return .adaptive(light: 0xE11D2E, dark: 0xF24A57)
        case .orange:   return .adaptive(light: 0xE2622A, dark: 0xF2894E)
        case .sand:     return .adaptive(light: 0xA87A3D, dark: 0xCBA06A)
        case .green:    return .adaptive(light: 0x2E8B57, dark: 0x53C485)
        case .teal:     return .adaptive(light: 0x0E8C8C, dark: 0x3FBEBE)
        case .blue:     return .adaptive(light: 0x2563EB, dark: 0x6098F0)
        case .purple:   return .adaptive(light: 0x7E3AA8, dark: 0xB07AD6)
        case .pink:     return .adaptive(light: 0xD6337F, dark: 0xF06CB0)
        }
    }
}
