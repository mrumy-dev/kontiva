import SwiftUI
import AppKit
import KontivaCore

/// Kontiva design tokens. Colours come from the approved brand brief. Semantic
/// colours are **adaptive** — they resolve to a light or dark value depending on
/// the system appearance, so the app follows light/dark mode fluidly. The Swiss-
/// red accent is used sparingly, for recognition, never to dominate.
public enum KontivaTheme {

    // MARK: Fixed brand constants (identical in light & dark — used for the
    // charcoal sidebar / lock surfaces and danger semantics).
    public static let charcoal   = Color(hex: 0x121A22)
    public static let offWhite   = Color(hex: 0xF6F7F8)
    public static let warmLight  = Color(hex: 0xF8F7F4)
    /// Swiss red — the brand/logo colour AND the danger semantic (negative
    /// balances, overdue bills, error text). Always red, regardless of theme.
    public static let swissRed   = Color(hex: 0xE11D2E)

    // MARK: Themeable accent
    /// The current accent colour, driven by the user's chosen `AccentTheme`
    /// (applied by `AppModel` at launch and whenever it changes). Used for
    /// interactive / brand-forward UI; never for danger semantics. Default: red.
    nonisolated(unsafe) public static var accent: Color = AccentTheme.swissRed.color

    // MARK: Adaptive semantic colours (light / dark)
    public static let pageBackground = Color.adaptive(light: 0xF8F7F4, dark: 0x0F151B)

    /// A whisper of depth behind the content (lighter at the top).
    public static var pageGradient: LinearGradient {
        LinearGradient(colors: [Color.adaptive(light: 0xFCFBF8, dark: 0x141C24),
                                Color.adaptive(light: 0xF4F3EF, dark: 0x0D131A)],
                       startPoint: .top, endPoint: .bottom)
    }
    public static let cardSurface    = Color.adaptive(light: 0xFFFFFF, dark: 0x1A222B)
    public static let textPrimary    = Color.adaptive(light: 0x121A22, dark: 0xF2F4F6)
    public static let textSecondary  = Color.adaptive(light: 0x5A6672, dark: 0x9BA7B3)
    public static let textTertiary   = Color.adaptive(light: 0x8A95A0, dark: 0x707C88)
    public static let softBorder     = Color.adaptive(light: 0xDCE1E5, dark: 0x2A333D)
    public static let positive       = Color.adaptive(light: 0x1F7A4D, dark: 0x44C088)
    public static let warning        = Color.adaptive(light: 0xB26A00, dark: 0xE0A042)

    /// Soft welcoming gradient behind the lock screen, adapting to appearance.
    public static let lockBackgroundTop    = Color.adaptive(light: 0xFCFBF8, dark: 0x1B2530)
    public static let lockBackgroundBottom = Color.adaptive(light: 0xEFEDE7, dark: 0x0B1014)

    // Chart palette (calm, brand-aligned; red reserved for bills/overdraw).
    public static let chartFixed     = Color.adaptive(light: 0x3E5C76, dark: 0x6E94B4)
    public static let chartVariable  = Color.adaptive(light: 0x8AA0B0, dark: 0xAEC0CD)
    public static let chartBills     = swissRed
    public static let chartSavings   = Color.adaptive(light: 0x6A4C93, dark: 0x9B7FC4)
    public static let chartAvailable = Color.adaptive(light: 0x1F7A4D, dark: 0x44C088)
    /// Cyclic palette for category breakdown charts.
    public static let chartCategories: [Color] = [
        Color.adaptive(light: 0x3E5C76, dark: 0x6E94B4),
        Color.adaptive(light: 0x1F7A4D, dark: 0x44C088),
        Color.adaptive(light: 0xB26A00, dark: 0xE0A042),
        Color.adaptive(light: 0x6A4C93, dark: 0x9B7FC4),
        Color.adaptive(light: 0x2A8C9E, dark: 0x4FB8CC),
        Color.adaptive(light: 0x8AA0B0, dark: 0xAEC0CD),
        Color.adaptive(light: 0xC23B5A, dark: 0xE06B86),
    ]

    // MARK: Spacing scale
    public enum Space {
        public static let xxs: CGFloat = 4
        public static let xs: CGFloat = 8
        public static let sm: CGFloat = 12
        public static let md: CGFloat = 16
        public static let lg: CGFloat = 24
        public static let xl: CGFloat = 32
        public static let xxl: CGFloat = 48
    }

    // MARK: Radii
    public enum Radius {
        public static let card: CGFloat = 14
        public static let control: CGFloat = 8
        public static let tile: CGFloat = 14
    }

    public static let cardCornerRadius = Radius.card
}

public extension Color {
    init(hex: UInt32) {
        let r = Double((hex >> 16) & 0xFF) / 255
        let g = Double((hex >> 8) & 0xFF) / 255
        let b = Double(hex & 0xFF) / 255
        self.init(.sRGB, red: r, green: g, blue: b, opacity: 1)
    }

    /// A colour that resolves to `light` or `dark` based on the current appearance.
    static func adaptive(light: UInt32, dark: UInt32) -> Color {
        Color(nsColor: NSColor(name: nil) { appearance in
            let isDark = appearance.bestMatch(from: [.aqua, .darkAqua]) == .darkAqua
            return NSColor(hex: isDark ? dark : light)
        })
    }
}

private extension NSColor {
    convenience init(hex: UInt32) {
        self.init(srgbRed: CGFloat((hex >> 16) & 0xFF) / 255,
                  green: CGFloat((hex >> 8) & 0xFF) / 255,
                  blue: CGFloat(hex & 0xFF) / 255,
                  alpha: 1)
    }
}

public extension AccentTheme {
    /// The adaptive accent colour for this theme. Light/dark variants are tuned so
    /// the accent keeps good contrast on both white cards and dark surfaces.
    var color: Color {
        switch self {
        case .swissRed: return Color.adaptive(light: 0xE11D2E, dark: 0xF24A57)
        case .orange:   return Color.adaptive(light: 0xE2622A, dark: 0xF2894E)
        case .sand:     return Color.adaptive(light: 0xA87A3D, dark: 0xCBA06A)
        case .green:    return Color.adaptive(light: 0x2E8B57, dark: 0x53C485)
        case .teal:     return Color.adaptive(light: 0x0E8C8C, dark: 0x3FBEBE)
        case .blue:     return Color.adaptive(light: 0x2563EB, dark: 0x6098F0)
        case .purple:   return Color.adaptive(light: 0x7E3AA8, dark: 0xB07AD6)
        case .pink:     return Color.adaptive(light: 0xD6337F, dark: 0xF06CB0)
        }
    }
}
