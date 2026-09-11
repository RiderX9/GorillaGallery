package com.gorilla.gallery.ui.components

import com.gorilla.gallery.ui.theme.GlassDepth
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.layout
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.gorilla.gallery.ui.theme.DesignTokens
import com.gorilla.gallery.ui.theme.LiquidGlassSurface
import com.gorilla.gallery.ui.theme.LocalAppColors
import com.gorilla.gallery.ui.theme.LocalLiquidGlassContentBackdrop
import com.gorilla.gallery.ui.theme.SheetShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModalSheetScaffold(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    skipPartiallyExpanded: Boolean = true,
    shape: RoundedCornerShape = SheetShape,
    enableLens: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val appColors = LocalAppColors.current
    val contentBackdrop = LocalLiquidGlassContentBackdrop.current
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = skipPartiallyExpanded,
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = shape,
        containerColor = Color.Transparent,
        scrimColor = Color.Black.copy(alpha = 0.55f),
        contentWindowInsets = { WindowInsets(0) },
        dragHandle = null,
        // No max-width cap and no forced height — the sheet sizes to its (scrollable) content
        // and can be dragged taller, up to full screen.
        sheetMaxWidth = Dp.Unspecified,
        modifier = Modifier.fillMaxWidth()
    ) {
        val extraBottomPadding = 6.dp
        LiquidGlassSurface(
            depth = GlassDepth.MID,
            shape = shape,
            shadow = false,
            surfaceColor = DesignTokens.BgSurface.copy(alpha = 0.92f),
            tintAlphaOverride = 0.07f,
            saturationOverride = 1.55f,
            enableLens = enableLens,
            backdrop = contentBackdrop,
            modifier = modifier
                .zIndex(-1f)
                .fillMaxWidth()
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    layout(placeable.width, placeable.height - extraBottomPadding.roundToPx()) {
                        placeable.placeRelative(0, 0)
                    }
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = extraBottomPadding)
            ) {
                // Centered drag handle inside the LiquidGlassSurface (8.dp top padding before the handle)
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 8.dp)
                        .size(width = 32.dp, height = 4.dp)
                        .background(
                            color = if (appColors.isDark) {
                                DesignTokens.BgGlassStrong
                            } else {
                                Color.Black.copy(alpha = 0.28f)
                            },
                            shape = RoundedCornerShape(6.dp)
                        )
                )

                // Enclosing box with 12.dp top padding and WindowInsets.navigationBars as bottom padding on content
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    content()
                }
            }
        }
    }
}
