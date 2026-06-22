package ch.kontiva.android.persistence

import ch.kontiva.android.core.AppSettings
import ch.kontiva.android.core.SecuritySettings
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * The household profile. Canton + member counts come with the settings stage;
 * onboarding only sets name + avatar (1:1 with iOS `Household`).
 */
@Serializable
data class Household(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val avatarName: String? = null,
    val adults: Int = 1,
    val children: Int = 0,
)

/**
 * The entire application dataset, serialized as one aggregate and AES-256-GCM
 * sealed before it ever touches disk (see [EncryptedStore]). The financial
 * collections (incomes, fixed/variable costs, bills, savings, debts) arrive with
 * the engine stage; `ignoreUnknownKeys` keeps older/newer stores readable.
 */
@Serializable
data class AppDataset(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val household: Household? = null,
    val appSettings: AppSettings = AppSettings(),
    val securitySettings: SecuritySettings = SecuritySettings(),
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
        val empty get() = AppDataset()
    }
}
