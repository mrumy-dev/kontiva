import Foundation
import CryptoKit

/// Key-derivation functions.
///
/// Phase 1 ships PBKDF2-HMAC-SHA256 (RFC 2898), implemented on top of CryptoKit's
/// hardware-accelerated HMAC so it can be validated against published test
/// vectors. Argon2id remains the preferred long-term KDF (memory-hard); it will
/// be vendored as a C target in a later pass. The `WrappedKeyMaterial` records
/// which KDF produced a vault, so an upgrade can re-wrap transparently.
public enum KDF {

    /// Derive `keyLength` bytes from `password`/`salt` using PBKDF2-HMAC-SHA256.
    public static func pbkdf2SHA256(password: Data, salt: Data,
                                    iterations: Int, keyLength: Int) -> Data {
        precondition(iterations >= 1 && keyLength >= 1)
        let hLen = SHA256.byteCount // 32
        let blockCount = (keyLength + hLen - 1) / hLen
        let key = SymmetricKey(data: password)

        var derived = Data(capacity: blockCount * hLen)
        for blockIndex in 1...blockCount {
            // INT_32_BE(blockIndex)
            let intBlock = Data([
                UInt8((blockIndex >> 24) & 0xff),
                UInt8((blockIndex >> 16) & 0xff),
                UInt8((blockIndex >> 8) & 0xff),
                UInt8(blockIndex & 0xff),
            ])

            // U1 = HMAC(salt || INT(i)); T = U1
            var u = Data(HMAC<SHA256>.authenticationCode(for: salt + intBlock, using: key))
            var t = u
            if iterations > 1 {
                for _ in 2...iterations {
                    u = Data(HMAC<SHA256>.authenticationCode(for: u, using: key))
                    for j in 0..<hLen { t[j] ^= u[j] }
                }
            }
            derived.append(t)
        }
        return Data(derived.prefix(keyLength))
    }
}
