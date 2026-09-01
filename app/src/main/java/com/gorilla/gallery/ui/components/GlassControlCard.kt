package com.gorilla.gallery.ui.components

import androidx.compose.runtime.setValue

import androidx.compose.runtime.getValue

import com.gorilla.gallery.ui.theme.GlassDepth
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.gorilla.gallery.ui.theme.LiquidGlassSurface
import com.gorilla.gallery.ui.theme.LocalDynamicColors

/** Glass panel used for the video controls — same material as the music control card. */
@Composable
fun GlassControlCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    LiquidGlassSurface(depth = GlassDepth.HIGH, modifier = modifier) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), content = content)
    }
}

/** A draggable/tappable seek bar over a Long position/duration in milliseconds. */
@Composable
fun GlassSeekBar(
    position: Long,
    duration: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = LocalDynamicColors.current.accent
    val density = LocalDensity.current
    var widthPx by remember { mutableStateOf(1f) }
    val fraction = (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)

    Box(
        modifier
            .fillMaxWidth()
            .height(28.dp)
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(duration) {
                detectTapGestures { off -> onSeek(((off.x / widthPx).coerceIn(0f, 1f) * duration).toLong()) }
            }
            .pointerInput(duration) {
                detectDragGestures { change, _ ->
                    onSeek(((change.position.x / widthPx).coerceIn(0f, 1f) * duration).toLong())
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)))
        Box(Modifier.fillMaxWidth(fraction).height(4.dp).clip(CircleShape).background(accent))
        val thumbX = with(density) { (fraction * widthPx).toDp() } - 10.dp
        Box(
            Modifier
                .padding(start = thumbX.coerceAtLeast(0.dp))
                .size(20.dp)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}
