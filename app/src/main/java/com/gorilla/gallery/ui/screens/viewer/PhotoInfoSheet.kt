package com.gorilla.gallery.ui.screens.viewer

import androidx.compose.runtime.setValue
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.LensBlur
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.basicMarquee
import com.gorilla.gallery.ui.theme.LocalDynamicColors

import androidx.compose.runtime.getValue

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.gorilla.gallery.data.model.MediaItem
import com.gorilla.gallery.data.model.PhotoExif
import com.gorilla.gallery.ui.AppViewModel
import com.gorilla.gallery.ui.components.LightweightGlassPanel
import com.gorilla.gallery.ui.components.ModalSheetScaffold
import com.gorilla.gallery.ui.components.formatFileSize
import com.gorilla.gallery.ui.components.formatResolution
import com.gorilla.gallery.ui.components.megapixels
import com.gorilla.gallery.ui.theme.LocalAppColors
import com.gorilla.gallery.ui.theme.DesignTokens
import com.gorilla.gallery.ui.theme.pressScale
import androidx.compose.ui.Alignment
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFmt = DateTimeFormatter.ofPattern("MMM d, yyyy · h:mm a", Locale.getDefault())



@Composable
fun PhotoInfoSheet(app: AppViewModel, item: MediaItem, onDismiss: () -> Unit, onEditMetadata: () -> Unit) {
    val context = LocalContext.current
    var exifLoaded by remember(item.id) { mutableStateOf(false) }
    val exif by produceState(initialValue = PhotoExif(), key1 = item) {
        value = app.container.photoEditorRepository.readExif(item)
        exifLoaded = true
    }

    if (!exifLoaded) return

    val dateZdt = Instant.ofEpochMilli(item.dateTakenMs).atZone(ZoneId.systemDefault())
    val dateStr = dateZdt.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault()))
    val timeStr = dateZdt.format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))

    ModalSheetScaffold(
        onDismiss = onDismiss,
        skipPartiallyExpanded = true,
        enableLens = false,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val textColorPrimary = LocalAppColors.current.textPrimary
            val textColorSecondary = LocalAppColors.current.textSecondary
            val accentColor = LocalDynamicColors.current.accent

            // 1. File Identity Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = item.displayName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColorPrimary,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee(),
                        letterSpacing = (-0.1).sp
                    )
                    
                    val labels by produceState(emptyList<String>(), item.id) {
                        value = app.container.imageLabelRepository.getTags(item.uri.toString())
                    }
                    val consolidated = consolidateLabels(labels)
                    val subjectLabel = if (consolidated.isNotEmpty()) consolidated.first() else ""

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 3.dp)
                    ) {
                        Text(dateStr, fontSize = 11.5.sp, fontWeight = FontWeight.Medium, color = textColorSecondary.copy(alpha = 0.7f))
                        Box(Modifier.size(3.dp).clip(androidx.compose.foundation.shape.CircleShape).background(textColorSecondary.copy(alpha = 0.4f)))
                        Text(timeStr, fontSize = 11.5.sp, fontWeight = FontWeight.Medium, color = textColorSecondary.copy(alpha = 0.7f))
                        if (subjectLabel.isNotEmpty()) {
                            Box(Modifier.size(3.dp).clip(androidx.compose.foundation.shape.CircleShape).background(textColorSecondary.copy(alpha = 0.4f)))
                            Text(subjectLabel, fontSize = 11.5.sp, fontWeight = FontWeight.Medium, color = textColorSecondary.copy(alpha = 0.7f))
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .background(accentColor.copy(alpha = 0.14f), RoundedCornerShape(10.dp))
                        .border(1.dp, accentColor.copy(alpha = 0.32f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 9.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = item.format.uppercase(),
                        color = accentColor,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.4.sp
                    )
                }
            }

            // 2. Interactive Location Card
            if (exif.locationName != null) {
                val locInteractionSource = remember { MutableInteractionSource() }
                LightweightGlassPanel(
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .pressScale(interactionSource = locInteractionSource, pressedScale = 0.94f)
                        .clickable(interactionSource = locInteractionSource, indication = null) {
                            val uri = "geo:${exif.latitude},${exif.longitude}?q=${exif.latitude},${exif.longitude}".toUri()
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 10.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                    .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Place, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = exif.locationName!!,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColorPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Geotagged",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = textColorSecondary.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                .border(1.dp, accentColor.copy(alpha = 0.28f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text("Maps", color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Icon(Icons.AutoMirrored.Rounded.ArrowForwardIos, contentDescription = null, tint = accentColor, modifier = Modifier.size(11.dp))
                        }
                    }
                }
            }

            // 3. Big Container Card: Image Profile
            LightweightGlassPanel(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val profileTitle = if (item.isVideo) "VIDEO PROFILE" else "IMAGE PROFILE"
                        val profileBadge = exif.profileBadge ?: if (item.isVideo) "SDR" else "sRGB"
                        Text(profileTitle, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = textColorSecondary.copy(alpha = 0.7f), letterSpacing = 0.8.sp)
                        Box(
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(8.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = profileBadge,
                                color = textColorSecondary.copy(alpha = 0.9f),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.3.sp
                            )
                        }
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        InnerMetricCard(tag = "Dimensions", value = "${item.width} × ${item.height}", modifier = Modifier.weight(1.25f))
                        InnerMetricCard(tag = "Megapixels", value = megapixels(item.width, item.height), modifier = Modifier.weight(1f))
                        InnerMetricCard(tag = "File Size", value = formatFileSize(item.sizeBytes), modifier = Modifier.weight(1f))
                    }
                }
            }

            // 4. Big Container Card: Camera Optics
            if (exif.cameraModel != null || exif.aperture != null || exif.shutter != null || exif.iso != null || exif.focalLength != null) {
                LightweightGlassPanel(
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("CAMERA OPTICS", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = textColorSecondary.copy(alpha = 0.7f), letterSpacing = 0.8.sp)
                            if (exif.cameraModel != null) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Rounded.Smartphone, contentDescription = null, tint = accentColor, modifier = Modifier.size(12.dp))
                                    Text(exif.cameraModel!!, color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            exif.aperture?.let { InnerMetricCard(tag = "Aperture", value = it, modifier = Modifier.weight(1f), centerAlign = true) }
                            exif.shutter?.let { InnerMetricCard(tag = "Shutter", value = it, modifier = Modifier.weight(1f), centerAlign = true) }
                            exif.iso?.let { InnerMetricCard(tag = "ISO", value = it, modifier = Modifier.weight(1f), centerAlign = true) }
                            exif.focalLength?.let { InnerMetricCard(tag = "Focal", value = it, modifier = Modifier.weight(1f), centerAlign = true) }
                        }
                    }
                }
            }

            // 5. Action Buttons Deck
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val dateInteractionSource = remember { MutableInteractionSource() }
                val searchInteractionSource = remember { MutableInteractionSource() }
                
                // Adjust Date
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .pressScale(interactionSource = dateInteractionSource, pressedScale = 0.94f)
                        .background(accentColor.copy(alpha = 0.14f), RoundedCornerShape(19.dp))
                        .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(19.dp))
                        .clip(RoundedCornerShape(19.dp))
                        .clickable(interactionSource = dateInteractionSource, indication = null) { onEditMetadata() }
                ) {
                    Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = accentColor, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Adjust Date", color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                // Search Image
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .pressScale(interactionSource = searchInteractionSource, pressedScale = 0.94f)
                        .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(19.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.09f), RoundedCornerShape(19.dp))
                        .clip(RoundedCornerShape(19.dp))
                        .clickable(interactionSource = searchInteractionSource, indication = null) {
                            onDismiss()
                            val lensIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "image/*"
                                putExtra(Intent.EXTRA_STREAM, item.uri)
                                setClassName(
                                    "com.google.android.googlequicksearchbox",
                                    "com.google.android.apps.search.lens.LensShareEntryPointActivity"
                                )
                            }
                            try {
                                context.startActivity(lensIntent)
                            } catch (e: Exception) {
                                // Fallback to disambiguation if the specific component fails
                                val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "image/*"
                                    putExtra(Intent.EXTRA_STREAM, item.uri)
                                    setPackage("com.google.android.googlequicksearchbox")
                                }
                                try {
                                    context.startActivity(fallbackIntent)
                                } catch (e2: Exception) {
                                    // ignore
                                }
                            }
                        }
                ) {
                    Icon(Icons.Rounded.LensBlur, contentDescription = null, tint = textColorPrimary.copy(alpha = 0.85f), modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Search Image", color = textColorPrimary.copy(alpha = 0.85f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun InnerMetricCard(tag: String, value: String, modifier: Modifier = Modifier, centerAlign: Boolean = false) {
    Box(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.035f), RoundedCornerShape(14.dp))
            .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = if (centerAlign) 3.dp else 9.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = if (centerAlign) Alignment.CenterHorizontally else Alignment.Start
        ) {
            Text(
                text = tag.uppercase(),
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                color = LocalAppColors.current.textSecondary.copy(alpha = 0.7f),
                letterSpacing = 0.4.sp
            )
            Text(
                text = value,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = LocalAppColors.current.textPrimary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}


private fun consolidateLabels(labels: List<String>): List<String> {
    val results = mutableSetOf<String>()
    
    // Convert to a set of words for fast and exact matching, splitting by space just in case a label is multiple words
    val words = labels.flatMap { it.lowercase().split(Regex("\\s+")) }.toSet()

    // Helper to check if any word in the given list is present
    fun hasAny(vararg terms: String): Boolean = terms.any { words.contains(it) }

    // Pets & Animals
    if (hasAny("cat", "kitten", "feline")) results.add("Cat")
    else if (hasAny("dog", "puppy", "canine")) results.add("Dog")
    else if (hasAny("bird", "parrot", "avian")) results.add("Bird")
    else if (hasAny("horse", "equine")) results.add("Horse")
    else if (hasAny("pet", "animal", "wildlife", "mammal", "reptile", "insect", "bug")) results.add("Animal")

    // People
    if (hasAny("person", "man", "woman", "boy", "girl", "face", "skin", "hair", "smile", "selfie", "portrait", "human", "people")) {
        results.add("Person")
    }

    // Food
    if (hasAny("food", "meal", "dessert", "fruit", "vegetable", "drink", "snack", "dish", "cuisine", "meat", "beverage")) {
        results.add("Food")
    }

    // Nature
    if (hasAny("nature", "landscape", "mountain", "water", "ocean", "beach", "sky", "tree", "plant", "flower", "forest", "outdoor", "grass", "petal", "leaf")) {
        results.add("Nature")
    }

    // Urban / Architecture
    if (hasAny("building", "architecture", "city", "street", "house", "urban", "bridge", "road")) {
        results.add("Architecture")
    }

    // Vehicles
    if (hasAny("vehicle", "car", "truck", "motorcycle", "bicycle", "airplane", "boat", "transport", "train", "bus")) {
        results.add("Vehicle")
    }

    // Electronics / Tech
    if (hasAny("electronics", "computer", "phone", "screen", "gadget", "device", "technology", "laptop", "tv")) {
        results.add("Electronics")
    }
    
    // Furniture / Indoor
    if (hasAny("furniture", "room", "indoor", "chair", "table", "bed", "couch", "desk", "sofa")) {
        results.add("Indoor")
    }

    if (results.isEmpty()) {
        results.addAll(labels.take(2).map { it.replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase(java.util.Locale.getDefault()) else c.toString() } })
    }

    return results.toList()
}
