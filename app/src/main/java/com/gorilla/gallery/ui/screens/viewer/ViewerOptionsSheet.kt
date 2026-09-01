package com.gorilla.gallery.ui.screens.viewer

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.automirrored.rounded.DriveFileMove
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Search
import kotlinx.coroutines.launch
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gorilla.gallery.data.model.MediaItem
import com.gorilla.gallery.ui.components.LightweightGlassPanel
import com.gorilla.gallery.ui.components.ModalSheetScaffold
import com.gorilla.gallery.ui.theme.DesignTokens
import com.gorilla.gallery.ui.theme.GlassDepth
import com.gorilla.gallery.ui.theme.LiquidGlassSurface
import com.gorilla.gallery.ui.theme.LocalAppColors
import com.gorilla.gallery.ui.theme.pressScale
import android.content.res.Configuration
import androidx.compose.foundation.basicMarquee
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.border

@Composable
fun ViewerOptionsSheet(
    item: MediaItem,
    onSecure: () -> Unit,
    onInfo: () -> Unit,
    onEditMetadata: () -> Unit,
    onCopy: () -> Unit,
    onMove: () -> Unit,
    onWallpaper: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalSheetScaffold(
        onDismiss = onDismiss,
        enableLens = false,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Options",
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = (-0.1).sp
                )
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickTile(
                    icon = Icons.Rounded.Wallpaper,
                    label = "Set Wallpaper",
                    sub = "Home & lock screen",
                    onClick = onWallpaper,
                    modifier = Modifier.weight(1f)
                )
                QuickTile(
                    icon = Icons.Rounded.ContentCopy,
                    label = "Copy Image",
                    sub = "To clipboard",
                    onClick = onCopy,
                    modifier = Modifier.weight(1f)
                )
            }

            LightweightGlassPanel(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    ActionRow(
                        icon = Icons.Rounded.Info,
                        label = "File Info",
                        sub = "EXIF details, camera specs & location",
                        onClick = onInfo
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 1.dp)
                    ActionRow(
                        icon = Icons.Rounded.Edit,
                        label = "Edit Metadata",
                        sub = "Adjust date, time and tags",
                        onClick = onEditMetadata
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 1.dp)
                    ActionRow(
                        icon = Icons.AutoMirrored.Rounded.DriveFileMove,
                        label = "Move to…",
                        sub = "Choose album or folder",
                        onClick = onMove
                    )
                }
            }

            SecurityActionCard(
                isSecured = item.isSecured,
                onClick = onSecure
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun QuickTile(icon: ImageVector, label: String, sub: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    LightweightGlassPanel(
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .pressScale(scale = 0.94f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp, 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(10.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(label, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color.White, lineHeight = 14.sp)
                Text(sub, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.45f), modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}

@Composable
private fun ActionRow(icon: ImageVector, label: String, sub: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(scale = 0.94f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(14.dp, 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(9.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(9.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.White.copy(alpha = 0.85f))
            }
            Column {
                Text(label, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = Color.White, lineHeight = 15.sp)
                Text(sub, fontSize = 10.5.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.42f), modifier = Modifier.padding(top = 2.dp))
            }
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SecurityActionCard(isSecured: Boolean, onClick: () -> Unit) {
    val title = if (isSecured) "Remove from Secure Folder" else "Move to Secure Folder"
    val sub = if (isSecured) "Return to main gallery" else "Encrypted & hidden from gallery"
    val icon = if (isSecured) Icons.Rounded.LockOpen else Icons.Rounded.Lock
    
    val greenColor = MaterialTheme.colorScheme.primary

    LightweightGlassPanel(
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(scale = 0.94f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .border(1.dp, greenColor.copy(alpha = 0.22f), RoundedCornerShape(18.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp, 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(greenColor.copy(alpha = 0.14f), RoundedCornerShape(10.dp))
                        .border(1.dp, greenColor.copy(alpha = 0.32f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp), tint = greenColor)
                }
                Column {
                    Text(title, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = Color.White, lineHeight = 15.sp)
                    Text(sub, fontSize = 10.5.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.42f), modifier = Modifier.padding(top = 2.dp))
                }
            }
            
            Box(
                modifier = Modifier
                    .background(greenColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                    .border(1.dp, greenColor.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text("Private", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = greenColor, letterSpacing = 0.03.sp)
            }
        }
    }
}

/** Liquid-glass edit metadata dialog with fields for name, date, and location. */
@Composable
fun EditMetadataDialog(
    currentBaseName: String,
    currentDateMs: Long?,
    currentLocationName: String?,
    onConfirm: (newName: String, newDateMs: Long?, newLat: Double?, newLng: Double?) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(currentBaseName) }
    var locationStr by remember { mutableStateOf(currentLocationName ?: "") }
    var selectedLat by remember { mutableStateOf<Double?>(null) }
    var selectedLng by remember { mutableStateOf<Double?>(null) }
    var locationResults by remember { mutableStateOf<List<android.location.Address>>(emptyList()) }
    var showLocationDropdown by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    
    // Simple strings for date and time
    val cal = java.util.Calendar.getInstance()
    if (currentDateMs != null) cal.timeInMillis = currentDateMs
    var dateStr by remember { 
        mutableStateOf(if (currentDateMs != null) android.text.format.DateFormat.format("yyyy-MM-dd", cal).toString() else "") 
    }
    var timeStr by remember { 
        mutableStateOf(if (currentDateMs != null) android.text.format.DateFormat.format("HH:mm:ss", cal).toString() else "") 
    }
    
    GlassDialog(onDismiss = onDismiss) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            val contentColor = LocalAppColors.current.textPrimary
        Text(
            "Edit Metadata",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
        )
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("File Name", color = contentColor.copy(alpha = 0.7f)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedTextColor = contentColor,
                unfocusedTextColor = contentColor,
                cursorColor = contentColor,
            )
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = dateStr,
                onValueChange = { dateStr = it },
                label = { Text("Date", color = contentColor.copy(alpha = 0.7f)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedTextColor = contentColor,
                    unfocusedTextColor = contentColor,
                    cursorColor = contentColor,
                )
            )
            OutlinedTextField(
                value = timeStr,
                onValueChange = { timeStr = it },
                label = { Text("Time", color = contentColor.copy(alpha = 0.7f)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedTextColor = contentColor,
                    unfocusedTextColor = contentColor,
                    cursorColor = contentColor,
                )
            )
        }
        androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = locationStr,
                onValueChange = { 
                    locationStr = it 
                    showLocationDropdown = false
                },
                label = { Text("Search Location (e.g. Paris)", color = contentColor.copy(alpha = 0.7f)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedTextColor = contentColor,
                    unfocusedTextColor = contentColor,
                    cursorColor = contentColor,
                ),
                trailingIcon = {
                    if (isSearching) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = contentColor,
                            strokeWidth = 2.dp
                        )
                    } else {
                        androidx.compose.material3.IconButton(
                            onClick = {
                                if (locationStr.isNotBlank()) {
                                    isSearching = true
                                    coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                        try {
                                            val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                                            @Suppress("DEPRECATION")
                                            val results = geocoder.getFromLocationName(locationStr, 5)
                                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                if (results != null && results.isNotEmpty()) {
                                                    locationResults = results
                                                    showLocationDropdown = true
                                                } else {
                                                    android.widget.Toast.makeText(context, "No results found", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                                isSearching = false
                                            }
                                        } catch (e: Exception) {
                                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                isSearching = false
                                                android.widget.Toast.makeText(context, "Search failed", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Rounded.Search, contentDescription = "Search Location", tint = contentColor.copy(alpha = 0.7f))
                        }
                    }
                }
            )
            
            androidx.compose.material3.DropdownMenu(
                expanded = showLocationDropdown,
                onDismissRequest = { showLocationDropdown = false },
                modifier = Modifier.fillMaxWidth(0.8f).background(com.gorilla.gallery.ui.screens.viewer.viewerChromeColor())
            ) {
                locationResults.forEach { address ->
                    val addressName = (0..address.maxAddressLineIndex).joinToString(", ") { address.getAddressLine(it) }
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(addressName, color = contentColor) },
                        onClick = {
                            locationStr = addressName
                            selectedLat = address.latitude
                            selectedLng = address.longitude
                            showLocationDropdown = false
                        }
                    )
                }
            }
        }
        
        DialogButtons(
            confirmLabel = "Save",
            onConfirm = {
                var newDateMs = currentDateMs
                try {
                    if (dateStr.isNotBlank() && timeStr.isNotBlank()) {
                        val format = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                        val parsed = format.parse("$dateStr $timeStr")
                        if (parsed != null) newDateMs = parsed.time
                    }
                } catch (e: Exception) {
                    // Ignore parse errors for now
                }
                
                onConfirm(
                    name, 
                    newDateMs,
                    if (locationStr.isNotBlank()) selectedLat else null,
                    if (locationStr.isNotBlank()) selectedLng else null
                )
            },
            onDismiss = onDismiss,
        )
        }
    }
}

/** Confirmation before overwriting the home-screen wallpaper. */
@Composable
fun WallpaperConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    GlassDialog(onDismiss = onDismiss) {
        val contentColor = LocalAppColors.current.textPrimary
        Text(
            "Set wallpaper",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
        )
        Text(
            "Set this image as your home screen wallpaper?",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 16.dp),
            color = contentColor,
        )
        DialogButtons(
            confirmLabel = "Confirm",
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )
    }
}

/** Pick a destination device folder. [folders] is a list of (display name, relative path). */
@Composable
fun FolderPickerDialog(
    folders: List<Pair<String, String>>,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    GlassDialog(onDismiss = onDismiss) {
        val contentColor = LocalAppColors.current.textPrimary
        Text(
            "Move to…",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp),
            color = contentColor,
        )
        LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
            items(folders) { (folderName, relativePath) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .pressScale(scale = 0.94f)
                        .clickable { onPick(relativePath) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val itemColor = LocalAppColors.current.textPrimary
                    Icon(Icons.Rounded.Folder, contentDescription = null, modifier = Modifier.size(22.dp), tint = itemColor)
                    Spacer(Modifier.width(14.dp))
                    Text(folderName, style = MaterialTheme.typography.bodyLarge, color = itemColor)
                }
            }
        }
        DialogButtons(
            confirmLabel = null,
            onConfirm = {},
            onDismiss = onDismiss,
        )
    }
}

// ---- Shared dialog chrome -----------------------------------------------------

@Composable
private fun GlassDialog(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    val currentBackdrop = com.gorilla.gallery.ui.theme.LocalLiquidGlassContentBackdrop.current ?: com.gorilla.gallery.ui.theme.LocalLiquidGlassBackdrop.current

    com.gorilla.gallery.ui.components.AnimatedGlassDialog(
        onDismissRequest = onDismiss
    ) { scale ->
        LiquidGlassSurface(
            depth = GlassDepth.MID,
            shape = RoundedCornerShape(28.dp),
            backdrop = currentBackdrop,
            surfaceColor = DesignTokens.BgSurface.copy(alpha = 0.92f),
            saturationOverride = 1.55f,
            tintAlphaOverride = 0.07f,
            modifier = Modifier
                .widthIn(max = 380.dp)
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .scale(scale)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
        ) {
            Column(Modifier.padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 8.dp)) { content() }
        }
    }
}

@Composable
private fun DialogButtons(
    confirmLabel: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmEnabled: Boolean = true,
) {
    Box(Modifier.fillMaxWidth().padding(top = 20.dp)) {
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val contentColor = MaterialTheme.colorScheme.primary
            TextButton(onClick = onDismiss) { Text("Cancel", color = contentColor) }
            if (confirmLabel != null) {
                TextButton(onClick = onConfirm, enabled = confirmEnabled) { Text(confirmLabel, color = contentColor) }
            }
        }
    }
}
