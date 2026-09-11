package com.gorilla.gallery.ui.screens.editor

import androidx.compose.runtime.setValue

import androidx.compose.runtime.getValue

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.dp
import com.gorilla.gallery.data.model.CropAspect
import com.gorilla.gallery.ui.theme.LocalDynamicColors
import kotlin.math.abs

enum class TouchZone {
    NONE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, TOP_EDGE, BOTTOM_EDGE, LEFT_EDGE, RIGHT_EDGE, INSIDE
}

@Composable
fun CropOverlay(
    cropAspect: CropAspect,
    cropRect: Rect?,
    onCropRectChange: (Rect) -> Unit,
    imageWidth: Float, // in Dp
    imageHeight: Float, // in Dp
) {
    if (cropRect == null) return

    // Local state ensures dragging draws instantaneously on the composition thread without lagging.
    var localCropRect by remember(cropRect) { mutableStateOf(cropRect) }
    var isDragging by remember { mutableStateOf(false) }

    val animatedCropRect by androidx.compose.animation.core.animateRectAsState(
        targetValue = localCropRect ?: Rect.Zero,
        animationSpec = if (isDragging) androidx.compose.animation.core.snap() else androidx.compose.animation.core.spring(dampingRatio = 0.8f, stiffness = 300f),
        label = "cropRectAnimation"
    )

    Box(
        modifier = Modifier
            .size(imageWidth.dp, imageHeight.dp)
            .pointerInput(cropAspect, imageWidth, imageHeight) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startRect = localCropRect ?: return@awaitEachGesture
                    
                    val density = this.density
                    val startX = down.position.x / density
                    val startY = down.position.y / density
                    
                    val dragZone = getTouchZone(startX, startY, startRect)
                    if (dragZone == TouchZone.NONE) return@awaitEachGesture
                    
                    down.consume()
                    isDragging = true
                    
                    var currentRect = startRect
                    
                    drag(down.id) { change ->
                        val dragAmount = change.positionChange()
                        val dx = dragAmount.x / density
                        val dy = dragAmount.y / density
                        
                        val newRect = updateCropRect(
                            cropRect = currentRect,
                            zone = dragZone,
                            dx = dx,
                            dy = dy,
                            imageWidth = imageWidth,
                            imageHeight = imageHeight,
                            cropAspect = cropAspect
                        )
                        
                        // Synchronous constraint clamping inside the drag loop
                        val clampedRect = Rect(
                            left = newRect.left.coerceIn(0f, imageWidth),
                            top = newRect.top.coerceIn(0f, imageHeight),
                            right = newRect.right.coerceIn(0f, imageWidth),
                            bottom = newRect.bottom.coerceIn(0f, imageHeight)
                        )
                        
                        currentRect = clampedRect
                        localCropRect = clampedRect
                        change.consume()
                        
                        onCropRectChange(clampedRect)
                    }
                    isDragging = false
                }
            }
    ) {
        val accent = LocalDynamicColors.current.accent

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val density = this.density
            
            val rect = animatedCropRect
            if (rect.width <= 0f || rect.height <= 0f) return@Canvas
            
            // Map cropRect from Dp to Px for canvas drawing
            val rectPx = Rect(
                left = rect.left * density,
                top = rect.top * density,
                right = rect.right * density,
                bottom = rect.bottom * density
            )
            
            // 1. Semi-transparent dark scrim (rgba(0,0,0,0.45)) outside the crop rect
            val scrimColor = Color.Black.copy(alpha = 0.45f)
            // Top
            drawRect(scrimColor, size = androidx.compose.ui.geometry.Size(w, rectPx.top.coerceAtLeast(0f)))
            // Bottom
            drawRect(
                scrimColor,
                topLeft = Offset(0f, rectPx.bottom),
                size = androidx.compose.ui.geometry.Size(w, (h - rectPx.bottom).coerceAtLeast(0f))
            )
            // Left
            drawRect(
                scrimColor,
                topLeft = Offset(0f, rectPx.top),
                size = androidx.compose.ui.geometry.Size(rectPx.left.coerceAtLeast(0f), rectPx.height.coerceAtLeast(0f))
            )
            // Right
            drawRect(
                scrimColor,
                topLeft = Offset(rectPx.right, rectPx.top),
                size = androidx.compose.ui.geometry.Size((w - rectPx.right).coerceAtLeast(0f), rectPx.height.coerceAtLeast(0f))
            )
            
            // 2. Crop Rect border (1.dp, White 40%)
            drawRect(
                color = Color.White.copy(alpha = 0.40f),
                topLeft = rectPx.topLeft,
                size = rectPx.size,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
            )
            
            // 3. 3x3 rule-of-thirds grid inside the crop rect (1.dp lines, White 25%)
            val gridColor = Color.White.copy(alpha = 0.25f)
            val strokeW = 1.dp.toPx()
            
            val col1 = rectPx.left + rectPx.width / 3f
            val col2 = rectPx.left + 2f * rectPx.width / 3f
            val row1 = rectPx.top + rectPx.height / 3f
            val row2 = rectPx.top + 2f * rectPx.height / 3f
            
            // Vertical lines
            drawLine(gridColor, Offset(col1, rectPx.top), Offset(col1, rectPx.bottom), strokeWidth = strokeW)
            drawLine(gridColor, Offset(col2, rectPx.top), Offset(col2, rectPx.bottom), strokeWidth = strokeW)
            // Horizontal lines
            drawLine(gridColor, Offset(rectPx.left, row1), Offset(rectPx.right, row1), strokeWidth = strokeW)
            drawLine(gridColor, Offset(rectPx.left, row2), Offset(rectPx.right, row2), strokeWidth = strokeW)

            // 4. Corner Handles (24.dp square, RadiusSmall (6.dp), Accent color border)
            val cornerSizePx = 24 * density
            val halfCornerSizePx = 12 * density
            val cornerRadiusPx = 6 * density
            val strokeCornerW = 1.5.dp.toPx()

            val corners = listOf(
                rectPx.topLeft,
                rectPx.topRight,
                rectPx.bottomLeft,
                rectPx.bottomRight
            )

            corners.forEach { corner ->
                val topLeftCorner = Offset(corner.x - halfCornerSizePx, corner.y - halfCornerSizePx)
                // Draw fill
                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.20f),
                    topLeft = topLeftCorner,
                    size = androidx.compose.ui.geometry.Size(cornerSizePx, cornerSizePx),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadiusPx, cornerRadiusPx)
                )
                // Draw border
                drawRoundRect(
                    color = accent,
                    topLeft = topLeftCorner,
                    size = androidx.compose.ui.geometry.Size(cornerSizePx, cornerSizePx),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadiusPx, cornerRadiusPx),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeCornerW)
                )
            }

            // 5. Edge midpoint handles (16.dp x 8.dp or 8.dp x 16.dp)
            val edgeW = 16 * density
            val edgeH = 8 * density
            val midX = rectPx.left + rectPx.width / 2f
            val midY = rectPx.top + rectPx.height / 2f
            val strokeEdgeW = 1.dp.toPx()
            val edgeColor = Color.White

            // Top edge handle (horizontal 16x8)
            val topEdgeTl = Offset(midX - edgeW / 2f, rectPx.top - edgeH / 2f)
            drawRect(
                color = Color.Black.copy(alpha = 0.20f),
                topLeft = topEdgeTl,
                size = androidx.compose.ui.geometry.Size(edgeW, edgeH)
            )
            drawRect(
                color = edgeColor,
                topLeft = topEdgeTl,
                size = androidx.compose.ui.geometry.Size(edgeW, edgeH),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeEdgeW)
            )

            // Bottom edge handle (horizontal 16x8)
            val bottomEdgeTl = Offset(midX - edgeW / 2f, rectPx.bottom - edgeH / 2f)
            drawRect(
                color = Color.Black.copy(alpha = 0.20f),
                topLeft = bottomEdgeTl,
                size = androidx.compose.ui.geometry.Size(edgeW, edgeH)
            )
            drawRect(
                color = edgeColor,
                topLeft = bottomEdgeTl,
                size = androidx.compose.ui.geometry.Size(edgeW, edgeH),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeEdgeW)
            )

            // Left edge handle (vertical 8x16)
            val leftEdgeTl = Offset(rectPx.left - edgeH / 2f, midY - edgeW / 2f)
            drawRect(
                color = Color.Black.copy(alpha = 0.20f),
                topLeft = leftEdgeTl,
                size = androidx.compose.ui.geometry.Size(edgeH, edgeW)
            )
            drawRect(
                color = edgeColor,
                topLeft = leftEdgeTl,
                size = androidx.compose.ui.geometry.Size(edgeH, edgeW),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeEdgeW)
            )

            // Right edge handle (vertical 8x16)
            val rightEdgeTl = Offset(rectPx.right - edgeH / 2f, midY - edgeW / 2f)
            drawRect(
                color = Color.Black.copy(alpha = 0.20f),
                topLeft = rightEdgeTl,
                size = androidx.compose.ui.geometry.Size(edgeH, edgeW)
            )
            drawRect(
                color = edgeColor,
                topLeft = rightEdgeTl,
                size = androidx.compose.ui.geometry.Size(edgeH, edgeW),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeEdgeW)
            )
        }
    }
}

private fun getTouchZone(x: Float, y: Float, rect: Rect): TouchZone {
    val threshold = 24f
    
    // Check corners first (higher priority for overlaps)
    if (abs(x - rect.left) < threshold && abs(y - rect.top) < threshold) return TouchZone.TOP_LEFT
    if (abs(x - rect.right) < threshold && abs(y - rect.top) < threshold) return TouchZone.TOP_RIGHT
    if (abs(x - rect.left) < threshold && abs(y - rect.bottom) < threshold) return TouchZone.BOTTOM_LEFT
    if (abs(x - rect.right) < threshold && abs(y - rect.bottom) < threshold) return TouchZone.BOTTOM_RIGHT
    
    // Check edges
    val midX = rect.left + rect.width / 2f
    val midY = rect.top + rect.height / 2f
    if (abs(x - midX) < threshold && abs(y - rect.top) < threshold) return TouchZone.TOP_EDGE
    if (abs(x - midX) < threshold && abs(y - rect.bottom) < threshold) return TouchZone.BOTTOM_EDGE
    if (abs(x - rect.left) < threshold && abs(y - midY) < threshold) return TouchZone.LEFT_EDGE
    if (abs(x - rect.right) < threshold && abs(y - midY) < threshold) return TouchZone.RIGHT_EDGE
    
    // Check inside
    if (rect.contains(Offset(x, y))) return TouchZone.INSIDE
    
    return TouchZone.NONE
}

private fun updateCropRect(
    cropRect: Rect,
    zone: TouchZone,
    dx: Float,
    dy: Float,
    imageWidth: Float,
    imageHeight: Float,
    cropAspect: CropAspect
): Rect {
    val minSize = 40f
    
    if (zone == TouchZone.INSIDE) {
        val w = cropRect.width
        val h = cropRect.height
        val newLeft = (cropRect.left + dx).coerceIn(0f, imageWidth - w)
        val newTop = (cropRect.top + dy).coerceIn(0f, imageHeight - h)
        return Rect(newLeft, newTop, newLeft + w, newTop + h)
    }
    
    val ratio = if (cropAspect == CropAspect.ORIGINAL) {
        imageWidth / imageHeight
    } else {
        cropAspect.ratio
    }

    if (ratio == null) {
        if (zone == TouchZone.TOP_EDGE) {
            val newTop = (cropRect.top + dy).coerceIn(0f, cropRect.bottom - minSize)
            return Rect(cropRect.left, newTop, cropRect.right, cropRect.bottom)
        }
        if (zone == TouchZone.BOTTOM_EDGE) {
            val newBottom = (cropRect.bottom + dy).coerceIn(cropRect.top + minSize, imageHeight)
            return Rect(cropRect.left, cropRect.top, cropRect.right, newBottom)
        }
        if (zone == TouchZone.LEFT_EDGE) {
            val newLeft = (cropRect.left + dx).coerceIn(0f, cropRect.right - minSize)
            return Rect(newLeft, cropRect.top, cropRect.right, cropRect.bottom)
        }
        if (zone == TouchZone.RIGHT_EDGE) {
            val newRight = (cropRect.right + dx).coerceIn(cropRect.left + minSize, imageWidth)
            return Rect(cropRect.left, cropRect.top, newRight, cropRect.bottom)
        }
    }
    
    if (ratio == null) {
        when (zone) {
            TouchZone.TOP_LEFT -> {
                val newLeft = (cropRect.left + dx).coerceIn(0f, cropRect.right - minSize)
                val newTop = (cropRect.top + dy).coerceIn(0f, cropRect.bottom - minSize)
                return Rect(newLeft, newTop, cropRect.right, cropRect.bottom)
            }
            TouchZone.TOP_RIGHT -> {
                val newRight = (cropRect.right + dx).coerceIn(cropRect.left + minSize, imageWidth)
                val newTop = (cropRect.top + dy).coerceIn(0f, cropRect.bottom - minSize)
                return Rect(cropRect.left, newTop, newRight, cropRect.bottom)
            }
            TouchZone.BOTTOM_LEFT -> {
                val newLeft = (cropRect.left + dx).coerceIn(0f, cropRect.right - minSize)
                val newBottom = (cropRect.bottom + dy).coerceIn(cropRect.top + minSize, imageHeight)
                return Rect(newLeft, cropRect.top, cropRect.right, newBottom)
            }
            TouchZone.BOTTOM_RIGHT -> {
                val newRight = (cropRect.right + dx).coerceIn(cropRect.left + minSize, imageWidth)
                val newBottom = (cropRect.bottom + dy).coerceIn(cropRect.top + minSize, imageHeight)
                return Rect(cropRect.left, cropRect.top, newRight, newBottom)
            }
            else -> return cropRect
        }
    } else {
        when (zone) {
            TouchZone.BOTTOM_RIGHT -> {
                val d = if (abs(dx) > abs(dy * ratio)) dx else dy * ratio
                var w = (cropRect.width + d).coerceAtLeast(minSize)
                var h = w / ratio
                
                if (cropRect.left + w > imageWidth) {
                    w = imageWidth - cropRect.left
                    h = w / ratio
                }
                if (cropRect.top + h > imageHeight) {
                    h = imageHeight - cropRect.top
                    w = h * ratio
                }
                return Rect(cropRect.left, cropRect.top, cropRect.left + w, cropRect.top + h)
            }
            TouchZone.BOTTOM_LEFT -> {
                val d = if (abs(dx) > abs(dy * ratio)) -dx else dy * ratio
                var w = (cropRect.width + d).coerceAtLeast(minSize)
                var h = w / ratio
                
                if (cropRect.right - w < 0f) {
                    w = cropRect.right
                    h = w / ratio
                }
                if (cropRect.top + h > imageHeight) {
                    h = imageHeight - cropRect.top
                    w = h * ratio
                }
                return Rect(cropRect.right - w, cropRect.top, cropRect.right, cropRect.top + h)
            }
            TouchZone.TOP_RIGHT -> {
                val d = if (abs(dx) > abs(dy * ratio)) dx else -dy * ratio
                var w = (cropRect.width + d).coerceAtLeast(minSize)
                var h = w / ratio
                
                if (cropRect.left + w > imageWidth) {
                    w = imageWidth - cropRect.left
                    h = w / ratio
                }
                if (cropRect.bottom - h < 0f) {
                    h = cropRect.bottom
                    w = h * ratio
                }
                return Rect(cropRect.left, cropRect.bottom - h, cropRect.left + w, cropRect.bottom)
            }
            TouchZone.TOP_LEFT -> {
                val d = if (abs(dx) > abs(dy * ratio)) -dx else -dy * ratio
                var w = (cropRect.width + d).coerceAtLeast(minSize)
                var h = w / ratio
                
                if (cropRect.right - w < 0f) {
                    w = cropRect.right
                    h = w / ratio
                }
                if (cropRect.bottom - h < 0f) {
                    h = cropRect.bottom
                    w = h * ratio
                }
                return Rect(cropRect.right - w, cropRect.bottom - h, cropRect.right, cropRect.bottom)
            }
            else -> return cropRect
        }
    }
}

fun snapToAspect(current: Rect, aspect: CropAspect, imageWidth: Float, imageHeight: Float): Rect {
    val ratio = if (aspect == CropAspect.ORIGINAL) {
        imageWidth / imageHeight
    } else {
        aspect.ratio ?: return current
    }
    
    val cx = current.center.x
    val cy = current.center.y
    
    val maxW = (2f * cx).coerceAtMost(2f * (imageWidth - cx))
    val maxH = (2f * cy).coerceAtMost(2f * (imageHeight - cy))
    
    var w = maxW
    var h = w / ratio
    if (h > maxH) {
        h = maxH
        w = h * ratio
    }
    
    val left = cx - w / 2f
    val top = cy - h / 2f
    val right = cx + w / 2f
    val bottom = cy + h / 2f
    return Rect(left, top, right, bottom)
}
