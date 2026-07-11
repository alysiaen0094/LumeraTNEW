package com.lumera.app.ui.activation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumera.app.data.activation.ActivationManager
import com.lumera.app.data.repository.AddonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import com.lumera.app.data.sync.LumeraBackupRepository

import com.lumera.app.data.identity.ClientIdentityStore
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

data class ActivationUiState(
    val authCode: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val activated: Boolean = false
)

@HiltViewModel
class ActivationViewModel @Inject constructor(
    private val activationManager: ActivationManager,
    private val addonRepository: AddonRepository,
    private val lumeraBackupRepository: LumeraBackupRepository,
    private val clientIdentityStore: ClientIdentityStore,
    private val okHttpClient: OkHttpClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ActivationUiState(
            activated = activationManager.isActivated()
        )
    )
    val uiState: StateFlow<ActivationUiState> = _uiState

    fun updateAuthCode(value: String) {
        val cleaned = value
            .trim()
            .uppercase()
            .filter { it.isLetterOrDigit() }
            .take(8)

        _uiState.value = _uiState.value.copy(
            authCode = cleaned,
            error = null
        )
    }

    fun validateAuthCode(codeOverride: String? = null) {
        val userId = (codeOverride ?: _uiState.value.authCode)
            .trim()
            .uppercase()
            .filter { it.isLetterOrDigit() }
            .take(8)
    
        if (userId.length != 8) {
            _uiState.value = _uiState.value.copy(
                authCode = userId,
                error = "Enter 8 character auth code"
            )
            return
        }
    
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                authCode = userId,
                isLoading = true,
                error = null
            )
    
            val activationResult = withContext(Dispatchers.IO) {
                runCatching {
                    activateWithVod(userId)
                }.getOrDefault(false)
            }
    
            if (activationResult) {
                activationManager.markActivated(userId)
    
                withContext(Dispatchers.IO) {
                    lumeraBackupRepository.restoreAccountBackupOnceForActivatedUser()
                }
    
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    activated = true,
                    error = null
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    activated = false,
                    error = "Auth code invalid or expired"
                )
            }
        }
    }

    private fun activateWithVod(authCode: String): Boolean {
        val token = clientIdentityStore.buildToken(authCode)
    
        val payload = JSONObject()
            .put("auth", authCode)
            .put("token", token)
            .toString()
    
        val request = Request.Builder()
            .url("${ActivationManager.TROY_BASE_URL}/api/activate")
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()
    
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return false
    
            val body = response.body?.string().orEmpty()
            if (body.isBlank()) return false
    
            val json = JSONObject(body)
    
            return json.optBoolean("activated", false)
        }
    }
}
