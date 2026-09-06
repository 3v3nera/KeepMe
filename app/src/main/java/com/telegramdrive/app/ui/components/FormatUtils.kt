package com.telegramdrive.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import java.util.Locale

fun formatDuration(seconds: Int?): String {
    if (seconds == null || seconds <= 0) return ""
    val m = seconds / 60
    val s = seconds % 60
    return String.format(Locale.US, "%d:%02d", m, s)
}

fun formatFileSize(sizeInBytes: Long): String {
    if (sizeInBytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(sizeInBytes.toDouble()) / Math.log10(1024.0)).toInt()
    val formattedSize = sizeInBytes / Math.pow(1024.0, digitGroups.toDouble())
    return String.format(Locale.US, "%.1f %s", formattedSize, units[digitGroups])
}

@Composable
fun FileTypeIcon(mimeType: String, modifier: Modifier = Modifier) {
    val icon: ImageVector = when {
        mimeType.startsWith("image/") -> Icons.Rounded.InsertDriveFile // fallback
        mimeType.startsWith("video/") -> Icons.Rounded.VideoFile
        mimeType.startsWith("audio/") -> Icons.Rounded.AudioFile
        mimeType == "application/pdf" -> Icons.Rounded.PictureAsPdf
        mimeType.startsWith("text/") -> Icons.Rounded.Description
        else -> Icons.Rounded.InsertDriveFile
    }

    Icon(
        imageVector = icon,
        contentDescription = "File Type Icon",
        modifier = modifier
    )
}
