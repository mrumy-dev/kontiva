// swift-tools-version: 6.0
//
// Kontiva — native macOS budgeting app (Phase 0 foundation).
//
// Built with Swift Package Manager so the core builds and tests with only the
// Command Line Tools toolchain (no full Xcode required). This same manifest
// opens directly in Xcode later with no changes.
//
// Layering (UI never touches storage/crypto directly):
//   Kontiva (app) ─▶ KontivaUI ─▶ KontivaCore
//
import PackageDescription

let package = Package(
    name: "Kontiva",
    defaultLocalization: "de",
    platforms: [
        .macOS(.v14)
    ],
    products: [
        .executable(name: "Kontiva", targets: ["Kontiva"]),
        .library(name: "KontivaCore", targets: ["KontivaCore"]),
        .library(name: "KontivaSecurity", targets: ["KontivaSecurity"]),
        .library(name: "KontivaPersistence", targets: ["KontivaPersistence"]),
        .library(name: "KontivaUI", targets: ["KontivaUI"]),
    ],
    targets: [
        // Pure domain logic. Foundation only — no SwiftUI, no persistence.
        // This is the always-testable heart of the product.
        .target(
            name: "KontivaCore"
        ),

        // Security core: KDF, key wrapping, authenticated encryption, auto-lock.
        // CryptoKit only.
        .target(
            name: "KontivaSecurity",
            dependencies: ["KontivaCore"]
        ),

        // Encrypted local persistence: the whole dataset is AES-256-GCM sealed at
        // rest under the master key. No plaintext private data is ever written.
        .target(
            name: "KontivaPersistence",
            dependencies: ["KontivaCore", "KontivaSecurity"]
        ),

        // SwiftUI layer: design system, brand, localization, screens.
        .target(
            name: "KontivaUI",
            dependencies: ["KontivaCore", "KontivaSecurity", "KontivaPersistence"],
            resources: [
                .process("Resources")
            ]
        ),

        // Thin @main executable that hosts the SwiftUI app.
        .executableTarget(
            name: "Kontiva",
            dependencies: ["KontivaUI"]
        ),

        // Automated proof of money, calculations, bills, redaction, L10n parity.
        .testTarget(
            name: "KontivaCoreTests",
            dependencies: ["KontivaCore"]
        ),

        // Automated proof of KDF determinism/vectors, key wrap round-trips,
        // wrong-passphrase rejection, tamper detection, and auto-lock logic.
        .testTarget(
            name: "KontivaSecurityTests",
            dependencies: ["KontivaSecurity"]
        ),

        // Automated proof of encryption-at-rest, persistence round-trips across
        // instances, wrong passphrase, tamper detection, and data deletion.
        .testTarget(
            name: "KontivaPersistenceTests",
            dependencies: ["KontivaPersistence"]
        ),

        // Proof that the PDF report renders to a valid multi-page document.
        .testTarget(
            name: "KontivaUITests",
            dependencies: ["KontivaUI"]
        ),
    ]
)
