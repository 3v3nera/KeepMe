package com.telegramdrive.app.ui.screen.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.telegramdrive.app.domain.model.TelegramFile
import com.telegramdrive.app.ui.components.WaterfallGrid
import com.telegramdrive.app.ui.theme.TDriveColors

import androidx.compose.material3.pulltorefresh.PullToRefreshBox

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
    val query by viewModel.searchQuery.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

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
        // Custom Header
        Text(
            text = "Search",
            style = MaterialTheme.typography.displaySmall,
            color = Color.White,
            modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 16.dp)
        )

        // Custom Search Input
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            color = TDriveColors.SurfaceElevated,
            border = androidx.compose.foundation.BorderStroke(1.dp, TDriveColors.Outline)
        ) {
            TextField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                placeholder = { Text("Looking for something?...", color = TDriveColors.OnSurfaceDim) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search", tint = TDriveColors.OnSurfaceDim) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = TDriveColors.Primary,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .screenTabSwipe(
                    onSwipeNext = onNavigateToFolders,
                    onSwipePrev = onNavigateToHome
                )
        ) {
            if (query.isNotBlank()) {
                WaterfallGrid(
                    files = searchResults,
                    isLoading = isSearching,
                    onFileClick = onFileClick,
                    onFileLongPress = { },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Search your photos, docs, clips, and more",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TDriveColors.OnSurfaceDim
                    )
                }
            }
        }
        }
    }
}
