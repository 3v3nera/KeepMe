package com.telegramdrive.app.ui.screen.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.telegramdrive.app.domain.update.AppUpdateInfo
import com.telegramdrive.app.ui.components.GlassChip
import com.telegramdrive.app.ui.components.GlassChipText
import com.telegramdrive.app.ui.components.GlassDialogHost
import com.telegramdrive.app.ui.components.GlassHeroSlot
import com.telegramdrive.app.ui.components.GlassPanel
import com.telegramdrive.app.ui.components.GlassPanelTokens
import com.telegramdrive.app.ui.components.GlassPrimaryButton
import com.telegramdrive.app.ui.components.GlassSecondaryButton
import com.telegramdrive.app.ui.components.GlassSectionHeader
import com.telegramdrive.app.ui.theme.TDriveColors
import java.text.DecimalFormat

@Composable
fun UpdatePromptDialog(
    updateInfo: AppUpdateInfo,
    onUpdateClick: () -> Unit,
    onDismissClick: () -> Unit
) {
    val formattedSize = remember(updateInfo.fileSize) {
        formatBytes(updateInfo.fileSize)
    }

    Dialog(
        onDismissRequest = onDismissClick,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        GlassDialogHost(onDismiss = onDismissClick, scrimStrength = 0.74f) {
            Column(
                modifier = Modifier.fillMaxWidth(0.88f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                GlassPanel(backdrop = null, modifier = Modifier.fillMaxWidth()) {
                    GlassHeroSlot(
                        height = 120.dp,
                        tint = TDriveColors.Primary,
                        chips = {
                            GlassChip {
                                GlassChipText(
                                    if (formattedSize.isNotBlank()) "v${updateInfo.versionName} • $formattedSize"
                                    else "v${updateInfo.versionName}"
                                )
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.RocketLaunch,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(44.dp)
                        )
                    }

                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = "New Update Available",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 22.sp,
                            letterSpacing = (-0.3).sp
                        ),
                        color = GlassPanelTokens.TextPrimary
                    )

                    if (updateInfo.changelog.isNotBlank()) {
                        Spacer(Modifier.height(20.dp))
                        GlassSectionHeader("What's new")
                        Spacer(Modifier.height(12.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(GlassPanelTokens.InnerRadius))
                                .background(GlassPanelTokens.PillFill)
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp)
                        ) {
                            Text(
                                text = updateInfo.changelog,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 19.sp),
                                color = GlassPanelTokens.TextPrimary
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassSecondaryButton(text = "Later", onClick = onDismissClick)
                    GlassPrimaryButton(text = "Update Now", onClick = onUpdateClick)
                }
            }
        }
    }
}
private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return ""
    val units = arrayOf("B", "KB", "MB", "GB")
    var digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    if (digitGroups >= units.size) digitGroups = units.size - 1
    val df = DecimalFormat("#,##0.#")
    return "${df.format(bytes / Math.pow(1024.0, digitGroups.toDouble()))} ${units[digitGroups]}"
}
