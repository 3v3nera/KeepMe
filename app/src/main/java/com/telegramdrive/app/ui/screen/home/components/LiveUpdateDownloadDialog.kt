package com.telegramdrive.app.ui.screen.home.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.telegramdrive.app.domain.update.AppUpdateInfo
import com.telegramdrive.app.domain.update.UpdateDownloadState
import com.telegramdrive.app.ui.theme.TDriveColors
import java.text.DecimalFormat

@Composable
fun LiveUpdateDownloadDialog(
    updateInfo: AppUpdateInfo,
    downloadState: UpdateDownloadState,
    onCancelClick: () -> Unit,
    onInstallClick: () -> Unit,
    onDismissClick: () -> Unit
) {
    Dialog(
        onDismissRequest = {
            if (downloadState !is UpdateDownloadState.Downloading) {
                onDismissClick()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = downloadState !is UpdateDownloadState.Downloading,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = TDriveColors.SurfaceElevated,
            border = androidx.compose.foundation.BorderStroke(1.dp, TDriveColors.Outline),
            shadowElevation = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = when (downloadState) {
                                is UpdateDownloadState.ReadyToInstall -> TDriveColors.SecondaryContainer
                                is UpdateDownloadState.Error -> TDriveColors.ErrorContainer
                                else -> TDriveColors.PrimaryContainer
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = when (downloadState) {
                                        is UpdateDownloadState.ReadyToInstall -> Icons.Rounded.CheckCircle
                                        is UpdateDownloadState.Error -> Icons.Rounded.ErrorOutline
                                        else -> Icons.Rounded.SystemUpdate
                                    },
                                    contentDescription = null,
                                    tint = when (downloadState) {
                                        is UpdateDownloadState.ReadyToInstall -> TDriveColors.Secondary
                                        is UpdateDownloadState.Error -> TDriveColors.Error
                                        else -> TDriveColors.Primary
                                    },
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = when (downloadState) {
                                    is UpdateDownloadState.ReadyToInstall -> "Download Complete"
                                    is UpdateDownloadState.Error -> "Update Failed"
                                    else -> "Updating KeepMe"
                                },
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TDriveColors.OnBackground
                            )
                            Text(
                                text = "Version ${updateInfo.versionName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TDriveColors.OnSurfaceDim
                            )
                        }
                    }

                    if (downloadState !is UpdateDownloadState.Downloading) {
                        IconButton(onClick = onDismissClick) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close",
                                tint = TDriveColors.OnSurfaceDim
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                when (downloadState) {
                    is UpdateDownloadState.Downloading -> {
                        val animatedProgress by animateFloatAsState(
                            targetValue = downloadState.progress,
                            label = "update_download_progress"
                        )

                        val downloadedFormatted = remember(downloadState.downloadedBytes) {
                            formatBytes(downloadState.downloadedBytes)
                        }
                        val totalFormatted = remember(downloadState.totalBytes) {
                            formatBytes(downloadState.totalBytes)
                        }
                        val percentage = (animatedProgress * 100).toInt()

                        // Progress bar with rounded caps and gradient
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(TDriveColors.SurfaceBright)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(animatedProgress)
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                TDriveColors.Primary,
                                                TDriveColors.Secondary
                                            )
                                        )
                                    )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Stats: Downloaded / Total & Percentage
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$downloadedFormatted / $totalFormatted",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = TDriveColors.OnSurface
                            )
                            Text(
                                text = "$percentage%",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = TDriveColors.Primary
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        OutlinedButton(
                            onClick = onCancelClick,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cancel Download", color = TDriveColors.OnSurface)
                        }
                    }

                    is UpdateDownloadState.ReadyToInstall -> {
                        Text(
                            text = "The latest APK has been securely downloaded from @siddTdrive. Tap Install to complete the update.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TDriveColors.OnSurface,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = onInstallClick,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TDriveColors.Secondary,
                                contentColor = Color(0xFF003824)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Install Update",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    is UpdateDownloadState.Error -> {
                        Text(
                            text = downloadState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TDriveColors.Error,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismissClick,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Close", color = TDriveColors.OnSurface)
                            }
                            Button(
                                onClick = onCancelClick,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = TDriveColors.Primary)
                            ) {
                                Text("Retry")
                            }
                        }
                    }

                    else -> {
                        // Preparing
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = TDriveColors.Primary
                            )
                            Text(
                                "Connecting to Telegram update channel...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TDriveColors.OnSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    var digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    if (digitGroups >= units.size) digitGroups = units.size - 1
    val df = DecimalFormat("#,##0.#")
    return "${df.format(bytes / Math.pow(1024.0, digitGroups.toDouble()))} ${units[digitGroups]}"
}
