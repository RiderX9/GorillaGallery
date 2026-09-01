package com.gorilla.gallery.ui.screens.albums

import androidx.compose.runtime.setValue

import androidx.compose.runtime.getValue

import android.graphics.Bitmap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Screenshot
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gorilla.gallery.GalleryApp
import com.gorilla.gallery.data.model.Album
import com.gorilla.gallery.data.model.AlbumKind
import com.gorilla.gallery.data.model.MediaItem
import com.gorilla.gallery.ui.theme.LocalAppColors
import com.gorilla.gallery.ui.theme.pressScale
import com.gorilla.gallery.ui.theme.rememberHaptic

private val AlbumThumbShape = RoundedCornerShape(12.dp)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlbumCard(
    album: Album,
    thumbnailSizePx: Int,
    highQualityThumbnails: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val haptic = rememberHaptic()
    val clickModifier = if (onLongClick != null) {
        Modifier.combinedClickable(
            interactionSource = interaction,
            indication = null,
            onClick = { haptic(); onClick() },
            onLongClick = { haptic(); onLongClick() },
        )
    } else {
        Modifier.clickable(interaction, indication = null) { haptic(); onClick() }
    }

    Column(
        modifier = modifier
            .pressScale(interaction, pressedScale = 0.97f)
            .then(clickModifier),
    ) {
        AlbumMosaic(
            items = album.previewItems,
            thumbnailSizePx = thumbnailSizePx,
            highQualityThumbnails = highQualityThumbnails,
            placeholderIcon = { Icon(album.kind.icon(), contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            album.name,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = LocalAppColors.current.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(
            "${album.itemCount}",
            fontSize = 13.sp,
            color = LocalAppColors.current.textSecondary,
            maxLines = 1,
        )
    }
}

@Composable
fun SecureAlbumCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val haptic = rememberHaptic()

    Column(
        modifier = modifier
            .pressScale(interaction, pressedScale = 0.97f)
            .clickable(interaction, indication = null) { haptic(); onClick() },
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(AlbumThumbShape)
                .background(Color(0xFF14141E)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Lock, contentDescription = null, tint = Color.White.copy(alpha = 0.82f), modifier = Modifier.size(40.dp))
        }
        Text(
            "Secure Folder",
            fontSize = 14.sp,
            color = LocalAppColors.current.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(
            "Locked",
            fontSize = 13.sp,
            color = LocalAppColors.current.textSecondary,
            maxLines = 1,
        )
    }
}

@Composable
private fun AlbumMosaic(
    items: List<MediaItem>,
    thumbnailSizePx: Int,
    highQualityThumbnails: Boolean = false,
    placeholderIcon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .aspectRatio(1f)
            .clip(AlbumThumbShape)
            .background(Color.White.copy(alpha = 0.06f)),
    ) {
        if (items.size <= 1) {
            val item = items.firstOrNull()
            if (item != null) {
                PreviewTile(item = item, thumbnailSizePx = thumbnailSizePx * 2, highQualityThumbnails = highQualityThumbnails, modifier = Modifier.fillMaxSize())
            } else {
                PlaceholderTile(content = placeholderIcon, modifier = Modifier.fillMaxSize())
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(1.5.dp),
                modifier = Modifier.fillMaxSize().background(Color.Black),
            ) {
                repeat(2) { row ->
                    Row(
                        Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(1.5.dp),
                    ) {
                        repeat(2) { column ->
                            val item = items.getOrNull(row * 2 + column)
                            if (item != null) {
                                PreviewTile(item = item, thumbnailSizePx = thumbnailSizePx, highQualityThumbnails = highQualityThumbnails, modifier = Modifier.weight(1f).fillMaxSize())
                            } else {
                                PlaceholderTile(content = placeholderIcon, modifier = Modifier.weight(1f).fillMaxSize())
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewTile(item: MediaItem, thumbnailSizePx: Int, highQualityThumbnails: Boolean = false, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app = context.applicationContext as GalleryApp
    var thumbnail by remember(item.id, item.uri, thumbnailSizePx, highQualityThumbnails) {
        mutableStateOf<Bitmap?>(app.container.thumbnailRepository.getCached(item.id, thumbnailSizePx, item.dateModifiedSec, highQualityThumbnails))
    }

    LaunchedEffect(item.id, item.uri, thumbnailSizePx, highQualityThumbnails) {
        thumbnail = app.container.thumbnailRepository.load(
            uri = item.uri,
            mediaId = item.id,
            sizePx = thumbnailSizePx,
            cacheVersion = item.dateModifiedSec,
            highQuality = highQualityThumbnails,
        )
    }

    Box(modifier.background(Color.White.copy(alpha = 0.06f))) {
        thumbnail?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = item.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (item.isVideo) {
            Icon(
                Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.align(Alignment.Center).size(22.dp),
            )
        }
    }
}

@Composable
private fun PlaceholderTile(content: @Composable () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.background(Color.White.copy(alpha = 0.05f)), contentAlignment = Alignment.Center) {
        Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides Color.White.copy(alpha = 0.22f),
                content = content,
            )
        }
    }
}

fun AlbumKind.icon() = when (this) {
    AlbumKind.CAMERA -> Icons.Rounded.CameraAlt
    AlbumKind.SCREENSHOTS -> Icons.Rounded.Screenshot
    AlbumKind.DOWNLOAD -> Icons.Rounded.Download
    AlbumKind.WHATSAPP -> Icons.Rounded.PhotoLibrary
    AlbumKind.GENERIC -> Icons.Rounded.PhotoLibrary
}
