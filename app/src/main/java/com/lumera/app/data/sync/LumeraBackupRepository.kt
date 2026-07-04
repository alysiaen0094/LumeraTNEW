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
import android.util.Log


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
            Log.d("LumeraBackup", "pushAccountBackup called")
    
            if (!activationManager.isActivated()) {
                Log.d("LumeraBackup", "Skip backup: not activated")
                return@withContext false
            }
    
            val userId = activationManager.getUserId()
            if (userId.isNullOrBlank()) {
                Log.d("LumeraBackup", "Skip backup: missing userId")
                return@withContext false
            }
    
            runCatching {
                profileConfigurationManager.saveActiveRuntimeState()
    
                val backup = profileConfigurationManager.buildAccountBackup(userId)
    
                Log.d(
                    "LumeraBackup",
                    "Built backup user=$userId profiles=${backup.profiles.size} watchHistory=${
                        backup.profiles.sumOf { it.snapshot.watchHistory.size }
                    }"
                )
    
                val bodyJson = JSONObject()
                    .put("userId", userId)
                    .put("payload", JSONObject(gson.toJson(backup)))
                    .toString()
    
                Log.d("LumeraBackup", "Posting backup bytes=${bodyJson.length}")
    
                val request = Request.Builder()
                    .url("${ActivationManager.TROY_BASE_URL}/lumera/account-backup-push")
                    .post(bodyJson.toRequestBody(jsonMediaType))
                    .build()
    
                okHttpClient.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string().orEmpty()
                    Log.d(
                        "LumeraBackup",
                        "Backup response code=${response.code} body=$responseBody"
                    )
                    response.isSuccessful
                }
            }.onFailure { err ->
                Log.e("LumeraBackup", "Backup failed", err)
            }.getOrDefault(false)
        }
    }
}
