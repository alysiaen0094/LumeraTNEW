package com.lumera.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.compose.material3.Text
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Scale
import com.lumera.app.ui.theme.LocalRoundCorners
import androidx.compose.ui.text.font.FontWeight

/**
 * ============================================================================
 * LUMERA CARD - Netflix-Grade Optimized Media Card
 * ============================================================================
 * 
 * Optimizations applied:
 * 1. AsyncImage instead of SubcomposeAsyncImage (reduces recomposition)
 * 2. Simple zIndex switch instead of per-card animation (reduces CPU overhead)
 * 3. Hardware layer only when focused (GPU acceleration where needed)
 * 4. Fixed poster size for consistent cache hits
 * 5. Minimal crossfade for smooth transitions without jank
 * ============================================================================
 */
@OptIn(ExperimentalTvMaterial3Api::class)
val LocalWatchedIds = compositionLocalOf { emptySet<String>() }

@Composable
fun LumeraCard(
    title: String,
    posterUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    progress: Float = 0f,
    isWatched: Boolean = false,
    hasNewEpisode: Boolean = false,
    onFocused: (() -> Unit)? = null
) {
    var isFocused by remember { mutableStateOf(false) }

    val glowColor = MaterialTheme.colorScheme.primary
    val roundCorners = LocalRoundCorners.current

    val cardShape =
        if (roundCorners) RoundedCornerShape(12.dp)
        else RectangleShape

    val focusedCardShape = cardShape
    val context = LocalContext.current

    val imageRequest = remember(context, posterUrl) {
        ImageRequest.Builder(context)
            .data(posterUrl)
            .crossfade(false)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .scale(Scale.FILL)
            .size(240, 360)
            .allowHardware(true)
            .build()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(2f / 3f)
            .zIndex(if (isFocused) 1f else 0f)
    ) {
        Surface(
            onClick = onClick,
            modifier = Modifier
                .fillMaxSize()
                .onFocusChanged { focusState ->
                    val becameFocused =
                        focusState.isFocused && !isFocused

                    isFocused = focusState.isFocused

                    if (becameFocused) {
                        onFocused?.invoke()
                    }
                },
            shape = ClickableSurfaceDefaults.shape(
                shape = cardShape,
                focusedShape = focusedCardShape
            ),
            scale = ClickableSurfaceDefaults.scale(
                focusedScale = 1f,
                pressedScale = 1f
            ),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surface,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                contentColor = Color.White
            ),
            border = ClickableSurfaceDefaults.border(
                focusedBorder = Border(
                    border = BorderStroke(
                        width = 2.dp,
                        color = glowColor
                    ),
                    shape = focusedCardShape
                )
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(cardShape)
                        .background(MaterialTheme.colorScheme.surface)
                )

                if (isWatched) {
                    val badgeColor =
                        MaterialTheme.colorScheme.primary

                    Canvas(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(28.dp)
                    ) {
                        val path =
                            androidx.compose.ui.graphics.Path().apply {
                                moveTo(size.width, 0f)
                                lineTo(0f, 0f)
                                lineTo(size.width, size.height)
                                close()
                            }

                        drawPath(
                            path = path,
                            color = badgeColor
                        )
                    }

                    Text(
                        text = "✓",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(
                                top = 1.dp,
                                end = 2.dp
                            )
                    )
                }

                if (hasNewEpisode) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(
                                horizontal = 6.dp,
                                vertical = 2.dp
                            )
                    ) {
                        Text(
                            text = "+1",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight =
                                androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    }
                }

                if (progress > 0f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(
                                horizontal = 6.dp,
                                vertical = 5.dp
                            )
                            .height(3.dp)
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(
                                Color.White.copy(alpha = 0.3f)
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(
                                    progress.coerceIn(0f, 1f)
                                )
                                .clip(
                                    RoundedCornerShape(1.5.dp)
                                )
                                .background(
                                    MaterialTheme.colorScheme.primary
                                )
                        )
                    }
                }
            }
        }
    }
}
