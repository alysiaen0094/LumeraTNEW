package com.lumera.app.ui.activation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lumera.app.R
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
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(560.dp)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.banner),
                contentDescription = "Lumera",
                modifier = Modifier
                    .width(300.dp)
                    .height(96.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(Modifier.height(34.dp))

            AuthCodeBox(value = state.authCode)

            Spacer(Modifier.height(10.dp))

            ActivationStatusMessage(
                value = state.authCode,
                isLoading = state.isLoading,
                error = state.error
            )

            Spacer(Modifier.height(28.dp))

            ModernQwertyKeyboard(
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
private fun AuthCodeBox(value: String) {
    Box(
        modifier = Modifier
            .widthIn(min = 360.dp)
            .height(54.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.105f),
                        Color.White.copy(alpha = 0.055f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.16f),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = value.ifBlank { "Enter VOD code" },
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            ),
            color = if (value.isBlank()) {
                Color.White.copy(alpha = 0.36f)
            } else {
                Color.White.copy(alpha = 0.96f)
            },
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun ActivationStatusMessage(
    value: String,
    isLoading: Boolean,
    error: String?
) {
    val remaining = (MAX_AUTH_CODE_LENGTH - value.length).coerceAtLeast(0)

    val message = when {
        isLoading -> "Checking code..."
        !error.isNullOrBlank() -> error
        value.isBlank() -> "Use the keyboard below to enter your code"
        remaining > 0 -> "$remaining characters remaining"
        else -> "Press OK to continue"
    }

    val color = when {
        !error.isNullOrBlank() -> Color(0xFFFF6B6B)
        isLoading -> Color.White.copy(alpha = 0.70f)
        else -> Color.White.copy(alpha = 0.42f)
    }

    Box(
        modifier = Modifier
            .height(22.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = color,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun ModernQwertyKeyboard(
    value: String,
    isLoading: Boolean,
    firstKeyRequester: FocusRequester,
    onValueChange: (String) -> Unit,
    onSubmit: (String) -> Unit
) {
    val rows = listOf(
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
        listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"),
        listOf("A", "S", "D", "F", "G", "H", "J", "K", "L"),
        listOf("Z", "X", "C", "V", "B", "N", "M")
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        rows.forEachIndexed { rowIndex, row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEachIndexed { index, key ->
                    KeyboardKey(
                        text = key,
                        enabled = !isLoading,
                        width = 42.dp,
                        height = 38.dp,
                        focusRequester = if (rowIndex == 0 && index == 0) firstKeyRequester else null,
                        onClick = {
                            val next = (value + key)
                                .uppercase()
                                .filter { it.isLetterOrDigit() }
                                .take(MAX_AUTH_CODE_LENGTH)

                            onValueChange(next)

                            if (next.length == MAX_AUTH_CODE_LENGTH) {
                                onSubmit(next)
                            }
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(2.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            KeyboardKey(
                text = "CLEAR",
                enabled = !isLoading && value.isNotEmpty(),
                width = 94.dp,
                height = 38.dp,
                onClick = { onValueChange("") }
            )

            KeyboardKey(
                text = "⌫",
                enabled = !isLoading && value.isNotEmpty(),
                width = 64.dp,
                height = 38.dp,
                onClick = {
                    onValueChange(value.dropLast(1))
                }
            )

            KeyboardKey(
                text = "OK",
                enabled = !isLoading && value.length == MAX_AUTH_CODE_LENGTH,
                width = 94.dp,
                height = 38.dp,
                onClick = {
                    if (value.length == MAX_AUTH_CODE_LENGTH) {
                        onSubmit(value)
                    }
                }
            )
        }
    }
}

@Composable
private fun KeyboardKey(
    text: String,
    enabled: Boolean,
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.055f else 1f,
        label = "activationKeyScale"
    )

    val background by animateColorAsState(
        targetValue = when {
            !enabled -> Color.White.copy(alpha = 0.035f)
            isFocused -> Color.White
            else -> Color.White.copy(alpha = 0.085f)
        },
        label = "activationKeyBackground"
    )

    val textColor by animateColorAsState(
        targetValue = when {
            !enabled -> Color.White.copy(alpha = 0.22f)
            isFocused -> Color.Black
            else -> Color.White.copy(alpha = 0.86f)
        },
        label = "activationKeyText"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            !enabled -> Color.Transparent
            isFocused -> Color.White.copy(alpha = 0.95f)
            else -> Color.White.copy(alpha = 0.11f)
        },
        label = "activationKeyBorder"
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
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
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
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            ),
            color = textColor,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}
