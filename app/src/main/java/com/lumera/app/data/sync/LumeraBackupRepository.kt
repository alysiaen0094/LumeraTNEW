package com.lumera.app.data.sync

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.lumera.app.data.activation.ActivationManager
import com.lumera.app.data.profile.LumeraAccountBackup
import com.lumera.app.data.profile.ProfileConfigurationManager
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @ApplicationContext private val context: Context,
    private val activationManager: ActivationManager,
    private val profileConfigurationManager: ProfileConfigurationManager,
    private val okHttpClient: OkHttpClient
) {
    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val restorePrefs by lazy {
        context.getSharedPreferences("lumera_restore_prefs", Context.MODE_PRIVATE)
    }

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

    suspend fun restoreAccountBackupOnceForActivatedUser(): Boolean {
        return withContext(Dispatchers.IO) {
            if (!activationManager.isActivated()) {
                Log.d("LumeraBackup", "Skip restore: not activated")
                return@withContext false
            }

            val userId = activationManager.getUserId()?.trim().orEmpty()
            if (userId.isBlank()) {
                Log.d("LumeraBackup", "Skip restore: missing userId")
                return@withContext false
            }

            val alreadyRestoredUserId = restorePrefs.getString(KEY_RESTORE_DONE_USER_ID, null)
            if (alreadyRestoredUserId == userId) {
                Log.d("LumeraBackup", "Skip restore: already restored user=$userId")
                return@withContext false
            }

            val restored = pullAndApplyAccountBackup(userId)

            // Mark as done even when no backup exists, so we do not keep pulling forever.
            restorePrefs.edit()
                .putString(KEY_RESTORE_DONE_USER_ID, userId)
                .apply()

            restored
        }
    }

    suspend fun pullAndApplyAccountBackup(userId: String): Boolean {
        return withContext(Dispatchers.IO) {
            val cleanUserId = userId.trim()
            if (cleanUserId.isBlank()) {
                return@withContext false
            }

            runCatching {
                val bodyJson = JSONObject()
                    .put("userId", cleanUserId)
                    .toString()

                val request = Request.Builder()
                    .url("${ActivationManager.TROY_BASE_URL}/lumera/account-backup-pull")
                    .post(bodyJson.toRequestBody(jsonMediaType))
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string().orEmpty()

                    Log.d(
                        "LumeraBackup",
                        "Restore response code=${response.code} bodyBytes=${responseBody.length}"
                    )

                    if (!response.isSuccessful || responseBody.isBlank()) {
                        return@withContext false
                    }

                    val root = JSONObject(responseBody)

                    if (!root.optBoolean("ok", false)) {
                        Log.d("LumeraBackup", "Restore response not ok: $responseBody")
                        return@withContext false
                    }

                    if (root.isNull("payload")) {
                        Log.d("LumeraBackup", "No backup payload found for user=$cleanUserId")
                        return@withContext false
                    }

                    val payloadJson = root.optJSONObject("payload")
                    if (payloadJson == null) {
                        Log.d("LumeraBackup", "Backup payload is not object for user=$cleanUserId")
                        return@withContext false
                    }

                    val backup = gson.fromJson(
                        payloadJson.toString(),
                        LumeraAccountBackup::class.java
                    )

                    if (backup == null || backup.profiles.isEmpty()) {
                        Log.d("LumeraBackup", "Backup has no profiles for user=$cleanUserId")
                        return@withContext false
                    }

                    val restored = profileConfigurationManager.restoreAccountBackup(backup)

                    Log.d(
                        "LumeraBackup",
                        "Applied backup restored=$restored user=$cleanUserId profiles=${backup.profiles.size} watchHistory=${
                            backup.profiles.sumOf { it.snapshot.watchHistory.size }
                        }"
                    )

                    restored
                }
            }.onFailure { err ->
                Log.e("LumeraBackup", "Restore failed", err)
            }.getOrDefault(false)
        }
    }

    fun clearRestoreMarker() {
        restorePrefs.edit()
            .remove(KEY_RESTORE_DONE_USER_ID)
            .apply()
    }

    private companion object {
        const val KEY_RESTORE_DONE_USER_ID = "lumera_restore_done_user_id"
    }
}
