#!/usr/bin/env bash
#
# Developer ID signing + notarization for distribution. REQUIRES a paid Apple
# Developer account — this is the script to run once that account is active.
# Until then, scripts/package_app.sh ad-hoc signs the sandboxed app for local use.
#
# One-time setup (after your account is active):
#   1. Install your "Developer ID Application" certificate into the login keychain
#      (Xcode ▸ Settings ▸ Accounts ▸ Manage Certificates, or developer.apple.com).
#   2. Create a notarytool keychain profile with an app-specific password:
#        xcrun notarytool store-credentials kontiva-notary \
#          --apple-id "you@example.com" --team-id "TEAMID" --password "app-specific-pw"
#
# Usage:
#   DEVELOPER_ID="Developer ID Application: Your Name (TEAMID)" ./scripts/sign_and_notarize.sh
#
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

APP="build/Kontiva.app"
ENTITLEMENTS="Resources/Kontiva.entitlements"
NOTARY_PROFILE="${NOTARY_PROFILE:-kontiva-notary}"
: "${DEVELOPER_ID:?Set DEVELOPER_ID, e.g. 'Developer ID Application: Your Name (TEAMID)'}"

echo "▸ Building + bundling…"
./scripts/package_app.sh release

echo "▸ Signing with Developer ID + hardened runtime + entitlements…"
codesign --force --deep --options runtime --timestamp \
  --entitlements "$ENTITLEMENTS" \
  --sign "$DEVELOPER_ID" \
  "$APP"
codesign --verify --strict --verbose=2 "$APP"

echo "▸ Notarizing (this uploads the app to Apple — the only network step, done by you)…"
ZIP="build/Kontiva-notarize.zip"
rm -f "$ZIP"
ditto -c -k --keepParent "$APP" "$ZIP"
xcrun notarytool submit "$ZIP" --keychain-profile "$NOTARY_PROFILE" --wait
rm -f "$ZIP"

echo "▸ Stapling the notarization ticket…"
xcrun stapler staple "$APP"
spctl --assess --type execute --verbose=4 "$APP" || true

echo "✓ Signed, notarized, stapled: $APP"
echo "  Next: ./scripts/make_dmg.sh to build a distributable disk image."
