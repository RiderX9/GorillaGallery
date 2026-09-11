package com.gorilla.gallery.ui.components

import androidx.compose.runtime.setValue

import androidx.compose.runtime.getValue

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.border
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.gorilla.gallery.ui.theme.CapsuleShape
import com.gorilla.gallery.ui.theme.LocalAppColors
import com.gorilla.gallery.ui.theme.LocalDynamicColors
import com.gorilla.gallery.ui.theme.DesignTokens
import com.gorilla.gallery.ui.theme.SpringSpecs
import com.gorilla.gallery.ui.theme.accentBloom
import com.gorilla.gallery.ui.theme.pressScale
import com.gorilla.gallery.ui.theme.rememberHaptic
import kotlin.math.abs

/** Glass capsule segmented control with a draggable selected pill. */
@Composable
fun <T> GlassSegmentedControl(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = LocalDynamicColors.current.accent
    val haptic = rememberHaptic()
    val density = LocalDensity.current
    val appColors = LocalAppColors.current
    val selectedIndex = options.indexOfFirst { it.first == selected }.coerceAtLeast(0)
    val selectedIndexState by rememberUpdatedState(selectedIndex)
    val onSelectState by rememberUpdatedState(onSelect)
    val itemSpacing = 4.dp
    val horizontalPadding = 16.dp
    val verticalPadding = 8.dp
    var segmentWidths by remember(options) { mutableStateOf(List(options.size) { 0 }) }
    var rowWidthPx by remember { mutableIntStateOf(0) }
    var dragIndex by remember { mutableIntStateOf(selectedIndex) }
    var dragOffsetPx by remember { mutableFloatStateOf(Float.NaN) }

    fun segmentOffsetPx(index: Int): Float {
        val spacingPx = with(density) { itemSpacing.toPx() }
        return segmentWidths.take(index).sum().toFloat() + spacingPx * index
    }

    fun nearestIndexForX(xPx: Float): Int {
        if (options.isEmpty()) return 0
        return options.indices.minBy { index ->
            val widthPx = segmentWidths.getOrNull(index)?.takeIf { it > 0 } ?: 1
            abs(xPx - (segmentOffsetPx(index) + widthPx / 2f))
        }
    }

    fun selectAt(xPx: Float) {
        if (options.isEmpty()) return
        val index = nearestIndexForX(xPx).coerceIn(0, options.lastIndex)
        val widthPx = segmentWidths.getOrNull(index)?.takeIf { it > 0 } ?: 1
        val maxOffsetPx = (rowWidthPx - widthPx).coerceAtLeast(0).toFloat()
        dragOffsetPx = (xPx - widthPx / 2f).coerceIn(0f, maxOffsetPx)
        if (index != dragIndex) {
            dragIndex = index
            haptic()
        }
        if (index != selectedIndexState) {
            onSelectState(options[index].first)
        }
    }

    LaunchedEffect(selectedIndex) {
        dragIndex = selectedIndex
    }

    val trackShape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp)
    val optionShape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)

    Box(
        modifier = modifier
            .clip(trackShape)
            .background(appColors.bgGlass)
            .border(1.dp, appColors.borderGlass, trackShape)
    ) {
        Box(
            Modifier
                .padding(4.dp)
                .onSizeChanged { rowWidthPx = it.width }
                .pointerInput(options, segmentWidths, rowWidthPx) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            dragIndex = selectedIndexState
                            selectAt(offset.x)
                        },
                        onHorizontalDrag = { change, _ ->
                            selectAt(change.position.x)
                            change.consume()
                        },
                        onDragEnd = { dragOffsetPx = Float.NaN },
                        onDragCancel = { dragOffsetPx = Float.NaN },
                    )
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            val selectedWidthPx = segmentWidths.getOrNull(selectedIndex).takeIf { it != null && it > 0 } ?: 0
            if (selectedWidthPx > 0) {
                val selectedOffsetDp by animateDpAsState(
                    targetValue = if (dragOffsetPx.isNaN()) {
                        with(density) { segmentOffsetPx(selectedIndex).toDp() }
                    } else {
                        with(density) { dragOffsetPx.toDp() }
                    },
                    animationSpec = spring(dampingRatio = 0.68f, stiffness = 380f),
                    label = "segmentedPillOffset",
                )
                val selectedWidthDp by animateDpAsState(
                    targetValue = with(density) { selectedWidthPx.toDp() },
                    animationSpec = SpringSpecs.DpSpring,
                    label = "segmentedPillWidth",
                )

                val activeBgColor = accent.copy(alpha = if (appColors.isDark) 0.22f else 0.26f)
                Box(
                    Modifier
                        .offset(x = selectedOffsetDp)
                        .size(width = selectedWidthDp, height = 34.dp)
                        .clip(optionShape)
                        .background(activeBgColor)
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = if (appColors.isDark) 0.24f else 0.48f),
                            shape = optionShape,
                        ),
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.zIndex(1f),
            ) {
                options.forEachIndexed { index, (value, label) ->
                    val isSel = index == selectedIndex
                    val interaction = remember { MutableInteractionSource() }
                    Box(
                        Modifier
                            .clip(optionShape)
                            .pressScale(interaction, pressedScale = 0.94f)
                            .clickable(interaction, indication = null) {
                                if (!isSel) haptic()
                                onSelect(value)
                            }
                            .onSizeChanged { size ->
                                if (segmentWidths.getOrNull(index) != size.width) {
                                    segmentWidths = segmentWidths.toMutableList().also { widths ->
                                        widths[index] = size.width
                                    }
                                }
                            }
                            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            label,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSel) accent else DesignTokens.TextSecondary,
                        )
                    }
                }
            }
        }
    }
}

/** A labelled setting block wrapped in glass. */
@Composable
fun SettingBlock(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val appColors = LocalAppColors.current
    val cardShape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp)
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(appColors.bgSurface.copy(alpha = 0.94f))
            .border(1.dp, appColors.borderGlass, cardShape)
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = appColors.textPrimary)
            if (subtitle != null) {
                Text(subtitle, fontSize = 13.sp, color = appColors.textSecondary, modifier = Modifier.padding(top = 2.dp))
            }
            Box(Modifier.padding(top = 12.dp)) { content() }
        }
    }
}

/** A glass switch row (title left, toggle right). */
@Composable
fun GlassSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
) {
    val accent = LocalDynamicColors.current.accent
    val haptic = rememberHaptic()
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val appColors = LocalAppColors.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(scale = 0.94f, enabled = enabled)
            .clip(shape)
            .background(appColors.bgSurface.copy(alpha = 0.94f))
            .border(1.dp, appColors.borderGlass, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
            ) {
                haptic()
                onCheckedChange(!checked)
            }
    ) {
        if (pressed) {
            Box(
                Modifier
                    .matchParentSize()
                    .background(Color.White.copy(alpha = 0.10f), shape)
            )
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                        color = if (enabled) LocalAppColors.current.textPrimary else LocalAppColors.current.textDisabled)
                    if (subtitle != null) {
                        Text(subtitle, fontSize = 13.sp, color = LocalAppColors.current.textSecondary, modifier = Modifier.padding(top = 2.dp))
                    }
                }
                val thumbOffset by animateFloatAsState(if (checked) 20f else 0f, SpringSpecs.Standard, label = "thumb")
                val trackColor by animateColorAsState(
                    if (checked) accent.copy(alpha = 0.9f) else if (LocalAppColors.current.isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.12f),
                    SpringSpecs.ColorSpring, label = "track",
                )
                Box(
                    Modifier
                        .size(width = 46.dp, height = 28.dp)
                        .clip(CapsuleShape)
                        .background(trackColor),
                ) {
                    Box(
                        Modifier
                            .padding(4.dp)
                            .offset { androidx.compose.ui.unit.IntOffset(thumbOffset.dp.roundToPx(), 0) }
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                    )
                }
            }
    }
}

/** A horizontal glass slider in [valueRange]. */
@Composable
fun GlassSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    neutralValue: Float? = null,
) {
    val accent = LocalDynamicColors.current.accent
    val density = LocalDensity.current
    val onValueChangeState by rememberUpdatedState(onValueChange)
    var widthPx by remember { androidx.compose.runtime.mutableStateOf(1f) }
    var activeValue by remember(valueRange.start, valueRange.endInclusive) { mutableFloatStateOf(value) }
    val rangeSize = valueRange.endInclusive - valueRange.start
    val fraction = ((activeValue - valueRange.start) / rangeSize).coerceIn(0f, 1f)

    LaunchedEffect(value) {
        activeValue = value
    }

    fun updateValue(xPx: Float) {
        val f = (xPx / widthPx).coerceIn(0f, 1f)
        val rawValue = valueRange.start + f * rangeSize
        val newValue = neutralValue?.let { neutral ->
            val neutralFraction = ((neutral - valueRange.start) / rangeSize).coerceIn(0f, 1f)
            if (abs(f - neutralFraction) <= 0.025f) neutral else rawValue
        } ?: rawValue
        activeValue = newValue
        onValueChangeState(newValue)
    }

    Box(
        modifier
            .fillMaxWidth()
            .height(36.dp)
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(valueRange) {
                detectDragGestures(
                    onDragStart = { off -> updateValue(off.x) },
                    onDrag = { change, _ ->
                        updateValue(change.position.x)
                        change.consume()
                    },
                )
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        val trackBgColor = if (LocalAppColors.current.isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.12f)
        if (widthPx <= 1f) return@Box
        Box(Modifier.fillMaxWidth().height(6.dp).clip(CapsuleShape).background(trackBgColor))
        Box(Modifier.fillMaxWidth(fraction).height(6.dp).clip(CapsuleShape).background(accent))
        neutralValue?.let { neutral ->
            val neutralFraction = ((neutral - valueRange.start) / rangeSize).coerceIn(0f, 1f)
            val markerX = with(density) { (neutralFraction * widthPx).toDp() } - 1.dp
            Box(
                Modifier
                    .offset(x = markerX.coerceAtLeast(0.dp))
                    .size(width = 2.dp, height = 18.dp)
                    .clip(CapsuleShape)
                    .background(Color.White.copy(alpha = 0.72f)),
            )
        }
        val thumbX = with(density) { (fraction * widthPx).toDp() } - 9.dp
        Box(
            Modifier
                .offset(x = thumbX.coerceAtLeast(0.dp))
                .size(18.dp)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}

/** Accent colour swatch row (8 swatches incl. the adaptive "A"). */
@Composable
fun AccentSwatchRow(
    swatches: List<Pair<Color, Boolean>>, // color, isAdaptive
    selectedIndex: Int,
    onPick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = rememberHaptic()
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        swatches.forEachIndexed { i, (color, isAdaptive) ->
            val selected = i == selectedIndex
            val size by animateFloatAsState(if (selected) 38f else 32f, SpringSpecs.Bouncy, label = "swatch")
            Box(
                Modifier
                    .size(size.dp)
                    .pressScale(scale = 0.94f)
                    .accentBloom(color, selected, CircleShape)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(color, color)))
                    .border(
                        width = if (selected) 3.dp else 1.5.dp,
                        color = if (selected) Color.White else Color.White.copy(alpha = 0.2f),
                        shape = CircleShape,
                    )
                    .clickable { haptic(); onPick(i) },
                contentAlignment = Alignment.Center,
            ) {
                if (isAdaptive) Text("A", color = DesignTokens.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
