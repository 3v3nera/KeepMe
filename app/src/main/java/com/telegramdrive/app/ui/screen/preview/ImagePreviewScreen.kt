package com.telegramdrive.app.ui.screen.preview

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.telegramdrive.app.domain.model.TelegramFile
import com.telegramdrive.app.ui.components.ZoomableImage
import com.telegramdrive.app.ui.theme.TDriveColors
import java.io.File

@Composable
fun ImagePreviewPage(
    file: TelegramFile,
    onScaleChanged: (Float) -> Unit = {},
    onTap: () -> Unit = {},
    viewModel: MediaPreviewViewModel = hiltViewModel(key = file.messageId.toString())
) {
    LaunchedEffect(file.messageId) {
        viewModel.prepareFile(file)
    }

    val localPath by viewModel.localPath.collectAsState()
    val thumbnailPath by viewModel.thumbnailPath.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val progress by viewModel.downloadProgress.collectAsState()

    // Determine best available image path: full-res downloaded -> local file path -> thumbnail from VM -> thumbnail from file
    val activePath = remember(localPath, file.localFilePath, thumbnailPath, file.thumbnailPath) {
        listOfNotNull(
            localPath,
            file.localFilePath,
            thumbnailPath,
            file.thumbnailPath
        ).firstOrNull { File(it).exists() }
    }
    val imageModel = remember(activePath) {
        if (activePath != null) {
            val f = File(activePath)
            if (f.exists()) f else null
        } else null
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (imageModel != null) {
            ZoomableImage(
                model = imageModel,
                contentDescription = file.fileName,
                onScaleChanged = onScaleChanged,
                onTap = onTap,
                modifier = Modifier.fillMaxSize()
            )
        } else if (!isLoading) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                ) {
                    Text(
                        text = "Unable to load image preview",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.3.sp
                        ),
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // ── Download progress indicator ──
        if (isLoading) {
            Column(
                modifier = Modifier
                    .align(if (imageModel == null) Alignment.Center else Alignment.BottomCenter)
                    .padding(bottom = if (imageModel == null) 0.dp else 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                ) {
                    Text(
                        text = "Loading full resolution",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.3.sp
                        ),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                if (progress > 0f) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(44.dp),
                        color = TDriveColors.Primary,
                        trackColor = Color.White.copy(alpha = 0.25f),
                        strokeWidth = 3.5.dp
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(44.dp),
                        color = TDriveColors.Primary,
                        trackColor = Color.White.copy(alpha = 0.25f),
                        strokeWidth = 3.5.dp
                    )
                }
            }
        }
    }
}
