package com.lumera.app.ui.details

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.lumera.app.ui.home.DpadRepeatGate
import com.lumera.app.data.model.stremio.MetaVideo
import com.lumera.app.data.model.stremio.Stream
import com.lumera.app.data.tmdb.TmdbEpisodeEnrichment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// --- STATE & UTILS ---

sealed class SidebarState {
    data object Closed : SidebarState()
    data class Episodes(val videos: List<MetaVideo>) : SidebarState()
    data class Sources(val streamTitle: String, val streams: List<Stream>?, val selectedStreamId: String? = null) : SidebarState()
}

// Handles Back key for sidebar dismissal. Left trapping is handled per-item via focusProperties.
private fun Modifier.dpadNavigation(onBack: () -> Unit, trapLeft: Boolean = true, repeatGate: DpadRepeatGate? = null) = this.onPreviewKeyEvent {
    if (repeatGate?.shouldConsume(it) == true) return@onPreviewKeyEvent true
    when {
        trapLeft && it.key == Key.DirectionLeft && it.type == KeyEventType.KeyDown -> true
        it.key == Key.Back && it.type == KeyEventType.KeyUp -> { onBack(); true }
        else -> false
    }
}

// --- MAIN SIDEBAR ---

@Composable
fun GlassSidebarScaffold(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    panelWidth: Dp = 500.dp,
    panelPadding: PaddingValues = PaddingValues(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 0.dp),
    overlayAlpha: Float = 0.4f,
    enter: EnterTransition = slideInHorizontally { it },
    exit: ExitTransition = slideOutHorizontally { it },
    content: @Composable ColumnScope.() -> Unit
) {
    if (visible) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = overlayAlpha))
                .clickable(onClick = onDismiss)
        )
    }

    AnimatedVisibility(
        visible = visible,
        enter = enter,
        exit = exit,
        modifier = modifier.fillMaxSize()
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
            Column(
                Modifier
                    .fillMaxHeight()
                    .width(panelWidth)
                    .background(Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0.0f to Color(0xCC000000),
                            0.15f to Color(0xD2000000),
                            0.3f to Color(0xD9000000),
                            0.45f to Color(0xE1000000),
                            0.6f to Color(0xE9000000),
                            0.75f to Color(0xF0000000),
                            0.88f to Color(0xF6000000),
                            1.0f to Color(0xFA000000)
                        )
                    ))
                    .pointerInput(Unit) { detectTapGestures { } }
                    .padding(panelPadding)
            ) {
                content()
            }
        }
    }
}

@Composable
fun GlassSidebar(
    state: SidebarState,
    currentEpisodeId: String? = null,
    episodeProgressMap: Map<String, DetailsViewModel.EpisodeProgress> = emptyMap(),
    episodeEnrichmentMap: Map<String, TmdbEpisodeEnrichment> = emptyMap(),
    onToggleWatched: (MetaVideo) -> Unit = {},
    onEpisodeSelected: (MetaVideo) -> Unit,
    onSourceSelected: (Stream) -> Unit,
    onBack: () -> Unit,
    onDismiss: () -> Unit
) {
    val isVisible = state !is SidebarState.Closed
    val focusRequester = remember { FocusRequester() }
    val episodesListState = rememberLazyListState()
    var savedSeason by remember { mutableStateOf<Int?>(null) }
    var savedIndex by remember { mutableIntStateOf(0) }
    var previousState by remember { mutableStateOf<SidebarState>(SidebarState.Closed) }

    // When sidebar opens with episodes, auto-navigate to the currently playing episode,
    // reset to the beginning if freshly opened from details (Closed→Episodes),
    // or preserve position when returning from sources (Sources→Episodes).
    LaunchedEffect(state, currentEpisodeId) {
        if (state is SidebarState.Episodes) {
            if (currentEpisodeId != null) {
                val seasonMap = state.videos.filter { it.season > 0 }.groupBy { it.season }
                for ((season, eps) in seasonMap) {
                    val idx = eps.indexOfFirst { ep ->
                        ep.id == currentEpisodeId || currentEpisodeId.endsWith(":${ep.season}:${ep.episode}")
                    }
                    if (idx >= 0) {
                        savedSeason = season
                        savedIndex = idx
                        break
                    }
                }
            } else if (previousState is SidebarState.Closed) {
                // Fresh open from details screen — reset to first episode
                savedSeason = null
                savedIndex = 0
                runCatching { episodesListState.scrollToItem(0) }
            }
            // Sources→Episodes: keep savedSeason/savedIndex from the episode click
        }
        previousState = state
    }

    LaunchedEffect(state) {
        if (isVisible) { delay(200); runCatching { focusRequester.requestFocus() } }
    }

    val isEpisodes = state is SidebarState.Episodes
    GlassSidebarScaffold(
        visible = isVisible,
        onDismiss = onDismiss,
        panelWidth = 920.dp
    ) {
        Crossfade(targetState = state, label = "Sidebar") { current ->
            when (current) {
                is SidebarState.Episodes -> EpisodesContent(
                    videos = current.videos,
                    listState = episodesListState,
                    savedSeason = savedSeason,
                    savedIndex = savedIndex,
                    currentEpisodeId = currentEpisodeId,
                    episodeProgressMap = episodeProgressMap,
                    episodeEnrichmentMap = episodeEnrichmentMap,
                    onToggleWatched = onToggleWatched,
                    focusRequester = focusRequester,
                    onEpisodeClick = { ep, s, i -> savedSeason = s; savedIndex = i; onEpisodeSelected(ep) },
                    onSeasonChange = { savedSeason = it },
                    onDismiss = onDismiss
                )
                is SidebarState.Sources -> SourcesContent(
                    title = current.streamTitle,
                    streams = current.streams,
                    selectedStreamId = current.selectedStreamId,
                    focusRequester = focusRequester,
                    onSourceClick = onSourceSelected,
                    onBack = onBack
                )
                else -> {}
                }
            }
        }
}

// --- CONTENT: SEASONS/EPISODES ---

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun EpisodesContent(
    videos: List<MetaVideo>,
    listState: LazyListState,
    savedSeason: Int?,
    savedIndex: Int,
    currentEpisodeId: String? = null,
    episodeProgressMap: Map<String, DetailsViewModel.EpisodeProgress> = emptyMap(),
    episodeEnrichmentMap: Map<String, TmdbEpisodeEnrichment> = emptyMap(),
    onToggleWatched: (MetaVideo) -> Unit = {},
    focusRequester: FocusRequester,
    onEpisodeClick: (MetaVideo, Int, Int) -> Unit,
    onSeasonChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val seasons = remember(videos) { videos.filter { it.season > 0 }.groupBy { it.season }.toSortedMap() }
    var selectedSeason by remember(savedSeason) { mutableIntStateOf(savedSeason ?: seasons.keys.minOrNull() ?: 1) }
    val episodes = seasons[selectedSeason] ?: emptyList()

    val tabRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope() // Needed for manual scrolling
    val repeatGate = remember { DpadRepeatGate(horizontalRepeatIntervalMs = 150L, verticalRepeatIntervalMs = 200L) }

    LaunchedEffect(selectedSeason) { onSeasonChange(selectedSeason) }

    // Scroll to the saved episode when the sidebar opens (or back to top when savedIndex is 0)
    LaunchedEffect(savedIndex, selectedSeason) {
        if (episodes.isNotEmpty() && savedIndex in episodes.indices) {
            runCatching { listState.scrollToItem(savedIndex) }
        }
    }

    Column {
        Text("More Episodes", style = MaterialTheme.typography.headlineSmall, color = Color.White, modifier = Modifier.padding(bottom = 16.dp))

        if (seasons.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 8.dp)) {
                itemsIndexed(seasons.keys.toList()) { idx, num ->
                    SeasonTab(
                        number = num,
                        isSelected = num == selectedSeason,
                        modifier = Modifier
                            .then(if (idx == 0) Modifier.onPreviewKeyEvent {
                                if (repeatGate.shouldConsume(it)) return@onPreviewKeyEvent true
                                it.key == Key.DirectionLeft && it.type == KeyEventType.KeyDown
                            } else Modifier.onPreviewKeyEvent { repeatGate.shouldConsume(it) })
                            .then(if (num == selectedSeason) Modifier.focusRequester(tabRequester) else Modifier)
                            .focusProperties { up = FocusRequester.Cancel },
                        onClick = {
                            selectedSeason = num
                            // Only reset scroll to top when manually changing seasons
                            scope.launch { listState.scrollToItem(0) }
                        }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(top = 0.dp, bottom = 24.dp),
            modifier = Modifier.dpadNavigation(onDismiss, trapLeft = false, repeatGate = repeatGate)
        ) {
            if (episodes.isEmpty()) item { Text("No episodes found.", color = Color.Gray) }
            else {
                itemsIndexed(episodes, key = { _, ep -> "${ep.season}:${ep.episode}:${ep.id}" }) { index, ep ->
                    // Focus Logic: Attach requester to saved index; Attach 'Up' navigation to first index
                    val isTarget = index == (if (savedIndex in episodes.indices) savedIndex else 0)
                    val mod = Modifier
                        .then(if (isTarget) Modifier.focusRequester(focusRequester) else Modifier)
                        .then(if (index == 0) Modifier.focusProperties { up = if (seasons.isNotEmpty()) tabRequester else FocusRequester.Cancel } else Modifier)

                    val isCurrentEpisode = currentEpisodeId != null && (
                        ep.id == currentEpisodeId ||
                        currentEpisodeId.endsWith(":${ep.season}:${ep.episode}")
                    )
                    val epKey = "S${ep.season}:E${ep.episode}"
                    val epProgress = episodeProgressMap[epKey]
                    val epEnrichment = episodeEnrichmentMap[epKey]
                    EpisodeItem(
                        episode = ep,
                        isPlaying = isCurrentEpisode,
                        progress = epProgress?.progress,
                        isWatched = epProgress?.watched ?: false,
                        enrichment = epEnrichment,
                        onToggleWatched = { onToggleWatched(ep) },
                        thumbnailModifier = mod,
                        listState = listState,
                        onClick = { onEpisodeClick(ep, selectedSeason, index) }
                    )
                }
            }
        }
    }
}

// --- CONTENT: SOURCES ---

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SourcesContent(
    title: String,
    streams: List<Stream>?,
    selectedStreamId: String? = null,
    focusRequester: FocusRequester,
    onSourceClick: (Stream) -> Unit,
    onBack: () -> Unit
) {
    val actualStreams = streams ?: emptyList()
    var filter by remember { mutableStateOf("All Addons") }

    val addonNames = remember(actualStreams) {
        listOf("All Addons") + actualStreams.mapNotNull { it.name?.substringAfter("[")?.substringBefore("]") }.distinct()
    }

    val filtered = remember(actualStreams, filter) {
        actualStreams.filter { filter == "All Addons" || (it.name?.contains(filter) == true) }
    }

    val selectedIndex = remember(filtered, selectedStreamId) {
        if (selectedStreamId == null) 0
        else {
            val idx = filtered.indexOfFirst { s ->
                (s.addonTransportUrl ?: s.url) == selectedStreamId
            }
            if (idx >= 0) idx else 0
        }
    }

    val listState = rememberLazyListState()
    val repeatGate = remember { DpadRepeatGate(verticalRepeatIntervalMs = 200L) }

    LaunchedEffect(selectedIndex, filtered) {
        if (filtered.isNotEmpty() && selectedIndex > 0) {
            runCatching { listState.scrollToItem(selectedIndex) }
        }
    }

    Column {
        Text("Select Source", style = MaterialTheme.typography.titleLarge, color = Color.White)
        Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))

        if (streams == null) {
            Box(
                Modifier.fillMaxSize()
                    .focusRequester(focusRequester)
                    .dpadNavigation(onBack)
                    .focusable(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(16.dp))
                    Text("Loading streams...", color = Color.LightGray)
                }
            }
        } else {
            FilterDropdown(
                currentValue = filter,
                options = addonNames,
                modifier = Modifier
                    .focusProperties { up = FocusRequester.Cancel }
                    .onPreviewKeyEvent { it.key == Key.DirectionLeft && it.type == KeyEventType.KeyDown },
                onSelect = { filter = it }
            )
            Spacer(Modifier.height(16.dp))
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.dpadNavigation(onBack, repeatGate = repeatGate)
            ) {
                if (filtered.isEmpty()) item {
                    Box(Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                        Text("No streams found.", color = Color.Gray, style = MaterialTheme.typography.bodyLarge)
                    }
                } else {
                    itemsIndexed(filtered, key = { index, s -> "${index}_${s.addonTransportUrl ?: s.url ?: index}" }) { index, s ->
                        RawSourceItem(
                            stream = s,
                            isPlaying = index == selectedIndex && selectedStreamId != null,
                            modifier = if (index == selectedIndex) Modifier.focusRequester(focusRequester) else Modifier
                        ) { onSourceClick(s) }
                    }
                }
            }
        }
    }
}

// --- ITEMS & UI COMPONENTS ---

@Composable
fun FilterDropdown(currentValue: String, options: List<String>, modifier: Modifier, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }

    // Size calculation for symmetry
    var rowWidth by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current

    val primary = MaterialTheme.colorScheme.primary

    Box(modifier) {
        Button(
            onClick = { expanded = true },
            colors = ButtonDefaults.buttonColors(containerColor = if (isFocused) Color.White.copy(0.1f) else Color.White.copy(0.05f)),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isFocused = it.isFocused }
                .onSizeChanged {
                    // Capture width of the button to apply to dropdown
                    rowWidth = with(density) { it.width.toDp() }
                }
                .border(if (isFocused) 2.dp else 0.dp, if (isFocused) primary else Color.Transparent, RoundedCornerShape(8.dp)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(currentValue, color = if(isFocused) Color.White else Color.LightGray, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ArrowDropDown, null, tint = if(isFocused) primary else Color.Gray)
        }

        // HACK: DropdownMenu in standard M3 doesn't support 'containerColor' param easily.
        // It uses the internal Surface color. To get transparency, we override the theme
        // strictly for this block to force the Surface to be transparent.
        MaterialTheme(
            colorScheme = MaterialTheme.colorScheme.copy(surface = Color.Transparent),
            shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(8.dp))
        ) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .width(rowWidth) // Enforce symmetry
                    // Apply the glass gradient here manually
                    .background(
                        brush = Brush.horizontalGradient(
                            colorStops = arrayOf(
                                0.0f to Color(0xCC000000),
                                0.15f to Color(0xD2000000),
                                0.3f to Color(0xD9000000),
                                0.45f to Color(0xE1000000),
                                0.6f to Color(0xE9000000),
                                0.75f to Color(0xF0000000),
                                0.88f to Color(0xF6000000),
                                1.0f to Color(0xFA000000)
                            )
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                    // Clip ensures the background respects the rounded shape
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            ) {
                // Calculate which item should be focused based on currentValue
                val selectedIndex = remember(options, currentValue) { options.indexOf(currentValue).coerceAtLeast(0) }

                // Create a FocusRequester for every item so we can target the specific one
                val itemRequesters = remember(options.size) { List(options.size) { FocusRequester() } }

                // Trigger focus on the selected item when the menu opens
                LaunchedEffect(Unit) {
                    delay(100)
                    runCatching { itemRequesters[selectedIndex].requestFocus() }
                }

                options.forEachIndexed { index, opt ->
                    var isItemFocused by remember { mutableStateOf(false) }

                    DropdownMenuItem(
                        text = {
                            Text(
                                text = opt,
                                color = if (isItemFocused) primary else Color.White
                            )
                        },
                        onClick = { onSelect(opt); expanded = false },
                        modifier = Modifier
                            .focusRequester(itemRequesters[index])
                            .onFocusChanged { isItemFocused = it.isFocused }
                    )
                }
            }
        }
    }
}

@Composable
fun SeasonTab(number: Int, isSelected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val primary = MaterialTheme.colorScheme.primary
    Box(
        modifier.clip(RoundedCornerShape(4.dp))
            .background(if (isFocused) Color.White else if (isSelected) primary else Color.DarkGray)
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(onClick = onClick).focusable().padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text("Season $number", color = if (isSelected || isFocused) Color.Black else Color.White, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun EpisodeItem(
    episode: MetaVideo,
    isPlaying: Boolean = false,
    progress: Float? = null,
    isWatched: Boolean = false,
    enrichment: TmdbEpisodeEnrichment? = null,
    onToggleWatched: () -> Unit = {},
    thumbnailModifier: Modifier = Modifier,
    listState: LazyListState? = null,
    onClick: () -> Unit
) {
    var thumbnailFocused by remember { mutableStateOf(false) }
    var buttonFocused by remember { mutableStateOf(false) }
    val isFocused = thumbnailFocused || buttonFocused
    val primary = MaterialTheme.colorScheme.primary
    val thumbnailRequester = remember { FocusRequester() }
    val buttonRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    // Counteract BringIntoView scroll when the mark button gets focus
    LaunchedEffect(buttonFocused) {
        if (buttonFocused && listState != null) {
            val savedIndex = listState.firstVisibleItemIndex
            val savedOffset = listState.firstVisibleItemScrollOffset
            delay(50) // Wait for BringIntoView to execute
            listState.scrollToItem(savedIndex, savedOffset) // Scroll back
        }
    }

    // Use TMDB data when available, fall back to addon data
    val title = enrichment?.title
        ?: episode.title.takeIf { it.isNotBlank() && it != "Episode" }
        ?: "Episode ${episode.episode}"
    val overview = condenseEpisodeSynopsis(enrichment?.overview ?: episode.overview)
    val thumbnail = enrichment?.thumbnail ?: episode.thumbnail
    val runtime = enrichment?.runtimeMinutes
    val releaseDate = remember(enrichment?.airDate, episode.released) {
        formatEpisodeDate(enrichment?.airDate ?: episode.released?.take(10))
    }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Thumbnail
        Box(
            thumbnailModifier
                .width(180.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(6.dp))
                .border(
                    if (thumbnailFocused) 2.dp else 0.dp,
                    if (thumbnailFocused) primary else Color.Transparent,
                    RoundedCornerShape(6.dp)
                )
                .focusRequester(thumbnailRequester)
                .focusProperties { left = FocusRequester.Cancel; right = buttonRequester }
                .onFocusChanged { thumbnailFocused = it.isFocused }
                .clickable(onClick = onClick)
                .focusable()
        ) {
            if (thumbnail != null) {
                AsyncImage(
                    thumbnail, null,
                    Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(Modifier.fillMaxSize().background(Color.White.copy(0.08f)))
            }

            // Darken thumbnail for watched episodes
            if (isWatched) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(0.4f)))
            }

            // S:E label at bottom-left
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
                    .background(Color.Black.copy(0.7f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    "S${episode.season} : E${episode.episode}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }

            // Progress bar floating near bottom of thumbnail
            if (progress != null && progress > 0f && !isWatched) {
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(Color.White.copy(0.3f))
                ) {
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(primary)
                    )
                }
            }
        }

        Spacer(Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            
                Spacer(Modifier.width(10.dp))
            
                WatchedToggleButton(
                    isWatched = isWatched,
                    isFocused = buttonFocused,
                    focusRequester = buttonRequester,
                    thumbnailRequester = thumbnailRequester,
                    onFocusChanged = { buttonFocused = it },
                    onClick = onToggleWatched
                )
            }
            
            if (overview.isNotBlank()) {
                Text(
                    overview,
                    color = Color.White.copy(0.76f),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 14.sp,
                        lineHeight = 18.sp
                    ),
                    maxLines = 4,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        
            Row(
                modifier = Modifier.padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (runtime != null) {
                    Text(
                        "(${runtime} min)",
                        color = Color.White.copy(0.45f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (releaseDate != null) {
                    Text(
                        releaseDate,
                        color = Color.White.copy(0.4f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (isPlaying) {
                    Text(
                        "Playing",
                        style = MaterialTheme.typography.labelSmall,
                        color = primary
                    )
                }
            }
        }
    }
}

@Composable
private fun WatchedToggleButton(
    isWatched: Boolean,
    isFocused: Boolean,
    focusRequester: FocusRequester,
    thumbnailRequester: FocusRequester,
    onFocusChanged: (Boolean) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary

    val buttonWidth = if (isWatched) 86.dp else 132.dp

    val bgColor = when {
        isWatched && isFocused -> primary.copy(0.2f)
        isWatched -> primary.copy(0.1f)
        isFocused -> Color.White.copy(0.15f)
        else -> Color.White.copy(0.08f)
    }
    val borderColor = when {
        isFocused -> primary
        isWatched -> primary.copy(0.4f)
        else -> Color.White.copy(0.15f)
    }
    val iconColor = when {
        isWatched -> primary
        isFocused -> Color.White
        else -> Color.White.copy(0.5f)
    }

    Row(
        modifier = modifier
            .width(buttonWidth)
            .height(24.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .focusRequester(focusRequester)
            .focusProperties { left = thumbnailRequester; right = FocusRequester.Cancel }
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && (event.key == Key.DirectionUp || event.key == Key.DirectionDown)) {
                    // Move to thumbnail first, then let the system handle up/down to adjacent episodes
                    thumbnailRequester.requestFocus()
                    false // Don't consume — let the key propagate from the thumbnail
                } else false
            }
            .onFocusChanged { onFocusChanged(it.isFocused) }
            .clickable(onClick = onClick)
            .focusable()
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            if (isWatched) "✓" else "+",
            color = iconColor,
            style = MaterialTheme.typography.labelMedium
        )

        Spacer(Modifier.width(4.dp))
        Text(
            if (isWatched) "Watched" else "Mark as watched",
            color = if (isWatched) primary else Color.White,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1
        )
    }
}

private fun formatEpisodeDate(date: String?): String? {
    val clean = date?.take(10)?.trim().orEmpty()
    if (clean.isBlank()) return null

    // Input usually comes as yyyy-MM-dd
    val parts = clean.split("-")
    if (parts.size != 3) return clean

    val year = parts[0]
    val month = when (parts[1]) {
        "01" -> "JAN"
        "02" -> "FEB"
        "03" -> "MAR"
        "04" -> "APR"
        "05" -> "MAY"
        "06" -> "JUN"
        "07" -> "JUL"
        "08" -> "AUG"
        "09" -> "SEP"
        "10" -> "OCT"
        "11" -> "NOV"
        "12" -> "DEC"
        else -> parts[1]
    }
    val day = parts[2]

    return "$day-$month-$year"
}

private fun condenseEpisodeSynopsis(
    text: String?,
    maxChars: Int = 420
): String {
    val clean = text
        ?.trim()
        ?.replace(Regex("\\s+"), " ")
        .orEmpty()

    if (clean.length <= maxChars) return clean

    val cut = clean.take(maxChars)
    val lastSentence = cut.lastIndexOf(". ")
    val lastSpace = cut.lastIndexOf(" ")

    val endIndex = when {
        lastSentence >= 220 -> lastSentence + 1
        lastSpace >= 220 -> lastSpace
        else -> maxChars
    }

    return clean
        .take(endIndex)
        .trimEnd('.', ',', ';', ':')
        .trim() + "…"
}

@Composable
fun RawSourceItem(stream: Stream, isPlaying: Boolean = false, modifier: Modifier = Modifier, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val primary = MaterialTheme.colorScheme.primary
    val mainText = stream.description ?: stream.title ?: stream.name ?: "Unknown"
    val subText = stream.name ?: ""

    Card(
        colors = CardDefaults.cardColors(containerColor = if (isFocused) Color.White.copy(0.1f) else Color.White.copy(0.05f)),
        modifier = modifier.fillMaxWidth().onFocusChanged { isFocused = it.isFocused }
            .border(if (isFocused) 3.dp else 0.dp, if (isFocused) primary else Color.Transparent, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(mainText, style = MaterialTheme.typography.bodyLarge, color = if (isFocused) Color.White else Color.LightGray, modifier = Modifier.weight(1f))
                if (isPlaying) {
                    Text(
                        "Playing",
                        style = MaterialTheme.typography.labelSmall,
                        color = primary,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
            if (subText.isNotEmpty() && subText != mainText) {
                Text(subText, style = MaterialTheme.typography.labelSmall, color = if (isFocused) primary else Color.Gray, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}
