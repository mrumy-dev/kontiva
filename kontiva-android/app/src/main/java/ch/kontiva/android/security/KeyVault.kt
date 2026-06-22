package ch.kontiva.android.security

import kotlinx.serialization.Serializable
import java.util.Base64

/**
 * Everything needed to unwrap the master key from a passphrase — and nothing secret.
 * The passphrase is never stored; the master key is stored only wrapped. Byte fields
 * are Base64 so the keystore is plain JSON on disk. 1:1 with iOS `WrappedKeyMaterial`.
 */
@Serializable
data class WrappedKeyMaterial(
    val kdf: String,        // "PBKDF2-HMAC-SHA256"
    val cipher: String,     // "AES-GCM-256"
    val salt: String,       // base64
    val iterations: Int,
    val wrappedKey: String, // base64 AES-GCM sealed box wrapping the master key
)

/**
 * Key hierarchy (1:1 with iOS KeyVault):
 *   passphrase --PBKDF2--> KEK --AES-GCM wrap--> [ master key ]  (stored wrapped)
 */
object KeyVault {
    const val KDF_ID = "PBKDF2-HMAC-SHA256"
    const val CIPHER_ID = "AES-GCM-256"
    const val DEFAULT_ITERATIONS = 210_000 // OWASP-aligned
    private const val SALT_BYTES = 32

    class Created(val material: WrappedKeyMaterial, val masterKey: ByteArray)

    /** Generate a fresh random master key and wrap it under the passphrase. */
    fun create(passphrase: String, iterations: Int = DEFAULT_ITERATIONS): Created {
        val salt = Crypto.randomBytes(SALT_BYTES)
        val kek = deriveKek(passphrase, salt, iterations)
        val masterKey = Crypto.randomBytes(32)
        val wrapped = Crypto.seal(masterKey, kek)
        return Created(
            WrappedKeyMaterial(KDF_ID, CIPHER_ID, salt.b64(), iterations, wrapped.b64()),
            masterKey,
        )
    }

    /** Recover the master key; throws [WrongPassphraseException] on a bad passphrase. */
    fun unwrap(passphrase: String, material: WrappedKeyMaterial): ByteArray {
        val kek = deriveKek(passphrase, material.salt.unb64(), material.iterations)
        return try {
            Crypto.open(material.wrappedKey.unb64(), kek)
        } catch (_: Exception) {
            throw WrongPassphraseException()
        }
    }

    /** Re-wrap the same master key under a new passphrase (fresh salt). */
    fun changePassphrase(
        old: String,
        new: String,
        material: WrappedKeyMaterial,
        newIterations: Int = DEFAULT_ITERATIONS,
    ): WrappedKeyMaterial {
        val masterKey = unwrap(old, material)
        val salt = Crypto.randomBytes(SALT_BYTES)
        val kek = deriveKek(new, salt, newIterations)
        return WrappedKeyMaterial(KDF_ID, CIPHER_ID, salt.b64(), newIterations, Crypto.seal(masterKey, kek).b64())
    }

    private fun deriveKek(passphrase: String, salt: ByteArray, iterations: Int): ByteArray =
        Crypto.pbkdf2Sha256(Crypto.normalizedPasswordBytes(passphrase), salt, iterations, 32)
}

private fun ByteArray.b64(): String = Base64.getEncoder().encodeToString(this)
private fun String.unb64(): ByteArray = Base64.getDecoder().decode(this)
