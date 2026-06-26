package com.lumera.app.data.activation

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_FILE = "activation_prefs"
        private const val KEY_ACTIVATED = "activated"
        private const val KEY_ACTIVATION_CODE = "activation_code"
        private const val KEY_ACTIVATED_AT = "activated_at"
    }

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
    }

    fun isActivated(): Boolean {
        return prefs.getBoolean(KEY_ACTIVATED, false)
    }

    fun markActivated(code: String) {
        prefs.edit()
            .putBoolean(KEY_ACTIVATED, true)
            .putString(KEY_ACTIVATION_CODE, code)
            .putLong(KEY_ACTIVATED_AT, System.currentTimeMillis())
            .apply()
    }

    fun getActivationCode(): String? {
        return prefs.getString(KEY_ACTIVATION_CODE, null)
    }

    fun clearActivation() {
        prefs.edit().clear().apply()
    }
}
