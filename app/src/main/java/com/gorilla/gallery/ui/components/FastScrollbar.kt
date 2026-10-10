package com.gorilla.gallery.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import kotlin.math.roundToInt
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import com.gorilla.gallery.ui.theme.LocalDynamicColors
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.gorilla.gallery.data.model.MediaItem
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private data class RowInfo(
    val gridItemIndex: Int,
    val mediaItem: MediaItem?,
    val sectionLabel: String,
    val isHeader: Boolean,
)

private val fullDateFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.getDefault())

/**
 * A fast scrollbar styled after Aves:
 * - A capsule scroll thumb (48dp height, 20dp width) with dark translucent border and up/down arrow notches.
 * - Starts strictly at the top of the grid row items (below the header/masthead) and ends evenly with the last item.
 * - While holding/dragging the thumb, displays a floating date bubble (Month Day, Year) aligned spatially with the thumb.
 * - Fades in during scroll or drag, and fades out after inactivity.
 * - Cumulative Pixel Offset Model: guaranteed monotonic, silky smooth, continuous thumb motion with zero jumping.
 * - GPU-rendered translation via graphicsLayer to eliminate recomposition jank and stutter.
 * - Non-interfering touch handling: only intercepts touches that land directly on the thumb.
 * - Frame-paced grid layout updates during dragging to eliminate frame drops.
 */
@Composable
fun FastScrollbar(
    state: LazyGridState,
    sections: List<MediaSection>,
    columns: Int,
    contentPadding: PaddingValues,
    hasHeader: Boolean,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val accentColor = LocalDynamicColors.current.accent

    // Precalculate row-by-row layout structure for 100% accurate date matching and continuous progress
    val (rowList, gridIndexToRow) = remember(sections, columns, hasHeader) {
        val rows = mutableListOf<RowInfo>()
        var currentGridIndex = 0

        val totalItems = (if (hasHeader) 1 else 0) + sections.sumOf { (if (it.label.isNotEmpty()) 1 else 0) + it.items.size }
        val indexToRow = IntArray(totalItems)

        if (hasHeader) {
            val firstPhoto = sections.firstOrNull()?.items?.firstOrNull()
            rows.add(RowInfo(gridItemIndex = currentGridIndex, mediaItem = firstPhoto, sectionLabel = "", isHeader = true))
            if (currentGridIndex < indexToRow.size) {
                indexToRow[currentGridIndex] = 0
            }
            currentGridIndex++
        }

        for (section in sections) {
            val sectionFirstItem = section.items.firstOrNull()
            if (section.label.isNotEmpty()) {
                val rowIndex = rows.size
                rows.add(RowInfo(gridItemIndex = currentGridIndex, mediaItem = sectionFirstItem, sectionLabel = section.label, isHeader = true))
                if (currentGridIndex < indexToRow.size) {
                    indexToRow[currentGridIndex] = rowIndex
                }
                currentGridIndex++
            }

            val items = section.items
            val colCount = columns.coerceAtLeast(1)
            for (i in items.indices step colCount) {
                val rowIndex = rows.size
                val rowItem = items[i]
                rows.add(RowInfo(gridItemIndex = currentGridIndex, mediaItem = rowItem, sectionLabel = section.label, isHeader = false))
                val rowEnd = minOf(i + colCount, items.size)
                for (j in i until rowEnd) {
                    if (currentGridIndex < indexToRow.size) {
                        indexToRow[currentGridIndex] = rowIndex
                    }
                    currentGridIndex++
                }
            }
        }
        Pair(rows, indexToRow)
    }

    val totalRows = rowList.size
    if (totalRows <= 1) return

    val thumbHeightDp = 48.dp
    val thumbWidthDp = 20.dp
    val thumbHeightPx = with(density) { thumbHeightDp.toPx() }

    var isDragging by remember { mutableStateOf(false) }
    var dragStartThumbY by remember { mutableStateOf(0f) }
    var dragProgress by remember { mutableFloatStateOf(0f) }
    var bubbleDateText by remember { mutableStateOf("") }
    var bubbleHeightPx by remember { mutableIntStateOf(0) }
    var targetGridIndex by remember { mutableIntStateOf(-1) }
    var targetScrollOffsetPx by remember { mutableIntStateOf(0) }

    // Frame-paced scroll execution: synchronizes with screen refresh during dragging
    LaunchedEffect(isDragging) {
        if (isDragging) {
            var lastScrolledIndex = -1
            var lastScrolledOffset = -1
            while (isDragging) {
                val target = targetGridIndex
                val offset = targetScrollOffsetPx
                if (target >= 0 && (target != lastScrolledIndex || offset != lastScrolledOffset)) {
                    lastScrolledIndex = target
                    lastScrolledOffset = offset
                    state.scrollToItem(target, offset)
                }
                withFrameMillis { }
            }
        }
    }

    // Thumb visibility animation
    var thumbVisibleTarget by remember { mutableStateOf(false) }
    val canScroll by remember(state) { derivedStateOf { state.canScrollForward || state.canScrollBackward } }
    val thumbAlpha by animateFloatAsState(
        targetValue = if (canScroll && (thumbVisibleTarget || isDragging)) 1f else 0f,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "FastScrollbarThumbAlpha"
    )

    // Schedule fade-out after scrolling stops
    LaunchedEffect(state.isScrollInProgress, isDragging) {
        if (state.isScrollInProgress || isDragging) {
            thumbVisibleTarget = true
        } else {
            delay(1200)
            if (!state.isScrollInProgress && !isDragging) {
                thumbVisibleTarget = false
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize().zIndex(10f)) {
        val containerHeightPx = constraints.maxHeight.toFloat()
        val containerWidthPx = constraints.maxWidth.toFloat()
        val topPaddingPx = with(density) { contentPadding.calculateTopPadding().toPx() }
        val bottomPaddingPx = with(density) { contentPadding.calculateBottomPadding().toPx() }

        val availableTrackHeight = (containerHeightPx - topPaddingPx - bottomPaddingPx).coerceAtLeast(0f)
        val maxThumbOffset = (availableTrackHeight - thumbHeightPx).coerceAtLeast(0f)

        if (maxThumbOffset <= 0f) return@BoxWithConstraints

        // Precompute cumulative pixel offset of every row for strictly monotonic, smooth progress
        val headerHeightPx = with(density) { 48.dp.toPx() }
        val photoRowHeightPx = containerWidthPx / columns.coerceAtLeast(1)

        val rowOffsets = remember(rowList, containerWidthPx, columns, density) {
            val offsets = FloatArray(totalRows + 1)
            var accum = 0f
            for (i in 0 until totalRows) {
                offsets[i] = accum
                accum += if (rowList[i].isHeader) headerHeightPx else photoRowHeightPx
            }
            offsets[totalRows] = accum
            offsets
        }

        val totalContentHeightPx = rowOffsets[totalRows]
        val viewportHeightPx = availableTrackHeight.coerceAtLeast(1f)
        val maxScrollPx = (totalContentHeightPx - viewportHeightPx).coerceAtLeast(1f)

        // Smooth, monotonic scroll progress calculation based on cumulative pixel offsets
        val normalProgressState = remember(state, rowOffsets, gridIndexToRow, maxScrollPx) {
            derivedStateOf {
                if (maxScrollPx <= 0f) return@derivedStateOf 0f
                if (!state.canScrollForward) {
                    1f
                } else if (!state.canScrollBackward && state.firstVisibleItemScrollOffset <= 0) {
                    0f
                } else {
                    val firstIndex = state.firstVisibleItemIndex
                    val currentRow = gridIndexToRow.getOrElse(firstIndex) { 0 }
                    val itemOffset = state.firstVisibleItemScrollOffset.toFloat()
                    val currentScrollPx = rowOffsets[currentRow] + itemOffset
                    (currentScrollPx / maxScrollPx).coerceIn(0f, 1f)
                }
            }
        }

        // Evaluated inside graphicsLayer lambda to guarantee ZERO recompositions during scrolling/dragging
        fun currentThumbOffset(): Float {
            val progress = if (isDragging) dragProgress else normalProgressState.value
            return topPaddingPx + (progress.coerceIn(0f, 1f) * maxThumbOffset)
        }

        // Helper to update bubble text and target scroll position when dragging
        fun updateDrag(progress: Float) {
            val clamped = progress.coerceIn(0f, 1f)
            dragProgress = clamped
            if (rowList.isEmpty()) return

            val targetScrollPx = clamped * maxScrollPx

            // 1. Binary search to find scroll target row (for top of viewport)
            var low = 0
            var high = totalRows - 1
            var targetRowIndex = 0
            while (low <= high) {
                val mid = (low + high) ushr 1
                if (rowOffsets[mid] <= targetScrollPx) {
                    targetRowIndex = mid
                    low = mid + 1
                } else {
                    high = mid - 1
                }
            }

            targetGridIndex = rowList[targetRowIndex].gridItemIndex
            targetScrollOffsetPx = (targetScrollPx - rowOffsets[targetRowIndex]).roundToInt().coerceAtLeast(0)

            // 2. Binary search to find the item spatially aligned with the floating thumb (Aves viewOffset + thumbOffset)
            val bubbleContentPx = (targetScrollPx + (clamped * viewportHeightPx)).coerceIn(0f, totalContentHeightPx)
            var bLow = 0
            var bHigh = totalRows - 1
            var bubbleRowIndex = 0
            while (bLow <= bHigh) {
                val bMid = (bLow + bHigh) ushr 1
                if (rowOffsets[bMid] <= bubbleContentPx) {
                    bubbleRowIndex = bMid
                    bLow = bMid + 1
                } else {
                    bHigh = bMid - 1
                }
            }

            val bubbleRowInfo = rowList[bubbleRowIndex]
            val item = bubbleRowInfo.mediaItem
            if (item != null) {
                val timeMs = if (item.dateTakenMs > 0) {
                    item.dateTakenMs
                } else if (item.dateModifiedSec > 0) {
                    item.dateModifiedSec * 1000L
                } else {
                    item.dateAddedSec * 1000L
                }

                if (timeMs > 0) {
                    val date = Instant.ofEpochMilli(timeMs).atZone(ZoneId.systemDefault()).toLocalDate()
                    bubbleDateText = date.format(fullDateFormatter)
                } else {
                    bubbleDateText = item.displayName
                }
            } else if (bubbleRowInfo.sectionLabel.isNotBlank()) {
                bubbleDateText = bubbleRowInfo.sectionLabel
            }
        }

        // Date Bubble (Aves ScrollLabel style: Month Day, Year only)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = thumbWidthDp + 16.dp)
                .graphicsLayer {
                    val idealY = currentThumbOffset() + (thumbHeightPx / 2f) - (bubbleHeightPx / 2f)
                    val minY = topPaddingPx + 4.dp.toPx()
                    val maxY = containerHeightPx - bottomPaddingPx - bubbleHeightPx - 4.dp.toPx()
                    translationY = if (maxY > minY) idealY.coerceIn(minY, maxY) else idealY
                }
                .onSizeChanged { bubbleHeightPx = it.height }
        ) {
            AnimatedVisibility(
                visible = isDragging && bubbleDateText.isNotEmpty(),
                enter = fadeIn(tween(140)) + scaleIn(tween(140), initialScale = 0.88f),
                exit = fadeOut(tween(180)) + scaleOut(tween(180), targetScale = 0.92f),
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 6.dp,
                    modifier = Modifier.shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(16.dp),
                        spotColor = Color(0x38000000),
                        ambientColor = Color(0x22000000),
                    )
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = bubbleDateText,
                            color = Color(0xFF1E1E1E),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Thumb Visual: GPU translated via graphicsLayer to eliminate recomposition jank
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 1.dp)
                .graphicsLayer {
                    translationY = currentThumbOffset()
                    alpha = thumbAlpha
                }
        ) {
            AvesThumbVisual(
                widthDp = thumbWidthDp,
                heightDp = thumbHeightDp,
                accentColor = accentColor,
            )
        }

        // Non-interfering gesture detector: tightly wraps the thumb so the rest of the track doesn't block grid scrolling
        // During drag, the hit box locks its position to prevent local coordinate jitter, while the visual thumb moves freely.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .width(48.dp) // 48dp wide for touch slop
                .height(thumbHeightDp + 40.dp) // thumb height + 20dp touch slop on top/bottom
                .offset {
                    val yPos = if (isDragging) dragStartThumbY else (currentThumbOffset() - 20.dp.toPx())
                    androidx.compose.ui.unit.IntOffset(x = 0, y = yPos.roundToInt())
                }
                .pointerInput(maxThumbOffset, topPaddingPx) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        if (thumbAlpha < 0.1f) return@awaitEachGesture

                        down.consume()
                        
                        // Capture the current visual offset BEFORE we flip isDragging to true
                        // otherwise currentThumbOffset() would instantly read the stale dragProgress from a previous drag!
                        val initialThumbY = currentThumbOffset()
                        dragStartThumbY = initialThumbY - 20.dp.toPx()
                        
                        var accumulatedOffset = (initialThumbY - topPaddingPx).coerceIn(0f, maxThumbOffset)
                        if (maxThumbOffset > 0f) {
                            updateDrag(accumulatedOffset / maxThumbOffset)
                        }

                        isDragging = true
                        thumbVisibleTarget = true

                        var lastY = down.position.y

                        try {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) break

                                val dy = change.position.y - lastY
                                lastY = change.position.y
                                change.consume()

                                accumulatedOffset = (accumulatedOffset + dy).coerceIn(0f, maxThumbOffset)
                                val newProgress = if (maxThumbOffset > 0f) accumulatedOffset / maxThumbOffset else 0f
                                updateDrag(newProgress)
                            }
                        } finally {
                            isDragging = false
                        }
                    }
                }
        )
    }
}

/**
 * Visual layout matching Aves ScrollThumb:
 * - 48dp height, 20dp width
 * - Outer pill: dark translucent pill background (Colors.black26 in Aves)
 * - Inner pill: theme accent color pill with 10dp rounded corners and elevation
 * - Centered chevron arrow cutout notches pointing up and down (matching Aves ArrowClipper)
 */
@Composable
private fun AvesThumbVisual(
    widthDp: androidx.compose.ui.unit.Dp,
    heightDp: androidx.compose.ui.unit.Dp,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val isLightAccent = accentColor.luminance() > 0.5f
    val chevronColor = if (isLightAccent) Color(0xFF1E1E1E) else Color.White

    Box(
        modifier = modifier
            .size(width = widthDp, height = heightDp)
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(10.dp))
    ) {
        // Accent thumb pill with up/down arrows
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val pillCornerRadius = 10.dp.toPx()
            // Draw inner accent pill
            drawRoundRect(
                color = accentColor,
                cornerRadius = CornerRadius(pillCornerRadius, pillCornerRadius)
            )

            // Draw up and down chevron arrow notches in the center (Aves ArrowClipper style)
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val arrowWidthPx = 7.dp.toPx()
            val arrowHeightPx = 3.5.dp.toPx()
            val arrowStrokePx = 1.6.dp.toPx()

            // Top chevron pointing UP (^)
            val topArrowCenterY = centerY - 3.2.dp.toPx()
            val upArrowPath = Path().apply {
                moveTo(centerX - arrowWidthPx / 2f, topArrowCenterY + arrowHeightPx / 2f)
                lineTo(centerX, topArrowCenterY - arrowHeightPx / 2f)
                lineTo(centerX + arrowWidthPx / 2f, topArrowCenterY + arrowHeightPx / 2f)
            }
            drawPath(
                path = upArrowPath,
                color = chevronColor,
                style = Stroke(width = arrowStrokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Bottom chevron pointing DOWN (v)
            val bottomArrowCenterY = centerY + 3.2.dp.toPx()
            val downArrowPath = Path().apply {
                moveTo(centerX - arrowWidthPx / 2f, bottomArrowCenterY - arrowHeightPx / 2f)
                lineTo(centerX, bottomArrowCenterY + arrowHeightPx / 2f)
                lineTo(centerX + arrowWidthPx / 2f, bottomArrowCenterY - arrowHeightPx / 2f)
            }
            drawPath(
                path = downArrowPath,
                color = chevronColor,
                style = Stroke(width = arrowStrokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}
