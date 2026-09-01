package com.gorilla.gallery.ui.screens.albums

import androidx.compose.runtime.setValue

import androidx.compose.runtime.getValue

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Screenshot
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gorilla.gallery.data.model.Album
import com.gorilla.gallery.data.model.MediaItem
import com.gorilla.gallery.ui.AppViewModel
import com.gorilla.gallery.ui.components.GlassAlertDialog
import com.gorilla.gallery.ui.theme.DesignTokens
import com.gorilla.gallery.ui.theme.GlassDepth
import com.gorilla.gallery.ui.theme.LiquidGlassSurface
import com.gorilla.gallery.ui.theme.LocalAppColors
import com.gorilla.gallery.ui.theme.LocalDynamicColors
import com.gorilla.gallery.ui.theme.LocalLiquidGlassBackdrop
import com.gorilla.gallery.ui.theme.LocalLiquidGlassContentBackdrop
import com.gorilla.gallery.ui.theme.SpringSpecs
import com.gorilla.gallery.ui.theme.pressScale
import com.gorilla.gallery.ui.theme.rememberHaptic

@Composable
fun AlbumsScreen(
    app: AppViewModel,
    contentPadding: PaddingValues,
    onOpenAlbum: (Long) -> Unit,
    onOpenSecure: () -> Unit,
    onOpenPhotos: () -> Unit,
    onOpenVideos: () -> Unit,
    onOpenSelfies: () -> Unit,
    onOpenScreenshots: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenEdited: () -> Unit,
    onOpenTrash: () -> Unit,
    onOpenViewer: (List<MediaItem>, android.net.Uri, Rect?) -> Unit,
    vm: AlbumsViewModel = viewModel(factory = AlbumsViewModel.Factory),
) {
    val albums by vm.albums.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()
    val settings by app.settings.collectAsStateWithLifecycle()
    val photoCount by vm.photoCount.collectAsStateWithLifecycle()
    val videoCount by vm.videoCount.collectAsStateWithLifecycle()
    val selfieCount by vm.selfieCount.collectAsStateWithLifecycle()
    val screenshotCount by vm.screenshotCount.collectAsStateWithLifecycle()
    val favoriteCount by vm.favoriteCount.collectAsStateWithLifecycle()
    val editedCount by vm.editedCount.collectAsStateWithLifecycle()
    val trashItems by vm.trashItems.collectAsStateWithLifecycle()
    val appColors = LocalAppColors.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current

    var showCreateAlbum by remember { mutableStateOf(false) }
    val showAllAlbums by app.albumsExpanded.collectAsStateWithLifecycle()
    var actionAlbum by remember { mutableStateOf<Album?>(null) }
    var moveAlbum by remember { mutableStateOf<Album?>(null) }
    var deleteAlbum by remember { mutableStateOf<Album?>(null) }
    val showSecure = settings.secureFolderEnabled && settings.showSecureInAlbums
    val visibleAlbums = if (showAllAlbums) albums else albums.take(4)
    val hasHiddenAlbums = albums.size > 4
    val albumPreviewSizePx = remember(density, configuration.screenWidthDp, settings.highQualityThumbnails) {
        with(density) {
            val screenWidthPx = configuration.screenWidthDp.dp.roundToPx()
            val albumWidthPx = ((screenWidthPx - 32.dp.roundToPx() - 12.dp.roundToPx()) / 2).coerceAtLeast(1)
            val size = (albumWidthPx / 2).coerceAtLeast(1)
            if (settings.highQualityThumbnails) size * 2 else size
        }
    }

    var headerHeight by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val headerHeightDp = with(LocalDensity.current) { headerHeight.toDp() }

    Box(Modifier.fillMaxSize().background(appColors.bgBase)) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = headerHeightDp,
                end = 16.dp,
                bottom = contentPadding.calculateBottomPadding() + 32.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize(),
        ) {

            item(span = { GridItemSpan(maxLineSpan) }, key = "myAlbumsHeader") {
                SectionHeader(
                    title = "My Albums",
                    action = if (hasHiddenAlbums) {
                        if (showAllAlbums) "See Less" else "See All"
                    } else {
                        null
                    },
                    onAction = { app.setAlbumsExpanded(!showAllAlbums) },
                )
            }

            if (!isLoading) {
                items(visibleAlbums, key = { it.bucketId }) { album ->
                    AlbumCard(
                        album = album,
                        thumbnailSizePx = albumPreviewSizePx,
                        highQualityThumbnails = settings.highQualityThumbnails,
                        onClick = { onOpenAlbum(album.bucketId) },
                        onLongClick = { actionAlbum = album },
                        modifier = Modifier.animateItem(placementSpec = SpringSpecs.OffsetSpring),
                    )
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }, key = "mediaTypesHeader") {
                SectionHeader(
                    title = "Media Types",
                    modifier = Modifier.animateItem(placementSpec = SpringSpecs.OffsetSpring),
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }, key = "mediaTypes") {
                AlbumsListSection(modifier = Modifier.animateItem(placementSpec = SpringSpecs.OffsetSpring)) {
                    AlbumListRow(
                        icon = Icons.Rounded.Photo,
                        iconColor = Color(0xFF00C7BE),
                        label = "Photos",
                        count = photoCount,
                        onClick = onOpenPhotos,
                    )
                    AlbumListRow(
                        icon = Icons.Rounded.PlayArrow,
                        iconColor = Color(0xFFA4E334),
                        label = "Videos",
                        count = videoCount,
                        dividerBefore = true,
                        onClick = onOpenVideos,
                    )
                    AlbumListRow(
                        icon = Icons.Rounded.Face,
                        iconColor = Color(0xFF0A84FF),
                        label = "Selfies",
                        count = selfieCount,
                        dividerBefore = true,
                        onClick = onOpenSelfies,
                    )
                    AlbumListRow(
                        icon = Icons.Rounded.Screenshot,
                        iconColor = Color(0xFF636366),
                        label = "Screenshots",
                        count = screenshotCount,
                        dividerBefore = true,
                        onClick = onOpenScreenshots,
                    )
                    AlbumListRow(
                        icon = Icons.Rounded.Favorite,
                        iconColor = Color(0xFFFF2D55),
                        label = "Favorites",
                        count = favoriteCount,
                        dividerBefore = true,
                        onClick = onOpenFavorites,
                    )
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }, key = "utilitiesHeader") {
                SectionHeader(
                    title = "Utilities",
                    modifier = Modifier.animateItem(placementSpec = SpringSpecs.OffsetSpring),
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }, key = "utilities") {
                AlbumsListSection(modifier = Modifier.animateItem(placementSpec = SpringSpecs.OffsetSpring)) {
                    AlbumListRow(
                        icon = Icons.Rounded.Edit,
                        iconColor = Color(0xFF30D158),
                        label = "Edited",
                        count = editedCount,
                        onClick = onOpenEdited,
                    )
                    if (showSecure) {
                        AlbumListRow(
                            icon = Icons.Rounded.Lock,
                            iconColor = Color(0xFF5E5CE6),
                            label = "Secure Folder",
                            dividerBefore = true,
                            trailing = {
                                Icon(
                                    imageVector = Icons.Rounded.Lock,
                                    contentDescription = "Locked",
                                    tint = DesignTokens.TextSecondary,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                            onClick = onOpenSecure,
                        )
                    }
                    AlbumListRow(
                        icon = Icons.Rounded.Delete,
                        iconColor = Color(0xFFFF3B30),
                        label = "Trash",
                        count = trashItems.size,
                        dividerBefore = true,
                        onClick = onOpenTrash,
                    )
                }
            }
        }



        if (showCreateAlbum) {
            NewAlbumDialog(
                onCreate = { name ->
                    if (app.container.albumRepository.createAlbum(name)) {
                        app.showSnackbar.tryEmit("Album created")
                    }
                    showCreateAlbum = false
                },
                onDismiss = { showCreateAlbum = false },
            )
        }

        actionAlbum?.let { album ->
            AlbumActionDialog(
                album = album,
                onMove = {
                    actionAlbum = null
                    moveAlbum = album
                },
                onDelete = {
                    actionAlbum = null
                    deleteAlbum = album
                },
                onDismiss = { actionAlbum = null },
            )
        }

        moveAlbum?.let { album ->
            AlbumMoveDialog(
                album = album,
                canMoveUp = albums.indexOfFirst { it.bucketId == album.bucketId } > 0,
                canMoveDown = albums.indexOfFirst { it.bucketId == album.bucketId } in 0 until albums.lastIndex,
                onMove = { direction ->
                    if (vm.moveAlbum(album, albums, direction)) {
                        app.showSnackbar.tryEmit("Album moved")
                    }
                    moveAlbum = null
                },
                onDismiss = { moveAlbum = null },
            )
        }

        deleteAlbum?.let { album ->
            GlassAlertDialog(
                onDismissRequest = { deleteAlbum = null },
                title = "Delete Album",
                text = "\"${album.name}\" will be removed from Albums. Its photos and videos will stay in your library.",
                confirmLabel = "Delete",
                onConfirm = {
                    vm.deleteAlbumEntry(album)
                    app.showSnackbar.tryEmit("Album deleted")
                    deleteAlbum = null
                },
                dismissLabel = "Cancel",
                onDismiss = { deleteAlbum = null },
                icon = Icons.Rounded.Delete,
            )
        }

        Column(
            Modifier
                .fillMaxWidth()
                .background(appColors.bgBase)
                .onSizeChanged { headerHeight = it.height }
        ) {
            AlbumsTitle(onAdd = { showCreateAlbum = true })
        }
    }
}

@Composable
private fun AlbumActionDialog(
    album: Album,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlbumOptionsDialog(title = album.name, onDismiss = onDismiss) {
        AlbumOptionRow(
            icon = Icons.Rounded.SwapVert,
            iconColor = LocalDynamicColors.current.accent,
            label = "Move",
            onClick = onMove,
        )
        AlbumOptionRow(
            icon = Icons.Rounded.Delete,
            iconColor = Color(0xFFFF3B30),
            label = "Delete",
            onClick = onDelete,
        )
    }
}

@Composable
private fun AlbumMoveDialog(
    album: Album,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMove: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlbumOptionsDialog(title = "Move ${album.name}", onDismiss = onDismiss) {
        AlbumOptionRow(
            icon = Icons.Rounded.ArrowUpward,
            iconColor = LocalDynamicColors.current.accent,
            label = "Move Up",
            enabled = canMoveUp,
            onClick = { onMove(-1) },
        )
        AlbumOptionRow(
            icon = Icons.Rounded.ArrowDownward,
            iconColor = LocalDynamicColors.current.accent,
            label = "Move Down",
            enabled = canMoveDown,
            onClick = { onMove(1) },
        )
    }
}

@Composable
private fun AlbumOptionsDialog(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val currentBackdrop = LocalLiquidGlassContentBackdrop.current ?: LocalLiquidGlassBackdrop.current
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
                .width(300.dp)
                .scale(scale)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = DesignTokens.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                    )
                    content()
                }
        }
    }
}

@Composable
private fun AlbumOptionRow(
    icon: ImageVector,
    iconColor: Color,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val haptic = rememberHaptic()
    val alpha = if (enabled) 1f else 0.42f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { haptic(); onClick() },
            )
            .padding(horizontal = 10.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(iconColor.copy(alpha = 0.18f * alpha)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = iconColor.copy(alpha = alpha), modifier = Modifier.size(19.dp))
        }
        Text(
            text = label,
            fontSize = 17.sp,
            color = LocalAppColors.current.textPrimary.copy(alpha = alpha),
            maxLines = 1,
            modifier = Modifier.padding(start = 12.dp).weight(1f),
        )
        Icon(
            Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = LocalAppColors.current.textSecondary.copy(alpha = alpha),
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun NewAlbumDialog(
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    val accent = LocalDynamicColors.current.accent
    val currentBackdrop = LocalLiquidGlassContentBackdrop.current ?: LocalLiquidGlassBackdrop.current
    val canCreate = name.isNotBlank()

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
                .width(300.dp)
                .scale(scale)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
        ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                ) {
                    Text(
                        text = "New Album",
                        style = MaterialTheme.typography.titleMedium,
                        color = DesignTokens.TextPrimary,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Box(Modifier.height(18.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        singleLine = true,
                        label = { Text("Album Name") },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Box(Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.pressScale(scale = 0.94f),
                        ) {
                            Text(
                                text = "Cancel",
                                style = MaterialTheme.typography.labelLarge,
                                color = DesignTokens.TextSecondary,
                            )
                        }
                        Box(Modifier.width(8.dp))
                        TextButton(
                            enabled = canCreate,
                            onClick = { onCreate(name) },
                            modifier = Modifier.pressScale(scale = 0.94f),
                        ) {
                            Text(
                                text = "Create",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (canCreate) accent else DesignTokens.TextSecondary.copy(alpha = 0.42f),
                            )
                        }
                    }
                }
        }
    }
}

@Composable
private fun AlbumsTitle(onAdd: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Albums",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = LocalAppColors.current.textPrimary,
            modifier = Modifier.weight(1f),
        )
        val appColors = LocalAppColors.current
        Box(
            modifier = Modifier
                .size(38.dp)
                .pressScale(0.94f)
                .clip(RoundedCornerShape(19.dp))
                .background(appColors.bgSurface.copy(alpha = 0.85f))
                .border(1.dp, appColors.borderGlass, RoundedCornerShape(19.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { onAdd() }
                ),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "Add Album",
                tint = appColors.textPrimary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = LocalAppColors.current.textPrimary,
            modifier = Modifier.weight(1f),
        )
        if (action != null) {
            Text(
                text = action,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = LocalDynamicColors.current.accent,
                maxLines = 1,
                modifier = if (onAction != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onAction,
                    )
                } else {
                    Modifier
                },
            )
        }
    }
}

@Composable
private fun AlbumsListSection(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LocalAppColors.current.bgSurface)
            .padding(horizontal = 16.dp),
        content = content,
    )
}

@Composable
private fun AlbumListRow(
    icon: ImageVector,
    iconColor: Color,
    label: String,
    count: Int? = null,
    trailing: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    dividerBefore: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val haptic = rememberHaptic()
    val clickable = if (onClick != null) {
        Modifier
            .pressScale(scale = 0.94f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { haptic(); onClick() },
            )
    } else {
        Modifier
    }

    Column(modifier.fillMaxWidth()) {
        if (dividerBefore) {
            Box(
                Modifier
                    .padding(start = 42.dp)
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(LocalAppColors.current.textSecondary.copy(alpha = 0.18f))
            )
        }
        Row(
            clickable
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(iconColor),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
            }
            Text(
                text = label,
                fontSize = 17.sp,
                color = LocalAppColors.current.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 12.dp).weight(1f),
            )
            if (trailing != null) {
                Box(Modifier.padding(end = 6.dp), contentAlignment = Alignment.Center) {
                    trailing()
                }
            } else if (count != null) {
                Text(
                    text = "$count",
                    fontSize = 17.sp,
                    color = LocalAppColors.current.textSecondary,
                    maxLines = 1,
                    modifier = Modifier.padding(end = 6.dp),
                )
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = LocalAppColors.current.textSecondary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
