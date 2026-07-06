package com.lumera.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Scale
import com.lumera.app.ui.theme.LocalRoundCorners

/**
 * ============================================================================
 * LUMERA LANDSCAPE CARD - Continue Watching Landscape Mode
 * ============================================================================
 *
 * Displays a 16:9 landscape card with:
 * - Hero/backdrop image (falls back to poster)
 * - Gradient scrim at bottom for readability
 * - Logo overlay in bottom-left (falls back to text title)
 * - Progress bar at bottom
 *
 * Matches horizontal hub card sizing (190dp wide, 16:9 aspect).
 * ============================================================================
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LumeraLandscapeCard(
    title: String,
    backdropUrl: String?,
    logoUrl: String?,
    posterUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    progress: Float = 0f,
    hasNewEpisode: Boolean = false,
    onFocused: (() -> Unit)? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    val glowColor = MaterialTheme.colorScheme.primary
    val roundCorners = LocalRoundCorners.current

    val cardShape = if (roundCorners) RoundedCornerShape(14.dp) else RectangleShape
    val focusedCardShape = if (roundCorners) RoundedCornerShape(18.dp) else RectangleShape

    Box(
        modifier = modifier
            .width(225.dp)
            .aspectRatio(16f / 9f)
            .zIndex(if (isFocused) 10f else 0f)
            .graphicsLayer { clip = false }
    ) {
        Surface(
            onClick = onClick,
            modifier = Modifier
                .fillMaxSize()
                .onFocusChanged {
                    isFocused = it.isFocused
                    if (it.isFocused) onFocused?.invoke()
                },
            shape = ClickableSurfaceDefaults.shape(
                shape = cardShape,
                focusedShape = focusedCardShape
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.055f),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surface,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                contentColor = Color.White
            ),
            border = ClickableSurfaceDefaults.border(
                focusedBorder = Border(
                    border = BorderStroke(2.dp, glowColor),
                    shape = focusedCardShape
                )
            )
        ) {
            val context = LocalContext.current
            val imageUrl = backdropUrl ?: posterUrl

            val imageRequest = remember(imageUrl) {
                ImageRequest.Builder(context)
                    .data(imageUrl)
                    .crossfade(false)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .scale(Scale.FILL)
                    .size(450, 254)
                    .allowHardware(true)
                    .build()
            }

            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(cardShape)
                        .background(MaterialTheme.colorScheme.surface)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.70f)
                        .align(Alignment.BottomStart)
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color.Transparent,
                                    0.22f to Color.Black.copy(alpha = 0.10f),
                                    0.45f to Color.Black.copy(alpha = 0.38f),
                                    0.72f to Color.Black.copy(alpha = 0.68f),
                                    1.0f to Color.Black.copy(alpha = 0.88f)
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(
                            start = 13.dp,
                            end = 13.dp,
                            bottom = if (progress > 0f) 16.dp else 12.dp
                        )
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            lineHeight = 17.sp
                        ),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                
                    val subtitle = posterUrl
                    if (!subtitle.isNullOrBlank() && subtitle.startsWith("S")) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Normal,
                                fontSize = 11.sp,
                                lineHeight = 13.sp
                            ),
                            color = Color.White.copy(alpha = 0.78f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (hasNewEpisode) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(9.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        androidx.compose.material3.Text(
                            "+1",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    }
                }

                if (progress > 0f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(Color.White.copy(0.28f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progress.coerceIn(0f, 1f))
                                .clip(RoundedCornerShape(1.5.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }
        }
    }
}
