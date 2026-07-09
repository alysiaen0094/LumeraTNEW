package com.lumera.app.data.identity

import android.content.Context
import android.net.Uri
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClientIdentityStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "lumera_client_identity"
        private const val KEY_INSTALL_UID = "install_uid"
        private const val TOKEN_QUERY_NAME = "token"
    }

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    @Synchronized
    fun getOrCreateInstallUid(): String {
        val existing = prefs.getString(KEY_INSTALL_UID, null)
        if (!existing.isNullOrBlank()) return existing

        val created = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_INSTALL_UID, created).apply()
        return created
    }

    fun buildToken(authCode: String): String {
        val cleanAuth = authCode.trim()
        val uid = getOrCreateInstallUid()

        val json = """{"auth":"$cleanAuth","uid":"$uid"}"""

        return Base64.encodeToString(
            json.toByteArray(Charsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
    }

    fun appendToken(rawUrl: String, authCode: String): String {
        val cleanUrl = rawUrl.trim()
        if (cleanUrl.isBlank()) return rawUrl

        val uri = runCatching { Uri.parse(cleanUrl) }.getOrNull()
            ?: return rawUrl

        return uri.buildUpon()
            .appendQueryParameter(TOKEN_QUERY_NAME, buildToken(authCode))
            .build()
            .toString()
    }
}
