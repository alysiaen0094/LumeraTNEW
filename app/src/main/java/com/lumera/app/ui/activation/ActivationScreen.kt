package com.lumera.app.ui.activation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lumera.app.ui.addons.VoidButton
import kotlinx.coroutines.delay

@Composable
fun ActivationScreen(
    onActivated: () -> Unit,
    onExit: () -> Unit,
    viewModel: ActivationViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val activateRequester = remember { FocusRequester() }

    BackHandler { onExit() }

    LaunchedEffect(Unit) {
        delay(150)
        activateRequester.requestFocus()
    }

    LaunchedEffect(state.activated) {
        if (state.activated) {
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
                    fontWeight = FontWeight.Bold,
                    letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified
                ),
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(18.dp))

            Text(
                text = "Enter this code on your Troy activation page.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(18.dp))
                    .padding(horizontal = 40.dp, vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = state.activationCode,
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center
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

            Spacer(Modifier.height(36.dp))

            VoidButton(
                text = if (state.isLoading) "Checking..." else "Activate Test",
                onClick = { viewModel.activateForTest() },
                isPrimary = true,
                enabled = !state.isLoading,
                modifier = Modifier.width(260.dp),
                focusRequester = activateRequester
            )
        }
    }
}
