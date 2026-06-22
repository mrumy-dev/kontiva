import SwiftUI

/// The bundled local profile pictures (Kontiva-branded avatar tiles). Loaded by
/// file name through the same resource finder as the brand assets.
enum ProfileIcons {
    /// All selectable profile pictures, by file name (no extension).
    static let all: [String] = [
        "human-01-charcoal", "human-02-warm", "human-03-outline", "human-04-soft-square",
        "human-05-arc", "human-06-offset", "human-07-quiet", "human-08-premium",
        "household-01-couple", "household-02-family", "household-03-home", "household-04-shared",
        "monogram-01-a", "monogram-02-m", "monogram-03-r", "monogram-04-s",
    ]

    static func image(_ name: String) -> Image? { BrandAsset.image(name) }
}

/// Renders the chosen profile picture, or a neutral placeholder when none is set.
struct ProfileAvatar: View {
    let name: String?
    var size: CGFloat = 40

    var body: some View {
        Group {
            if let name, let image = ProfileIcons.image(name) {
                image.resizable().interpolation(.high)
            } else {
                ZStack {
                    RoundedRectangle(cornerRadius: size * 0.24, style: .continuous)
                        .fill(KontivaTheme.charcoal)
                    Image(systemName: "person.fill")
                        .font(.system(size: size * 0.46))
                        .foregroundStyle(KontivaTheme.offWhite.opacity(0.9))
                }
            }
        }
        .frame(width: size, height: size)
        .clipShape(RoundedRectangle(cornerRadius: size * 0.24, style: .continuous))
        .accessibilityHidden(true)
    }
}
