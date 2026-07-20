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
        private const val KEY_USER_ID = "user_id"
        private const val KEY_ACTIVATED_AT = "activated_at"

        const val TROY_BASE_URL = "https://wuomtznsotoqkk.masa.st"
        const val TROY_CINEMETA_MANIFEST_URL = "$TROY_BASE_URL/cinemeta/manifest.json"
        const val OPENSUBTITLES_MANIFEST_URL = "https://opensubtitles-v3.strem.io/manifest.json"
    }

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
    }

    fun isActivated(): Boolean {
        return prefs.getBoolean(KEY_ACTIVATED, false) && !getUserId().isNullOrBlank()
    }

    fun markActivated(userId: String) {
        prefs.edit()
            .putBoolean(KEY_ACTIVATED, true)
            .putString(KEY_USER_ID, userId.trim())
            .putLong(KEY_ACTIVATED_AT, System.currentTimeMillis())
            .apply()
    }

    fun getUserId(): String? {
        return prefs.getString(KEY_USER_ID, null)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    }

    fun getAuthCode(): String {
        return getUserId().orEmpty()
    }

    fun getUserAddonManifestUrl(): String? {
        val userId = getUserId() ?: return null
        return "$TROY_BASE_URL/$userId/manifest.json"
    }

    fun getDefaultAddonManifestUrls(): List<String> {
        val userAddonUrl = getUserAddonManifestUrl() ?: return emptyList()

        return listOf(
            "https://aiometadata.elfhosted.com/stremio/37288929-1b19-4e75-a678-f597fd0babb0/manifest.json",
            OPENSUBTITLES_MANIFEST_URL,
            userAddonUrl
        )
    }

    fun clearActivation() {
        prefs.edit().clear().apply()
    }
}
