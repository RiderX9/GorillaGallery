package com.gorilla.gallery.ui.screens.settings

import androidx.compose.runtime.setValue

import androidx.compose.runtime.getValue

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gorilla.gallery.data.settings.AccentChoice
import com.gorilla.gallery.data.settings.AppSettings
import com.gorilla.gallery.ui.theme.ThemeMode
import com.gorilla.gallery.ui.theme.LocalAppColors
import com.gorilla.gallery.ui.theme.LocalDynamicColors
import com.gorilla.gallery.ui.theme.SpringSpecs
import com.gorilla.gallery.ui.theme.accentBloom
import com.gorilla.gallery.ui.theme.pressScale
import com.gorilla.gallery.ui.theme.rememberHaptic
import com.gorilla.gallery.ui.components.SettingBlock
import com.gorilla.gallery.ui.components.GlassSettingsItem
import androidx.compose.material.icons.outlined.Palette

@Composable
fun rememberThemeSettingsItem(
    settings: AppSettings,
    vm: SettingsViewModel,
): GlassSettingsItem {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val haptic = rememberHaptic()
    val colorLabel = settings.accent.name.lowercase().replace("_", " ").split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }

    return GlassSettingsItem(
        title = "Theme",
        subtitle = "${settings.themeMode.displayLabel()} • $colorLabel",
        icon = Icons.Outlined.Palette,
        trailingContent = {
            Text(
                text = if (expanded) "Hide" else "Edit",
                color = LocalDynamicColors.current.accent,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        haptic()
                        expanded = !expanded
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        },
        bottomContent = {
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(
                    modifier = Modifier.padding(top = 12.dp, bottom = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SettingBlock(
                        title = "Theme mode",
                        subtitle = "Choose how light and dark surfaces are displayed",
                    ) {
                        ThemeModeGrid(
                            selected = settings.themeMode,
                            onSelect = vm::setThemeMode,
                        )
                    }

                    SettingBlock(
                        title = "Accent color",
                        subtitle = colorLabel,
                    ) {
                        ColorSwatchRow(
                            choices = AccentChoice.entries,
                            selectedChoice = settings.accent,
                            onPick = vm::setAccent,
                            enabled = true,
                        )
                    }
                }
            }
        },
        onClick = {
            haptic()
            expanded = !expanded
        }
    )
}

private fun ThemeMode.displayLabel(): String =
    when (this) {
        ThemeMode.AUTO -> "Auto"
        ThemeMode.LIGHT -> "Light"
        ThemeMode.DARK -> "Dark"
        ThemeMode.AMOLED -> "AMOLED"
    }

private data class ThemeModeItem(
    val mode: ThemeMode,
    val label: String,
    val icon: ImageVector,
)

@Composable
fun ThemeModeGrid(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
) {
    val entries = listOf(
        ThemeModeItem(ThemeMode.AUTO, "Auto", Icons.Outlined.BrightnessAuto),
        ThemeModeItem(ThemeMode.LIGHT, "Light", Icons.Outlined.LightMode),
        ThemeModeItem(ThemeMode.DARK, "Dark", Icons.Outlined.DarkMode),
        ThemeModeItem(ThemeMode.AMOLED, "AMOLED", Icons.Outlined.Contrast),
    )
    val accent = LocalDynamicColors.current.accent
    val appColors = LocalAppColors.current
    val haptic = rememberHaptic()
    val trackShape = RoundedCornerShape(22.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(trackShape)
            .background(appColors.bgGlass)
            .border(1.dp, appColors.borderGlass, trackShape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        entries.forEach { item ->
            val isSel = item.mode == selected
            val interaction = remember { MutableInteractionSource() }
            val optionShape = RoundedCornerShape(18.dp)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .accentBloom(accent, active = isSel, shape = optionShape)
                    .pressScale(interaction, pressedScale = 0.95f)
                    .clip(optionShape)
                    .background(if (isSel) accent else Color.Transparent)
                    .clickable(interaction, indication = null) {
                        if (!isSel) haptic()
                        onSelect(item.mode)
                    }
                    .padding(vertical = 10.dp, horizontal = 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                val contentColor = if (isSel) {
                    if (accent == Color.White) Color.Black else Color.White
                } else {
                    appColors.textSecondary
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = contentColor,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = item.label,
                        color = contentColor,
                        fontSize = 11.sp,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
fun ColorSwatchRow(
    choices: List<AccentChoice>,
    selectedChoice: AccentChoice,
    onPick: (AccentChoice) -> Unit,
    enabled: Boolean = true,
) {
    val haptic = rememberHaptic()
    val borderGlass = LocalAppColors.current.borderGlass
    val isDark = LocalAppColors.current.isDark

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (enabled) Modifier else Modifier.alpha(0.4f)),
    ) {
        choices.chunked(5).forEach { rowChoices ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for (i in 0 until 5) {
                    if (i < rowChoices.size) {
                        val choice = rowChoices[i]
                        key(choice) {
                            val selected = enabled && choice == selectedChoice
                            val interaction = remember { MutableInteractionSource() }
                            val scale by animateFloatAsState(
                                targetValue = if (selected) 1.08f else 1f,
                                animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
                                label = "swatchScale",
                            )
                            val outlineWidth by animateDpAsState(
                                targetValue = if (selected) 3.dp else 1.5.dp,
                                animationSpec = SpringSpecs.DpSpring,
                                label = "swatchOutlineWidth",
                            )
                            val outlineColor by animateColorAsState(
                                targetValue = if (selected) Color.White else borderGlass,
                                animationSpec = SpringSpecs.ColorSpring,
                                label = "swatchOutlineColor",
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable(interaction, indication = null, enabled = enabled) {
                                        haptic()
                                        onPick(choice)
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .graphicsLayer { scaleX = scale; scaleY = scale }
                                        .pressScale(interaction)
                                        .clip(CircleShape)
                                        .background(choice.resolve(isDark))
                                        .border(width = outlineWidth, color = outlineColor, shape = CircleShape),
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
