package com.aistudio.pianobar.kxuvzq.data

import android.content.Context
import android.content.SharedPreferences
import com.aistudio.pianobar.kxuvzq.security.SecureEnclaveKeyStore

class PianobarPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("pianobar_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USERNAME = "key_pandora_username"
        private const val KEY_ENCRYPTED_SECRET = "key_pandora_enclave_secret"
        private const val KEY_IS_LOGGED_IN = "key_is_logged_in"
        private const val KEY_HIGH_QUALITY = "key_high_quality"
        private const val KEY_BACKGROUND_STREAMING = "key_background_streaming"
        private const val KEY_LAST_STATION_ID = "key_last_station_id"
        private const val KEY_THEME_ID = "key_theme_id"
        private const val DEFAULT_THEME_ID = "midnight"
    }

    var lastStationId: String
        get() = prefs.getString(KEY_LAST_STATION_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_STATION_ID, value).apply()

    var username: String
        get() = prefs.getString(KEY_USERNAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USERNAME, value).apply()

    /**
     * Retrieves the decrypted secret using hardware-isolated keys in AndroidKeyStore.
     */
    var password: String
        get() {
            val encrypted = prefs.getString(KEY_ENCRYPTED_SECRET, "") ?: ""
            return if (encrypted.isNotEmpty()) {
                SecureEnclaveKeyStore.decrypt(encrypted)
            } else {
                ""
            }
        }
        set(value) {
            val encrypted = if (value.isNotEmpty()) SecureEnclaveKeyStore.encrypt(value) else ""
            prefs.edit().putString(KEY_ENCRYPTED_SECRET, encrypted).apply()
        }

    var isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_LOGGED_IN, value).apply()

    var isHighQualityAudio: Boolean
        get() = prefs.getBoolean(KEY_HIGH_QUALITY, true)
        set(value) = prefs.edit().putBoolean(KEY_HIGH_QUALITY, value).apply()

    var isBackgroundStreaming: Boolean
        get() = prefs.getBoolean(KEY_BACKGROUND_STREAMING, true)
        set(value) = prefs.edit().putBoolean(KEY_BACKGROUND_STREAMING, value).apply()

    /** Stable id from PianobarThemes; survives sign-out and app restarts. */
    var themeId: String
        get() = prefs.getString(KEY_THEME_ID, DEFAULT_THEME_ID) ?: DEFAULT_THEME_ID
        set(value) = prefs.edit().putString(KEY_THEME_ID, value).apply()

    /**
     * Stores credentials with the secret password/token encrypted via hardware StrongBox/TEE.
     */
    fun saveAccount(user: String, pass: String, highQuality: Boolean, backgroundStreaming: Boolean) {
        val encryptedSecret = if (pass.isNotEmpty()) SecureEnclaveKeyStore.encrypt(pass) else ""
        prefs.edit()
            .putString(KEY_USERNAME, user)
            .putString(KEY_ENCRYPTED_SECRET, encryptedSecret)
            .putBoolean(KEY_IS_LOGGED_IN, user.isNotBlank())
            .putBoolean(KEY_HIGH_QUALITY, highQuality)
            .putBoolean(KEY_BACKGROUND_STREAMING, backgroundStreaming)
            .apply()
    }

    fun clearAccount() {
        prefs.edit()
            .remove(KEY_USERNAME)
            .remove(KEY_ENCRYPTED_SECRET)
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .apply()
    }
}
