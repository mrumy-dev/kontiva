# Kontiva — Asset Usage Report

Documents exactly which approved brand assets were used, how, and what was
generated — so the brand masters stay verifiably untouched.

## Brand masters (read-only, never modified)

All masters live under `assets/brand/masters/`. None were edited, recolored,
cropped, distorted, or regenerated. Integrity was verified with `cmp` after every
copy (byte-identical).

## What was used

| Purpose | Source master | How |
|---------|---------------|-----|
| **macOS app icon (`.icns`)** | `kontiva-icon-1024.png` | Downscaled with `sips` to all required sizes, assembled with `iconutil`. Output: `Resources/AppIcon/Kontiva.icns` (kept **separate** from masters). |
| **Sidebar glyph** | `kontiva-mark.svg` (geometry) | Reproduced as a faithful native SwiftUI vector (`KontivaMark`) using the **exact coordinates** from the master, tinted to context (off-white on the dark sidebar) with the red accent preserved. No redesign. |
| **Onboarding / lock tile** | `kontiva-icon.svg` (geometry) | Reproduced as `KontivaAppTile` using the master's tile radius, gradient, and mark geometry. |
| **In-app wordmark resources** | `kontiva-wordmark.png`, `kontiva-wordmark-dark.png` | Copied **unmodified** into `Sources/KontivaUI/Resources/Brand/` for faithful display; light/dark variants chosen by appearance. |
| **README header** | `kontiva-wordmark.png` | Referenced directly from masters. |

## Generated outputs (derived only from approved masters)

- `Resources/AppIcon/Kontiva.icns` and `Resources/AppIcon/Kontiva.iconset/`
  (16²…1024², derived from `kontiva-icon-1024.png` only).
- `Sources/KontivaUI/Resources/Brand/*.png` (unmodified copies of approved PNGs).

## Rules honoured

- ✅ No redesign, recolor, stretch, crop, blur, or distortion of the logo
- ✅ Swiss-red accent kept as `#E11D2E`, used sparingly
- ✅ No canton coats of arms, flags, crests, or emblems anywhere
- ✅ No generic finance icons substituted for the brand
- ✅ No AI-generated replacement logo
- ✅ Generated app-icon outputs kept separate from the untouched masters

## Why these versions

- The **1024 PNG** master is the highest-resolution approved raster, so it is the
  correct source for downscaled icon sizes.
- The **SVG mark geometry** is reproduced natively (rather than embedding a
  bitmap) so the sidebar glyph stays crisp at every window size and appearance,
  while remaining the approved geometry verbatim.
- The **PNG wordmarks** are displayed as-is (not re-typeset) to avoid any font
  substitution of the approved wordmark.
