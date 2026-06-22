import SwiftUI

/// The Kontiva mark rendered as a faithful native vector, using the **exact
/// coordinates from the approved `kontiva-mark.svg` master** (512×512 space):
/// the geometric K, the top/bottom ledger balance bars, and the small Swiss-red
/// minus accent. The monochrome parts are tintable for context (charcoal on
/// light surfaces, off-white on dark); the red accent is always `swissRed`.
///
/// This is reproduction of the approved geometry, not a redesign or recolour.
public struct KontivaMark: View {
    public var foreground: Color
    public var accent: Color

    public init(foreground: Color = KontivaTheme.charcoal,
                accent: Color = KontivaTheme.swissRed) {
        self.foreground = foreground
        self.accent = accent
    }

    public var body: some View {
        Canvas { context, size in
            let scale = min(size.width, size.height) / 512.0
            func r(_ x: CGFloat, _ y: CGFloat, _ w: CGFloat, _ h: CGFloat, _ radius: CGFloat) -> Path {
                Path(roundedRect: CGRect(x: x * scale, y: y * scale,
                                         width: w * scale, height: h * scale),
                     cornerRadius: radius * scale)
            }

            // Top and bottom ledger balance bars.
            context.fill(r(116, 78, 280, 28, 9), with: .color(foreground))
            context.fill(r(116, 406, 280, 28, 9), with: .color(foreground))
            // K vertical stem.
            context.fill(r(176, 154, 38, 204, 6), with: .color(foreground))

            // K arms (exact path from the master).
            var arms = Path()
            func p(_ x: CGFloat, _ y: CGFloat) -> CGPoint { CGPoint(x: x * scale, y: y * scale) }
            arms.move(to: p(258, 256))
            arms.addLine(to: p(384, 154))
            arms.addLine(to: p(326, 154))
            arms.addLine(to: p(222, 247))
            arms.addLine(to: p(222, 265))
            arms.addLine(to: p(326, 358))
            arms.addLine(to: p(384, 358))
            arms.addLine(to: p(258, 256))
            arms.closeSubpath()
            context.fill(arms, with: .color(foreground))

            // Swiss-red minus accent.
            context.fill(r(58, 236, 68, 22, 7), with: .color(accent))
        }
        .accessibilityLabel("Kontiva")
    }
}

/// The dark app-tile version of the icon (charcoal rounded square + white mark +
/// red accent), reproduced from `kontiva-icon.svg`. Used for onboarding/about.
public struct KontivaAppTile: View {
    public var size: CGFloat
    public init(size: CGFloat = 64) { self.size = size }

    public var body: some View {
        RoundedRectangle(cornerRadius: size * (176.0 / 1024.0), style: .continuous)
            .fill(
                LinearGradient(colors: [Color(hex: 0x26313A), Color(hex: 0x070B0F)],
                               startPoint: .topLeading, endPoint: .bottomTrailing)
            )
            .overlay(
                RoundedRectangle(cornerRadius: size * (176.0 / 1024.0), style: .continuous)
                    .fill(KontivaTheme.charcoal).opacity(0.55)
            )
            .overlay(
                KontivaMark(foreground: KontivaTheme.offWhite, accent: KontivaTheme.swissRed)
                    .padding(size * 0.16)
            )
            .frame(width: size, height: size)
    }
}
