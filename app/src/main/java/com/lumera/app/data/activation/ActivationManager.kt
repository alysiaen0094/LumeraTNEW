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
            TROY_CINEMETA_MANIFEST_URL,
            OPENSUBTITLES_MANIFEST_URL,
            userAddonUrl,
            "https://tmdb.elfhosted.com/N4IgTgDgJgRgsgUygSwIYBUCeEEGcQBcoEA9rgC4JiHlgCuCANCADYkDmJhAZqi7kxAxUAYwDWUMCQg8+AgL7NyAW1gBBCMgDSCTIRCooAJlTcAHFADMAFhGoYABgBsqAIyukAViMBOVzCcRKCgfEQQnEGYIKQA3ZCgEAElVGESofVoGSPAEcjowADtk2DSM+gRsllQC9jpUdgqCEAQCgFoAVQBlbIowBFRlZBrCAG0AXWY7cj4OfAIR0HiMlIA6KsoKbPJsRpBlEjiK5gKB3YAZVA3yAAIAJQQWfoF8ZlwACxIAd0SCgAkSZSNTIIRSLdJNFSwNaXPDkLY7fQCMDIPDZE6A-QXK53B5PVGvD7fP4AoHlUEgJYQ1a0FooYZKBFNfaHNGnfToPoFOnsHqEn7-DEEYHkykgSEwFY0rlDHkMnCIqgol4gdG7Dm0mW8r78kk0MmMMHLKHkaTw+VMg4o1mCkAABWkdCq1AJ2uJguFBop4LF1NNct2SKV1t29ogjtQzpA71dAtJDBF3vFK0w-Uj23Ne0tRxVbKaAE1U1qibG9fHPaKkymI2aA4r8TmbQXqy7i7qhfrDVSoVUanUGjX9MyrcdcyALr36tno633R2vUaJT3apOB01A-XVZjqsv+y2dbP4xMQHYKABhEh0ApwgieFtqBq3S4yn4AcRafTmYvK2Ra9ke94QR9yE1JpeH4KdCQAoCZQAdWQcg3mKGBoOGUC5CnWhkBEch7nYZASAKAAxZAWEoag0PA7IUDw6YWHuR5UAEYjSKoUsQSAA/manifest.json",
        )
    }

    fun clearActivation() {
        prefs.edit().clear().apply()
    }
}
