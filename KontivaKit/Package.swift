// swift-tools-version: 6.0
//
// KontivaKit — the shared, platform-agnostic engine behind every Kontiva app.
//
// This package is the single source of truth for the domain model, cryptography,
// and encrypted persistence. It contains NO UI and NO platform (AppKit/UIKit)
// code, so it builds unchanged for iOS, iPadOS, and macOS. App workspaces
// (kontiva-ios, future kontiva-ipados, and the existing macOS app) consume it.
//
// Layering (consumers never touch storage/crypto directly except through these):
//   App UI ─▶ KontivaPersistence ─▶ KontivaSecurity ─▶ KontivaCore
//
import PackageDescription

let package = Package(
    name: "KontivaKit",
    defaultLocalization: "de",
    platforms: [
        .iOS("26.0"),
        .macOS(.v14),
    ],
    products: [
        .library(name: "KontivaCore", targets: ["KontivaCore"]),
        .library(name: "KontivaSecurity", targets: ["KontivaSecurity"]),
        .library(name: "KontivaPersistence", targets: ["KontivaPersistence"]),
    ],
    targets: [
        // Pure domain logic — Foundation only. Money (Int64 Rappen), entities,
        // bills, debts, insights, availability, and the 4-language localization.
        .target(name: "KontivaCore"),

        // Security core: PBKDF2-HMAC-SHA256 KDF, AES-256-GCM key wrapping &
        // authenticated encryption, auto-lock rules. CryptoKit only.
        .target(name: "KontivaSecurity", dependencies: ["KontivaCore"]),

        // Encrypted local persistence: the whole AppDataset is AES-256-GCM sealed
        // at rest under the master key. No plaintext private data is ever written.
        .target(name: "KontivaPersistence", dependencies: ["KontivaCore", "KontivaSecurity"]),

        .testTarget(name: "KontivaCoreTests", dependencies: ["KontivaCore"]),
        .testTarget(name: "KontivaSecurityTests", dependencies: ["KontivaSecurity"]),
        .testTarget(name: "KontivaPersistenceTests", dependencies: ["KontivaPersistence"]),
    ]
)
