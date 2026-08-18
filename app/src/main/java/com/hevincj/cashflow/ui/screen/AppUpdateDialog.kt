package com.hevincj.cashflow.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hevincj.cashflow.domain.models.AppUpdateInfo
import com.hevincj.cashflow.ui.theme.CardBackground
import com.hevincj.cashflow.ui.theme.LocalDarkTheme
import com.hevincj.cashflow.ui.theme.PositiveGreen
import com.hevincj.cashflow.ui.theme.TextPrimary
import com.hevincj.cashflow.ui.theme.TextSecondary
import com.hevincj.cashflow.utils.DownloadStatus
import java.io.File
import java.text.DecimalFormat

@Composable
fun AppUpdateDialog(
    updateInfo: AppUpdateInfo,
    downloadStatus: DownloadStatus,
    onDismiss: () -> Unit,
    onDownloadClick: () -> Unit,
    onInstallClick: (File) -> Unit
) {
    val isDark = LocalDarkTheme.current

    Dialog(
        onDismissRequest = {
            if (downloadStatus !is DownloadStatus.Downloading) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = downloadStatus !is DownloadStatus.Downloading,
            dismissOnClickOutside = downloadStatus !is DownloadStatus.Downloading,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp)
                .border(
                    width = 1.dp,
                    color = if (isDark) Color(0xFF2C2C2E) else Color.Transparent,
                    shape = RoundedCornerShape(24.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF635BFF).copy(alpha = if (isDark) 0.25f else 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.RocketLaunch,
                        contentDescription = "Update Available",
                        tint = Color(0xFF635BFF),
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "New Update Available!",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Version Badge Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDark) Color(0xFF2C2C2E) else Color(0xFFF2F2F7)
                    ) {
                        Text(
                            text = "v${updateInfo.currentVersion}",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Text("➔", color = TextSecondary, fontSize = 12.sp)

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF635BFF).copy(alpha = if (isDark) 0.3f else 0.15f)
                    ) {
                        Text(
                            text = "v${updateInfo.latestVersion}",
                            color = Color(0xFF635BFF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    if (updateInfo.isDeltaPatch && updateInfo.patchSize != null && updateInfo.patchSize > 0) {
                        val sizeInMb = updateInfo.patchSize.toDouble() / (1024 * 1024)
                        val df = DecimalFormat("#.#")
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = PositiveGreen.copy(alpha = if (isDark) 0.25f else 0.12f)
                        ) {
                            Text(
                                text = "Patch: ${df.format(sizeInMb)} MB",
                                color = PositiveGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    } else if (updateInfo.apkSize > 0) {
                        val sizeInMb = updateInfo.apkSize.toDouble() / (1024 * 1024)
                        val df = DecimalFormat("#.#")
                        Text(
                            text = "(${df.format(sizeInMb)} MB)",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Release Notes Card
                Text(
                    text = "WHAT'S NEW",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) Color(0xFF2C2C2E) else Color(0xFFF9FAFB),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 160.dp)
                ) {
                    Text(
                        text = updateInfo.releaseNotes.trim(),
                        color = TextPrimary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState())
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Progress Bar or Error Display
                when (downloadStatus) {
                    is DownloadStatus.Downloading -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            LinearProgressIndicator(
                                progress = { downloadStatus.progress / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = Color(0xFF635BFF),
                                trackColor = if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA)
                            )
                            val downloadedMb = downloadStatus.downloadedBytes.toDouble() / (1024 * 1024)
                            val totalMb = downloadStatus.totalBytes.toDouble() / (1024 * 1024)
                            val df = DecimalFormat("#.#")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (downloadStatus.isPatch) "${downloadStatus.progress.toInt()}% patch downloaded" else "${downloadStatus.progress.toInt()}% downloaded",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "${df.format(downloadedMb)} / ${df.format(totalMb)} MB",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    is DownloadStatus.Patching -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = Color(0xFF635BFF),
                                trackColor = if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA)
                            )
                            Text(
                                text = downloadStatus.message,
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    is DownloadStatus.Error -> {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFDE8E8),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Error: ${downloadStatus.message}",
                                color = Color(0xFFE53935),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    else -> {}
                }

                // Action Buttons
                val isBusy = downloadStatus is DownloadStatus.Downloading || downloadStatus is DownloadStatus.Patching
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!isBusy) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Later", color = TextSecondary)
                        }
                    }

                    when (downloadStatus) {
                        is DownloadStatus.Completed -> {
                            Button(
                                onClick = { onInstallClick(downloadStatus.apkFile) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PositiveGreen)
                            ) {
                                Icon(Icons.Rounded.SystemUpdate, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Install Now")
                            }
                        }
                        is DownloadStatus.Downloading -> {
                            Button(
                                onClick = {},
                                enabled = false,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Downloading...")
                            }
                        }
                        is DownloadStatus.Patching -> {
                            Button(
                                onClick = {},
                                enabled = false,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Applying Patch...")
                            }
                        }
                        else -> {
                            Button(
                                onClick = onDownloadClick,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF635BFF))
                            ) {
                                Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Update Now")
                            }
                        }
                    }
                }
            }
        }
    }
}
