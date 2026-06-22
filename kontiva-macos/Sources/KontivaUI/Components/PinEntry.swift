import SwiftUI

/// A horizontal shake, used for wrong-PIN feedback (animate `animatableData`).
struct Shake: GeometryEffect {
    var animatableData: CGFloat
    var travel: CGFloat = 9
    var shakes: CGFloat = 3
    func effectValue(size: CGSize) -> ProjectionTransform {
        ProjectionTransform(CGAffineTransform(translationX: travel * sin(animatableData * .pi * shakes), y: 0))
    }
}

/// The filled/empty PIN indicator dots.
struct PinDots: View {
    let count: Int
    let filled: Int
    var error: Bool = false

    var body: some View {
        HStack(spacing: 20) {
            ForEach(0..<count, id: \.self) { i in
                Circle()
                    .fill(i < filled ? (error ? KontivaTheme.swissRed : KontivaTheme.accent) : .clear)
                    .frame(width: 14, height: 14)
                    .overlay(
                        Circle().strokeBorder(
                            error ? KontivaTheme.swissRed
                                  : (i < filled ? .clear : KontivaTheme.textTertiary.opacity(0.45)),
                            lineWidth: 1.5))
                    .animation(.snappy(duration: 0.18), value: filled)
            }
        }
    }
}

/// Keyboard-driven PIN entry for macOS: dots that fill as the user types digits.
/// Focusable and auto-focusing; backspace deletes; `onComplete` fires at `length`.
struct PinField: View {
    @Binding var pin: String
    var length: Int = 6
    var error: Bool = false
    var onComplete: () -> Void
    @FocusState private var focused: Bool

    var body: some View {
        PinDots(count: length, filled: pin.count, error: error)
            .padding(.vertical, KontivaTheme.Space.sm)
            .padding(.horizontal, KontivaTheme.Space.xl)
            .contentShape(Rectangle())
            .focusable()
            .focusEffectDisabled()
            .focused($focused)
            .onAppear { focused = true }
            .onTapGesture { focused = true }
            .onKeyPress(action: handle)
    }

    private func handle(_ press: KeyPress) -> KeyPress.Result {
        if let ch = press.characters.first, ch.isNumber, pin.count < length {
            pin.append(ch)
            if pin.count == length { onComplete() }
            return .handled
        }
        if press.key == .delete, !pin.isEmpty {
            pin.removeLast()
            return .handled
        }
        return .ignored
    }
}
