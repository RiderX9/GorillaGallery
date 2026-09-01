package com.gorilla.gallery.ui.screens.search

import androidx.compose.runtime.setValue

import androidx.compose.runtime.getValue

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Camera
import androidx.compose.material.icons.rounded.PanoramaHorizontal
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.background
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gorilla.gallery.data.model.MediaItem
import com.gorilla.gallery.ui.AppViewModel
import com.gorilla.gallery.ui.components.EmptyState
import com.gorilla.gallery.ui.components.MediaGrid
import com.gorilla.gallery.ui.components.MediaSection
import com.gorilla.gallery.ui.components.ScreenTitle
import com.gorilla.gallery.ui.theme.LocalAppColors
import com.gorilla.gallery.ui.theme.LocalDynamicColors
import com.gorilla.gallery.ui.theme.pressScale
import android.net.Uri

@Composable
fun SearchScreen(
    app: AppViewModel,
    contentPadding: PaddingValues,
    viewerOpen: Boolean = false,
    onOpenViewer: (List<MediaItem>, android.net.Uri, Rect?) -> Unit,
    autoFocus: Boolean = false,
    onOpenVideos: () -> Unit = {},
    onOpenFavorites: () -> Unit = {},
    onOpenRaw: () -> Unit = {},
    onOpenPanoramas: () -> Unit = {},
    onOpenPersonDetail: (Int) -> Unit = {},
    onOpenSeeAllPeople: () -> Unit = {},
    vm: SearchViewModel = viewModel(factory = SearchViewModel.Factory),
) {
    val query by vm.query.collectAsStateWithLifecycle()
    val results by vm.results.collectAsStateWithLifecycle()
    val settings by app.settings.collectAsStateWithLifecycle()
    val focusedItem by app.focusedItem.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val density = androidx.compose.ui.platform.LocalDensity.current
    var headerHeight by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val headerHeightDp = with(density) { headerHeight.toDp() }
    
    var gridColumns by remember { mutableStateOf(settings.gridColumns) }

    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize()) {
                when {
                query.isBlank() ->                 SearchCategories(
                    contentPadding = PaddingValues(
                        top = headerHeightDp,
                        bottom = contentPadding.calculateBottomPadding() + 32.dp
                    ),
                    vm = vm,
                    onOpenVideos = onOpenVideos,
                    onOpenFavorites = onOpenFavorites,
                    onOpenRaw = onOpenRaw,
                    onOpenPanoramas = onOpenPanoramas,
                    onOpenPersonDetail = onOpenPersonDetail,
                    onOpenSeeAllPeople = onOpenSeeAllPeople,
                )
                results.isEmpty() -> EmptyState(
                    title = "No results",
                    subtitle = "Nothing matches \"$query\".",
                    icon = Icons.Rounded.SearchOff,
                )
                else -> MediaGrid(
                    viewerOpen = viewerOpen,
                    focusedItem = focusedItem,
                    sections = listOf(MediaSection("search", "", results)),
                    columns = gridColumns,
                    selectionMode = false,
                    selectedIds = emptySet(),
                    highQualityThumbnails = settings.highQualityThumbnails,
                    onColumnsChange = { gridColumns = it },
                    onClick = { item, bounds ->
                        focusManager.clearFocus(force = true)
                        keyboardController?.hide()
                        onOpenViewer(results, item.uri, bounds)
                    },
                    onLongClick = {},
                    contentPadding = PaddingValues(
                        top = headerHeightDp,
                        bottom = contentPadding.calculateBottomPadding()
                    ),
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        
        Column(
            Modifier
                .fillMaxWidth()
                .background(com.gorilla.gallery.ui.theme.DesignTokens.BgBase)
                .onSizeChanged { headerHeight = it.height }
        ) {
            ScreenTitle("Search")
            SearchBar(
                query = query,
                onQueryChange = vm::setQuery,
                onClear = { vm.setQuery("") },
                autoFocus = autoFocus,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).padding(bottom = 8.dp),
            )
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    autoFocus: Boolean,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val focusRequester = remember { FocusRequester() }

    androidx.compose.material3.OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Photos, albums, dates, people...", color = LocalAppColors.current.textSecondary, fontSize = 14.5.sp) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = "Search",
                tint = if (query.isNotEmpty()) LocalDynamicColors.current.accent else LocalAppColors.current.textSecondary
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                androidx.compose.material3.IconButton(onClick = onClear) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Clear",
                        tint = LocalAppColors.current.textSecondary
                    )
                }
            }
        },
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .border(1.dp, LocalAppColors.current.borderGlass, RoundedCornerShape(24.dp)),
        colors = androidx.compose.material3.TextFieldDefaults.colors(
            focusedContainerColor = LocalAppColors.current.bgSurface.copy(alpha = 0.5f),
            unfocusedContainerColor = LocalAppColors.current.bgSurface.copy(alpha = 0.5f),
            focusedIndicatorColor = LocalDynamicColors.current.accent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        singleLine = true,
        interactionSource = interaction,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search)
    )
}

@Composable
private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier {
    val interaction = remember { MutableInteractionSource() }
    return this
        .pressScale(interaction, pressedScale = 0.88f)
        .clickable(
            interactionSource = interaction,
            indication = null,
            onClick = onClick,
        )
}

@Composable
private fun SearchCategories(
    contentPadding: PaddingValues,
    vm: SearchViewModel,
    onOpenVideos: () -> Unit = {},
    onOpenFavorites: () -> Unit = {},
    onOpenRaw: () -> Unit = {},
    onOpenPanoramas: () -> Unit = {},
    onOpenPersonDetail: (Int) -> Unit = {},
    onOpenSeeAllPeople: () -> Unit = {},
) {
    val peopleCategories by vm.peopleCategories.collectAsStateWithLifecycle()

    androidx.compose.foundation.lazy.LazyColumn(
        contentPadding = contentPadding,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(26.dp)
    ) {
        item {
            CategorySection(
                eyebrow = "CATEGORIES",
                title = "Media Types",
                showSeeAll = false
            ) {
                MediaTypesGrid(
                    onOpenVideos = onOpenVideos,
                    onOpenFavorites = onOpenFavorites,
                    onOpenRaw = onOpenRaw,
                    onOpenPanoramas = onOpenPanoramas,
                )
            }
        }

    }
}

@Composable
private fun CategorySection(
    eyebrow: String,
    title: String,
    showSeeAll: Boolean,
    seeAllText: String = "",
    onSeeAll: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val textColorPrimary = LocalAppColors.current.textPrimary
    val textColorSecondary = LocalAppColors.current.textSecondary
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Column {
                Text(eyebrow, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, color = textColorSecondary)
                Text(title, fontSize = 18.5.sp, fontWeight = FontWeight.Bold, color = textColorPrimary)
            }
            if (showSeeAll) {
                Text(seeAllText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = LocalDynamicColors.current.accent, modifier = Modifier.clickable { onSeeAll?.invoke() })
            }
        }
        content()
    }
}

@Composable
private fun MediaTypesGrid(
    onOpenVideos: () -> Unit = {},
    onOpenFavorites: () -> Unit = {},
    onOpenRaw: () -> Unit = {},
    onOpenPanoramas: () -> Unit = {},
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(horizontal = 20.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MediaTypeTile(
                title = "Videos",
                icon = Icons.Rounded.PlayArrow,
                iconColor = Color(0xFF38BDF8),
                iconBg = Color(0xFF38BDF8).copy(alpha = 0.14f),
                modifier = Modifier.weight(1f),
                onClick = onOpenVideos,
            )
            MediaTypeTile(
                title = "Favorites",
                icon = Icons.Rounded.FavoriteBorder,
                iconColor = Color(0xFFFB7185),
                iconBg = Color(0xFFFB7185).copy(alpha = 0.14f),
                modifier = Modifier.weight(1f),
                onClick = onOpenFavorites,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MediaTypeTile(
                title = "RAW Shots",
                icon = Icons.Rounded.Camera,
                iconColor = Color(0xFF34D399),
                iconBg = Color(0xFF34D399).copy(alpha = 0.14f),
                modifier = Modifier.weight(1f),
                onClick = onOpenRaw,
            )
            MediaTypeTile(
                title = "Panoramas",
                icon = Icons.Rounded.PanoramaHorizontal,
                iconColor = Color(0xFFF0ABFC),
                iconBg = Color(0xFFF0ABFC).copy(alpha = 0.14f),
                modifier = Modifier.weight(1f),
                onClick = onOpenPanoramas,
            )
        }
    }
}

@Composable
private fun MediaTypeTile(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, iconColor: Color, iconBg: Color, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    val appColors = LocalAppColors.current
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .pressScale(0.94f)
            .height(52.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
            .background(appColors.bgSurface.copy(alpha = 0.85f))
            .border(1.dp, appColors.borderGlass, androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(28.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(9.dp)).background(iconBg), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(15.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(title, color = appColors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.1).sp)
        }
    }
}

