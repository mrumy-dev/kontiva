# Kontiva — Android

A native **Kotlin + Jetpack Compose** port of the iOS Kontiva app: the same private,
on-device Swiss household-budgeting app, feature-for-feature, design-for-design.

- Package: `ch.kontiva.android` · minSdk 26 · targetSdk/compileSdk 35
- Build toolchain: Gradle 8.11 (wrapper) · AGP 8.7 · Kotlin 2.1 · Compose BOM 2024.12
- Builds with **JDK 21** (the machine's JDK 26 is too new for stable AGP):
  ```bash
  JAVA_HOME="$(brew --prefix openjdk@21)/libexec/openjdk.jdk/Contents/Home" \
    ./gradlew :app:assembleDebug
  ```

## Module mapping (iOS Swift → Android Kotlin)

| iOS (KontivaKit / kontiva-ios)      | Android (`ch.kontiva.android`)        |
|-------------------------------------|----------------------------------------|
| `KontivaCore` (model, money, l10n)  | `core/`, `core.l10n/`                  |
| `KontivaSecurity` (KDF, SecretBox)  | `security/` *(planned)*                |
| `KontivaPersistence` (EncryptedStore)| `persistence/` *(planned)*            |
| `DesignSystem/KontivaTheme`         | `ui.theme/KontivaTheme`               |
| SwiftUI `Screens/`                  | `ui.screen/` (Compose)                |

## Localization — done, 1:1 with iOS ✅

All **279 keys × 35 languages** are ported with full parity. They are **generated**
from the iOS Swift source by [`tools/transpile_l10n.py`](tools/transpile_l10n.py) — never
hand-edit `core/l10n/L10nKey.kt` or `Loc_*.kt`; re-run the transpiler when the iOS
strings change so the platforms stay in sync:

```bash
python3 tools/transpile_l10n.py
```

The runtime `Localizer` mirrors iOS (key → de-CH fallback → raw key) and the in-app
language picker overrides the device locale, exactly like iOS.

## Status

**Foundation — building & verified** (`./gradlew :app:assembleDebug` → APK):
- [x] Gradle/Compose project scaffold, manifest, adaptive launcher icon
- [x] Design system: full palette (light/dark), 8 accent themes, spacing/radii, chart colours
- [x] Enums ported 1:1: `AppLanguage` (35, endonyms/RTL/groups/`bestForDevice`), `AccentTheme`, `AppAppearance`, `AutoLockInterval`, `AppSettings`
- [x] Localization: 35 languages, runtime `Localizer`, device-language default
- [x] Onboarding **Welcome** screen (wordmark, intro, feature cues, CTA)

**Roadmap — remaining stages** (each lands as its own building commit):
1. **Engine** — `Money` (Rappen-precise) + format/parse, `Entities`, `Bills`, `Debts`,
   `Insights`, `AvailabilityEngine`
2. **Security** — `KDF` (PBKDF2), `SecretBox` (AES-256-GCM), `KeyVault`, biometric unlock
3. **Persistence** — `EncryptedStore`, `AppDataset`, backup/restore
4. **Screens** — Lock/PIN, onboarding (language + profile + code), Overview (donut),
   Monthly Planning, Bills, Savings, Settings
5. **Polish** — motion, haptics, RTL, theme selector, PDF export

This is a large, faithful port being built in verified stages — not a one-shot dump.
