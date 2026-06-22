package ch.kontiva.android.persistence

import ch.kontiva.android.security.Crypto
import ch.kontiva.android.security.KeyVault
import ch.kontiva.android.security.WrappedKeyMaterial
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/** On-disk vault locations (mirrors iOS StoreLocation): keystore.json + store.kenc. */
class StoreLocation(baseDir: File) {
    val dir: File = File(baseDir, "Kontiva").apply { mkdirs() }
    val keystore: File = File(dir, "keystore.json")
    val data: File = File(dir, "store.kenc")
}

class StoreLockedException : Exception("vault is locked")
class VaultExistsException : Exception("vault already exists")
class CorruptVaultException : Exception("vault corrupt")

/**
 * The encrypted local store — a faithful port of iOS `EncryptedStore`. The whole
 * dataset is AES-256-GCM sealed under the master key before being written; the
 * master key is itself only ever stored wrapped (keystore, derived from the
 * passphrase). No plaintext private data is ever written to disk. The decrypted
 * key + dataset live here only while unlocked and are dropped on [lock].
 *
 * The expensive KDF runs off the main thread (callers use Dispatchers.Default).
 */
class EncryptedStore(private val loc: StoreLocation) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private var masterKey: ByteArray? = null
    private var current: AppDataset? = null

    /** Whether a vault already exists on disk (→ show Unlock, not Setup). */
    fun hasExistingVault(): Boolean = loc.keystore.exists()

    val isUnlocked: Boolean get() = masterKey != null

    /** First-run setup: generate + wrap a master key and write the initial dataset. */
    fun createVault(passphrase: String, initial: AppDataset = AppDataset.empty) {
        if (hasExistingVault()) throw VaultExistsException()
        val created = KeyVault.create(passphrase)
        loc.keystore.writeText(json.encodeToString(created.material))
        writeDataset(initial, created.masterKey)
        masterKey = created.masterKey
        current = initial
    }

    /** Unlock an existing vault. Throws WrongPassphraseException on a bad passphrase. */
    fun unlock(passphrase: String) {
        val material = readKeystore()
        val key = KeyVault.unwrap(passphrase, material)
        val dataset = readDataset(key)
        masterKey = key
        current = dataset
    }

    /** Drop the decrypted key + dataset from memory. */
    fun lock() {
        masterKey = null
        current = null
    }

    /** Snapshot of the decrypted dataset (only while unlocked). */
    fun snapshot(): AppDataset = current ?: throw StoreLockedException()

    /** Mutate the dataset and persist it (re-encrypt + write). */
    fun mutate(block: (AppDataset) -> AppDataset) {
        val key = masterKey ?: throw StoreLockedException()
        val updated = block(current ?: throw StoreLockedException())
        writeDataset(updated, key)
        current = updated
    }

    /** Re-wrap the same master key under a new passphrase. */
    fun changePassphrase(old: String, new: String) {
        val rotated = KeyVault.changePassphrase(old, new, readKeystore())
        loc.keystore.writeText(json.encodeToString(rotated))
    }

    /** Danger zone: remove all local data. */
    fun deleteAllData() {
        masterKey = null
        current = null
        loc.data.delete()
        loc.keystore.delete()
    }

    // I/O ---------------------------------------------------------------------

    private fun readKeystore(): WrappedKeyMaterial =
        try {
            json.decodeFromString(loc.keystore.readText())
        } catch (_: Exception) {
            throw CorruptVaultException()
        }

    private fun writeDataset(dataset: AppDataset, key: ByteArray) {
        val sealed = Crypto.seal(json.encodeToString(dataset).toByteArray(Charsets.UTF_8), key)
        val tmp = File(loc.data.parentFile, loc.data.name + ".tmp")
        tmp.writeBytes(sealed)
        if (!tmp.renameTo(loc.data)) {
            loc.data.writeBytes(sealed)
            tmp.delete()
        }
    }

    private fun readDataset(key: ByteArray): AppDataset {
        if (!loc.data.exists()) return AppDataset.empty
        val plain = Crypto.open(loc.data.readBytes(), key) // throws on tamper
        return try {
            json.decodeFromString(String(plain, Charsets.UTF_8))
        } catch (_: Exception) {
            throw CorruptVaultException()
        }
    }
}
