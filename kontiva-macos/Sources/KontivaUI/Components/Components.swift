import SwiftUI
import AppKit
import KontivaCore

/// A calm, bordered surface used for grouping content. Consistent radius, padding,
/// and a soft border per the brand palette.
public struct KontivaCard<Content: View>: View {
    private let content: Content
    public init(@ViewBuilder content: () -> Content) { self.content = content() }

    public var body: some View {
        content
            .padding(KontivaTheme.Space.lg)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                RoundedRectangle(cornerRadius: KontivaTheme.Radius.card, style: .continuous)
                    .fill(KontivaTheme.cardSurface)
            )
            .overlay(
                RoundedRectangle(cornerRadius: KontivaTheme.Radius.card, style: .continuous)
                    .strokeBorder(KontivaTheme.softBorder.opacity(0.5), lineWidth: 1)
            )
            .shadow(color: KontivaTheme.charcoal.opacity(0.04), radius: 14, x: 0, y: 5)
    }
}


/// A small labelled section title inside cards.
public struct CardTitle: View {
    let text: String
    public init(_ text: String) { self.text = text }
    public var body: some View {
        Text(text.uppercased())
            .font(.system(size: 11, weight: .semibold))
            .tracking(0.6)
            .foregroundStyle(KontivaTheme.textTertiary)
    }
}

/// A label/value money row, with optional emphasis and sign-aware colour.
public struct MoneyRow: View {
    let label: String
    let amount: Money
    var emphasised: Bool = false
    var subtractive: Bool = false

    public init(label: String, amount: Money, emphasised: Bool = false, subtractive: Bool = false) {
        self.label = label
        self.amount = amount
        self.emphasised = emphasised
        self.subtractive = subtractive
    }

    public var body: some View {
        HStack(alignment: .firstTextBaseline) {
            Text(subtractive ? "− \(label)" : label)
                .font(emphasised ? .system(size: 15, weight: .semibold) : .system(size: 14))
                .foregroundStyle(emphasised ? KontivaTheme.textPrimary : KontivaTheme.textSecondary)
            Spacer(minLength: KontivaTheme.Space.md)
            Text(amount.formattedCHF())
                .font(.system(size: emphasised ? 16 : 14, weight: emphasised ? .semibold : .regular))
                .monospacedDigit()
                .contentTransition(.numericText())
                .foregroundStyle(amount.isNegative ? KontivaTheme.swissRed : KontivaTheme.textPrimary)
        }
    }
}

/// A compact metric tile for the dashboard grid: an optional accent icon tile, a
/// label, and a big value.
public struct MetricTile: View {
    let title: String
    let value: String
    var icon: String? = nil
    var iconColor: Color = KontivaTheme.accent
    var valueColor: Color = KontivaTheme.textPrimary
    var caption: String? = nil

    public init(title: String, value: String, icon: String? = nil,
                iconColor: Color = KontivaTheme.accent,
                valueColor: Color = KontivaTheme.textPrimary, caption: String? = nil) {
        self.title = title
        self.value = value
        self.icon = icon
        self.iconColor = iconColor
        self.valueColor = valueColor
        self.caption = caption
    }

    public var body: some View {
        KontivaCard {
            VStack(alignment: .leading, spacing: KontivaTheme.Space.sm) {
                HStack(spacing: KontivaTheme.Space.sm) {
                    if let icon { KontivaIconTile(icon, color: iconColor, size: 30) }
                    CardTitle(title)
                    Spacer(minLength: 0)
                }
                Text(value)
                    .font(.system(size: 23, weight: .semibold))
                    .monospacedDigit()
                    .contentTransition(.numericText())
                    .foregroundStyle(valueColor)
                    .lineLimit(1)
                    .minimumScaleFactor(0.6)
                if let caption {
                    Text(caption)
                        .font(.caption)
                        .foregroundStyle(KontivaTheme.textTertiary)
                        .fixedSize(horizontal: false, vertical: true)
                }
            }
        }
    }
}

/// A guiding empty state, optionally with a primary call-to-action.
public struct EmptyState: View {
    let systemImage: String
    let title: String
    let message: String
    var actionTitle: String? = nil
    var action: (() -> Void)? = nil

    public init(systemImage: String, title: String, message: String,
                actionTitle: String? = nil, action: (() -> Void)? = nil) {
        self.systemImage = systemImage
        self.title = title
        self.message = message
        self.actionTitle = actionTitle
        self.action = action
    }

    public var body: some View {
        VStack(spacing: KontivaTheme.Space.sm) {
            Image(systemName: systemImage)
                .font(.system(size: 40, weight: .light))
                .foregroundStyle(KontivaTheme.accent.opacity(0.85))
                .symbolEffect(.pulse, options: .repeating.speed(0.4))
                .padding(.bottom, KontivaTheme.Space.xxs)
            Text(title)
                .font(.title3.weight(.semibold))
                .foregroundStyle(KontivaTheme.textPrimary)
            Text(message)
                .font(.callout)
                .foregroundStyle(KontivaTheme.textSecondary)
                .multilineTextAlignment(.center)
                .frame(maxWidth: 420)
                .fixedSize(horizontal: false, vertical: true)
            if let actionTitle, let action {
                Button(action: action) {
                    Label(actionTitle, systemImage: "plus")
                }
                .buttonStyle(.borderedProminent)
                .tint(KontivaTheme.accent)
                .controlSize(.large)
                .padding(.top, KontivaTheme.Space.xs)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, KontivaTheme.Space.xxl)
    }
}

/// A circular progress ring with the percentage in the centre.
public struct ProgressRing: View {
    let progress: Double            // 0…1 (clamped)
    var size: CGFloat = 46
    var lineWidth: CGFloat = 5
    var color: Color = KontivaTheme.accent

    public init(progress: Double, size: CGFloat = 46, lineWidth: CGFloat = 5,
                color: Color = KontivaTheme.accent) {
        self.progress = progress; self.size = size; self.lineWidth = lineWidth; self.color = color
    }

    public var body: some View {
        let clamped = min(max(progress, 0), 1)
        ZStack {
            Circle().stroke(color.opacity(0.15), lineWidth: lineWidth)
            Circle()
                .trim(from: 0, to: clamped)
                .stroke(color, style: StrokeStyle(lineWidth: lineWidth, lineCap: .round))
                .rotationEffect(.degrees(-90))
                .animation(.snappy, value: clamped)
            Text("\(Int((clamped * 100).rounded()))%")
                .font(.system(size: size * 0.26, weight: .semibold)).monospacedDigit()
                .foregroundStyle(KontivaTheme.textSecondary)
        }
        .frame(width: size, height: size)
    }
}

/// A rounded, tinted icon tile — the consistent glyph used in every section/group
/// card header and list row across Monatsplanung, Sparen, and Rechnungen.
public struct KontivaIconTile: View {
    let systemImage: String
    var color: Color
    var size: CGFloat

    public init(_ systemImage: String, color: Color = KontivaTheme.accent, size: CGFloat = 38) {
        self.systemImage = systemImage
        self.color = color
        self.size = size
    }

    public var body: some View {
        RoundedRectangle(cornerRadius: size * 0.26, style: .continuous)
            .fill(color.opacity(0.12))
            .frame(width: size, height: size)
            .overlay(
                Image(systemName: systemImage)
                    .font(.system(size: size * 0.45, weight: .semibold))
                    .foregroundStyle(color)
            )
    }
}

/// A small count pill shown next to a card's title (number of entries in the group).
public struct CountBadge: View {
    let count: Int
    var color: Color

    public init(_ count: Int, color: Color = KontivaTheme.accent) {
        self.count = count
        self.color = color
    }

    public var body: some View {
        Text("\(count)")
            .font(.caption.weight(.semibold))
            .monospacedDigit()
            .foregroundStyle(color)
            .padding(.horizontal, 7)
            .padding(.vertical, 2)
            .background(color.opacity(0.12), in: Capsule())
    }
}

/// A labelled money statistic used in the summary cards (label above, value below).
public struct SummaryStat: View {
    let title: String
    let value: String
    var color: Color

    public init(_ title: String, value: String, color: Color = KontivaTheme.textPrimary) {
        self.title = title
        self.value = value
        self.color = color
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: KontivaTheme.Space.xxs) {
            Text(title).font(.caption).foregroundStyle(KontivaTheme.textTertiary).lineLimit(1)
            Text(value)
                .font(.headline).monospacedDigit()
                .contentTransition(.numericText())
                .foregroundStyle(color)
                .lineLimit(1).minimumScaleFactor(0.7)
        }
    }
}

/// A status pill (e.g. bill state).
public struct StatusPill: View {
    let text: String
    let color: Color
    public init(text: String, color: Color) { self.text = text; self.color = color }
    public var body: some View {
        Text(text)
            .font(.system(size: 11, weight: .semibold))
            .padding(.horizontal, KontivaTheme.Space.xs)
            .padding(.vertical, 3)
            .background(color.opacity(0.12), in: Capsule())
            .foregroundStyle(color)
    }
}

/// Primary (prominent red) button style.
///
/// Implemented as a SwiftUI `ButtonStyle` rather than `.borderedProminent` on
/// purpose: the AppKit-backed prominent style swallows the first mouse click when
/// a text field is being edited (the click resigns the field instead of firing
/// the button). A custom style uses SwiftUI's own gesture handling, so the button
/// fires on the same click that commits the field.
public struct KontivaPrimaryButtonStyle: ButtonStyle {
    @Environment(\.isEnabled) private var isEnabled
    public init() {}

    public func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .padding(.vertical, 8)
            .padding(.horizontal, KontivaTheme.Space.sm)
            .background(
                RoundedRectangle(cornerRadius: KontivaTheme.Radius.control, style: .continuous)
                    .fill(KontivaTheme.accent.opacity(
                        isEnabled ? (configuration.isPressed ? 0.82 : 1.0) : 0.4))
            )
            .foregroundStyle(.white)
            .contentShape(Rectangle())
            .animation(.easeOut(duration: 0.12), value: configuration.isPressed)
    }
}

/// Standard scrolling container for a screen's content with consistent padding.
public struct ScreenScroll<Content: View>: View {
    private let content: Content
    public init(@ViewBuilder content: () -> Content) { self.content = content() }
    public var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: KontivaTheme.Space.lg) {
                content
            }
            .padding(KontivaTheme.Space.xl)
            // Fill the window up to a comfortable reading width (so small/windowed
            // and full-screen both look right), then centre the column on very wide
            // displays instead of pinning it to the left.
            .frame(maxWidth: 1600, alignment: .leading)
            .frame(maxWidth: .infinity, alignment: .center)
        }
    }
}
