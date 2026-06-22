package ch.kontiva.android.security

import java.security.SecureRandom
import java.text.Normalizer
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/** Wrong passphrase (AES-GCM authentication failed on unwrap). */
class WrongPassphraseException : Exception("wrong passphrase")

/**
 * Crypto primitives — a faithful port of KontivaSecurity (KDF + SecretBox).
 *
 * - PBKDF2-HMAC-SHA256 over the explicit UTF-8 bytes of the NFC-normalized
 *   passphrase (byte-for-byte identical to the iOS implementation).
 * - AES-256-GCM "combined" layout: iv(12) || ciphertext || tag(16), matching
 *   CryptoKit's `.combined`.
 */
object Crypto {
    private val rng = SecureRandom()
    private const val IV_LEN = 12
    private const val TAG_BITS = 128
    private const val HLEN = 32

    fun randomBytes(n: Int): ByteArray = ByteArray(n).also(rng::nextBytes)

    /** NFC-normalize then UTF-8 encode — matches iOS `precomposedStringWithCanonicalMapping`. */
    fun normalizedPasswordBytes(passphrase: String): ByteArray =
        Normalizer.normalize(passphrase, Normalizer.Form.NFC).toByteArray(Charsets.UTF_8)

    /** PBKDF2-HMAC-SHA256 (RFC 2898). */
    fun pbkdf2Sha256(password: ByteArray, salt: ByteArray, iterations: Int, keyLength: Int): ByteArray {
        require(iterations >= 1 && keyLength >= 1)
        val mac = Mac.getInstance("HmacSHA256").apply { init(SecretKeySpec(password, "HmacSHA256")) }
        val blocks = (keyLength + HLEN - 1) / HLEN
        val out = ByteArray(blocks * HLEN)
        val intBlock = ByteArray(4)
        for (i in 1..blocks) {
            intBlock[0] = (i ushr 24).toByte()
            intBlock[1] = (i ushr 16).toByte()
            intBlock[2] = (i ushr 8).toByte()
            intBlock[3] = i.toByte()
            mac.update(salt)
            var u = mac.doFinal(intBlock) // U1 = HMAC(salt || INT(i))
            val t = u.copyOf()
            for (c in 2..iterations) {
                u = mac.doFinal(u)
                for (j in 0 until HLEN) t[j] = (t[j].toInt() xor u[j].toInt()).toByte()
            }
            System.arraycopy(t, 0, out, (i - 1) * HLEN, HLEN)
        }
        return out.copyOf(keyLength)
    }

    /** Encrypt → iv(12) || ciphertext || tag(16). */
    fun seal(plaintext: ByteArray, key: ByteArray): ByteArray {
        val iv = randomBytes(IV_LEN)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, iv))
        return iv + cipher.doFinal(plaintext)
    }

    /** Decrypt a combined box; throws on wrong key / tamper. */
    fun open(combined: ByteArray, key: ByteArray): ByteArray {
        require(combined.size > IV_LEN) { "ciphertext too short" }
        val iv = combined.copyOfRange(0, IV_LEN)
        val ct = combined.copyOfRange(IV_LEN, combined.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, iv))
        return cipher.doFinal(ct)
    }
}
