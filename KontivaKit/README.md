# KontivaKit

The shared, platform-agnostic engine behind every Kontiva app (iOS, iPadOS, macOS).

KontivaKit is the **single source of truth** for the domain model, cryptography, and
encrypted persistence. It contains **no UI and no platform (AppKit/UIKit) code**, so it
builds unchanged for every Apple platform. App workspaces consume it as a SwiftPM
dependency and add their own platform-native UI on top.

## Layering

```
App UI  ─▶  KontivaPersistence  ─▶  KontivaSecurity  ─▶  KontivaCore
```

| Library | Responsibility | Dependencies |
|---|---|---|
| **KontivaCore** | Money (Int64 Rappen — never float), entities, bills, debts, insights, availability, and the 4-language localization (de/fr/it/en). | Foundation |
| **KontivaSecurity** | PBKDF2-HMAC-SHA256 KDF, AES-256-GCM key wrapping & authenticated encryption, auto-lock rules. | CryptoKit, KontivaCore |
| **KontivaPersistence** | The whole `AppDataset` AES-256-GCM sealed at rest under the master key; portable encrypted backups. No plaintext private data is ever written. | KontivaCore, KontivaSecurity |

## Provenance

Lifted verbatim from the original macOS Kontiva app's platform-clean core (it imported
zero AppKit). The macOS app keeps its own frozen embedded copy; KontivaKit is the living
engine for the mobile apps.

## Build & test

```sh
swift build          # host (macOS)
swift test           # 106 engine tests
```

iOS compilation is verified against the iOS 26 SDK in CI / locally via the app workspaces.

## Platforms

iOS 26+, macOS 14+. (iPadOS is covered by the iOS platform.)

## Rule

This package is the **only** place the shared engine is edited. Consuming apps never
fork it — they depend on a pinned version.
