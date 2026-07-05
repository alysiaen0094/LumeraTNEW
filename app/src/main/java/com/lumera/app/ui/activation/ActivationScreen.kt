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
import androidx.compose.foundation.layout.size
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
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1B1B1B),
                        Color(0xFF070707),
                        Color.Black
                    ),
                    radius = 950f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(640.dp)
                .clip(RoundedCornerShape(34.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.11f),
                            Color.White.copy(alpha = 0.045f),
                            Color.Black.copy(alpha = 0.18f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(34.dp)
                )
                .padding(1.dp)
        ) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(33.dp))
                    .background(Color(0xFF070707).copy(alpha = 0.97f))
                    .padding(horizontal = 32.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .width(250.dp)
                        .height(70.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.94f))
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(18.dp)
                        )
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.banner),
                        contentDescription = "Lumera",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(Modifier.height(22.dp))

                AuthCodeDisplay(value = state.authCode)

                Spacer(Modifier.height(10.dp))

                ActivationStatusMessage(
                    value = state.authCode,
                    isLoading = state.isLoading,
                    error = state.error
                )

                Spacer(Modifier.height(22.dp))

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
}

@Composable
private fun AuthCodeDisplay(value: String) {
    Box(
        modifier = Modifier
            .widthIn(min = 390.dp)
            .height(62.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.13f),
                        Color.White.copy(alpha = 0.06f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.18f),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = value.ifBlank { "Enter VOD code" },
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            ),
            color = if (value.isBlank()) {
                Color.White.copy(alpha = 0.34f)
            } else {
                Color.White
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
        remaining > 0 -> "$remaining characters remaining"
        else -> "Validating code..."
    }

    val color = when {
        !error.isNullOrBlank() -> Color(0xFFFF7777)
        isLoading -> Color.White.copy(alpha = 0.70f)
        else -> Color.White.copy(alpha = 0.40f)
    }

    Box(
        modifier = Modifier
            .height(24.dp)
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
private fun TvAuthKeyboard(
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
        listOf("Z", "X", "C", "V", "B", "N", "M", "⌫")
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        rows.forEachIndexed { rowIndex, row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(9.dp),
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
        width = if (text == "⌫") 74.dp else 46.dp,
        height = 42.dp,
        focusRequester = focusRequester,
        onClick = onClick
    )
}

@Composable
private fun AuthButtonBase(
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
        targetValue = if (isFocused) 1.045f else 1f,
        label = "authKeyScale"
    )

    val background by animateColorAsState(
        targetValue = when {
            !enabled -> Color.White.copy(alpha = 0.045f)
            isFocused -> Color.White
            else -> Color.White.copy(alpha = 0.105f)
        },
        label = "authKeyBackground"
    )

    val contentColor by animateColorAsState(
        targetValue = when {
            !enabled -> Color.White.copy(alpha = 0.25f)
            isFocused -> Color.Black
            else -> Color.White.copy(alpha = 0.88f)
        },
        label = "authKeyContent"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            !enabled -> Color.Transparent
            isFocused -> Color.White
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
            .clip(RoundedCornerShape(11.dp))
            .background(background)
            .border(1.dp, borderColor, RoundedCornerShape(11.dp))
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
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            ),
            color = contentColor,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}
