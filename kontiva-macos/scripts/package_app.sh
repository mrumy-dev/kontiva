#!/usr/bin/env bash
#
# Package the SwiftPM-built Kontiva executable into a macOS .app bundle.
# This avoids needing full Xcode: it builds with `swift build` and assembles the
# bundle by hand (Info.plist + Resources + icon). Run from the repo root.
#
# Prerequisite: the macOS SDK license must be accepted once:
#   sudo xcodebuild -license accept
#
set -euo pipefail

CONFIG="${1:-release}"
APP_NAME="Kontiva"
BUNDLE_ID="ch.kontiva.app"
VERSION="0.0.1"

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

echo "▸ Building ($CONFIG)…"
swift build -c "$CONFIG"

BIN_PATH="$(swift build -c "$CONFIG" --show-bin-path)"
APP_DIR="build/${APP_NAME}.app"
MACOS_DIR="$APP_DIR/Contents/MacOS"
RES_DIR="$APP_DIR/Contents/Resources"

echo "▸ Assembling $APP_DIR…"
rm -rf "$APP_DIR"
mkdir -p "$MACOS_DIR" "$RES_DIR"

cp "$BIN_PATH/$APP_NAME" "$MACOS_DIR/$APP_NAME"

# App icon (generated from the approved master, see Resources/AppIcon).
if [[ -f "Resources/AppIcon/Kontiva.icns" ]]; then
  cp "Resources/AppIcon/Kontiva.icns" "$RES_DIR/AppIcon.icns"
fi

# SwiftPM resource bundle (localization/brand) sits next to the binary; copy it in.
if [[ -d "$BIN_PATH/Kontiva_KontivaUI.bundle" ]]; then
  cp -R "$BIN_PATH/Kontiva_KontivaUI.bundle" "$RES_DIR/"
fi

cat > "$APP_DIR/Contents/Info.plist" <<PLIST
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
  <key>CFBundleName</key><string>${APP_NAME}</string>
  <key>CFBundleDisplayName</key><string>${APP_NAME}</string>
  <key>CFBundleIdentifier</key><string>${BUNDLE_ID}</string>
  <key>CFBundleVersion</key><string>${VERSION}</string>
  <key>CFBundleShortVersionString</key><string>${VERSION}</string>
  <key>CFBundlePackageType</key><string>APPL</string>
  <key>CFBundleExecutable</key><string>${APP_NAME}</string>
  <key>CFBundleIconFile</key><string>AppIcon</string>
  <key>LSMinimumSystemVersion</key><string>14.0</string>
  <key>NSHumanReadableCopyright</key><string>Kontiva — local-first, no network.</string>
  <key>LSApplicationCategoryType</key><string>public.app-category.finance</string>
  <key>NSHighResolutionCapable</key><true/>
</dict>
</plist>
PLIST

# Ad-hoc sign with the App Sandbox entitlements. Ad-hoc signing ("-") needs no
# Apple Developer account and engages the sandbox locally (no network + only
# user-selected files). A real Developer ID signature + notarization is applied
# later by scripts/sign_and_notarize.sh once an account is available.
ENTITLEMENTS="$ROOT/Resources/Kontiva.entitlements"
if [[ -f "$ENTITLEMENTS" ]]; then
  echo "▸ Ad-hoc signing with App Sandbox entitlements…"
  codesign --force --deep --sign - \
    --entitlements "$ENTITLEMENTS" \
    "$APP_DIR"
  codesign --display --entitlements - "$APP_DIR" >/dev/null 2>&1 \
    && echo "  signed (ad-hoc, sandboxed)" || echo "  WARNING: codesign failed"
fi

echo "▸ Done: $APP_DIR"
echo "  Open with:  open \"$APP_DIR\""
echo "  Note: ad-hoc signed + sandboxed. Developer ID signing + notarization"
echo "        is a separate step (scripts/sign_and_notarize.sh) for V1."
