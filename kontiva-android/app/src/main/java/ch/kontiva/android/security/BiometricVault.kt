package ch.kontiva.android.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Stores the vault passphrase encrypted under a hardware-backed AndroidKeyStore AES
 * key, as a *convenience* layer gated in-app by a BiometricPrompt check (mirrors the
 * iOS BiometricVault trade-off: the passphrase stays the root secret, the no-recovery
 * guarantee is unchanged, and the item never leaves the device — the Keystore key is
 * non-exportable). The biometric gate is enforced in-app by the caller, not by a
 * Keystore CryptoObject, so it works on devices/emulators without an enrolled lock.
 */
object BiometricVault {
    private const val ALIAS = "ch.kontiva.android.biometric"
    private const val PREFS = "kontiva_biometric"
    private const val KEY = "passphrase"
    private const val TRANSFORM = "AES/GCM/NoPadding"

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Is a passphrase stored? (No biometric prompt — plain presence check.) */
    fun hasStored(ctx: Context): Boolean = prefs(ctx).contains(KEY)

    fun store(ctx: Context, passphrase: String): Boolean = try {
        val cipher = Cipher.getInstance(TRANSFORM)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val ct = cipher.doFinal(passphrase.toByteArray(Charsets.UTF_8))
        val blob = cipher.iv + ct // iv(12) || ciphertext+tag
        prefs(ctx).edit().putString(KEY, Base64.encodeToString(blob, Base64.NO_WRAP)).commit()
    } catch (_: Exception) {
        false
    }

    fun retrieve(ctx: Context): String? = try {
        val enc = prefs(ctx).getString(KEY, null) ?: return null
        val blob = Base64.decode(enc, Base64.NO_WRAP)
        val iv = blob.copyOfRange(0, 12)
        val ct = blob.copyOfRange(12, blob.size)
        val key = (keystore().getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey
        if (key == null) null else {
            val cipher = Cipher.getInstance(TRANSFORM)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
            String(cipher.doFinal(ct), Charsets.UTF_8)
        }
    } catch (_: Exception) {
        null
    }

    fun clear(ctx: Context) {
        prefs(ctx).edit().remove(KEY).commit()
        try { keystore().deleteEntry(ALIAS) } catch (_: Exception) {}
    }

    private fun keystore() = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    private fun getOrCreateKey(): SecretKey {
        val ks = keystore()
        (ks.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return gen.generateKey()
    }
}
