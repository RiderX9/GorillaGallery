package com.gorilla.gallery.ui.components

import androidx.compose.ui.draw.alpha

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.unit.IntOffset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.animation.togetherWith
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.gorilla.gallery.data.model.MediaItem
import com.gorilla.gallery.ui.theme.SpringSpecs
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
/** A titled section of media sharing one date-header pill. */
data class MediaSection(val key: String, val label: String, val items: List<MediaItem>)

/**
 * Wraps a grid with a full-page spring transition that activates when the sort state changes.
 */
@Composable
fun <T> SortTransition(
    sortState: T,
    modifier: Modifier = Modifier,
    contentKey: (T) -> Any? = { it },
    content: @Composable (T) -> Unit
) {
    androidx.compose.animation.AnimatedContent(
        targetState = sortState,
        modifier = modifier,
        transitionSpec = {
            val offsetSpring = androidx.compose.animation.core.spring<androidx.compose.ui.unit.IntOffset>(
                dampingRatio = 0.5f, stiffness = 200f
            )
            (androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically(offsetSpring) { it / 6 })
                .togetherWith(androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically(offsetSpring) { -it / 6 })
                .using(androidx.compose.animation.SizeTransform(clip = false) { _, _ -> androidx.compose.animation.core.snap() })
        },
        contentKey = contentKey,
        label = "SortTransition"
    ) { state ->
        content(state)
    }
}

/**
 * The shared media grid: full-span glass date headers interleaved with square cells,
 */
@Composable
fun MediaGrid(
    sections: List<MediaSection>,
    columns: Int,
    selectionMode: Boolean,
    selectedIds: Set<Long>,
    onColumnsChange: (Int) -> Unit,
    onClick: (MediaItem, Rect) -> Unit,
    onLongClick: (MediaItem) -> Unit,
    dragSelectionEnabled: Boolean = false,
    onDragSelectRange: (List<MediaItem>) -> Unit = {},
    onDragSelectionChange: (Set<Long>) -> Unit = {},
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    minColumns: Int = 2,
    maxColumns: Int = 6,
    headerContent: (@Composable () -> Unit)? = null,
    viewerOpen: Boolean = false,
    focusedItem: MediaItem? = null,
    highQualityThumbnails: Boolean = false,
    state: LazyGridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState(),
) {
    val colsState = rememberUpdatedState(columns)
    val selectedIdsState = rememberUpdatedState(selectedIds)
    val onDragSelectRangeState = rememberUpdatedState(onDragSelectRange)
    val onDragSelectionChangeState = rememberUpdatedState(onDragSelectionChange)
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val mediaItems = remember(sections) {
        sections.flatMap { it.items }
    }
    val mediaIndexById = remember(mediaItems) {
        mediaItems.mapIndexed { index, item -> item.id to index }.toMap()
    }
    
    val thumbnailSizePx = remember(minColumns, density, configuration.screenWidthDp, highQualityThumbnails) {
        with(density) {
            val minDimensionDp = minOf(configuration.screenWidthDp, configuration.screenHeightDp)
            val screenWidthPx = minDimensionDp.dp.roundToPx()
            val spacingPx = (1.5.dp * 2).roundToPx()
            // Use minColumns so the resolved pixel size never changes during pinch-to-zoom,
            // preventing the entire grid from cache-missing and reloading from disk.
            val sizePx = ((screenWidthPx / minColumns) - spacingPx).coerceAtLeast(1)
            sizePx
        }
    }

    val placementSpec = if (state.isScrollInProgress) null else SpringSpecs.BouncyOffsetSpring

    Box(modifier = modifier) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            state = state,
            contentPadding = contentPadding,
            modifier = Modifier
                .fillMaxSize()
            .dragSelectVisibleItems(
                enabled = dragSelectionEnabled,
                state = state,
                mediaItems = mediaItems,
                mediaIndexById = mediaIndexById,
                topPaddingPx = with(density) { contentPadding.calculateTopPadding().toPx() },
                selectedIds = { selectedIdsState.value },
                onSelectRange = { onDragSelectRangeState.value(it) },
                onSelectionChange = { onDragSelectionChangeState.value(it) },
            )
            .pinchColumns(
                current = { colsState.value },
                min = minColumns,
                max = maxColumns,
                onChange = onColumnsChange,
            ),
    ) {
        if (headerContent != null) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "top") { 
                Box(modifier = Modifier.animateItem(placementSpec = placementSpec)) {
                    headerContent()
                }
            }
        }
        sections.forEach { section ->
            if (section.label.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }, key = "h_${section.key}") {
                    GlassDateHeaderPill(
                        label = section.label,
                        modifier = Modifier.animateItem(placementSpec = placementSpec)
                    )
                }
            }
            items(section.items, key = { it.id }) { item ->

                MediaCell(
                    item = item,
                    thumbnailSizePx = thumbnailSizePx,
                    onClick = { bounds -> onClick(item, bounds) },
                    onLongClick = { onLongClick(item) },
                    selectionMode = selectionMode,
                    selected = item.id in selectedIds,
                    highQualityThumbnails = highQualityThumbnails,
                    modifier = Modifier
                        .animateItem(placementSpec = placementSpec)
                        .alpha(if (viewerOpen && focusedItem?.id == item.id) 0f else 1f)
                        .padding(1.5.dp),
                )
            }
        }
    }


}
}

private fun Modifier.dragSelectVisibleItems(
    enabled: Boolean,
    state: LazyGridState,
    mediaItems: List<MediaItem>,
    mediaIndexById: Map<Long, Int>,
    topPaddingPx: Float,
    selectedIds: () -> Set<Long>,
    onSelectRange: (List<MediaItem>) -> Unit,
    onSelectionChange: (Set<Long>) -> Unit,
): Modifier = if (!enabled) {
    this
} else {
    pointerInput(state, mediaItems, mediaIndexById, topPaddingPx) {
        coroutineScope {
            var anchorIndex: Int? = null
            var lastRange: IntRange? = null
            var baselineSelection = emptySet<Long>()
            var latestPosition = Offset.Zero
            var autoScrollJob: Job? = null

            fun itemIndexAt(position: Offset): Int? {
                val hitPosition = Offset(
                    x = position.x.coerceIn(0f, size.width.toFloat()),
                    y = position.y.coerceIn(0f, size.height.toFloat()),
                )
                
                val id = state.layoutInfo.visibleItemsInfo
                    .firstOrNull { info ->
                        val key = info.key as? Long ?: return@firstOrNull false
                        
                        val left = info.offset.x.toFloat()
                        val right = left + info.size.width.toFloat()
                        // Use raw visual coordinate by adding topPaddingPx if the lazy grid uses it as viewport offset
                        val top = info.offset.y.toFloat() + topPaddingPx
                        val bottom = top + info.size.height.toFloat()
                        
                        hitPosition.x >= left && hitPosition.x <= right &&
                        hitPosition.y >= top && hitPosition.y <= bottom
                    }
                    ?.key as? Long
                return id?.let { mediaIndexById[it] }
            }

            fun selectRangeTo(currentIndex: Int) {
                val anchor = anchorIndex ?: currentIndex.also { anchorIndex = it }
                val start = minOf(anchor, currentIndex)
                val end = maxOf(anchor, currentIndex)
                val range = start..end
                if (range != lastRange) {
                    lastRange = range
                    val rangeItems = mediaItems.slice(range)
                    val rangeIds = rangeItems.mapTo(mutableSetOf()) { it.id }
                    onSelectRange(rangeItems)
                    onSelectionChange(baselineSelection + rangeIds)
                }
            }

            fun selectAtLatestPosition() {
                itemIndexAt(latestPosition)?.let(::selectRangeTo)
            }

            fun startAutoScroll() {
                if (autoScrollJob != null) return
                autoScrollJob = launch {
                    val edgeSize = 96.dp.toPx()
                    val maxScroll = 42.dp.toPx()
                    while (true) {
                        val y = latestPosition.y
                        val delta = when {
                            y > size.height - edgeSize -> {
                                ((y - (size.height - edgeSize)) / edgeSize).coerceIn(0f, 1f) * maxScroll
                            }
                            y < edgeSize -> {
                                -((edgeSize - y) / edgeSize).coerceIn(0f, 1f) * maxScroll
                            }
                            else -> 0f
                        }
                        if (delta != 0f) {
                            state.scrollBy(delta)
                            selectAtLatestPosition()
                        }
                        delay(16)
                    }
                }
            }

            fun stopAutoScroll() {
                autoScrollJob?.cancel()
                autoScrollJob = null
                anchorIndex = null
                lastRange = null
                baselineSelection = emptySet()
            }

            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val longPress = awaitLongPressOrCancellation(down.id)
                
                if (longPress != null) {
                    var pointerId = longPress.id
                    latestPosition = longPress.position
                    baselineSelection = selectedIds()
                    anchorIndex = itemIndexAt(down.position)
                    lastRange = null
                    selectAtLatestPosition()
                    startAutoScroll()
                    
                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == pointerId }
                                ?: event.changes.firstOrNull { it.pressed }
                                ?: break
                            if (!change.pressed) break
                            
                            pointerId = change.id
                            change.consume()
                            latestPosition = change.position
                            selectAtLatestPosition()
                        }
                    } finally {
                        stopAutoScroll()
                    }
                }
            }
        }
    }
}

/**
 * Pinch detector that only fires while ≥2 pointers are down — single-finger drags fall
 * through to the grid's scroll. Crossing a zoom threshold steps the column count.
 */
private fun Modifier.pinchColumns(
    current: () -> Int,
    min: Int,
    max: Int,
    onChange: (Int) -> Unit,
): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        var zoom = 1f
        var wasPinching = false
        do {
            val event = awaitPointerEvent()
            if (event.changes.size >= 2) {
                wasPinching = true
                val z = event.calculateZoom()
                if (z != 1f) {
                    zoom *= z
                    val cols = current()
                    when {
                        zoom > 1.18f && cols > min -> { onChange(cols - 1); zoom = 1f }
                        zoom < 0.84f && cols < max -> { onChange(cols + 1); zoom = 1f }
                    }
                }
                event.changes.forEach { it.consume() }
            } else if (wasPinching) {
                event.changes.forEach { it.consume() }
            }
        } while (event.changes.any { it.pressed })
    }
}
