package com.lumera.app.ui.components

import android.annotation.SuppressLint
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import androidx.compose.animation.core.snap

@OptIn(ExperimentalFoundationApi::class)
class CenterPivotSpec : BringIntoViewSpec {

    @Deprecated("", level = DeprecationLevel.HIDDEN)
    override val scrollAnimationSpec = snap<Float>()

    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        val targetOffset = (containerSize / 2f) - (size / 2f)
        val scrollDelta = offset - targetOffset
        return if (abs(scrollDelta) < 1f) 0f else scrollDelta
    }
}

@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CenterCarouselRow(
    itemWidth: Dp,
    itemSpacing: Dp,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    content: LazyListScope.() -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val edgePadding = (screenWidth - itemWidth) / 2
    
    val centerPivotSpec = remember { CenterPivotSpec() }

    // Pre-scroll warmup: compose off-screen items to populate recycler + compile GPU shaders
    LaunchedEffect(Unit) {
        withFrameNanos { }
        val idx = state.firstVisibleItemIndex
        val off = state.firstVisibleItemScrollOffset
        state.scrollToItem(idx + 3)
        state.scrollToItem(idx, off)
    }

    CompositionLocalProvider(LocalBringIntoViewSpec provides centerPivotSpec) {
        LazyRow(
            state = state,
            contentPadding = PaddingValues(horizontal = edgePadding),
            horizontalArrangement = Arrangement.spacedBy(itemSpacing),
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier,
            content = content
        )
    }
}
