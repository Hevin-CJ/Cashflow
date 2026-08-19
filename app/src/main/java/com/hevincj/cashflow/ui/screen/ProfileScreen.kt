package com.hevincj.cashflow.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
import android.content.Context
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarDuration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import com.hevincj.cashflow.data.local.ThemeMode
import com.hevincj.cashflow.ui.screen.state.ProfileUiState
import com.hevincj.cashflow.ui.screen.viewmodel.ProfileViewModel
import com.hevincj.cashflow.ui.screen.viewmodel.ExportDateRange
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import com.hevincj.cashflow.ui.theme.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.hevincj.cashflow.utils.CsvExporter
import com.hevincj.cashflow.utils.PdfExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ProfileMenuItem(
    val title: String,
    val icon: ImageVector,
    val backgroundColor: Color,
    val subtitle: String? = null,
    val badgeText: String? = null
)

@Composable
fun ProfileScreen(
    rootNavController: NavController,
    innerPaddingValues: PaddingValues,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.state.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    
    var selectedRange by remember { mutableStateOf(ExportDateRange.CURRENT_MONTH) }
    var expandedRange by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    fun openSavedFile(uri: Uri, mimeType: String) {
        val mimeCandidates = when (mimeType) {
            "text/csv" -> listOf(
                "text/csv",
                "text/comma-separated-values",
                "application/csv",
                "application/vnd.ms-excel",
                "text/plain",
                "*/*"
            )
            "application/pdf" -> listOf(
                "application/pdf",
                "*/*"
            )
            else -> listOf(mimeType, "*/*")
        }

        var launched = false
        for (candidate in mimeCandidates) {
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, candidate)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                val chooser = Intent.createChooser(intent, "Open file").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
                launched = true
                break
            } catch (e: Exception) {
                // Try next candidate MIME type
            }
        }

        if (!launched) {
            try {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = if (mimeType == "text/csv") "text/*" else mimeType
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                val shareChooser = Intent.createChooser(shareIntent, "Open or share file").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(shareChooser)
            } catch (e: Exception) {
                Toast.makeText(context, "No app found to open this file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // CSV Launcher
    val createCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                val transactions = viewModel.getTransactionsForRange(selectedRange)
                val success = CsvExporter.exportToCsv(context, uri, transactions)
                withContext(Dispatchers.Main) {
                    if (success) {
                        coroutineScope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = "Export saved successfully",
                                actionLabel = "Open",
                                duration = SnackbarDuration.Long
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                openSavedFile(uri, "text/csv")
                            }
                        }
                    } else {
                        Toast.makeText(context, "Failed to export CSV.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // PDF Launcher
    val createPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                val transactions = viewModel.getTransactionsForRange(selectedRange)
                val success = PdfExporter.exportToPdf(context, uri, transactions)
                withContext(Dispatchers.Main) {
                    if (success) {
                        coroutineScope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = "Export saved successfully",
                                actionLabel = "Open",
                                duration = SnackbarDuration.Long
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                openSavedFile(uri, "application/pdf")
                            }
                        }
                    } else {
                        Toast.makeText(context, "Failed to export PDF.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    LaunchedEffect(uiState.isLoggedOut) {
        if (uiState.isLoggedOut) {
            rootNavController.navigate("login") {
                popUpTo("main") { inclusive = true }
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to log out?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
                    }
                ) {
                    Text("Confirm", color = Color(0xFFE93B3A))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = CardBackground
        )
    }

    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("Theme Settings", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Choose application theme:")
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val options = listOf(
                        ThemeMode.LIGHT to "Light Mode",
                        ThemeMode.DARK to "Dark Mode",
                        ThemeMode.SYSTEM to "Same as device (System)"
                    )
                    
                    options.forEach { (mode, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setThemeMode(mode)
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(
                                selected = (uiState.themeMode == mode),
                                onClick = { viewModel.setThemeMode(mode) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("Close", color = Color(0xFF635BFF))
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = CardBackground
        )
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Export Transactions", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Select Date Range:", fontSize = 14.sp, color = Color.Gray)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(BackgroundGray)
                            .clickable { expandedRange = true }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when (selectedRange) {
                                    ExportDateRange.CURRENT_MONTH -> "Current Month"
                                    ExportDateRange.PREVIOUS_MONTH -> "Previous Month"
                                    ExportDateRange.CURRENT_YEAR -> "Current Year"
                                    ExportDateRange.PREVIOUS_YEAR -> "Previous Year"
                                    ExportDateRange.ALL -> "All Transactions"
                                },
                                fontWeight = FontWeight.Medium
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Select Range"
                            )
                        }
                        DropdownMenu(
                            expanded = expandedRange,
                            onDismissRequest = { expandedRange = false },
                            modifier = Modifier.fillMaxWidth(0.6f).background(CardBackground)
                        ) {
                            ExportDateRange.values().forEach { range ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = when (range) {
                                                ExportDateRange.CURRENT_MONTH -> "Current Month"
                                                ExportDateRange.PREVIOUS_MONTH -> "Previous Month"
                                                ExportDateRange.CURRENT_YEAR -> "Current Year"
                                                ExportDateRange.PREVIOUS_YEAR -> "Previous Year"
                                                ExportDateRange.ALL -> "All Transactions"
                                            }
                                        )
                                    },
                                    onClick = {
                                        selectedRange = range
                                        expandedRange = false
                                    }
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Choose format to export:")
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            showExportDialog = false
                            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy_HH-mm-ss"))
                            createCsvLauncher.launch("transactions_${selectedRange.name.lowercase()}_$timestamp.csv")
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = IncomePurpleColor)
                    ) {
                        Text("CSV")
                    }
                    Button(
                        onClick = {
                            showExportDialog = false
                            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy_HH-mm-ss"))
                            createPdfLauncher.launch("transactions_${selectedRange.name.lowercase()}_$timestamp.pdf")
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = IncomePurpleColor)
                    ) {
                        Text("PDF")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = CardBackground
        )
    }

    LaunchedEffect(Unit) {
        viewModel.fetchUserProfile()
        viewModel.checkForUpdates(isManualCheck = false)
    }

    LaunchedEffect(uiState.updateMessage) {
        uiState.updateMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUpdateMessage()
        }
    }

    uiState.updateInfo?.let { updateInfo ->
        AppUpdateDialog(
            updateInfo = updateInfo,
            downloadStatus = uiState.downloadStatus,
            onDismiss = { viewModel.dismissUpdateDialog() },
            onDownloadClick = { viewModel.startDownload(updateInfo) },
            onInstallClick = { apkFile ->
                if (!com.hevincj.cashflow.utils.ApkInstaller.canRequestPackageInstalls(context)) {
                    com.hevincj.cashflow.utils.ApkInstaller.openInstallPermissionSettings(context)
                } else {
                    com.hevincj.cashflow.utils.ApkInstaller.installApk(context, apkFile)
                }
            }
        )
    }

    val updateSubtitle = when {
        uiState.isCheckingUpdate -> "Checking newer versions..."
        uiState.hasUpdateAvailable && uiState.latestAvailableVersion != null ->
            "v${com.hevincj.cashflow.BuildConfig.VERSION_NAME} • v${uiState.latestAvailableVersion} available"
        else ->
            "v${com.hevincj.cashflow.BuildConfig.VERSION_NAME} (Latest)"
    }
    val updateBadge = if (uiState.hasUpdateAvailable && !uiState.isCheckingUpdate) "UPDATE" else null

    val currentMenuItems = remember(uiState.hasUpdateAvailable, uiState.latestAvailableVersion, uiState.isCheckingUpdate) {
        listOf(
            ProfileMenuItem("Account Info", Icons.Default.Person, Color(0xFF635BFF)),
            ProfileMenuItem("Export To CSV/PDF", Icons.Default.Share, Color(0xFF65C466)),
            ProfileMenuItem("Exchange Currency", Icons.Default.AttachMoney, Color(0xFF0288D1)),
            ProfileMenuItem("Subscriptions & Recurring", Icons.Default.Autorenew, Color(0xFFFF9F1C)),
            ProfileMenuItem(
                title = "Check for Updates",
                icon = Icons.Default.SystemUpdate,
                backgroundColor = Color(0xFF8121FD),
                subtitle = updateSubtitle,
                badgeText = updateBadge
            ),
            ProfileMenuItem("Settings", Icons.Default.Settings, Color(0xFF32827A)),
            ProfileMenuItem("Logout", Icons.AutoMirrored.Filled.ExitToApp, Color(0xFFE93B3A))
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundGray)
                .padding(innerPaddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            ProfileHeader(uiState = uiState, onClick = { rootNavController.navigate("edit_profile") })
            Spacer(modifier = Modifier.height(28.dp))

            currentMenuItems.forEach { item ->
                val isChecking = item.title == "Check for Updates" && uiState.isCheckingUpdate
                MenuItemRow(
                    item = item,
                    isLoading = isChecking,
                    onClick = {
                        when (item.title) {
                            "Account Info" -> rootNavController.navigate("edit_profile")
                            "Logout" -> showLogoutDialog = true
                            "Settings" -> showSettingsDialog = true
                            "Exchange Currency" -> rootNavController.navigate("exchange_currency")
                            "Subscriptions & Recurring" -> rootNavController.navigate("subscription_manager")
                            "Check for Updates" -> {
                                if (uiState.hasUpdateAvailable) {
                                    viewModel.openUpdateDialog()
                                } else {
                                    viewModel.checkForUpdates(isManualCheck = true)
                                }
                            }
                            "Export To CSV/PDF" -> showExportDialog = true
                        }
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = innerPaddingValues.calculateBottomPadding() + 16.dp)
        )
    }
}

@Composable
private fun ProfileHeader(
    uiState: ProfileUiState,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.BottomEnd,
        modifier = Modifier
            .size(96.dp)
            .clickable(onClick = onClick)
    ) {
        // Profile Image Box
        Box(
            modifier = Modifier
                .size(92.dp)
                .clip(CircleShape)
                .background(if (LocalDarkTheme.current) Color(0xFF2C2C2E) else Color(0xFFEFEFFF)),
            contentAlignment = Alignment.Center
        ) {
            val bitmap = remember(uiState.profileImage) { base64ToBitmap(uiState.profileImage) }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Profile Picture",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile Picture Placeholder",
                    modifier = Modifier.size(50.dp),
                    tint = Color(0xFF635BFF)
                )
            }
        }

        // Edit Icon Badge
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(CardBackground)
                .padding(2.5.dp) // Creates the stroke effect
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(BackgroundGray),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Profile",
                    modifier = Modifier.size(14.dp),
                    tint = Color(0xFF635BFF)
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    val fullName = if (uiState.firstName.isBlank() && uiState.lastName.isBlank()) {
        uiState.username.substringBefore("@")
    } else {
        "${uiState.firstName} ${uiState.lastName}".trim()
    }

    Text(
        text = fullName,
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextPrimary
    )

    Spacer(modifier = Modifier.height(3.dp))

    Text(
        text = uiState.username,
        fontSize = 13.5.sp,
        color = TextSecondary
    )
}

fun base64ToBitmap(base64Str: String?): android.graphics.Bitmap? {
    if (base64Str.isNullOrBlank()) return null
    return try {
        val cleanBase64 = if (base64Str.contains(",")) {
            base64Str.substringAfter(",")
        } else {
            base64Str
        }.replace("\n", "").replace("\r", "").trim()
        val bytes = android.util.Base64.decode(cleanBase64, android.util.Base64.DEFAULT or android.util.Base64.NO_WRAP)
        android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@Composable
private fun MenuItemRow(
    item: ProfileMenuItem,
    isLoading: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isLoading, onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Leading Icon Container
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(item.backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Title & Subtitle Text
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = item.title,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                if (item.badgeText != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF635BFF),
                        contentColor = Color.White
                    ) {
                        Text(
                            text = item.badgeText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            if (item.subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.subtitle,
                    fontSize = 12.sp,
                    color = if (item.badgeText != null) Color(0xFF635BFF) else TextSecondary,
                    fontWeight = if (item.badgeText != null) FontWeight.Medium else FontWeight.Normal
                )
            }
        }

        // Trailing Chevron / Progress Indicator
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = Color(0xFF635BFF)
            )
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Navigate to ${item.title}",
                tint = if (item.badgeText != null) Color(0xFF635BFF) else Color(0xFFB3B3B3),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}