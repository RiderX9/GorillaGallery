package com.gorilla.gallery.ui.components

import androidx.compose.runtime.getValue

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gorilla.gallery.ui.theme.LocalAppColors
import com.gorilla.gallery.ui.theme.LocalDynamicColors
import com.gorilla.gallery.ui.theme.pressScale

data class GlassSettingsItem(
    val title: String = "",
    val subtitle: String? = null,
    val icon: ImageVector? = null,
    val trailingContent: (@Composable () -> Unit)? = null,
    val bottomContent: (@Composable () -> Unit)? = null,
    val customContent: (@Composable () -> Unit)? = null,
    val onClick: (() -> Unit)? = null
)

@Composable
fun GlassSettingsGroup(
    title: String? = null,
    items: List<GlassSettingsItem>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        title?.let {
            Text(
                text = it,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = LocalDynamicColors.current.accent,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp, top = 8.dp)
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items.forEachIndexed { index, item ->
                val shape = when {
                    items.size == 1 -> RoundedCornerShape(22.dp)
                    index == 0 -> RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
                    index == items.size - 1 -> RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 22.dp, bottomEnd = 22.dp)
                    else -> RoundedCornerShape(8.dp)
                }
                
                GlassSettingsItemRow(item = item, shape = shape)
            }
        }
    }
}

@Composable
private fun GlassSettingsItemRow(
    item: GlassSettingsItem,
    shape: Shape
) {
    val appColors = LocalAppColors.current
    val accent = LocalDynamicColors.current.accent
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(scale = 0.96f)
            .clip(shape)
            .background(appColors.bgSurface.copy(alpha = 0.94f))
            .border(1.dp, appColors.borderGlass, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = item.onClick != null,
                onClick = { item.onClick?.invoke() }
            )
    ) {
        if (pressed && item.onClick != null) {
            Box(
                Modifier
                    .matchParentSize()
                    .background(Color.White.copy(alpha = 0.10f), shape)
            )
        }
        if (item.customContent != null) {
            item.customContent.invoke()
        } else {
            Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item.icon?.let { icon ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(accent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                    }
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = appColors.textPrimary
                        )
                        item.subtitle?.let { desc ->
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = desc,
                                fontSize = 13.sp,
                                color = appColors.textSecondary
                            )
                        }
                    }
                    
                    if (item.trailingContent != null) {
                        Spacer(modifier = Modifier.width(16.dp))
                        item.trailingContent.invoke()
                    } else if (item.onClick != null) {
                        Spacer(modifier = Modifier.width(16.dp))
                        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = appColors.textSecondary)
                    }
                }
                item.bottomContent?.let { bottom ->
                    bottom()
                }
            }
        }
    }
}
