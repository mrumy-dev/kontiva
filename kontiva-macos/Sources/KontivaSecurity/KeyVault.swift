import Foundation
import CryptoKit
import Security

/// Everything needed to unwrap the master key from a passphrase — and **nothing
/// secret**. The passphrase is never stored; the master key is stored only in
/// wrapped (encrypted) form. Safe to persist as-is in a later phase.
public struct WrappedKeyMaterial: Codable, Equatable, Sendable {
    public let kdf: String          // e.g. "PBKDF2-HMAC-SHA256"
    public let cipher: String       // e.g. "AES-GCM-256"
    public let salt: Data
    public let iterations: Int
    /// AES-GCM sealed box (nonce + ciphertext + tag) wrapping the master key.
    public let wrappedKey: Data

    public init(kdf: String, cipher: String, salt: Data, iterations: Int, wrappedKey: Data) {
        self.kdf = kdf
        self.cipher = cipher
        self.salt = salt
        self.iterations = iterations
        self.wrappedKey = wrappedKey
    }
}

/// Creates and opens the key hierarchy:
///
/// ```
/// passphrase --PBKDF2--> KEK --AES-GCM wrap--> [ master key ]  (stored wrapped)
/// ```
public enum KeyVault {

    public static let kdfIdentifier = "PBKDF2-HMAC-SHA256"
    public static let cipherIdentifier = "AES-GCM-256"
    /// OWASP-aligned default for PBKDF2-HMAC-SHA256. Tests use a smaller count.
    public static let defaultIterations = 210_000
    private static let saltBytes = 32

    /// Generate a fresh random master key and wrap it under the passphrase.
    /// Returns the storable material plus the in-memory master key (the caller
    /// holds it only while unlocked).
    public static func create(passphrase: String,
                              iterations: Int = defaultIterations) throws
        -> (material: WrappedKeyMaterial, masterKey: SymmetricKey) {

        let salt = randomData(saltBytes)
        let kek = deriveKEK(passphrase: passphrase, salt: salt, iterations: iterations)
        let masterKey = SymmetricKey(size: .bits256)

        let wrapped = try wrap(masterKey, with: kek)
        let material = WrappedKeyMaterial(
            kdf: kdfIdentifier, cipher: cipherIdentifier,
            salt: salt, iterations: iterations, wrappedKey: wrapped)
        return (material, masterKey)
    }

    /// Recover the master key from the passphrase. Throws `.wrongPassphrase`
    /// when the passphrase is incorrect (AES-GCM authentication fails).
    public static func unwrap(passphrase: String,
                              material: WrappedKeyMaterial) throws -> SymmetricKey {
        let kek = deriveKEK(passphrase: passphrase, salt: material.salt,
                            iterations: material.iterations)
        guard let box = try? AES.GCM.SealedBox(combined: material.wrappedKey) else {
            throw SecurityError.invalidMaterial
        }
        guard let raw = try? AES.GCM.open(box, using: kek) else {
            throw SecurityError.wrongPassphrase
        }
        return SymmetricKey(data: raw)
    }

    /// Re-wrap the *same* master key under a new passphrase (with a fresh salt),
    /// so existing data stays decryptable. Throws `.wrongPassphrase` if the old
    /// passphrase is wrong.
    public static func changePassphrase(old: String, new: String,
                                        material: WrappedKeyMaterial,
                                        newIterations: Int = defaultIterations) throws
        -> WrappedKeyMaterial {

        let masterKey = try unwrap(passphrase: old, material: material)
        let salt = randomData(saltBytes)
        let kek = deriveKEK(passphrase: new, salt: salt, iterations: newIterations)
        let wrapped = try wrap(masterKey, with: kek)
        return WrappedKeyMaterial(
            kdf: kdfIdentifier, cipher: cipherIdentifier,
            salt: salt, iterations: newIterations, wrappedKey: wrapped)
    }

    // MARK: - Internals

    /// Public KDF for deriving an independent key from a passphrase + salt
    /// (e.g. a portable backup key, separate from the local master key).
    public static func deriveKey(passphrase: String, salt: Data, iterations: Int) -> SymmetricKey {
        deriveKEK(passphrase: passphrase, salt: salt, iterations: iterations)
    }

    /// Generate fresh random salt bytes (public for backup callers).
    public static func newSalt(_ count: Int = 32) -> Data { randomData(count) }

    static func deriveKEK(passphrase: String, salt: Data, iterations: Int) -> SymmetricKey {
        // Normalize so visually identical passphrases derive the same key.
        let normalized = passphrase.precomposedStringWithCanonicalMapping
        let pwData = Data(normalized.utf8)
        let derived = KDF.pbkdf2SHA256(password: pwData, salt: salt,
                                       iterations: iterations, keyLength: 32)
        return SymmetricKey(data: derived)
    }

    private static func wrap(_ masterKey: SymmetricKey, with kek: SymmetricKey) throws -> Data {
        let raw = masterKey.withUnsafeBytes { Data($0) }
        guard let combined = try AES.GCM.seal(raw, using: kek).combined else {
            throw SecurityError.encryptionFailed
        }
        return combined
    }

    /// Cryptographically secure random bytes from the system CSPRNG.
    static func randomData(_ count: Int) -> Data {
        var bytes = [UInt8](repeating: 0, count: count)
        let status = SecRandomCopyBytes(kSecRandomDefault, count, &bytes)
        precondition(status == errSecSuccess, "secure RNG failure")
        return Data(bytes)
    }
}
