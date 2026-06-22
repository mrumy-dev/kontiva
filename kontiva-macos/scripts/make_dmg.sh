#!/usr/bin/env bash
#
# Build a distributable Kontiva.dmg from build/Kontiva.app.
# For a public release, sign + notarize first (scripts/sign_and_notarize.sh) so
# the disk image's app passes Gatekeeper on other Macs.
#
# Uses `create-dmg` if installed (brew install create-dmg) for a polished layout,
# otherwise falls back to plain `hdiutil`.
#
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

APP="build/Kontiva.app"
DMG="build/Kontiva.dmg"

[[ -d "$APP" ]] || { echo "Build the app first: ./scripts/package_app.sh release"; exit 1; }
rm -f "$DMG"

if command -v create-dmg >/dev/null 2>&1; then
  echo "▸ Building DMG with create-dmg…"
  create-dmg \
    --volname "Kontiva" \
    --window-size 540 380 \
    --icon-size 120 \
    --icon "Kontiva.app" 150 200 \
    --app-drop-link 390 200 \
    "$DMG" "$APP"
else
  echo "▸ create-dmg not found — using hdiutil (basic layout)."
  STAGE="$(mktemp -d)"
  cp -R "$APP" "$STAGE/"
  ln -s /Applications "$STAGE/Applications"
  hdiutil create -volname "Kontiva" -srcfolder "$STAGE" -ov -format UDZO "$DMG"
  rm -rf "$STAGE"
fi

echo "✓ Created $DMG"
