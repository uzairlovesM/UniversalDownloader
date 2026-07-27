package com.waheed.universaldownloader.data.settings

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores the app-lock PIN using AndroidX Security Crypto (AES-256 backed by the
 * platform Keystore) — never stored in plain SharedPreferences or Room.
 */
@Singleton
class PinManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "secure_pin_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun isPinSet(): Boolean = prefs.contains(KEY_PIN)

    fun setPin(pin: String) {
        prefs.edit().putString(KEY_PIN, pin).apply()
    }

    fun verifyPin(input: String): Boolean {
        return prefs.getString(KEY_PIN, null) == input
    }

    fun clearPin() {
        prefs.edit().remove(KEY_PIN).apply()
    }

    companion object {
        private const val KEY_PIN = "app_lock_pin"
    }
}
