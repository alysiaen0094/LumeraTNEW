package com.lumera.app.data.sync

import com.google.gson.Gson
import com.lumera.app.data.activation.ActivationManager
import com.lumera.app.data.profile.ProfileConfigurationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LumeraBackupRepository @Inject constructor(
    private val activationManager: ActivationManager,
    private val profileConfigurationManager: ProfileConfigurationManager,
    private val okHttpClient: OkHttpClient
) {
    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun pushAccountBackup(): Boolean {
        return withContext(Dispatchers.IO) {
            if (!activationManager.isActivated()) {
                return@withContext false
            }

            val userId = activationManager.getUserId()
                ?: return@withContext false

            runCatching {
                profileConfigurationManager.saveActiveRuntimeState()
            
                val backup = profileConfigurationManager.buildAccountBackup(userId)
            
                val bodyJson = JSONObject()
                    .put("userId", userId)
                    .put("payload", JSONObject(gson.toJson(backup)))
                    .toString()
            
                val request = Request.Builder()
                    .url("${ActivationManager.TROY_BASE_URL}/lumera/account-backup-push")
                    .post(bodyJson.toRequestBody(jsonMediaType))
                    .build()
            
                okHttpClient.newCall(request).execute().use { response ->
                    response.isSuccessful
                }
            }.getOrDefault(false)
        }
    }
}
