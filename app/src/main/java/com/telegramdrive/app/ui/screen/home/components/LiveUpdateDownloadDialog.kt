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
import androidx.compose.ui.unit.sp
import com.telegramdrive.app.ui.theme.TDriveColors
import androidx.compose.foundation.background as glassBackground
import androidx.compose.foundation.shape.RoundedCornerShape as GlassRoundedShape
import com.telegramdrive.app.ui.components.GlassChip
import com.telegramdrive.app.ui.components.GlassChipText
import com.telegramdrive.app.ui.components.GlassDialogHost
import com.telegramdrive.app.ui.components.GlassHeroSlot
import com.telegramdrive.app.ui.components.GlassPanel
import com.telegramdrive.app.ui.components.GlassPanelTokens
import com.telegramdrive.app.ui.components.GlassPrimaryButton
import com.telegramdrive.app.ui.components.GlassSecondaryButton
import java.text.DecimalFormat

@Composable
fun LiveUpdateDownloadDialog(
    updateInfo: AppUpdateInfo,
    downloadState: UpdateDownloadState,
    onCancelClick: () -> Unit,
    onInstallClick: () -> Unit,
    onDismissClick: () -> Unit,
    onRetryClick: () -> Unit = onCancelClick
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    // Keep screen awake while actively downloading update so the download isn't suspended by screen lock
    androidx.compose.runtime.DisposableEffect(downloadState is UpdateDownloadState.Downloading) {
        val window = (context as? android.app.Activity)?.window
        if (downloadState is UpdateDownloadState.Downloading) {
            window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val isDownloading = downloadState is UpdateDownloadState.Downloading
    val dismiss: () -> Unit = { if (!isDownloading) onDismissClick() }

    Dialog(
        onDismissRequest = dismiss,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        GlassDialogHost(onDismiss = dismiss, scrimStrength = 0.74f) {
            Column(
                modifier = Modifier.fillMaxWidth(0.88f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                GlassPanel(backdrop = null, modifier = Modifier.fillMaxWidth()) {
                    GlassHeroSlot(
                        height = 112.dp,
                        tint = when (downloadState) {
                            is UpdateDownloadState.ReadyToInstall -> TDriveColors.Secondary
                            is UpdateDownloadState.Error -> Color(0xFFFF453A)
                            else -> TDriveColors.Primary
                        },
                        chips = { GlassChip { GlassChipText("Version ${updateInfo.versionName}") } }
                    ) {
                        Icon(
                            imageVector = when (downloadState) {
                                is UpdateDownloadState.ReadyToInstall -> Icons.Rounded.CheckCircle
                                is UpdateDownloadState.Error -> Icons.Rounded.ErrorOutline
                                else -> Icons.Rounded.SystemUpdate
                            },
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(44.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = when (downloadState) {
                            is UpdateDownloadState.ReadyToInstall -> "Download Complete"
                            is UpdateDownloadState.Error -> "Update Failed"
                            else -> "Updating KeepMe"
                        },
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 22.sp,
                            letterSpacing = (-0.3).sp
                        ),
                        color = GlassPanelTokens.TextPrimary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

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

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape)
                                    .glassBackground(GlassPanelTokens.PillFill)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(animatedProgress)
                                        .height(8.dp)
                                        .clip(CircleShape)
                                        .glassBackground(Color.White.copy(alpha = 0.92f))
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$downloadedFormatted / $totalFormatted",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp
                                    ),
                                    color = GlassPanelTokens.TextSecondary
                                )
                                Text(
                                    text = "$percentage%",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = GlassPanelTokens.TextPrimary
                                )
                            }
                        }

                        is UpdateDownloadState.ReadyToInstall -> {
                            Text(
                                text = "The latest APK has been securely downloaded from @siddTdrive. Tap Install to complete the update.",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp),
                                color = GlassPanelTokens.TextSecondary
                            )
                        }

                        is UpdateDownloadState.Error -> {
                            Text(
                                text = downloadState.message,
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp),
                                color = Color(0xFFFF8A80)
                            )
                        }

                        else -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                                Text(
                                    "Connecting to Telegram update channel...",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                                    color = GlassPanelTokens.TextSecondary
                                )
                            }
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (downloadState) {
                        is UpdateDownloadState.Downloading ->
                            GlassSecondaryButton(text = "Cancel Download", onClick = onCancelClick)

                        is UpdateDownloadState.ReadyToInstall -> {
                            GlassSecondaryButton(text = "Close", onClick = onDismissClick)
                            GlassPrimaryButton(text = "Install Update", onClick = onInstallClick)
                        }

                        is UpdateDownloadState.Error -> {
                            GlassSecondaryButton(text = "Close", onClick = onDismissClick)
                            GlassPrimaryButton(text = "Retry", onClick = onRetryClick)
                        }

                        else -> GlassSecondaryButton(text = "Close", onClick = onDismissClick)
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