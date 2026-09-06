package com.telegramdrive.app.ui.screen.preview

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.telegramdrive.app.domain.model.FileType
import com.telegramdrive.app.domain.model.TelegramFile
import com.telegramdrive.app.ui.components.formatFileSize
import com.telegramdrive.app.ui.utils.rememberAppHaptics

@Composable
fun DocumentPreviewPage(
    file: TelegramFile,
    viewModel: MediaPreviewViewModel = hiltViewModel(key = file.messageId.toString())
) {
    val context = LocalContext.current
    val haptics = rememberAppHaptics()
    val localPath by viewModel.localPath.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val progress by viewModel.downloadProgress.collectAsState()
    
    LaunchedEffect(file.messageId) { viewModel.prepareFile(file) }
    
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Large file type icon
        Icon(
            imageVector = when (file.fileType) {
                FileType.DOCUMENT -> Icons.Rounded.Description
                FileType.AUDIO -> Icons.Rounded.MusicNote
                else -> Icons.Rounded.InsertDriveFile
            },
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color.White.copy(alpha = 0.8f)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // File name
        Text(
            text = file.fileName,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // File size
        Text(
            text = formatFileSize(file.fileSize),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.6f)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Download progress OR open button
        if (isLoading) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.White.copy(alpha = 0.3f)
            )
        } else if (localPath != null) {
            Button(
                onClick = {
                    haptics.click()
                    FileOpenUtils.openDocumentInExternalApp(context, file.copy(localFilePath = localPath))
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Rounded.OpenInNew, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Open in App")
            }
        }
    }
}
