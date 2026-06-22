import Foundation

/// Errors from the persistence layer.
public enum StoreError: Error, Equatable, Sendable {
    case vaultAlreadyExists
    case noVault
    case locked
    case corrupted
}

/// Resolves the on-disk locations for the keystore and the encrypted data blob.
/// Files live outside the source tree (Application Support), so private data is
/// never anywhere near the repository.
public struct StoreLocation: Sendable {
    public let directory: URL

    public init(directory: URL) { self.directory = directory }

    /// Non-secret: salt + KDF params + wrapped master key. Safe to persist.
    public var keystoreURL: URL { directory.appendingPathComponent("keystore.json") }
    /// AES-256-GCM sealed blob of the entire dataset. Opaque ciphertext.
    public var dataURL: URL { directory.appendingPathComponent("store.kenc") }

    /// `~/Library/Application Support/<appFolder>/`, created if missing.
    public static func applicationSupport(appFolder: String = "Kontiva") throws -> StoreLocation {
        let base = try FileManager.default.url(
            for: .applicationSupportDirectory, in: .userDomainMask,
            appropriateFor: nil, create: true)
        let dir = base.appendingPathComponent(appFolder, isDirectory: true)
        try FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        return StoreLocation(directory: dir)
    }
}
