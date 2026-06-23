# Kontiva — Google Play release runbook

Everything to take the Android app from build → closed beta → production. Mirrors
the iOS `kontiva-ios/store/RELEASE.md`, adapted for Play.

App facts: package `ch.kontiva.android` · minSdk 26 · target/compileSdk 35 ·
versionName `0.0.1` / versionCode `1` (bump `versionCode` on every upload).

## Testing tracks (the TestFlight equivalents)
- **Internal testing** — up to 100 testers, live in minutes, no review. Use first.
- **Closed testing** (alpha) — email lists / Google Groups.
- **Open testing** (beta) — public opt-in link.
- **Internal app sharing** — one-off build links.

> ⚠️ New **personal** developer accounts must run a **closed test with ≥ 20 testers
> for 14 continuous days** before they can apply for production. Plan ~2 weeks of
> closed testing. Organisation accounts are usually exempt. (Play registration is a
> one-time **$25**.)

## Phase 0 — account
1. Create a Play Console developer account (<https://play.google.com/console>), pay $25.
2. Complete identity verification (Google may take a few days).

## Phase 1 — release signing
Use **Play App Signing** (Google holds the app signing key; you hold an upload key).
1. Create an upload keystore (do this yourself — never commit it):
   ```bash
   keytool -genkey -v -keystore kontiva-upload.jks -keyalg RSA -keysize 2048 \
     -validity 9125 -alias kontiva
   ```
2. Put a `keystore.properties` at `kontiva-android/` (gitignored):
   ```properties
   storeFile=/absolute/path/kontiva-upload.jks
   storePassword=•••
   keyAlias=kontiva
   keyPassword=•••
   ```
   The Gradle build picks this up automatically (see `app/build.gradle.kts`).

## Phase 2 — build the App Bundle
Play takes an **.aab**, not an APK:
```bash
cd kontiva-android
JAVA_HOME="$(brew --prefix openjdk@21)/libexec/openjdk.jdk/Contents/Home" \
  ./gradlew :app:bundleRelease
# → app/build/outputs/bundle/release/app-release.aab
```

## Phase 3 — Play Console setup
1. **Create app** → name "Kontiva", default language German (Switzerland), app type
   App, free, accept declarations.
2. **App content**:
   - **Data safety**: declare **no data collected / no data shared** (everything is
     on-device, encrypted; the app declares no INTERNET permission). This is the
     headline trust point — fill it carefully.
   - **Content rating** questionnaire → finance app, no objectionable content.
   - **Privacy policy URL**: `https://mrumy-dev.github.io/kontiva/privacy.html`
     (the same page the iOS build uses).
   - Target audience, ads (none), government app (no).
3. **Store listing**: app name, short + full description (reuse the copy from
   `kontiva-ios/store/app-store-listing.md`), screenshots (the 6.9" PNGs in
   `kontiva-ios/store/screenshots/` work; Play wants min 2, phone), feature graphic
   (1024×500), app icon (512×512 — export from the iOS `AppIcon-1024`).

## Phase 4 — closed test → production
1. **Testing → Closed testing** → create a track, add ≥ 20 testers (emails / Group),
   upload the AAB, add release notes, roll out.
2. Run it **14+ days**, gather feedback, fix, upload new builds (bump `versionCode`).
3. **Apply for production access** (personal accounts), then promote the release to
   **Production** and submit for review.

## Notes / gotchas
- `versionCode` must increase on every upload.
- The app has **no INTERNET permission** on purpose — keep it that way; it backs the
  "Data Not Collected" promise.
- For a real launch, bump `versionName` to `1.0` in `app/build.gradle.kts`.
- Never commit `keystore.properties`, `*.jks`, or `*.keystore` (already gitignored).
