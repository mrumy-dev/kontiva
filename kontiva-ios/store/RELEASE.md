# Kontiva — TestFlight launch runbook

Everything needed to ship the first **TestFlight beta** the moment the Apple Developer
membership goes active. Do the phases in order. Most can't start until the account is
active (it assigns the **Team ID** that signing and App Store Connect need).

Project facts this assumes:
- Xcode project is **generated** from `project.yml` by XcodeGen — never hand-edit
  `Kontiva.xcodeproj`; edit `project.yml` then run `xcodegen generate`.
- Bundle ID `ch.kontiva.app` · scheme `Kontiva` · deployment target iOS 26.
- Encryption already declared exempt (`ITSAppUsesNonExemptEncryption=NO`) and a
  privacy manifest (`PrivacyInfo.xcprivacy`) is bundled — no extra prompts expected.
- Repo is **public**, so don't commit anything secret. (A Team ID is *not* secret —
  it ships inside every app — so committing `DEVELOPMENT_TEAM` is fine.)

---

## Phase 0 — When the membership activates

1. Confirm you got the "Welcome to the Apple Developer Program" email.
2. Find your **Team ID**: <https://developer.apple.com/account> → Membership details
   → "Team ID" (10 chars, e.g. `AB12CD34EF`). You'll paste it in Phase 1.
3. Sign in to <https://appstoreconnect.apple.com> at least once to accept any
   latest agreements (Business → Agreements, Tax, and Banking — accept the free
   "Paid Apps"/"Free Apps" agreement; required before a build can go to testers).

---

## Phase 1 — Real code signing

1. In `kontiva-ios/project.yml`, set the team and switch to automatic signing:
   ```yaml
   # under targets > Kontiva > settings > base
   DEVELOPMENT_TEAM: "YOURTEAMID"
   CODE_SIGN_STYLE: Automatic
   # delete or stop relying on: CODE_SIGN_IDENTITY: "-"
   ```
   (Automatic signing + `-allowProvisioningUpdates` lets Xcode create the App ID,
   certificate, and provisioning profile for you. Keep `CODE_SIGN_ENTITLEMENTS:
   Kontiva.entitlements` — Face ID unlock needs the Keychain entitlement.)
2. Regenerate and open:
   ```bash
   cd ~/kontiva/kontiva-ios
   /opt/homebrew/bin/xcodegen generate
   open Kontiva.xcodeproj
   ```
3. In Xcode: select the **Kontiva** target → **Signing & Capabilities** → tick
   "Automatically manage signing" and pick your Team. The status should go green.

---

## Phase 2 — App Store Connect record

1. <https://appstoreconnect.apple.com> → **Apps** → ➕ → **New App**.
   - Platform: iOS · Name: **Kontiva** (reserves the name) · Primary language:
     German (Switzerland) or English · Bundle ID: `ch.kontiva.app` · SKU: `kontiva-ios`.
2. If the bundle ID isn't listed, register it first at
   <https://developer.apple.com/account/resources/identifiers> (or just let Xcode's
   automatic signing create it in Phase 3, then refresh the New-App dialog).

---

## Phase 3 — Archive & upload the first build

**Easiest (Xcode GUI):**
1. Top device selector → **Any iOS Device (arm64)** (not a simulator).
2. **Product → Archive**. When it finishes, the Organizer opens.
3. **Distribute App → TestFlight & App Store → Upload**. Accept the automatic
   signing prompts. Wait for "Upload successful".

**CLI alternative** (after Phase 1):
```bash
cd ~/kontiva/kontiva-ios
xcodebuild -project Kontiva.xcodeproj -scheme Kontiva \
  -destination 'generic/platform=iOS' \
  -archivePath build/Kontiva.xcarchive archive -allowProvisioningUpdates
# then export + upload (Transporter app is the simplest uploader for a first release)
```

> Build numbers must increase on every upload. Bump `CURRENT_PROJECT_VERSION` in
> `project.yml` (1 → 2 → …) and `xcodegen generate` before each new archive.
> `MARKETING_VERSION` (0.0.1) only needs bumping for user-visible version changes.

---

## Phase 4 — TestFlight

1. App Store Connect → your app → **TestFlight**. The build appears after ~5–15 min
   of processing.
2. **Internal testing** (you + up to 100 of your own team, no review): add yourself
   to an internal group, install via the TestFlight app. Smoke-test on a real device.
3. **External testing** (friends / public, needs a one-time **Beta App Review**):
   - Create an external group, add testers by email or enable a public link.
   - Fill **Test Information**: Beta App Description, feedback email
     (`mohamed.rumy@bluewin.ch`), and the "What to test" notes — all drafted in
     [`app-store-listing.md`](./app-store-listing.md) → TestFlight section.
   - Submit for Beta App Review (usually < 24h). Export compliance should auto-pass
     thanks to the encryption declaration.

---

## Phase 5 — Metadata, privacy & screenshots (can prep before the build lands)

- **App Privacy**: answer **"Data Not Collected"** (backed by `PrivacyInfo.xcprivacy`).
- **Privacy policy URL**: required — host `privacy-policy.md` (see below) and paste the URL.
- **Listing text**: name/subtitle/keywords/description for EN + de-CH are in
  [`app-store-listing.md`](./app-store-listing.md).
- **Screenshots**: the 6.9" set is in [`./screenshots/`](./screenshots/) — drag into
  the 6.9" slot. (6.5" set optional.)
- **Support URL / email**: `mailto:mohamed.rumy@bluewin.ch`.

### Host the privacy policy — done ✓
Served via GitHub Pages from `docs/` on `main`:
- **Privacy policy:** <https://mrumy-dev.github.io/kontiva/privacy.html>
- Landing/support page: <https://mrumy-dev.github.io/kontiva/>

The source HTML lives in `docs/privacy.html` (and `docs/index.html`); `docs/.nojekyll`
makes Pages serve the files as-is. Paste the privacy URL into App Store Connect → App
Privacy → Privacy Policy URL. If you ever change `kontiva-ios/store/privacy-policy.md`,
update `docs/privacy.html` to match.

---

## Quick gotchas
- Don't edit `Kontiva.xcodeproj` by hand — regenerate from `project.yml`.
- Each upload needs a higher build number.
- Accept the App Store Connect agreements (Phase 0.3) or builds won't reach testers.
- Keep `DEVELOPMENT_TEAM` set but never commit certificates or `.p12`/`.mobileprovision`
  files to this public repo.
