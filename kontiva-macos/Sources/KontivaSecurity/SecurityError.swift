import Foundation

/// Errors from the security layer. Deliberately coarse so callers cannot
/// distinguish *why* decryption failed (avoids oracles); a wrong passphrase and
/// tampered material both surface as authentication failures.
public enum SecurityError: Error, Equatable, Sendable {
    /// The passphrase did not unwrap the master key (authentication failed).
    case wrongPassphrase
    /// Encryption failed unexpectedly.
    case encryptionFailed
    /// Decryption/authentication of a sealed box failed (wrong key or tampering).
    case decryptionFailed
    /// Stored key material is malformed.
    case invalidMaterial
}
