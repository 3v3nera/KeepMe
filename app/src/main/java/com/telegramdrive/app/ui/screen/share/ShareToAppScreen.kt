package com.telegramdrive.app.ui.screen.share

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.telegramdrive.app.domain.model.TelegramFolder
import com.telegramdrive.app.ui.screen.folders.FoldersViewModel
import com.telegramdrive.app.ui.utils.rememberAppHaptics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareToAppScreen(
    sharedUris: List<Uri>,
    onClose: () -> Unit,
    viewModel: FoldersViewModel = hiltViewModel(),
    shareViewModel: ShareViewModel = hiltViewModel()
) {
    val haptics = rememberAppHaptics()
    val folders by viewModel.folders.collectAsState()
    var isUploading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Save to Folder") },
                navigationIcon = {
                    IconButton(onClick = {
                        haptics.click()
                        onClose()
                    }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close")
                    }
                }
            )
        }
    ) { padding ->
        if (isUploading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
                Text("Uploading...", modifier = Modifier.padding(top = 48.dp))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        "Select a folder to save ${sharedUris.size} item(s)",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                items(folders, key = { it.id }) { folder ->
                    Card(
                        onClick = {
                            haptics.click()
                            isUploading = true
                            shareViewModel.uploadFiles(folder.id, sharedUris) {
                                haptics.success()
                                isUploading = false
                                onClose()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Folder, contentDescription = null, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(folder.name, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    }
}
