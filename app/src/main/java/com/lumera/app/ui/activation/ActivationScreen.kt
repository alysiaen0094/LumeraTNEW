package com.lumera.app.ui.activation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
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
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay

private const val MAX_AUTH_CODE_LENGTH = 8
@Composable
fun ActivationScreen(
    onActivated: () -> Unit,
    onExit: () -> Unit,
    viewModel: ActivationViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val firstKeyRequester = remember { FocusRequester() }

    BackHandler { onExit() }

    LaunchedEffect(Unit) {
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
        delay(250)
        firstKeyRequester.requestFocus()
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
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF161616),
                        Color(0xFF050505),
                        Color.Black
                    ),
                    radius = 900f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(700.dp)
                .padding(horizontal = 34.dp, vertical = 28.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(Color(0xFF080808).copy(alpha = 0.96f))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(32.dp))
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "ACTIVATE LUMERA",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "Enter your Troy auth code to continue.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.58f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(28.dp))

            AuthCodeDisplay(value = state.authCode)

            if (!state.error.isNullOrBlank()) {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = state.error.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFFF7777),
                    textAlign = TextAlign.Center
                )
            } else {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Use the remote to enter your code.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.35f),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(26.dp))

            TvAuthKeyboard(
                value = state.authCode,
                isLoading = state.isLoading,
                firstKeyRequester = firstKeyRequester,
                onValueChange = viewModel::updateAuthCode,
                onSubmit = { code -> viewModel.validateAuthCode(code) }
            )
        }
    }
}

@Composable
private fun AuthCodeDisplay(value: String) {
    Box(
        modifier = Modifier
            .widthIn(min = 420.dp)
            .height(68.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.07f))
            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(18.dp))
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = value.ifBlank { "AUTH CODE" },
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = if (value.isBlank()) Color.White.copy(alpha = 0.28f) else Color.White,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun TvAuthKeyboard(
    value: String,
    isLoading: Boolean,
    firstKeyRequester: FocusRequester,
    onValueChange: (String) -> Unit,
    onSubmit: (String) -> Unit
) {
    val rows = listOf(
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
        listOf("A", "B", "C", "D", "E", "F", "G", "H", "I", "J"),
        listOf("K", "L", "M", "N", "O", "P", "Q", "R", "S", "T"),
        listOf("U", "V", "W", "X", "Y", "Z", "⌫")
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        rows.forEachIndexed { rowIndex, row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEachIndexed { index, key ->
                    AuthKeyButton(
                        text = key,
                        enabled = !isLoading,
                        focusRequester = if (rowIndex == 0 && index == 0) firstKeyRequester else null,
                        onClick = {
                            if (key == "⌫") {
                                val next = value.dropLast(1)
                                onValueChange(next)
                            } else {
                                val next = (value + key)
                                    .uppercase()
                                    .filter { it.isLetterOrDigit() }
                                    .take(MAX_AUTH_CODE_LENGTH)

                                onValueChange(next)

                                if (next.length == MAX_AUTH_CODE_LENGTH) {
                                    onSubmit(next)
                                }
                            }
                        }
                    )
                }
            }
        }

        if (isLoading) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Checking...",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.65f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AuthKeyButton(
    text: String,
    enabled: Boolean,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit
) {
    AuthButtonBase(
        text = text,
        enabled = enabled,
        width = 54.dp,
        height = 48.dp,
        focusRequester = focusRequester,
        onClick = onClick
    )
}

@Composable
private fun AuthActionButton(
    text: String,
    enabled: Boolean,
    width: androidx.compose.ui.unit.Dp,
    isPrimary: Boolean = false,
    onClick: () -> Unit
) {
    AuthButtonBase(
        text = text,
        enabled = enabled,
        width = width,
        height = 50.dp,
        isPrimary = isPrimary,
        onClick = onClick
    )
}

@Composable
private fun AuthButtonBase(
    text: String,
    enabled: Boolean,
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    isPrimary: Boolean = false,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1f,
        label = "authKeyScale"
    )

    val background by animateColorAsState(
        targetValue = when {
            !enabled -> Color.White.copy(alpha = 0.05f)
            isFocused && isPrimary -> MaterialTheme.colorScheme.primary
            isFocused -> Color.White
            isPrimary -> MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)
            else -> Color.White.copy(alpha = 0.10f)
        },
        label = "authKeyBackground"
    )

    val contentColor by animateColorAsState(
        targetValue = when {
            !enabled -> Color.White.copy(alpha = 0.25f)
            isFocused -> Color.Black
            isPrimary -> Color.Black
            else -> Color.White.copy(alpha = 0.86f)
        },
        label = "authKeyContent"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            !enabled -> Color.Transparent
            isFocused -> Color.White
            isPrimary -> MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
            else -> Color.White.copy(alpha = 0.14f)
        },
        label = "authKeyBorder"
    )

    val requesterModifier = if (focusRequester != null) {
        Modifier.focusRequester(focusRequester)
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .scale(scale)
            .then(requesterModifier)
            .clip(RoundedCornerShape(13.dp))
            .background(background)
            .border(1.dp, borderColor, RoundedCornerShape(13.dp))
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .onPreviewKeyEvent { event ->
                if (
                    enabled &&
                    event.type == KeyEventType.KeyDown &&
                    (event.key == Key.Enter || event.key == Key.DirectionCenter)
                ) {
                    onClick()
                    true
                } else {
                    false
                }
            }
            .focusable(enabled = enabled, interactionSource = interactionSource),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = contentColor,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}
