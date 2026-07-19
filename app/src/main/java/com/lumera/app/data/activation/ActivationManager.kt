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
            "https://tmdb.elfhosted.com/N4IgTgDgJgRgsgUygSwIYBUCeEEGcQBcoEA9rgC4JiHlgCuCANCADYkDmJhAZqi7kxAxUAYwDWUMCQg8+AgL7MIUgG7IoCAJIBbWJqiEQtBiGYtUAO3Z1U7BIYQWAtAFUAyqZAUwCVNuRWhADaALrMIqjkfBz4BEGg6obkujAAdOaUFJ7k2PYEINokavbMFn55IAAykXjkAAQASggsvgL4zLgAFiQA7poWABIk2nnGCIoJBvnJsOk1Wcw5OIYCYMh4nmUjhtWZ9U0tqG2eXb39Q9sEYxMgidMpqbSOKIGLuYaFxZvlhug+Fi92Cdun1BsNRvRxoxJkkHk8AQEgW9lvlVut2iAthU-s9EcCzmDLtdobcpkY4dJsu98p91t9LiAAArSOjmagdEHncE0SE3O7k2bkSnIipojalH75ZkQVmodleTmEiEMPlkmZpTC+eVLCq0kqYyUgACaWvxoIuyqhMPus01cqpKK8VHR9IqJvtHIJFp5KpJ-PVcysNjsDt1RTpEoZ1SDtn1p3N3KuvL9aoe5hjIZFK2d4oNUcs1ljZq5RN5YRAEQoAGESHQLORCABWT09ACCdgakUR-QA4o4fLEjJDPI5UDAWu2EJ3yHj8rx+HGQZPp4iAOrIcidHSwFeBOdyOO0ZAichNdjIEgWABiyBYlGo+4XnhQ56iLAOrQQN7vVB94yAA/manifest.json",
        )
    }

    fun clearActivation() {
        prefs.edit().clear().apply()
    }
}
