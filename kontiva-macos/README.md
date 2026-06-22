<div align="center">
  <img src="assets/brand/masters/kontiva-wordmark.png" alt="Kontiva" width="320" />
</div>

# Kontiva

A serious, private, Swiss budgeting app for regular households — native macOS.

> ⚠️ **Status: private-alpha (Phases 0–6 done; Phase 7 packaging/signing pending).**
> **Not beta-ready. Not V1.** It is usable locally: encrypted local database
> (AES-256-GCM) that survives relaunch, encrypted document vault, full in-app
> add/edit/delete for income/fixed/variable/savings/bills, a tax checklist, a
> portable **encrypted backup with guarded restore**, and a local redacted
> bug-report composer. Remaining for beta: **code-signing + notarization** (needs
> an Apple Developer account + full Xcode), a signed installer, and a formal
> accessibility audit. Do not treat this as a finished product.

Kontiva helps a normal Swiss household see, calmly and without judgement:
what comes in each month, what is already committed, which one-off bills are due,
what money is actually still available, which documents are needed for taxes, and
where salary statements, bills, the Lohnausweis, insurance documents and receipts
are stored.

## Principles

- **Local-first, offline-first.** No cloud sync, no server login, no bank login.
- **No telemetry, no analytics, no advertising or crash-reporting SDKs.**
- **No network calls at all** in the app.
- **Privacy by construction.** Encrypted local database, encrypted document
  vault, passphrase-derived keys, auto-lock, and encrypted portable backup are
  the target architecture (see [docs/SECURITY.md](docs/SECURITY.md)). Until that
  layer ships, **no private data is persisted at all.**
- **Money is never floating point.** All amounts are stored and calculated as
  integer Rappen (`Int64`); CHF strings are presentation only. This is enforced
  by tests.

## Platform

macOS only, native Swift (SwiftUI + AppKit + Foundation + CryptoKit). No Tauri,
no Electron, no React, no web wrapper. Windows will be a separate future,
Windows-native version.

## Project layout

```
assets/brand/masters/   Approved Kontiva brand masters — read-only, never edited
Sources/KontivaCore/    Pure domain: money, calculation engine, bills, L10n, redaction
Sources/KontivaUI/      SwiftUI design system, brand, screens, localization
Sources/Kontiva/        @main app entry
Tests/KontivaCoreTests/ Money / availability / bills / documents / redaction / L10n
Resources/AppIcon/      Generated .icns (derived only from the approved master)
scripts/package_app.sh  Build + assemble Kontiva.app (no full Xcode needed)
docs/                   Build plan, privacy, security, asset usage
```

## Build & run

This is a Swift Package, so it builds with the Command Line Tools — full Xcode
is not required for the core.

**One-time prerequisite** (the macOS SDK license must be accepted):

```sh
sudo xcodebuild -license accept
```

Then:

```sh
swift test                 # build + run the tested financial core
swift run Kontiva          # run the app (requires the macOS SDK)
./scripts/package_app.sh   # assemble build/Kontiva.app
```

You can also open `Package.swift` directly in full Xcode later — no changes needed.

## What is and isn't here (Phase 0)

**Here:** branded native skeleton, sidebar navigation, all seven sections as
structured screens with empty states, four-language localization (de-CH default,
fr-CH, it-CH, en) with key parity, the transparent available-this-month
calculation, bill classification, and a tested money model. A lock screen exists
as a UI gate only.

**Not here yet:** real encryption, persistence of any private data, the document
vault, taxes editing, backup/restore, and signing/notarization. See
[docs/KONTIVA_MACOS_ZERO_BUILD_PLAN.md](docs/KONTIVA_MACOS_ZERO_BUILD_PLAN.md)
for the phased plan.

## Brand

Brand masters live in `assets/brand/masters/` and are never modified. App-icon
outputs are generated only from the approved high-resolution master and kept
separately in `Resources/AppIcon/`. See [docs/ASSET_USAGE.md](docs/ASSET_USAGE.md).

## Licence / commercial

Full functionality is free for now: no paywalls, subscriptions, or purchase
screens. A possible future CHF 50 one-time purchase (with a household-friendly
option) is only a documented direction, not implemented.
