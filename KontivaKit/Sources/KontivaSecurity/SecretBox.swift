import Foundation
import CryptoKit

/// Authenticated encryption of arbitrary data with the master key (AES-256-GCM).
/// This is the primitive the encrypted database and document vault will use in
/// later phases. A random nonce is generated per seal; tampering or a wrong key
/// fails authentication and throws.
public enum SecretBox {

    /// Encrypt `plaintext`, returning a combined sealed box (nonce + ct + tag).
    public static func seal(_ plaintext: Data, with key: SymmetricKey) throws -> Data {
        guard let combined = try AES.GCM.seal(plaintext, using: key).combined else {
            throw SecurityError.encryptionFailed
        }
        return combined
    }

    /// Decrypt a combined sealed box. Throws `.decryptionFailed` if the key is
    /// wrong or the ciphertext was tampered with.
    public static func open(_ combined: Data, with key: SymmetricKey) throws -> Data {
        guard let box = try? AES.GCM.SealedBox(combined: combined) else {
            throw SecurityError.invalidMaterial
        }
        guard let plaintext = try? AES.GCM.open(box, using: key) else {
            throw SecurityError.decryptionFailed
        }
        return plaintext
    }

    // Convenience for encrypting Codable values (used by repositories later).

    public static func seal<T: Encodable>(_ value: T, with key: SymmetricKey) throws -> Data {
        try seal(JSONEncoder().encode(value), with: key)
    }

    public static func open<T: Decodable>(_ type: T.Type, from combined: Data,
                                          with key: SymmetricKey) throws -> T {
        try JSONDecoder().decode(T.self, from: open(combined, with: key))
    }
}
