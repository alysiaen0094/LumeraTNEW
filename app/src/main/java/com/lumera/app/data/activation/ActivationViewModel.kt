package com.lumera.app.ui.activation

import androidx.lifecycle.ViewModel
import com.lumera.app.data.activation.ActivationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID
import javax.inject.Inject

data class ActivationUiState(
    val activationCode: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val activated: Boolean = false
)

@HiltViewModel
class ActivationViewModel @Inject constructor(
    private val activationManager: ActivationManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ActivationUiState(
            activationCode = generateCode(),
            activated = activationManager.isActivated()
        )
    )
    val uiState: StateFlow<ActivationUiState> = _uiState

    fun activateForTest() {
        val code = _uiState.value.activationCode
        activationManager.markActivated(code)
        _uiState.value = _uiState.value.copy(
            activated = true,
            isLoading = false,
            error = null
        )
    }

    private fun generateCode(): String {
        return UUID.randomUUID()
            .toString()
            .replace("-", "")
            .take(6)
            .uppercase()
    }
}
