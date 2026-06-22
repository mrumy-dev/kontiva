import Foundation
import CryptoKit
import KontivaCore
import KontivaSecurity

/// The encrypted local store. The entire dataset is sealed with AES-256-GCM under
/// the master key before being written; the master key is itself only ever stored
/// wrapped (in the keystore, derived from the passphrase). **No plaintext private
/// data is ever written to disk.**
///
/// Implemented as an `actor` so the (deliberately expensive) KDF and all file I/O
/// run off the main thread and mutable state is race-free. The decrypted master
/// key and dataset live here only while unlocked and are dropped on `lock()`.
public actor EncryptedStore {
    private let location: StoreLocation
    private var masterKey: SymmetricKey?
    private var current: AppDataset?

    public init(location: StoreLocation) {
        self.location = location
    }

    /// Whether a vault already exists on disk (→ show Unlock, not Setup).
    /// Nonisolated: only reads the immutable location and the filesystem.
    public nonisolated func hasExistingVault() -> Bool {
        FileManager.default.fileExists(atPath: location.keystoreURL.path)
    }

    public var isUnlocked: Bool { masterKey != nil }

    // MARK: - Lifecycle

    /// First-run setup: generate a master key, wrap it under the passphrase, and
    /// write an empty encrypted dataset. Throws if a vault already exists.
    public func createVault(passphrase: String,
                            iterations: Int = KeyVault.defaultIterations) throws {
        guard !hasExistingVault() else { throw StoreError.vaultAlreadyExists }
        let (material, key) = try KeyVault.create(passphrase: passphrase, iterations: iterations)
        try writeKeystore(material)
        let dataset = AppDataset.empty
        try writeDataset(dataset, key: key)
        masterKey = key
        current = dataset
    }

    /// Unlock an existing vault. Throws `.wrongPassphrase` on a bad passphrase.
    public func unlock(passphrase: String) throws {
        let material = try readKeystore()
        let key = try KeyVault.unwrap(passphrase: passphrase, material: material)
        let dataset = try readDataset(key: key)
        masterKey = key
        current = dataset
    }

    /// Drop the decrypted key and dataset from memory.
    public func lock() {
        masterKey = nil
        current = nil
    }

    // MARK: - Access & mutation

    /// Snapshot of the current decrypted dataset (only while unlocked).
    public func snapshot() throws -> AppDataset {
        guard let current else { throw StoreError.locked }
        return current
    }

    /// Mutate the dataset and persist it (re-encrypt + atomic write).
    public func mutate(_ block: @Sendable (inout AppDataset) -> Void) throws {
        guard let key = masterKey, var data = current else { throw StoreError.locked }
        block(&data)
        try writeDataset(data, key: key)
        current = data
    }

    /// Replace the whole dataset and persist it.
    public func replace(with dataset: AppDataset) throws {
        guard let key = masterKey else { throw StoreError.locked }
        try writeDataset(dataset, key: key)
        current = dataset
    }

    /// Re-wrap the same master key under a new passphrase. Existing encrypted data
    /// stays readable. Throws `.wrongPassphrase` if the old passphrase is wrong.
    public func changePassphrase(old: String, new: String,
                                 newIterations: Int = KeyVault.defaultIterations) throws {
        let material = try readKeystore()
        let rotated = try KeyVault.changePassphrase(old: old, new: new,
                                                    material: material,
                                                    newIterations: newIterations)
        try writeKeystore(rotated)
    }

    /// Danger zone: remove all local data (keystore + dataset).
    public func deleteAllData() throws {
        masterKey = nil
        current = nil
        try? FileManager.default.removeItem(at: location.dataURL)
        try? FileManager.default.removeItem(at: location.keystoreURL)
    }

    // MARK: - Portable encrypted backup & guarded restore
    //
    // A backup re-encrypts the entire dataset under a key derived from a SEPARATE
    // backup passphrase, so it is portable to another Mac and independent of the
    // local master key. Restore decrypts the backup and re-encrypts the dataset
    // under the *local* master key.

    private static let backupFormat = "kontiva.backup.v1"

    /// Produce a `.kontivabackup` blob protected by `backupPassphrase`.
    public func makeBackup(backupPassphrase: String, appVersion: String) throws -> Data {
        guard let key = masterKey, let dataset = current else { throw StoreError.locked }

        let payload = BackupPayload(schemaVersion: AppDataset.currentSchemaVersion,
                                    dataset: dataset)
        let plaintext = try JSONEncoder().encode(payload)

        let salt = KeyVault.newSalt()
        let iterations = KeyVault.defaultIterations
        let backupKey = KeyVault.deriveKey(passphrase: backupPassphrase, salt: salt, iterations: iterations)
        let sealed = try SecretBox.seal(plaintext, with: backupKey)

        let container = BackupContainer(
            format: Self.backupFormat, kdf: KeyVault.kdfIdentifier, cipher: KeyVault.cipherIdentifier,
            salt: salt, iterations: iterations, createdAt: Date(), appVersion: appVersion,
            counts: dataset.counts, sealed: sealed)
        return try JSONEncoder().encode(container)
    }

    /// Validate a backup and return its preview without changing any local data.
    /// Throws `.wrongPassphrase` if the passphrase doesn't open it, `.corrupted`
    /// if the file is not a valid backup.
    public func previewBackup(data: Data, backupPassphrase: String) throws -> BackupPreview {
        let container = try decodeContainer(data)
        _ = try openPayload(container, passphrase: backupPassphrase) // validates passphrase
        return BackupPreview(createdAt: container.createdAt, appVersion: container.appVersion,
                             counts: container.counts)
    }

    /// Guarded restore: replace ALL local data with the backup contents. The
    /// caller is responsible for confirming this destructive action first.
    public func restoreBackup(data: Data, backupPassphrase: String) throws {
        guard let key = masterKey else { throw StoreError.locked }
        let container = try decodeContainer(data)
        let payload = try openPayload(container, passphrase: backupPassphrase)

        // Replace the dataset (re-encrypted under the local master key).
        try writeDataset(payload.dataset, key: key)
        current = payload.dataset
    }

    private func decodeContainer(_ data: Data) throws -> BackupContainer {
        guard let container = try? JSONDecoder().decode(BackupContainer.self, from: data),
              container.format == Self.backupFormat else {
            throw StoreError.corrupted
        }
        return container
    }

    private func openPayload(_ container: BackupContainer, passphrase: String) throws -> BackupPayload {
        let backupKey = KeyVault.deriveKey(passphrase: passphrase, salt: container.salt,
                                           iterations: container.iterations)
        let plaintext: Data
        do {
            plaintext = try SecretBox.open(container.sealed, with: backupKey)
        } catch {
            throw SecurityError.wrongPassphrase
        }
        guard let payload = try? JSONDecoder().decode(BackupPayload.self, from: plaintext) else {
            throw StoreError.corrupted
        }
        return payload
    }

    // MARK: - I/O

    private func writeKeystore(_ material: WrappedKeyMaterial) throws {
        let data = try JSONEncoder().encode(material)
        try data.write(to: location.keystoreURL, options: [.atomic])
    }

    private func readKeystore() throws -> WrappedKeyMaterial {
        guard let data = try? Data(contentsOf: location.keystoreURL) else { throw StoreError.noVault }
        guard let material = try? JSONDecoder().decode(WrappedKeyMaterial.self, from: data) else {
            throw StoreError.corrupted
        }
        return material
    }

    private func writeDataset(_ dataset: AppDataset, key: SymmetricKey) throws {
        let plaintext = try JSONEncoder().encode(dataset)
        let sealed = try SecretBox.seal(plaintext, with: key)
        try sealed.write(to: location.dataURL, options: [.atomic])
    }

    private func readDataset(key: SymmetricKey) throws -> AppDataset {
        // Vault exists but no data file yet → treat as empty.
        guard let sealed = try? Data(contentsOf: location.dataURL) else { return AppDataset.empty }
        let plaintext = try SecretBox.open(sealed, with: key) // throws .decryptionFailed on tamper
        guard let dataset = try? JSONDecoder().decode(AppDataset.self, from: plaintext) else {
            throw StoreError.corrupted
        }
        return dataset
    }
}
