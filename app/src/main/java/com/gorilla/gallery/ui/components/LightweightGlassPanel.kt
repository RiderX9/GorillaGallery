package com.gorilla.gallery.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gorilla.gallery.ui.theme.DesignTokens
import com.gorilla.gallery.ui.theme.LocalAppColors

@Composable
fun LightweightGlassPanel(
    shape: RoundedCornerShape,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val appColors = LocalAppColors.current
    val baseColor = if (appColors.isDark) {
        DesignTokens.BgSurface.copy(alpha = 0.72f)
    } else {
        DesignTokens.BgSurface.copy(alpha = 0.64f)
    }
    val borderColor = if (appColors.isDark) {
        Color.White.copy(alpha = 0.10f)
    } else {
        Color.Black.copy(alpha = 0.08f)
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(baseColor)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (appColors.isDark) 0.055f else 0.16f),
                        Color.Transparent,
                    ),
                ),
            )
            .border(1.dp, borderColor, shape),
        content = content,
    )
}
