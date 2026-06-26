package com.lumera.app.ui.activation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
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

    val authBoxRequester = remember { FocusRequester() }
    val activateRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var showInputDialog by remember { mutableStateOf(false) }

    BackHandler { onExit() }

    LaunchedEffect(Unit) {
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
        delay(250)
        authBoxRequester.requestFocus()
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

            AuthCodeBox(
                value = state.authCode,
                focusRequester = authBoxRequester,
                onClick = {
                    keyboardController?.hide()
                    showInputDialog = true
                }
            )

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
                    activateRequester.requestFocus()
                    viewModel.validateAuthCode()
                },
                isPrimary = true,
                enabled = !state.isLoading,
                modifier = Modifier.width(260.dp),
                focusRequester = activateRequester
            )
        }
    }

    if (showInputDialog) {
        AuthCodeInputDialog(
            initialValue = state.authCode,
            onValueConfirmed = { value ->
                viewModel.updateAuthCode(value)
                showInputDialog = false
                keyboardController?.hide()
                focusManager.clearFocus(force = true)
                activateRequester.requestFocus()
            },
            onDismiss = {
                showInputDialog = false
                keyboardController?.hide()
                focusManager.clearFocus(force = true)
                activateRequester.requestFocus()
            }
        )
    }
}

@Composable
private fun AuthCodeBox(
    value: String,
    focusRequester: FocusRequester,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Box(
        modifier = Modifier
            .width(360.dp)
            .height(64.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isFocused) Color.White.copy(alpha = 0.14f)
                else Color.White.copy(alpha = 0.07f)
            )
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.16f),
                shape = RoundedCornerShape(14.dp)
            )
            .focusRequester(focusRequester)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                onClick()
            }
            .onPreviewKeyEvent { event ->
                if (
                    event.type == KeyEventType.KeyDown &&
                    (event.key == Key.Enter || event.key == Key.DirectionCenter)
                ) {
                    onClick()
                    true
                } else {
                    false
                }
            }
            .focusable(interactionSource = interactionSource),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = value.ifBlank { "Auth Code" },
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = if (value.isBlank()) Color.Gray else Color.White,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AuthCodeInputDialog(
    initialValue: String,
    onValueConfirmed: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val inputRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    var value by remember { mutableStateOf(initialValue) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .width(460.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Black)
                .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(24.dp))
                .padding(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Enter Auth Code",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(22.dp))

                Box(modifier = Modifier.width(340.dp)) {
                    VoidInput(
                        value = value,
                        onValueChange = { value = it.trim() },
                        placeholder = "Auth Code",
                        modifier = Modifier.focusRequester(inputRequester),
                        onDone = {
                            keyboardController?.hide()
                            focusManager.clearFocus(force = true)
                            onValueConfirmed(value)
                        }
                    )
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    text = "Press Enter/Done to close keyboard.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        delay(250)
        inputRequester.requestFocus()
    }
}
