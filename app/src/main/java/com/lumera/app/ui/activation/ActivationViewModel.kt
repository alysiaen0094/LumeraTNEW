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

data class ActivationUiState(
    val authCode: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val activated: Boolean = false
)

@HiltViewModel
class ActivationViewModel @Inject constructor(
    private val activationManager: ActivationManager,
    private val addonRepository: AddonRepository
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

            val manifestUrl = "${ActivationManager.TROY_BASE_URL}/$userId/manifest.json"

            val isValid = withContext(Dispatchers.IO) {
                runCatching {
                    addonRepository.fetchManifest(manifestUrl)
                }.isSuccess
            }

            if (isValid) {
                activationManager.markActivated(userId)

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
}
