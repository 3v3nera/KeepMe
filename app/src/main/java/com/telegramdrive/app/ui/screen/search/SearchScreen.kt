package com.telegramdrive.app.ui.screen.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.telegramdrive.app.domain.model.TelegramFile
import com.telegramdrive.app.ui.theme.TDriveColors
import com.telegramdrive.app.ui.components.WaterfallGrid
import com.telegramdrive.app.ui.components.screenTabSwipe
import com.telegramdrive.app.ui.utils.rememberAppHaptics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onFileClick: (TelegramFile) -> Unit,
    onNavigateToHome: () -> Unit = {},
    onNavigateToFolders: () -> Unit = {},
    viewModel: SearchViewModel = hiltViewModel()
) {
    val haptics = rememberAppHaptics()
    val keyboard = LocalSoftwareKeyboardController.current
    val query by viewModel.searchQuery.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val searchType by viewModel.type.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            if (query.isNotBlank()) {
                haptics.tick()
                viewModel.refresh()
            }
        },
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            Text(
                text = "Search",
                style = MaterialTheme.typography.displaySmall,
                color = com.telegramdrive.app.ui.theme.TDriveColors.OnCanvas,
                modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 16.dp)
            )

            // Pill input, same material as every other field in the app
            BasicTextField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = TDriveColors.OnCanvas,
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp
                ),
                cursorBrush = SolidColor(com.telegramdrive.app.ui.theme.TDriveColors.OnCanvas),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    viewModel.commitSearch(query)
                    keyboard?.hide()
                }),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                decorationBox = { inner ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .clip(CircleShape)
                            .background(TDriveColors.FillSoft)
                            .padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Search",
                            tint = TDriveColors.OnCanvasMuted,
                            modifier = Modifier.size(20.dp)
                        )
                        Box(modifier = Modifier.weight(1f)) {
                            if (query.isEmpty()) {
                                Text(
                                    text = "Looking for something?...",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
                                    color = com.telegramdrive.app.ui.theme.TDriveColors.OnCanvasFaint
                                )
                            }
                            inner()
                        }
                        if (query.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(com.telegramdrive.app.ui.theme.TDriveColors.LineSoft)
                                    .clickable {
                                        haptics.tick()
                                        viewModel.onQueryChange("")
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Clear",
                                    tint = com.telegramdrive.app.ui.theme.TDriveColors.OnCanvas,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            )

            // Result type chips (only while searching)
            if (query.isNotBlank()) {
                androidx.compose.foundation.lazy.LazyRow(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                ) {
                    items(SearchType.values().toList()) { type ->
                        val selected = type == searchType
                        Text(
                            text = type.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) Color(0xFF0B0C0F) else TDriveColors.OnCanvas,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (selected) TDriveColors.OnCanvas else TDriveColors.FillSoft)
                                .clickable {
                                    haptics.click()
                                    viewModel.setType(type)
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .screenTabSwipe(
                        onSwipeNext = onNavigateToFolders,
                        onSwipePrev = onNavigateToHome
                    )
            ) {
                when {
                    query.isBlank() -> RecentSearches(
                        recents = recentSearches,
                        onPick = { viewModel.onQueryChange(it) },
                        onClear = { viewModel.clearRecentSearches() }
                    )
                    searchResults.isEmpty() && !isSearching ->
                        CenteredHint("No files match \u201C${query.trim()}\u201D")
                    else -> WaterfallGrid(
                        files = searchResults,
                        isLoading = isSearching,
                        onFileClick = { file ->
                            viewModel.commitSearch(query)
                            onFileClick(file)
                        },
                        onFileLongPress = { },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun CenteredHint(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = TDriveColors.OnCanvasMuted
        )
    }
}

@Composable
private fun RecentSearches(
    recents: List<String>,
    onPick: (String) -> Unit,
    onClear: () -> Unit
) {
    if (recents.isEmpty()) {
        CenteredHint("Search your photos, docs, clips, and more")
        return
    }
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent searches",
                style = MaterialTheme.typography.titleMedium,
                color = TDriveColors.OnCanvas
            )
            Text(
                text = "Clear",
                style = MaterialTheme.typography.labelLarge,
                color = TDriveColors.OnCanvasMuted,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onClear)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        recents.forEach { q ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                    .clickable { onPick(q) }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Rounded.History,
                    contentDescription = null,
                    tint = TDriveColors.OnCanvasFaint,
                    modifier = Modifier.size(20.dp)
                )
                Text(text = q, style = MaterialTheme.typography.bodyLarge, color = TDriveColors.OnCanvas)
            }
        }
    }
}