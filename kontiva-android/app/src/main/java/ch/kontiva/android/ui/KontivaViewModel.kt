package ch.kontiva.android.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ch.kontiva.android.core.AppLanguage
import ch.kontiva.android.core.AppSettings
import ch.kontiva.android.core.SettingsStore
import ch.kontiva.android.persistence.AppDataset
import ch.kontiva.android.persistence.EncryptedStore
import ch.kontiva.android.persistence.Household
import ch.kontiva.android.persistence.StoreLocation
import ch.kontiva.android.security.WrongPassphraseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

enum class AppPhase { ONBOARDING, LOCKED, UNLOCKED }

/**
 * App state machine (1:1 with the iOS AppModel): needs-setup → locked → unlocked.
 * Owns the encrypted store and the non-secret settings. The expensive KDF runs on
 * Dispatchers.Default so the UI stays responsive.
 */
class KontivaViewModel(app: Application) : AndroidViewModel(app) {
    private val store = EncryptedStore(StoreLocation(app.filesDir))
    private val settingsStore = SettingsStore(app)

    var phase by mutableStateOf(
        if (store.hasExistingVault()) AppPhase.LOCKED else AppPhase.ONBOARDING,
    )
        private set

    var settings: AppSettings by mutableStateOf(
        settingsStore.load(deviceDefault = AppLanguage.bestForDevice(deviceLocales())),
    )
        private set

    var household by mutableStateOf<Household?>(null)
        private set
    var busy by mutableStateOf(false)
        private set
    var unlockFailed by mutableStateOf(false)
        private set

    init {
        // Persist the resolved default so the lock screen reads the same language.
        settingsStore.save(settings)
    }

    fun setLanguage(language: AppLanguage) {
        settings = settings.copy(language = language)
        settingsStore.save(settings)
        if (store.isUnlocked) runCatching { store.mutate { it.copy(appSettings = settings) } }
    }

    /** Finish onboarding: create the encrypted vault, store profile + settings, unlock. */
    fun completeSetup(passphrase: String, name: String, avatar: String?) {
        if (busy) return
        busy = true
        viewModelScope.launch {
            val hh = if (name.isNotBlank()) Household(name = name, avatarName = avatar) else null
            withContext(Dispatchers.Default) {
                store.createVault(passphrase, AppDataset(household = hh, appSettings = settings))
            }
            household = hh
            busy = false
            phase = AppPhase.UNLOCKED
        }
    }

    /** Unlock an existing vault; flips [unlockFailed] on a wrong code. */
    fun unlock(passphrase: String) {
        if (busy) return
        busy = true
        unlockFailed = false
        viewModelScope.launch {
            val ok = withContext(Dispatchers.Default) {
                try {
                    store.unlock(passphrase); true
                } catch (_: WrongPassphraseException) {
                    false
                }
            }
            if (ok) {
                val snap = store.snapshot()
                household = snap.household
                settings = snap.appSettings
                settingsStore.save(settings)
                phase = AppPhase.UNLOCKED
            } else {
                unlockFailed = true
            }
            busy = false
        }
    }

    fun clearUnlockError() {
        unlockFailed = false
    }

    fun lock() {
        store.lock()
        household = null
        phase = AppPhase.LOCKED
    }

    private fun deviceLocales(): List<Locale> {
        val list = getApplication<Application>().resources.configuration.locales
        return (0 until list.size()).map { list[it] }.ifEmpty { listOf(Locale.getDefault()) }
    }
}
