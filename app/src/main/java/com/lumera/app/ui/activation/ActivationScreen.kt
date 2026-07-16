package com.lumera.app.ui.activation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val firstKeyRequester = remember { FocusRequester() }

    BackHandler { onExit() }

    LaunchedEffect(Unit) {
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
        delay(40)
        firstKeyRequester.requestFocus()
    }

    LaunchedEffect(state.activated) {
        if (state.activated) {
            keyboardController?.hide()
            onActivated()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF182033),
                        Color(0xFF0B1020),
                        Color(0xFF05070D)
                    ),
                    center = Offset(0.22f, 0.14f),
                    radius = 1250f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            Color.Transparent
                        ),
                        center = Offset(0.82f, 0.20f),
                        radius = 900f
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.025f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.18f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .width(660.dp)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Enter Activation Code",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 30.sp,
                    letterSpacing = 0.2.sp
                ),
                color = Color.White.copy(alpha = 0.96f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(9.dp))

            Text(
                text = "Use the code provided with your account to continue.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = Color.White.copy(alpha = 0.58f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(30.dp))

            AuthCodeInput(value = state.authCode)

            Spacer(Modifier.height(12.dp))

            ActivationStatusMessage(
                value = state.authCode,
                isLoading = state.isLoading,
                error = state.error
            )

            Spacer(Modifier.height(32.dp))

            ActivationKeyboard(
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
private fun AuthCodeInput(value: String) {
    Row(
        modifier = Modifier.height(72.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(MAX_AUTH_CODE_LENGTH) { index ->
            val char = value.getOrNull(index)?.toString().orEmpty()
            val isActive = index == value.length.coerceAtMost(MAX_AUTH_CODE_LENGTH - 1)
            val hasValue = char.isNotEmpty()

            Box(
                modifier = Modifier
                    .width(52.dp)
                    .height(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Color.White.copy(
                            alpha = when {
                                hasValue -> 0.16f
                                isActive -> 0.12f
                                else -> 0.075f
                            }
                        )
                    )
                    .border(
                        width = if (isActive) 2.dp else 1.dp,
                        color = if (isActive) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.95f)
                        } else {
                            Color.White.copy(alpha = 0.16f)
                        },
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = char,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 27.sp,
                        letterSpacing = 0.sp
                    ),
                    color = Color.White.copy(alpha = if (hasValue) 0.98f else 0.22f),
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
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
        value.isBlank() -> "Waiting for code"
        remaining > 0 -> "$remaining characters remaining"
        else -> "Checking code..."
    }

    val color = when {
        !error.isNullOrBlank() -> Color(0xFFFF7474)
        isLoading -> Color.White.copy(alpha = 0.74f)
        else -> Color.White.copy(alpha = 0.42f)
    }

    Box(
        modifier = Modifier
            .height(24.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal
            ),
            color = color,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ActivationKeyboard(
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
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        rows.forEachIndexed { rowIndex, row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEachIndexed { index, key ->
                    val isBackspace = key == "⌫"

                    ActivationKeyButton(
                        text = key,
                        enabled = !isLoading && (value.isNotEmpty() || !isBackspace),
                        width = if (isBackspace) 78.dp else 50.dp,
                        height = 46.dp,
                        focusRequester = if (rowIndex == 0 && index == 0) firstKeyRequester else null,
                        onClick = {
                            if (isBackspace) {
                                onValueChange(value.dropLast(1))
                                return@ActivationKeyButton
                            }

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
    }
}

@Composable
private fun ActivationKeyButton(
    text: String,
    enabled: Boolean,
    width: Dp,
    height: Dp,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val background by animateColorAsState(
        targetValue = when {
            !enabled -> Color.White.copy(alpha = 0.035f)
            isFocused -> Color.White.copy(alpha = 0.92f)
            else -> Color.White.copy(alpha = 0.085f)
        },
        animationSpec = snap(),
        label = "activationKeyBackground"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            !enabled -> Color.Transparent
            isFocused -> MaterialTheme.colorScheme.primary.copy(alpha = 0.95f)
            else -> Color.White.copy(alpha = 0.12f)
        },
        animationSpec = snap(),
        label = "activationKeyBorder"
    )

    val contentColor by animateColorAsState(
        targetValue = when {
            !enabled -> Color.White.copy(alpha = 0.18f)
            isFocused -> Color.Black.copy(alpha = 0.96f)
            else -> Color.White.copy(alpha = 0.86f)
        },
        animationSpec = snap(),
        label = "activationKeyContent"
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
            .then(requesterModifier)
            .graphicsLayer {
                scaleX = if (isFocused) 1.035f else 1f
                scaleY = if (isFocused) 1.035f else 1f
            }
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
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
                fontWeight = FontWeight.SemiBold,
                fontSize = if (text == "⌫") 19.sp else 15.sp,
                letterSpacing = 0.35.sp
            ),
            color = contentColor,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}
