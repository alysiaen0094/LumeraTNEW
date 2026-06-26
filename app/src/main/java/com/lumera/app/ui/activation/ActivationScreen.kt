package com.lumera.app.ui.activation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lumera.app.ui.addons.VoidButton
import com.lumera.app.ui.addons.VoidInput
import kotlinx.coroutines.delay

@Composable
fun ActivationScreen(
    onActivated: () -> Unit,
    onExit: () -> Unit,
    viewModel: ActivationViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val buttonRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    BackHandler { onExit() }

    LaunchedEffect(Unit) {
        delay(150)
        buttonRequester.requestFocus()
    }

    LaunchedEffect(state.activated) {
        if (state.activated) {
            keyboardController?.hide()
            focusManager.clearFocus(force = true)
            onActivated()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(560.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF080808))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(28.dp))
                .padding(horizontal = 40.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "ACTIVATE LUMERAT",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(18.dp))

            Text(
                text = "Enter your Troy auth code to continue.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(30.dp))

            Box(modifier = Modifier.width(360.dp)) {
                VoidInput(
                    value = state.authCode,
                    onValueChange = { viewModel.updateAuthCode(it) },
                    placeholder = "Auth Code",
                    onDone = {
                        keyboardController?.hide()
                        focusManager.clearFocus(force = true)
                    }
                )
            }

            if (!state.error.isNullOrBlank()) {
                Spacer(Modifier.height(18.dp))
                Text(
                    text = state.error ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFFF7777),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(30.dp))

            VoidButton(
                text = if (state.isLoading) "Checking..." else "Activate",
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus(force = true)
                    viewModel.validateAuthCode()
                },
                isPrimary = true,
                enabled = !state.isLoading,
                modifier = Modifier.width(260.dp),
                focusRequester = buttonRequester
            )
        }
    }
}
