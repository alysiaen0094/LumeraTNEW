package com.lumera.app.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.lumera.app.R
import com.lumera.app.data.model.ProfileEntity
import com.lumera.app.data.model.WatchHistoryEntity
import com.lumera.app.data.model.stremio.MetaItem
import com.lumera.app.domain.DashboardTab
import com.lumera.app.domain.HeroConfig
import com.lumera.app.domain.HubGroupRow
import com.lumera.app.domain.CategoryRow
import com.lumera.app.domain.HomeRowItem
import com.lumera.app.domain.heroFor
import com.lumera.app.domain.layoutFor
import com.lumera.app.ui.components.LumeraBackground
import com.lumera.app.ui.components.LumeraCard
import kotlinx.coroutines.delay

private const val RAPID_VERTICAL_NAV_WINDOW_MS = 220L
private const val RAPID_PREVIEW_UPDATE_MIN_INTERVAL_MS = 120L
private const val DPAD_REPEAT_INTERVAL_HORIZONTAL_MS = 90L
private const val DPAD_REPEAT_INTERVAL_VERTICAL_MS = 120L

private class HomeFocusTimingTracker {
    var previous: Long = 0L
    var current: Long = 0L

    fun mark(now: Long = System.currentTimeMillis()) {
        previous = current
        current = now
    }

    fun isRapid(windowMs: Long): Boolean = current - previous in 1..windowMs
}

private class PreviewUpdateGate {
    var lastUpdateMs: Long = 0L
}

@Composable
fun HomeScreen(
    tab: DashboardTab,
    entryRequester: FocusRequester,
    drawerRequester: FocusRequester,
    viewModel: HomeViewModel = hiltViewModel(),
    currentProfile: ProfileEntity?,
    onMovieClick: (MetaItem) -> Unit,
    onViewMore: (String, List<MetaItem>, String) -> Unit = { _, _, _ -> }
) {
    val state by viewModel.state.collectAsState()
    val layoutMode = currentProfile?.layoutFor(tab) ?: "simple"
    val isTopNav = currentProfile?.navPosition == "top"
    val isLandscapeContinueWatching = currentProfile?.continueWatchingShape == "landscape"
    val infoTopPadding = if (isTopNav) 84.dp else 54.dp
    val startPadding = if (isTopNav) 50.dp else 88.dp

    // Avoid rendering heavy rows during navigation transition.
    var isTransitioning by remember { mutableStateOf(true) }

    LaunchedEffect(tab) {
        isTransitioning = true
        delay(250)
        isTransitioning = false
    }

    val screenName = remember(tab) {
        when (tab) {
            DashboardTab.HOME -> "home"
            DashboardTab.MOVIES -> "movies"
            DashboardTab.SERIES -> "series"
        }
    }

    // During tab switch, ignore persisted focus/scroll until this tab is actually loaded.
    // This avoids one-frame carry-over from the previous tab.
    val isCurrentTabLoaded = state.loadedScreen == screenName && state.loadedProfileId == currentProfile?.id
    val lastFocusedKey = if (isCurrentTabLoaded) state.lastFocusedKey else null
    val rowScrollPositions = if (isCurrentTabLoaded) viewModel.getRowScrollPositions() else emptyMap()
    val verticalScrollPosition = if (isCurrentTabLoaded) viewModel.getVerticalScrollPosition() else Pair(0, 0)

    // Track if content has focus to conditionally enable BackHandler
    var isContentFocused by remember { mutableStateOf(false) }
    // Guards against double-back race: when returning from details, focus restoration
    // takes ~200ms. Until focus is established, keep BackHandler enabled so a quick
    // second back press doesn't exit the app. Resets on each fresh composition.
    var focusEverSet by remember { mutableStateOf(false) }

    // Only enable explicit back handling if:
    // 1. We are in Side-Nav mode (Always handle)
    // 2. We are in Top-Nav mode AND content is focused (Handle = Open Nav)
    // 3. Top-Nav mode AND focus not yet established (transition guard)
    // If Top-Nav mode AND content is NOT focused AND focus was already set, disable this
    // handler so TopNavigationBar's handler can "Close Nav" (return to content).
    BackHandler(enabled = !isTopNav || isContentFocused || !focusEverSet) {
        drawerRequester.requestFocus()
    }

    Box(
    modifier = Modifier
        .fillMaxSize()
        .onFocusChanged {
            isContentFocused = it.hasFocus
            if (it.hasFocus) focusEverSet = true
        }
    ) {
        LumeraBackground {
            CompositionLocalProvider(com.lumera.app.ui.components.LocalWatchedIds provides state.watchedIds) {
        // LOGIC: If we are just starting OR the ViewModel is loading, show the Loading Box.
        // This box accepts focus immediately, which forces the NavDrawer to collapse.
        if (
            isTransitioning ||
            state.isLoading ||
            state.loadedScreen != screenName ||
            state.loadedProfileId != currentProfile?.id
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(entryRequester)
                    .focusable()
            ) {
            }
        } else {
            // DATA IS READY: Show Content
            val hasInProgressHistory = remember(state.history, state.seriesNextUp) {
                state.history.any { !it.watched } || state.seriesNextUp.any { !it.isComplete }
            }
            if (layoutMode == "cinematic") {
                CinematicLayout(
                    infoTopPadding = infoTopPadding,
                    startPadding = startPadding,
                    isTopNav = isTopNav,
                    state = state,
                    onMovieClick = onMovieClick,
                    onViewMore = onViewMore,
                    onHubClick = { hubItem ->
                        viewModel.openHub(hubItem) { title, items ->
                            onViewMore(title, items, hubItem.categoryId)
                        }
                    },
                    onLoadMore = { configId -> viewModel.loadMoreItems(configId) },
                    entryRequester = entryRequester,
                    drawerRequester = drawerRequester,
                    lastFocusedKey = lastFocusedKey,
                    rowScrollPositions = rowScrollPositions,
                    verticalScrollPosition = verticalScrollPosition,
                    historyScrollAdjustment = if (viewModel.needsHistoryScrollAdjustment(hasInProgressHistory)) 1 else 0,
                    onFocusChange = { viewModel.setLastFocusedKey(it) },
                    onScrollPositionChange = { key, pos -> viewModel.setRowScrollPosition(key, pos) },
                    onVerticalScrollChange = { viewModel.setVerticalScrollPosition(it, hasInProgressHistory) },
                    onPreviewItemVisible = { viewModel.ensureMetadataFallback(it); viewModel.ensureTmdbEnrichment(it) },
                    isLandscapeContinueWatching = isLandscapeContinueWatching
                )
            } else {
                val heroConfig = currentProfile?.heroFor(tab) ?: HeroConfig(null, 10, 0)
                val heroItems = remember(state.heroRow, heroConfig.posterCount) {
                    state.heroRow?.items?.take(heroConfig.posterCount) ?: emptyList()
                }

                SimpleLayout(
                    startPadding = startPadding,
                    isTopNav = isTopNav,
                    state = state,
                    heroItems = heroItems,
                    heroAutoScrollSeconds = heroConfig.autoScrollSeconds,
                    onMovieClick = onMovieClick,
                    onViewMore = onViewMore,
                    onHubClick = { hubItem ->
                        viewModel.openHub(hubItem) { title, items ->
                            onViewMore(title, items, hubItem.categoryId)
                        }
                    },
                    onLoadMore = { configId -> viewModel.loadMoreItems(configId) },
                    entryRequester = entryRequester,
                    drawerRequester = drawerRequester,
                    lastFocusedKey = lastFocusedKey,
                    rowScrollPositions = rowScrollPositions,
                    verticalScrollPosition = verticalScrollPosition,
                    historyScrollAdjustment = if (
                        viewModel.needsHistoryScrollAdjustment(hasInProgressHistory) &&
                        (heroItems.isEmpty() || verticalScrollPosition.first > 0)
                    ) 1 else 0,
                    onFocusChange = { viewModel.setLastFocusedKey(it) },
                    onScrollPositionChange = { key, pos -> viewModel.setRowScrollPosition(key, pos) },
                    onVerticalScrollChange = { viewModel.setVerticalScrollPosition(it, hasInProgressHistory) },
                    onHeroItemVisible = { viewModel.ensureMetadataFallback(it); viewModel.ensureTmdbEnrichment(it) },
                    isLandscapeContinueWatching = isLandscapeContinueWatching
                )
            }
        }
        } // CompositionLocalProvider
    }
}
}

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CinematicLayout(
    infoTopPadding: androidx.compose.ui.unit.Dp,
    startPadding: androidx.compose.ui.unit.Dp,
    isTopNav: Boolean,
    state: HomeViewModel.HomeState,
    onMovieClick: (MetaItem) -> Unit,
    onViewMore: (String, List<MetaItem>, String) -> Unit,
    onHubClick: (com.lumera.app.domain.HubItem) -> Unit,
    onLoadMore: (String) -> Unit,
    entryRequester: FocusRequester,
    drawerRequester: FocusRequester,
    lastFocusedKey: String?,
    rowScrollPositions: Map<String, Pair<Int, Int>>,
    verticalScrollPosition: Pair<Int, Int>,
    historyScrollAdjustment: Int,
    onFocusChange: (String) -> Unit,
    onScrollPositionChange: (String, Pair<Int, Int>) -> Unit,
    onVerticalScrollChange: (Pair<Int, Int>) -> Unit,
    onPreviewItemVisible: (MetaItem) -> Unit,
    isLandscapeContinueWatching: Boolean = false
) {
    var instantFocusItem by remember { mutableStateOf<MetaItem?>(null) }
    var displayedItem by remember { mutableStateOf<MetaItem?>(null) }
    var hasRequestedFocus by remember { mutableStateOf(false) }
    val previewUpdateGate = remember { PreviewUpdateGate() }

    // Cache the history items transformation to avoid allocating new list on every recomposition
    // Stable key: only rebuild when items/order/watched-state change, not when images update
    val historyKey = remember(state.history) {
        state.history.map { "${it.id}:${it.watched}:${it.lastWatched}:${it.poster != null}" }
    }
    val nextUpKey = remember(state.seriesNextUp) {
        state.seriesNextUp.map { "${it.seriesId}:${it.isComplete}:${it.nextSeason}:${it.nextEpisode}:${it.updatedAt}:${it.poster != null}" }
    }
    val historyItems = remember(historyKey, nextUpKey) {
        buildContinueWatchingItems(state.history, state.seriesNextUp)
    }

    // Proactive metadata fetch for continue watching cards (posters + landscape)
    LaunchedEffect(historyItems) {
        historyItems.take(15).forEach { item ->
            onPreviewItemVisible(previewLookupItemForFocus(item, historyItems))
        }
    }

    // Redirect stale continue-watching focus key after progress was cleared
    val effectiveLastFocusedKey = remember(lastFocusedKey, historyItems) {
        resolveEffectiveFocusKey(lastFocusedKey, historyItems)
    }
    val historyFocusRedirected = effectiveLastFocusedKey != lastFocusedKey

    val restoredPreviewItem = remember(effectiveLastFocusedKey, state.mixedRows, historyItems) {
        resolveCinematicPreviewItem(
            lastFocusedKey = effectiveLastFocusedKey,
            mixedRows = state.mixedRows,
            historyItems = historyItems
        )
    }
    val renderedPreviewItem = remember(displayedItem, state.mixedRows, state.heroRow, historyItems, state.enrichedMeta) {
        resolveLatestPreviewItem(
            current = displayedItem,
            state = state,
            historyItems = historyItems
        )
    }

    LaunchedEffect(state.rows, state.history, restoredPreviewItem, effectiveLastFocusedKey) {
        if (instantFocusItem == null) {
            val first = when {
                restoredPreviewItem != null -> restoredPreviewItem
                // Returning from details with a saved focus key: avoid showing the wrong
                // poster while focus restoration is still in progress.
                effectiveLastFocusedKey != null -> null
                else -> historyItems.firstOrNull() ?: state.rows.firstOrNull()?.items?.firstOrNull()
            }
            instantFocusItem = first
            displayedItem = first
            if (first != null) {
                onPreviewItemVisible(previewLookupItemForFocus(first, historyItems))
            }
        }

        // SMART FOCUS:
        // We are now safe to request focus because HomeScreen ensures we only reach here when data is ready.
        if (!hasRequestedFocus && (historyItems.isNotEmpty() || state.mixedRows.isNotEmpty())) {
            delay(100)
            entryRequester.requestFocus()
            hasRequestedFocus = true
        }
    }

    LaunchedEffect(instantFocusItem) {
        val target = instantFocusItem ?: return@LaunchedEffect
        delay(300)
        displayedItem = target
    }

    val density = LocalDensity.current
    // Adjust pivot with 40dp offset to prevent slide-up (5.dp content padding + 40.dp)
    val titleHeadroomPx = remember(density) { with(density) { 45.dp.toPx() } }
    
    // Skip vertical scroll on restoration (when we have a saved focus key)
    val isVerticalRestoration = effectiveLastFocusedKey != null
    var skipVerticalScroll by remember { mutableStateOf(isVerticalRestoration) }

    // Reset skip flag after restoration is complete
    LaunchedEffect(isVerticalRestoration) {
        if (isVerticalRestoration) {
            delay(300)
            skipVerticalScroll = false
        }
    }

    // Track focus timing without triggering recomposition on every focus hop.
    val verticalFocusTiming = remember { HomeFocusTimingTracker() }

    val verticalPivot = remember(titleHeadroomPx) {
        FocusPivotSpec(
            customOffset = titleHeadroomPx,
            skipScrollProvider = { skipVerticalScroll },
            stiffnessProvider = { Spring.StiffnessMediumLow }
        )
    }
    
    // Wrap onFocusChange to track timing for vertical dynamic stiffness
    val wrappedOnFocusChange: (String) -> Unit = remember(onFocusChange) {
        { key ->
            verticalFocusTiming.mark()
            onFocusChange(key)
        }
    }

    fun updatePreviewItem(item: MetaItem?) {
    if (item == null) return

    val now = System.currentTimeMillis()
    val isRapid = verticalFocusTiming.isRapid(RAPID_VERTICAL_NAV_WINDOW_MS)
    val allowUpdate = !isRapid || now - previewUpdateGate.lastUpdateMs >= RAPID_PREVIEW_UPDATE_MIN_INTERVAL_MS

    if (!allowUpdate) return

    onPreviewItemVisible(previewLookupItemForFocus(item, historyItems))

    if (instantFocusItem?.id != item.id || instantFocusItem?.type != item.type) {
        instantFocusItem = item
    }

    previewUpdateGate.lastUpdateMs = now
}

    Box(modifier = Modifier.fillMaxSize()) {
        // Only show background when TMDB enrichment is done (or disabled) to prevent flash
        val bgItem = if (!state.tmdbEnabled || renderedPreviewItem == null ||
            state.tmdbEnrichedIds.contains("${renderedPreviewItem.type}:${renderedPreviewItem.id}")) {
            renderedPreviewItem
        } else null
        CinematicBackground(bgItem)

        Column(modifier = Modifier.fillMaxSize().zIndex(2f)) {
            // INFO SECTION
            Box(
                modifier = Modifier
                    .weight(0.45f)
                    .fillMaxWidth()
                    .padding(start = startPadding, top = infoTopPadding),
                contentAlignment = Alignment.TopStart
            ) {
                AnimatedContent(
                    targetState = renderedPreviewItem,
                    transitionSpec = { fadeIn(tween(0)).togetherWith(fadeOut(tween(0))) },
                    label = "Info"    
                ) { item ->
                    if (item != null) {
                        // Hide info content while waiting for TMDB enrichment
                        val itemReady = !state.tmdbEnabled || state.tmdbEnrichedIds.contains("${item.type}:${item.id}")
                        Column(modifier = Modifier.alpha(if (itemReady) 1f else 0f)) {
                            Box(
                                modifier = Modifier
                                    .width(700.dp)
                                    .height(90.dp),
                                contentAlignment = Alignment.BottomStart
                            ) {
                                if (!item.logo.isNullOrEmpty()) {
                                    SubcomposeAsyncImage(
                                        model = item.logo,
                                        contentDescription = item.name,
                                        contentScale = ContentScale.Fit,
                                        alignment = Alignment.BottomStart,
                                        modifier = Modifier
                                            .widthIn(max = 500.dp)
                                            .heightIn(max = 90.dp),
                                        error = {
                                            Text(
                                                text = item.name,
                                                style = MaterialTheme.typography.displaySmall.copy(
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 32.sp,
                                                    lineHeight = 1.05.em
                                                ),
                                                color = Color.White,
                                                maxLines = 2,
                                                softWrap = true,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    )
                                } else {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.displaySmall.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 32.sp,
                                            lineHeight = 1.05.em
                                        ),
                                        color = Color.White,
                                        maxLines = 2,
                                        softWrap = true,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            HomeMetaStrip(item = item)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = item.description ?: "",
                                maxLines = if (isTopNav) 3 else 4,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, letterSpacing = 0.sp, lineHeight = 1.2.em),
                                color = Color.White.copy(0.85f),
                                textAlign = TextAlign.Start,
                                modifier = Modifier.width(450.dp)
                            )
                        }
                    }
                }
            }

            // LIST SECTION
            Box(modifier = Modifier.weight(0.55f)) {
                CompositionLocalProvider(LocalBringIntoViewSpec provides verticalPivot) {
                    // Use persisted vertical scroll position for instant restoration.
                    // When the continue watching row first appears at index 0, all saved
                    // indices are off by 1 — adjust the initial index to compensate.
                    val verticalListState = rememberLazyListState(
                        initialFirstVisibleItemIndex = verticalScrollPosition.first + historyScrollAdjustment,
                        initialFirstVisibleItemScrollOffset = verticalScrollPosition.second
                    )

                    // Global UP key debouncer to prevent accidental navbar navigation
                    val upKeyDebouncer = remember { UpKeyDebouncer() }
                    val dpadRepeatGate = remember {
                        DpadRepeatGate(
                            horizontalRepeatIntervalMs = DPAD_REPEAT_INTERVAL_HORIZONTAL_MS,
                            verticalRepeatIntervalMs = DPAD_REPEAT_INTERVAL_VERTICAL_MS
                        )
                    }

                    PersistLazyListPosition(
                        listState = verticalListState,
                        key = "cinematic_vertical",
                        minOffsetDeltaPx = 36,
                        onPositionChanged = onVerticalScrollChange
                    )

                    // Pre-scroll warmup: compose off-screen row to populate recycler + compile GPU shaders
                    LaunchedEffect(Unit) {
                        withFrameNanos { }
                        val idx = verticalListState.firstVisibleItemIndex
                        val off = verticalListState.firstVisibleItemScrollOffset
                        verticalListState.scrollToItem(idx + 1)
                        verticalListState.scrollToItem(idx, off)
                    }

                    LazyColumn(
                        state = verticalListState,
                        modifier = Modifier
                            .fillMaxSize(), // Focus managed by items
                        contentPadding = PaddingValues(top = 5.dp, bottom = 400.dp),
                        verticalArrangement = Arrangement.spacedBy((-12).dp)
                    ) {
                        if (historyItems.isNotEmpty()) {
                            item(key = "history_header", contentType = "row") {
                                // Reset scroll position when focus was redirected (cleared item removed)
                                val savedPosition = if (historyFocusRedirected) Pair(0, 0)
                                    else rowScrollPositions["history"] ?: Pair(0, 0)
                                val historyRowState = rememberLazyListState(
                                    initialFirstVisibleItemIndex = savedPosition.first,
                                    initialFirstVisibleItemScrollOffset = savedPosition.second
                                )

                                PersistLazyListPosition(
                                    listState = historyRowState,
                                    key = "cinematic_history",
                                    minOffsetDeltaPx = 36,
                                    onPositionChanged = { onScrollPositionChange("history", it) }
                                )

                                InfiniteLoopRow(
                                    startPadding = startPadding,
                                    isTopNav = isTopNav,
                                    rowIndex = -1,
                                    title = "Continue Watching",
                                    items = historyItems,
                                    onMovieClick = onMovieClick,
                                    onViewMore = { onViewMore("Continue Watching", historyItems, "") },
                                    onFocused = remember(wrappedOnFocusChange) {
                                        { item: MetaItem?, key: String ->
                                            updatePreviewItem(item)
                                            wrappedOnFocusChange(key)
                                        }
                                    },
                                    entryRequester = entryRequester,
                                    drawerRequester = drawerRequester,
                                    locallyFocusedItemId = if (effectiveLastFocusedKey?.startsWith("-1_") == true) effectiveLastFocusedKey else null,
                                    isGlobalFocusPresent = effectiveLastFocusedKey != null,
                                    isFirstRow = true,
                                    isInfiniteLoopEnabled = false,
                                    visibleItemCount = 15,
                                    isInfiniteScrollingEnabled = true,
                                    externalListState = historyRowState,
                                    upKeyDebouncer = upKeyDebouncer,
                                    repeatGate = dpadRepeatGate,
                                    isLandscapeCards = isLandscapeContinueWatching,
                                    enrichedItems = emptyMap(),
                                    rowHeight = if (isLandscapeContinueWatching) 165.dp else 210.dp
                                )
                            }
                        }

                        itemsIndexed(
                            items = state.mixedRows,
                            key = { _, item -> 
                                when(item) {
                                    is HubGroupRow -> "hub_row_${item.id}"
                                    is CategoryRow -> "row_${item.id}"
                                    else -> "unknown_${item.id}"
                                }
                            },
                            contentType = { _, item -> 
                                when(item) {
                                    is HubGroupRow -> "hub_row"
                                    is CategoryRow -> "row"
                                    else -> "unknown"
                                }
                            }
                        ) { index, item ->
                            when (item) {
                                is HubGroupRow -> {

                                    // Create list state with saved scroll position for this hub row
                                    // Reset when focus was redirected from a removed history item to this first row
                                    val hubKey = "hub_${item.id}"
                                    val resetScroll = historyFocusRedirected && historyItems.isEmpty() && index == 0
                                    val savedHubPosition = if (resetScroll) null else rowScrollPositions[hubKey]
                                    val hubListState = rememberLazyListState(
                                        initialFirstVisibleItemIndex = savedHubPosition?.first ?: 0,
                                        initialFirstVisibleItemScrollOffset = savedHubPosition?.second ?: 0
                                    )

                                    PersistLazyListPosition(
                                        listState = hubListState,
                                        key = "cinematic_$hubKey",
                                        minOffsetDeltaPx = 36,
                                        onPositionChanged = { onScrollPositionChange(hubKey, it) }
                                    )

                                    HubRow(
                                        hubGroup = item,
                                        startPadding = startPadding,
                                        onHubClick = onHubClick,
                                        onFocused = { _, focusKey ->
                                            instantFocusItem = null
                                            wrappedOnFocusChange("hub_$focusKey")
                                        },
                                        entryRequester = entryRequester,
                                        drawerRequester = drawerRequester,
                                        locallyFocusedItemId = if (effectiveLastFocusedKey?.startsWith("hub_") == true) effectiveLastFocusedKey else null,
                                        isTopNav = isTopNav,
                                        rowIndex = index,
                                        externalListState = hubListState,
                                        upKeyDebouncer = upKeyDebouncer,
                                        repeatGate = dpadRepeatGate,
                                        isLastRow = index == state.mixedRows.lastIndex,
                                        isFirstRow = historyItems.isEmpty() && index == 0,
                                        isGlobalFocusPresent = effectiveLastFocusedKey != null
                                    )
                                }
                                is CategoryRow -> {
                                    // Create list state with saved scroll position for this row
                                    // Reset when focus was redirected from a removed history item to this first row
                                    val rowKey = item.id
                                    val resetScroll = historyFocusRedirected && historyItems.isEmpty() && index == 0
                                    val savedPosition = if (resetScroll) null else rowScrollPositions[rowKey]

                                    val initialIndex = savedPosition?.first ?: 0
                                    val initialOffset = savedPosition?.second ?: 0

                                    val rowListState = rememberLazyListState(
                                        initialFirstVisibleItemIndex = initialIndex,
                                        initialFirstVisibleItemScrollOffset = initialOffset
                                    )

                                    PersistLazyListPosition(
                                        listState = rowListState,
                                        key = "cinematic_$rowKey",
                                        minOffsetDeltaPx = 36,
                                        onPositionChanged = { onScrollPositionChange(rowKey, it) }
                                    )
                                    
                                    // Lazy pagination: load more items when scrolling near the end
                                    // Use snapshotFlow to avoid reading layoutInfo during composition
                                    val itemId = item.id
                                    val itemSize = item.items.size
                                    LaunchedEffect(rowListState, itemId) {
                                        snapshotFlow {
                                            rowListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                                        }.collect { lastVisibleIndex ->
                                            if (itemSize > 0 && lastVisibleIndex >= itemSize - 10) {
                                                onLoadMore(itemId)
                                            }
                                        }
                                    }

                                    InfiniteLoopRow(
                                        startPadding = startPadding,
                                        isTopNav = isTopNav,
                                        rowIndex = index,
                                        title = item.title,
                                        items = item.items,
                                        onMovieClick = onMovieClick,
                                        onViewMore = remember(item.title, item.items, item.id, onViewMore) {
                                            { onViewMore(item.title, item.items, item.id) }
                                        },
                                        onFocused = remember(wrappedOnFocusChange) {
                                            { focusItem: MetaItem?, key: String ->
                                                updatePreviewItem(focusItem)
                                                wrappedOnFocusChange(key)
                                            }
                                        },
                                        entryRequester = entryRequester,
                                        drawerRequester = drawerRequester,
                                        locallyFocusedItemId = if (effectiveLastFocusedKey?.startsWith("${index}_") == true) effectiveLastFocusedKey else null,
                                        isGlobalFocusPresent = effectiveLastFocusedKey != null,
                                        isFirstRow = historyItems.isEmpty() && index == 0,
                                        isInfiniteLoopEnabled = item.isInfiniteLoopEnabled,
                                        visibleItemCount = item.visibleItemCount,
                                        isInfiniteScrollingEnabled = item.isInfiniteScrollingEnabled,
                                        externalListState = rowListState,
                                        upKeyDebouncer = upKeyDebouncer,
                                        repeatGate = dpadRepeatGate
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Redirect the saved continue-watching focus key when the row has changed since navigation:
 * 1. Item removed (progress cleared) → focus first remaining item, or first regular row
 * 2. Item reordered (e.g. resumed playback bumped it to front) → update to new index
 */
private fun resolveEffectiveFocusKey(
    lastFocusedKey: String?,
    historyItems: List<MetaItem>
): String? {
    if (lastFocusedKey == null || !lastFocusedKey.startsWith("-1_")) return lastFocusedKey

    val firstSep = lastFocusedKey.indexOf('_')
    if (firstSep < 0) return lastFocusedKey
    val remainder = lastFocusedKey.substring(firstSep + 1)
    val itemId = remainder.substringBeforeLast("_")
    val savedIndex = remainder.substringAfterLast("_").toIntOrNull()

    val currentIndex = historyItems.indexOfFirst { it.id == itemId }

    if (currentIndex < 0) {
        // Item was removed: redirect to first remaining item, or null (first regular row gets focus)
        return if (historyItems.isNotEmpty()) {
            "-1_${historyItems.first().id}_0"
        } else {
            null
        }
    }

    // Item exists but moved position (e.g. became most-recently-watched after playback)
    if (savedIndex != null && currentIndex != savedIndex) {
        return "-1_${itemId}_${currentIndex}"
    }

    return lastFocusedKey
}

private fun resolveCinematicPreviewItem(
    lastFocusedKey: String?,
    mixedRows: List<HomeRowItem>,
    historyItems: List<MetaItem>
): MetaItem? {
    if (lastFocusedKey.isNullOrEmpty()) return null
    if (lastFocusedKey.startsWith("hub_") || lastFocusedKey.startsWith("hero_")) return null

    val firstSeparator = lastFocusedKey.indexOf('_')
    if (firstSeparator <= 0) return null

    val rowToken = lastFocusedKey.substring(0, firstSeparator)
    val itemToken = lastFocusedKey.substring(firstSeparator + 1).substringBeforeLast("_")
    if (itemToken.isEmpty() || itemToken == "viewmore") return null

    if (rowToken == "-1") {
        return historyItems.firstOrNull { it.id == itemToken }
    }

    val rowIndex = rowToken.toIntOrNull() ?: return null
    val row = mixedRows.getOrNull(rowIndex) as? CategoryRow ?: return null
    return row.items.firstOrNull { it.id == itemToken }
}

private fun cleanContinueWatchingBackground(url: String?): String? {
    return url
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?.takeIf {
            it.startsWith("http://", ignoreCase = true) ||
                it.startsWith("https://", ignoreCase = true)
        }
}

private fun findNextUpForCurrentSeries(
    canonicalId: String,
    nextUpBySeriesId: Map<String, com.lumera.app.data.model.SeriesNextUpEntity>
): com.lumera.app.data.model.SeriesNextUpEntity? {
    nextUpBySeriesId[canonicalId]?.let { return it }

    val cleanCanonical = canonicalId.trim()
    if (cleanCanonical.isBlank()) return null

    return nextUpBySeriesId.entries.firstOrNull { (seriesId, _) ->
        seriesId == cleanCanonical ||
            seriesId.endsWith(":$cleanCanonical") ||
            cleanCanonical.endsWith(":$seriesId")
    }?.value
}

private fun buildContinueWatchingItems(
    history: List<WatchHistoryEntity>,
    seriesNextUp: List<com.lumera.app.data.model.SeriesNextUpEntity> = emptyList()
): List<MetaItem> {
    val result = mutableListOf<Pair<Long, MetaItem>>()
    val seriesIdsIncluded = mutableSetOf<String>()
    val nextUpBySeriesId = seriesNextUp.associateBy { it.seriesId }
    
    val continueWatchingBackgroundBySeriesId = history
        .filter { it.type == "series" }
        .groupBy { canonicalSeriesId(it.id) }
        .mapValues { (_, entries) ->
            entries
                .sortedByDescending { it.lastWatched }
                .firstNotNullOfOrNull { entry ->
                    cleanContinueWatchingBackground(entry.background)
                        ?: cleanContinueWatchingBackground(entry.poster)
                }
        }

    // 1. In-progress items (partially watched, not completed)
    val inProgress = history.filter { !it.watched }

    val seriesByCanonicalId = mutableMapOf<String, MutableList<WatchHistoryEntity>>()
    val movieById = mutableMapOf<String, WatchHistoryEntity>()

    inProgress.forEach { entry ->
        if (entry.type == "series") {
            val canonicalId = canonicalSeriesId(entry.id)
            seriesByCanonicalId.getOrPut(canonicalId) { mutableListOf() }.add(entry)
        } else {
            movieById.putIfAbsent(entry.id, entry)
        }
    }

    val chosenSeries = mutableMapOf<String, WatchHistoryEntity>()
    seriesByCanonicalId.forEach { (canonicalId, entries) ->
        val preferred = entries.firstOrNull { isEpisodePlaybackId(it.id) } ?: entries.firstOrNull()
        if (preferred != null) {
            chosenSeries[canonicalId] = preferred
        }
    }

    inProgress.forEach { entry ->
        if (entry.type == "series") {
            val canonicalId = canonicalSeriesId(entry.id)
            val chosen = chosenSeries[canonicalId] ?: return@forEach
            if (chosen.id != entry.id) return@forEach

            val nextUp = findNextUpForCurrentSeries(
                canonicalId = canonicalId,
                nextUpBySeriesId = nextUpBySeriesId
            )

            val continueWatchingBackground = continueWatchingBackgroundBySeriesId[canonicalId]
                ?: cleanContinueWatchingBackground(chosen.background)
                ?: cleanContinueWatchingBackground(chosen.poster)
            
            val seriesTitle = resolveContinueWatchingSeriesTitle(
                nextUpTitle = nextUp?.title,
                historyTitle = chosen.title,
                canonicalId = canonicalId
            )
            
            val episodeTitle = resolveContinueWatchingEpisodeTitle(
                historyTitle = chosen.title,
                storedEpisodeTitle = chosen.logo
            )
            
            seriesIdsIncluded.add(canonicalId)

            result.add(
                chosen.lastWatched to MetaItem(
                    id = canonicalId,
                    type = "series",
            
                    // Main card title: show name
                    name = seriesTitle,
            
                    // Card thumbnail: stable series background only
                    poster = continueWatchingBackground,
                    background = continueWatchingBackground,
            
                    // Do not use history logo as real logo; we used it only to carry episode title
                    logo = null,
            
                    // Small line below title: S01 E02 • Episode Name
                    description = buildEpisodeLine(
                        playbackId = chosen.id,
                        episodeTitle = episodeTitle
                    ),
            
                    imdbRating = null,
            
                    // Top-right: time remaining
                    runtime = buildContinueWatchingRemainingText(
                        position = chosen.position,
                        duration = chosen.duration
                    ),
            
                    progress = chosen.progress(),
                    hasNewEpisode = false
                )
            )
        } else {
            val chosen = movieById[entry.id] ?: return@forEach
            if (chosen.id != entry.id) return@forEach

            result.add(
                entry.lastWatched to MetaItem(
                    id = entry.id,
                    type = entry.type,
                    name = entry.title,
                    poster = entry.poster,
                    background = entry.background,
                    logo = null,
                    description = null,
                    runtime = buildContinueWatchingRemainingText(
                        position = entry.position,
                        duration = entry.duration
                    ),
                    progress = entry.progress()
                )
            )
        }
    }

    // 2. Next-up entries: series where all in-progress episodes are watched,
    //    but there's a next episode available. Only include if the episode has aired.
    val today = java.time.LocalDate.now().toString()
    for (nextUp in seriesNextUp) {
        if (nextUp.seriesId in seriesIdsIncluded) continue

        val released = nextUp.nextReleased
        if (released != null && released > today) continue

        if (nextUp.isComplete && released == null) continue

        val isReturning = nextUp.isComplete || nextUp.isNewEpisode

        val continueWatchingBackground = continueWatchingBackgroundBySeriesId[nextUp.seriesId]
            ?: cleanContinueWatchingBackground(nextUp.poster)
        
        result.add(
            nextUp.updatedAt to MetaItem(
                id = nextUp.seriesId,
                type = "series",
        
                // Main card title: show name
                name = nextUp.title,
        
                // Card thumbnail: stable series background only
                poster = continueWatchingBackground,
                background = continueWatchingBackground,
        
                logo = null,
        
                // Small line below title: S01 E03 • Episode Name
                description = buildEpisodeLine(
                    season = nextUp.nextSeason,
                    episode = nextUp.nextEpisode,
                    episodeTitle = nextUp.nextEpisodeTitle
                ),
        
                imdbRating = null,
        
                // Top-right: New episode
                runtime = "New episode",
        
                hasNewEpisode = isReturning
            )
        )
    }

    return result.sortedByDescending { it.first }.map { it.second }
}

private fun resolveContinueWatchingSeriesTitle(
    nextUpTitle: String?,
    historyTitle: String?,
    canonicalId: String
): String {
    val cleanNextUpTitle = nextUpTitle
        ?.trim()
        ?.takeIf { it.isNotBlank() }

    if (cleanNextUpTitle != null) return cleanNextUpTitle

    val raw = historyTitle
        ?.trim()
        .orEmpty()

    val looksLikeEpisodeTitle =
        Regex("^S\\d{1,2}\\s*:?\\s*E\\d{1,2}\\s*[-:•.]?\\s*", RegexOption.IGNORE_CASE)
            .containsMatchIn(raw) ||
            Regex("^S\\d{1,2}E\\d{1,2}\\s*[-:•.]?\\s*", RegexOption.IGNORE_CASE)
                .containsMatchIn(raw) ||
            Regex("^Season\\s*\\d+\\s*Episode\\s*\\d+\\s*[-:•.]?\\s*", RegexOption.IGNORE_CASE)
                .containsMatchIn(raw)

    return if (looksLikeEpisodeTitle) {
        canonicalId
    } else {
        raw.takeIf { it.isNotBlank() } ?: canonicalId
    }
}

private fun resolveContinueWatchingEpisodeTitle(
    historyTitle: String?,
    storedEpisodeTitle: String?
): String? {
    val stored = storedEpisodeTitle
        ?.trim()
        ?.takeIf { it.isNotBlank() }

    if (stored != null) return stored

    val raw = historyTitle
        ?.trim()
        .orEmpty()

    val cleaned = raw
        .replace(
            Regex("^S\\d{1,2}\\s*:?\\s*E\\d{1,2}\\s*[-:•.]?\\s*", RegexOption.IGNORE_CASE),
            ""
        )
        .replace(
            Regex("^S\\d{1,2}E\\d{1,2}\\s*[-:•.]?\\s*", RegexOption.IGNORE_CASE),
            ""
        )
        .replace(
            Regex("^Season\\s*\\d+\\s*Episode\\s*\\d+\\s*[-:•.]?\\s*", RegexOption.IGNORE_CASE),
            ""
        )
        .trim()

    return cleaned.takeIf { it.isNotBlank() }
}

private fun buildEpisodeLine(
    playbackId: String,
    episodeTitle: String?
): String? {
    val parts = playbackId.split(":")
    val season = parts.getOrNull(parts.size - 2)?.toIntOrNull()
    val episode = parts.lastOrNull()?.toIntOrNull()

    return buildEpisodeLine(
        season = season,
        episode = episode,
        episodeTitle = episodeTitle
    )
}

private fun buildEpisodeLine(
    season: Int?,
    episode: Int?,
    episodeTitle: String?
): String? {
    val seasonEpisode = if (season != null && episode != null) {
        "S${season.toString().padStart(2, '0')} E${episode.toString().padStart(2, '0')}"
    } else {
        null
    }

    val cleanTitle = cleanEpisodeTitleForCard(episodeTitle)

    return when {
        !seasonEpisode.isNullOrBlank() && !cleanTitle.isNullOrBlank() ->
            "$seasonEpisode • $cleanTitle"

        !seasonEpisode.isNullOrBlank() ->
            seasonEpisode

        !cleanTitle.isNullOrBlank() ->
            cleanTitle

        else ->
            null
    }
}

private fun cleanEpisodeTitleForCard(rawTitle: String?): String? {
    val cleaned = rawTitle
        ?.trim()
        .orEmpty()
        .replace(
            Regex(
                pattern = "^S\\d{1,2}\\s*[: ]?\\s*E\\d{1,2}\\s*[-:•.]?\\s*",
                option = RegexOption.IGNORE_CASE
            ),
            ""
        )
        .replace(
            Regex(
                pattern = "^S\\d{1,2}E\\d{1,2}\\s*[-:•.]?\\s*",
                option = RegexOption.IGNORE_CASE
            ),
            ""
        )
        .replace(
            Regex(
                pattern = "^Season\\s*\\d+\\s*Episode\\s*\\d+\\s*[-:•.]?\\s*",
                option = RegexOption.IGNORE_CASE
            ),
            ""
        )
        .trim()

    return cleaned.takeIf { it.isNotBlank() }
}

private fun buildContinueWatchingRemainingText(
    position: Long,
    duration: Long
): String? {
    if (duration <= 0L) return null

    val remainingMs = (duration - position).coerceAtLeast(0L)
    if (remainingMs <= 0L) return null

    val totalMinutes = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(remainingMs)
        .coerceAtLeast(1L)

    return when {
        totalMinutes >= 60L -> {
            val hours = totalMinutes / 60L
            val minutes = totalMinutes % 60L
            if (minutes > 0L) "${hours}h ${minutes}m left" else "${hours}h left"
        }

        else -> "${totalMinutes}m left"
    }
}

private fun canonicalSeriesId(playbackId: String): String {
    if (!isEpisodePlaybackId(playbackId)) return playbackId
    val parts = playbackId.split(":")
    return parts.dropLast(2).joinToString(":")
}

private fun isEpisodePlaybackId(playbackId: String): Boolean {
    val parts = playbackId.split(":")
    if (parts.size < 3) return false
    return parts[parts.lastIndex - 1].toIntOrNull() != null &&
        parts.last().toIntOrNull() != null
}

private fun previewLookupItemForFocus(
    item: MetaItem,
    historyItems: List<MetaItem>
): MetaItem {
    val isContinueWatchingItem = historyItems.any { it.type == item.type && it.id == item.id }

    if (!isContinueWatchingItem) return item

    return item.copy(
        // Keep only series identity for metadata lookup.
        // Do not let Continue Watching card text/image overwrite series metadata.
        poster = null,
        background = null,
        logo = null,
        description = null,
        runtime = null,
        imdbRating = null,
        hasNewEpisode = false
    )
}

private fun resolveLatestPreviewItem(
    current: MetaItem?,
    state: HomeViewModel.HomeState,
    historyItems: List<MetaItem>
): MetaItem? {
    val item = current ?: return null
    val isContinueWatchingItem = historyItems.any { it.type == item.type && it.id == item.id }

    val enriched = state.enrichedMeta["${item.type}:${item.id}"]
    if (enriched != null && hasUsefulPreviewMetadata(enriched)) {
        return if (isContinueWatchingItem) {
            enriched.copy(
                name = item.name.ifBlank { enriched.name },
                imdbRating = enriched.imdbRating
            )
        } else {
            enriched
        }
    }

    val fromHero = state.heroRow?.items?.firstOrNull { it.type == item.type && it.id == item.id }
    if (fromHero != null && hasUsefulPreviewMetadata(fromHero)) return fromHero

    val fromRows = state.mixedRows
        .asSequence()
        .filterIsInstance<CategoryRow>()
        .flatMap { row -> row.items.asSequence() }
        .firstOrNull { it.type == item.type && it.id == item.id }

    if (fromRows != null && hasUsefulPreviewMetadata(fromRows)) return fromRows

    return if (isContinueWatchingItem) {
        item.copy(
            // Do not show Continue Watching card text in the main metadata area.
            description = null,
            runtime = null,
    
            // Do not let the main metadata image use the episode thumbnail/card image.
            // The focused top area should wait for/use enriched series metadata.
            poster = null,
            background = null,
            logo = null,
    
            imdbRating = null
        )
    } else {
        item
    }
}

private fun hasUsefulPreviewMetadata(item: MetaItem): Boolean {
    return !item.logo.isNullOrBlank() ||
        !item.description.isNullOrBlank() ||
        !item.background.isNullOrBlank() ||
        !item.releaseInfo.isNullOrBlank() ||
        !item.genres.isNullOrEmpty()
}

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SimpleLayout(
    startPadding: androidx.compose.ui.unit.Dp,
    isTopNav: Boolean,
    state: HomeViewModel.HomeState,
    heroItems: List<MetaItem> = emptyList(),
    heroAutoScrollSeconds: Int = 0,
    onMovieClick: (MetaItem) -> Unit,
    onViewMore: (String, List<MetaItem>, String) -> Unit,
    onHubClick: (com.lumera.app.domain.HubItem) -> Unit,
    onLoadMore: (String) -> Unit,
    entryRequester: FocusRequester,
    drawerRequester: FocusRequester,
    lastFocusedKey: String?,
    rowScrollPositions: Map<String, Pair<Int, Int>>,
    verticalScrollPosition: Pair<Int, Int>,
    historyScrollAdjustment: Int,
    onFocusChange: (String) -> Unit,
    onScrollPositionChange: (String, Pair<Int, Int>) -> Unit,
    onVerticalScrollChange: (Pair<Int, Int>) -> Unit,
    onHeroItemVisible: (MetaItem) -> Unit,
    isLandscapeContinueWatching: Boolean = false
) {
    var hasRequestedFocus by remember { mutableStateOf(false) }
    // Stable key: only rebuild when items/order/watched-state change, not when images update
    val historyKey = remember(state.history) {
        state.history.map { "${it.id}:${it.watched}:${it.lastWatched}:${it.poster != null}" }
    }
    val nextUpKey = remember(state.seriesNextUp) {
        state.seriesNextUp.map { "${it.seriesId}:${it.isComplete}:${it.nextSeason}:${it.nextEpisode}:${it.updatedAt}:${it.poster != null}" }
    }
    val historyItems = remember(historyKey, nextUpKey) {
        buildContinueWatchingItems(state.history, state.seriesNextUp)
    }

    // Proactive metadata fetch for continue watching cards (posters + landscape)
    LaunchedEffect(historyItems) {
        historyItems.take(15).forEach { item ->
            onHeroItemVisible(previewLookupItemForFocus(item, historyItems))
        }
    }

    // Redirect stale continue-watching focus key after progress was cleared
    val effectiveLastFocusedKey = remember(lastFocusedKey, historyItems) {
        resolveEffectiveFocusKey(lastFocusedKey, historyItems)
    }
    val historyFocusRedirected = effectiveLastFocusedKey != lastFocusedKey

    // Global UP key debouncer to prevent accidental navbar navigation
    val upKeyDebouncer = remember { UpKeyDebouncer() }
    val dpadRepeatGate = remember {
        DpadRepeatGate(
            horizontalRepeatIntervalMs = DPAD_REPEAT_INTERVAL_HORIZONTAL_MS,
            verticalRepeatIntervalMs = DPAD_REPEAT_INTERVAL_VERTICAL_MS
        )
    }
    // Focus requester for the first row's pivot item (used by hero carousel DOWN key)
    val firstRowPivotRequester = remember { FocusRequester() }

    LaunchedEffect(state.rows, historyItems) {
        if (!hasRequestedFocus && (state.rows.isNotEmpty() || historyItems.isNotEmpty())) {
            delay(100)
            entryRequester.requestFocus()
            hasRequestedFocus = true
        }
    }

    // Smooth vertical scrolling pivot for Simple layout
    val density = LocalDensity.current

    // Skip vertical scroll on restoration (when we have a saved focus key)
    val isVerticalRestoration = effectiveLastFocusedKey != null
    var skipVerticalScroll by remember { mutableStateOf(isVerticalRestoration) }

    // Reset skip flag after restoration is complete
    LaunchedEffect(isVerticalRestoration) {
        if (isVerticalRestoration) {
            delay(300)
            skipVerticalScroll = false
        }
    }

    val verticalPivotPx = remember(density, isTopNav) { with(density) { (if (isTopNav) 91.dp else 71.dp).toPx() } }

    val verticalPivot = remember(verticalPivotPx) {
        FocusPivotSpec(
            customOffset = verticalPivotPx,
            skipScrollProvider = { skipVerticalScroll },
            stiffnessProvider = { Spring.StiffnessMediumLow }
        )
    }

    // Wrap onFocusChange to track timing for vertical dynamic stiffness
    val wrappedOnFocusChange: (String) -> Unit = remember(onFocusChange) {
        { key ->
            onFocusChange(key)
        }
    }
    
    CompositionLocalProvider(LocalBringIntoViewSpec provides verticalPivot) {
        // Use persisted vertical scroll position for instant restoration.
        // When the continue watching row first appears at index 0, all saved
        // indices are off by 1 — adjust the initial index to compensate.
        val verticalListState = rememberLazyListState(
            initialFirstVisibleItemIndex = verticalScrollPosition.first + historyScrollAdjustment,
            initialFirstVisibleItemScrollOffset = verticalScrollPosition.second
        )

        PersistLazyListPosition(
            listState = verticalListState,
            key = "simple_vertical",
            minOffsetDeltaPx = 36,
            onPositionChanged = onVerticalScrollChange
        )

        // Pre-scroll warmup: compose off-screen row to populate recycler + compile GPU shaders
        LaunchedEffect(Unit) {
            withFrameNanos { }
            val idx = verticalListState.firstVisibleItemIndex
            val off = verticalListState.firstVisibleItemScrollOffset
            verticalListState.scrollToItem(idx + 1)
            verticalListState.scrollToItem(idx, off)
        }

        LazyColumn(
            state = verticalListState,
            modifier = Modifier.fillMaxSize(), // Focus managed by items
            contentPadding = PaddingValues(top = if (heroItems.isNotEmpty()) 24.dp else if (isTopNav) 84.dp else 64.dp, bottom = 0.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            if (heroItems.isNotEmpty()) {
                item(key = "hero_carousel") {
                    HeroCarousel(
                        items = heroItems,
                        autoScrollSeconds = heroAutoScrollSeconds,
                        onItemClick = onMovieClick,
                        startPadding = startPadding,
                        onFocusChange = { wrappedOnFocusChange("hero_$it") },
                        onCurrentItemVisible = onHeroItemVisible,
                        entryRequester = if (effectiveLastFocusedKey == null || effectiveLastFocusedKey.startsWith("hero_")) entryRequester else null,
                        drawerRequester = drawerRequester,
                        isFirstItem = true,
                        isTopNav = isTopNav,
                        upKeyDebouncer = upKeyDebouncer,
                        repeatGate = dpadRepeatGate,
                        onNavigateDown = { firstRowPivotRequester.requestFocus() },
                        restoreItemId = if (effectiveLastFocusedKey?.startsWith("hero_") == true) effectiveLastFocusedKey.removePrefix("hero_") else null,
                        tmdbEnabled = state.tmdbEnabled,
                        tmdbEnrichedIds = state.tmdbEnrichedIds
                    )
                }
            }

            if (historyItems.isNotEmpty()) {
                item(key = "simple_history_header", contentType = "row") {
                    // Reset scroll position when focus was redirected (cleared item removed)
                    val savedPosition = if (historyFocusRedirected) Pair(0, 0)
                        else rowScrollPositions["history"] ?: Pair(0, 0)
                    val historyRowState = rememberLazyListState(
                        initialFirstVisibleItemIndex = savedPosition.first,
                        initialFirstVisibleItemScrollOffset = savedPosition.second
                    )

                    PersistLazyListPosition(
                        listState = historyRowState,
                        key = "simple_history",
                        minOffsetDeltaPx = 36,
                        onPositionChanged = { onScrollPositionChange("history", it) }
                    )

                    InfiniteLoopRow(
                        startPadding = startPadding,
                        isTopNav = isTopNav,
                        rowIndex = -1,
                        title = "Continue Watching",
                        items = historyItems,
                        onMovieClick = onMovieClick,
                        onViewMore = { onViewMore("Continue Watching", historyItems, "") },
                        onFocused = remember(wrappedOnFocusChange) {
                            { _: MetaItem?, key: String -> wrappedOnFocusChange(key) }
                        },
                        entryRequester = entryRequester,
                        drawerRequester = drawerRequester,
                        locallyFocusedItemId = if (effectiveLastFocusedKey?.startsWith("-1_") == true) effectiveLastFocusedKey else null,
                        isGlobalFocusPresent = effectiveLastFocusedKey != null,
                        isFirstRow = heroItems.isEmpty(),
                        isInfiniteLoopEnabled = false,
                        visibleItemCount = 15,
                        isInfiniteScrollingEnabled = true,
                        externalListState = historyRowState,
                        upKeyDebouncer = upKeyDebouncer,
                        repeatGate = dpadRepeatGate,
                        pivotFocusRequester = if (heroItems.isNotEmpty()) firstRowPivotRequester else null,
                        isLandscapeCards = isLandscapeContinueWatching,
                        enrichedItems = emptyMap(),
                        rowHeight = if (isLandscapeContinueWatching) 165.dp else 210.dp
                    )
                }
            }

            itemsIndexed(
                items = state.mixedRows,
                key = { _, item -> 
                    when(item) {
                        is HubGroupRow -> "simple_hub_${item.id}"
                        is CategoryRow -> "simple_row_${item.id}"
                        else -> "simple_unknown_${item.id}"
                    }
                },
                contentType = { _, item -> 
                    when(item) {
                        is HubGroupRow -> "hub_row"
                        is CategoryRow -> "row"
                        else -> "unknown"
                    }
                }
            ) { rowIndex, item ->
                when (item) {
                    is HubGroupRow -> {

                        // Create list state with saved scroll position for this hub row
                        // Reset when focus was redirected from a removed history item to this first row
                        val hubKey = "hub_${item.id}"
                        val resetScroll = historyFocusRedirected && historyItems.isEmpty() && rowIndex == 0
                        val savedHubPosition = if (resetScroll) null else rowScrollPositions[hubKey]
                        val hubListState = rememberLazyListState(
                            initialFirstVisibleItemIndex = savedHubPosition?.first ?: 0,
                            initialFirstVisibleItemScrollOffset = savedHubPosition?.second ?: 0
                        )

                        PersistLazyListPosition(
                            listState = hubListState,
                            key = "simple_$hubKey",
                            minOffsetDeltaPx = 36,
                            onPositionChanged = { onScrollPositionChange(hubKey, it) }
                        )

                        HubRow(
                            hubGroup = item,
                            startPadding = startPadding,
                            onHubClick = onHubClick,
                            onFocused = { _, focusKey ->
                                wrappedOnFocusChange("hub_$focusKey")
                            },
                            entryRequester = entryRequester,
                            drawerRequester = drawerRequester,
                            locallyFocusedItemId = if (effectiveLastFocusedKey?.startsWith("hub_") == true) effectiveLastFocusedKey else null,
                            isTopNav = isTopNav,
                            rowIndex = rowIndex,
                            isFirstRow = heroItems.isEmpty() && historyItems.isEmpty() && rowIndex == 0, // Only steal focus if no hero/history row
                            externalListState = hubListState,
                            upKeyDebouncer = upKeyDebouncer,
                            repeatGate = dpadRepeatGate,
                            isLastRow = rowIndex == state.mixedRows.lastIndex,
                            pivotFocusRequester = if (heroItems.isNotEmpty() && historyItems.isEmpty() && rowIndex == 0) firstRowPivotRequester else null,
                            isGlobalFocusPresent = effectiveLastFocusedKey != null
                        )
                    }
                    is CategoryRow -> {
                        // Create list state with saved scroll position for this row
                        // Reset when focus was redirected from a removed history item to this first row
                        val rowKey = item.id
                        val resetScroll = historyFocusRedirected && historyItems.isEmpty() && rowIndex == 0
                        val savedPosition = if (resetScroll) null else rowScrollPositions[rowKey]

                        val initialIndex = savedPosition?.first ?: 0
                        val initialOffset = savedPosition?.second ?: 0

                        val rowListState = rememberLazyListState(
                            initialFirstVisibleItemIndex = initialIndex,
                            initialFirstVisibleItemScrollOffset = initialOffset
                        )

                        PersistLazyListPosition(
                            listState = rowListState,
                            key = "simple_$rowKey",
                            minOffsetDeltaPx = 36,
                            onPositionChanged = { onScrollPositionChange(rowKey, it) }
                        )
                        
                        // Lazy pagination: load more items when scrolling near the end
                        // Use snapshotFlow to avoid reading layoutInfo during composition
                        val itemId = item.id
                        val itemSize = item.items.size
                        LaunchedEffect(rowListState, itemId) {
                            snapshotFlow {
                                rowListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                            }.collect { lastVisibleIndex ->
                                if (itemSize > 0 && lastVisibleIndex >= itemSize - 10) {
                                    onLoadMore(itemId)
                                }
                            }
                        }
                        
                        // Custom row height: 193dp for last row in Top Nav, 210dp standard
                        val rowHeight = if (isTopNav && rowIndex == state.mixedRows.lastIndex) 193.dp else 210.dp

                        // STABILIZE LAMBDAS:
                        // These specific lambda instances must remain referentially equal
                        // across recompositions to allow InfiniteLoopRow to skip logic.
                        val onRowViewMore = remember(item.items, item.title, item.id, onViewMore) {
                            { onViewMore(item.title, item.items, item.id) }
                        }
                        
                        val onRowFocused = remember(wrappedOnFocusChange) {
                            { _: MetaItem?, key: String -> wrappedOnFocusChange(key) }
                        }

                        InfiniteLoopRow(
                            startPadding = startPadding,
                            isTopNav = isTopNav,
                            rowIndex = rowIndex,
                            title = item.title,
                            items = item.items,
                            onMovieClick = onMovieClick,
                            onViewMore = onRowViewMore,
                            onFocused = onRowFocused,
                            entryRequester = entryRequester,
                            drawerRequester = drawerRequester,
                            locallyFocusedItemId = if (effectiveLastFocusedKey?.startsWith("${rowIndex}_") == true) effectiveLastFocusedKey else null,
                            isGlobalFocusPresent = effectiveLastFocusedKey != null,
                            isFirstRow = heroItems.isEmpty() && historyItems.isEmpty() && rowIndex == 0,
                            isInfiniteLoopEnabled = item.isInfiniteLoopEnabled,
                            visibleItemCount = item.visibleItemCount,
                            isInfiniteScrollingEnabled = item.isInfiniteScrollingEnabled,
                            externalListState = rowListState,
                            rowHeight = rowHeight,
                            upKeyDebouncer = upKeyDebouncer,
                            repeatGate = dpadRepeatGate,
                            pivotFocusRequester = if (heroItems.isNotEmpty() && historyItems.isEmpty() && rowIndex == 0) firstRowPivotRequester else null
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PersistLazyListPosition(
    listState: androidx.compose.foundation.lazy.LazyListState,
    key: String,
    minOffsetDeltaPx: Int = 32,
    onPositionChanged: (Pair<Int, Int>) -> Unit
) {
    var lastIndex by remember(key, listState) { mutableIntStateOf(Int.MIN_VALUE) }
    var lastOffset by remember(key, listState) { mutableIntStateOf(Int.MIN_VALUE) }

    LaunchedEffect(key, listState) {
        snapshotFlow {
            Pair(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset)
        }.collect { (index, offset) ->
            val shouldEmit =
                lastIndex == Int.MIN_VALUE ||
                    index != lastIndex ||
                    kotlin.math.abs(offset - lastOffset) >= minOffsetDeltaPx

            if (shouldEmit) {
                lastIndex = index
                lastOffset = offset
                onPositionChanged(Pair(index, offset))
            }
        }
    }

    DisposableEffect(key, listState) {
        onDispose {
            val finalIndex = listState.firstVisibleItemIndex
            val finalOffset = listState.firstVisibleItemScrollOffset
            if (finalIndex != lastIndex || finalOffset != lastOffset) {
                onPositionChanged(Pair(finalIndex, finalOffset))
            }
        }
    }
}

@Composable
fun CinematicBackground(item: MetaItem?) {
    // Use the theme's actual background color
    val backgroundColor = androidx.compose.material3.MaterialTheme.colorScheme.background
    
    // Only fade LEFT and BOTTOM edges - top/right are at screen edge
    val leftFade = Brush.horizontalGradient(
        colorStops = arrayOf(
            0.0f to backgroundColor,
            0.1f to backgroundColor.copy(0.95f),
            0.2f to backgroundColor.copy(0.85f),
            0.3f to backgroundColor.copy(0.72f),
            0.4f to backgroundColor.copy(0.55f),
            0.55f to backgroundColor.copy(0.35f),
            0.7f to backgroundColor.copy(0.18f),
            0.85f to backgroundColor.copy(0.07f),
            1.0f to Color.Transparent
        ),
        startX = 0f,
        endX = 500f
    )
    
    // Bottom fade: blend into the rows area
    val bottomFade = Brush.verticalGradient(
        colorStops = arrayOf(
            0.0f to Color.Transparent,
            0.2f to backgroundColor.copy(0.05f),
            0.35f to backgroundColor.copy(0.15f),
            0.45f to backgroundColor.copy(0.25f),
            0.55f to backgroundColor.copy(0.38f),
            0.65f to backgroundColor.copy(0.52f),
            0.75f to backgroundColor.copy(0.68f),
            0.85f to backgroundColor.copy(0.82f),
            0.92f to backgroundColor.copy(0.92f),
            1.0f to backgroundColor
        )
    )

    Box(modifier = Modifier.fillMaxSize().zIndex(0f)) {
        Box(modifier = Modifier.align(Alignment.TopEnd).fillMaxWidth(0.65f).fillMaxHeight(0.65f)) {
            Crossfade(
                targetState = item, 
                animationSpec = tween(700), 
                label = "HeroBg"
            ) { currentItem ->
                if (currentItem != null) {
                    val image = currentItem.background ?: currentItem.poster
                    val context = LocalContext.current
                    val imageRequest = remember(image) {
                        ImageRequest.Builder(context)
                            .data(image)
                            .crossfade(false)
                            .size(1920, 1080)
                            .build()
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = imageRequest,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        // Left and bottom edge fades only
                        Spacer(
                            modifier = Modifier
                                .fillMaxSize()
                                .drawWithCache {
                                    onDrawWithContent {
                                        drawRect(brush = leftFade)
                                        drawRect(brush = bottomFade)
                                    }
                                }
                        )
                        com.lumera.app.ui.components.NoiseOverlay()
                    }
                }
            }
        }
    }
}


@Composable
fun Badge(text: String, color: Color, textColor: Color) {
    Text(text = text, color = textColor, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), modifier = Modifier.background(color, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp))
}

@Composable
private fun HomeMetaStrip(item: MetaItem) {
    val typeLabel = item.type.replaceFirstChar { it.uppercase() }
    val genreLabel = item.genres
        ?.firstOrNull()
        ?.replaceFirstChar { it.uppercase() }
        ?: "Unknown"
    val yearLabel = extractHomePrimaryYear(item.releaseInfo)
    val valueColor = Color.White.copy(alpha = 0.92f)
    val textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = typeLabel, style = textStyle, color = valueColor)
        HomeMetaDot()
        Text(text = genreLabel, style = textStyle, color = valueColor)
        HomeMetaDot()
        Text(text = yearLabel, style = textStyle, color = valueColor)

        val runtimeMinutes = item.runtime
            ?.trim()
            ?.let { Regex("^\\d{1,4}m$").matchEntire(it)?.value }
            ?.removeSuffix("m")
            ?.toIntOrNull()

        runtimeMinutes?.let { mins ->
            HomeMetaDot()
            val hours = mins / 60
            val m = mins % 60
            val display = if (hours > 0) "${hours}h ${m}m" else "${m}m"
            Text(text = display, style = textStyle, color = valueColor)
        }

        item.imdbRating?.takeIf { it.isNotBlank() }?.let { rating ->
            HomeMetaDot()
            HomeImdbBadge()
            Text(text = rating, style = textStyle, color = valueColor)
        }
    }
}

@Composable
private fun HomeMetaDot() {
    Text(
        text = ".",
        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
        color = Color.White.copy(alpha = 0.55f)
    )
}

@Composable
private fun HomeImdbBadge() {
    Image(
        painter = painterResource(id = R.drawable.imdb_logo),
        contentDescription = "IMDb",
        modifier = Modifier.height(16.dp)
    )
}

private fun extractHomePrimaryYear(releaseInfo: String?): String {
    if (releaseInfo.isNullOrBlank()) return "----"
    return Regex("\\d{4}").find(releaseInfo)?.value ?: releaseInfo.take(4)
}
