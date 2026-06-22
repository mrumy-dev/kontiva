import SwiftUI
import AppKit

/// Loads the approved Kontiva brand PNGs (copied unmodified from
/// `assets/brand/masters/`) that are bundled as module resources. Using the real
/// raster masters for the app icon / lock screen, rather than a vector stand-in.
private final class BundleToken {}

enum BrandAsset {
    /// Locate the KontivaUI resource bundle WITHOUT `Bundle.module` — that
    /// accessor `fatalError`s when it can't find the bundle, which happens for a
    /// hand-assembled `.app`. This searches the real candidate locations and
    /// returns nil on failure so callers can fall back gracefully.
    private static let resourceBundle: Bundle? = {
        let name = "Kontiva_KontivaUI.bundle"
        var bases: [URL] = []
        if let r = Bundle.main.resourceURL { bases.append(r) }
        bases.append(Bundle.main.bundleURL.appendingPathComponent("Contents/Resources"))
        bases.append(Bundle(for: BundleToken.self).bundleURL.deletingLastPathComponent())
        for base in bases {
            let candidate = base.appendingPathComponent(name)
            if FileManager.default.fileExists(atPath: candidate.path),
               let bundle = Bundle(url: candidate) {
                return bundle
            }
        }
        return nil
    }()

    static func image(_ name: String, ext: String = "png") -> Image? {
        guard let url = resourceBundle?.url(forResource: name, withExtension: ext),
              let nsImage = NSImage(contentsOf: url) else { return nil }
        return Image(nsImage: nsImage)
    }

    /// The dark app-icon tile (rounded square, white K, red accent) — the master.
    static var appIcon: Image? { image("kontiva-icon-1024") }
    /// Light-context wordmark (charcoal mark + "Kontiva" + red accent).
    static var wordmark: Image? { image("kontiva-wordmark") }
    /// Dark-context wordmark (white mark + "Kontiva" on a charcoal tile that
    /// matches the sidebar, so it blends seamlessly).
    static var wordmarkDark: Image? { image("kontiva-wordmark-dark") }
}

/// The Kontiva wordmark, automatically choosing the light or dark master to match
/// the current colour scheme (so it reads correctly on light and dark backgrounds).
/// Falls back to the vector mark + text if the bundled image can't be loaded.
struct BrandWordmark: View {
    @Environment(\.colorScheme) private var scheme
    var height: CGFloat

    private var onDark: Bool { scheme == .dark }

    var body: some View {
        Group {
            if let image = onDark ? BrandAsset.wordmarkDark : BrandAsset.wordmark {
                image.resizable().interpolation(.high).scaledToFit()
            } else {
                HStack(spacing: KontivaTheme.Space.xs) {
                    KontivaMark(foreground: onDark ? KontivaTheme.offWhite : KontivaTheme.charcoal,
                                accent: KontivaTheme.swissRed)
                        .frame(width: height, height: height)
                    Text("Kontiva").font(.system(size: height * 0.62, weight: .semibold))
                        .foregroundStyle(onDark ? KontivaTheme.offWhite : KontivaTheme.textPrimary)
                }
            }
        }
        .frame(height: height)
        .accessibilityLabel("Kontiva")
    }
}

/// The approved app icon rendered from the master PNG, with a soft shadow.
/// Falls back to the faithful vector tile if the resource can't be loaded.
struct BrandIcon: View {
    var size: CGFloat
    init(size: CGFloat = 96) { self.size = size }

    var body: some View {
        Group {
            if let icon = BrandAsset.appIcon {
                icon.resizable().interpolation(.high)
            } else {
                KontivaAppTile(size: size)
            }
        }
        .frame(width: size, height: size)
        .shadow(color: .black.opacity(0.35), radius: size * 0.12, x: 0, y: size * 0.06)
        .accessibilityLabel("Kontiva")
    }
}
