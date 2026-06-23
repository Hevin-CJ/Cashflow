package com.hevincj.cashflow.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.hevincj.cashflow.data.local.ThemeMode
import com.hevincj.cashflow.ui.screen.viewmodel.ProfileViewModel
import com.hevincj.cashflow.ui.theme.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ProfileMenuItem(
    val title: String,
    val icon: ImageVector,
    val backgroundColor: Color
)

val menuItems = listOf(
    ProfileMenuItem("Account Info", Icons.Default.Person, Color(0xFF635BFF)),
    ProfileMenuItem("Security Code", Icons.Default.Shield, Color(0xFF65C466)),
    ProfileMenuItem("Subscriptions & Recurring", Icons.Default.Autorenew, Color(0xFFFF9F1C)),
    ProfileMenuItem("Privacy Policy", Icons.Default.Lock, Color(0xFF3B5973)),
    ProfileMenuItem("Settings", Icons.Default.Settings, Color(0xFF32827A)),
    ProfileMenuItem("Logout", Icons.AutoMirrored.Filled.ExitToApp, Color(0xFFE93B3A))
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGray)
            .padding(innerPaddingValues)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(64.dp))
        ProfileHeader()
        Spacer(modifier = Modifier.height(48.dp))

        menuItems.forEach { item ->
            MenuItemRow(
                item = item,
                onClick = {
                    when (item.title) {
                        "Logout" -> showLogoutDialog = true
                        "Settings" -> showSettingsDialog = true
                        "Subscriptions & Recurring" -> rootNavController.navigate("subscription_manager")
                    }
                }
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ProfileHeader() {
    Box(
        contentAlignment = Alignment.BottomEnd,
        modifier = Modifier.size(104.dp)
    ) {
        // Profile Image Placeholder
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(if (LocalDarkTheme.current) Color(0xFF2C2C2E) else Color(0xFFEFEFFF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Profile Picture Placeholder",
                modifier = Modifier.size(56.dp),
                tint = Color(0xFF635BFF)
            )
        }

        // Edit Icon Badge
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(CardBackground)
                .padding(3.dp) // Creates the white stroke effect
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
                    modifier = Modifier.size(16.dp),
                    tint = Color(0xFF635BFF)
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "Leslie Alexander",
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextPrimary
    )

    Spacer(modifier = Modifier.height(4.dp))

    Text(
        text = "leslie@gmail.com",
        fontSize = 14.sp,
        color = TextSecondary
    )
}

@Composable
private fun MenuItemRow(
    item: ProfileMenuItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Leading Icon Container
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(item.backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(20.dp))

        // Title Text
        Text(
            text = item.title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )

        // Trailing Chevron
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Navigate to ${item.title}",
            tint = Color(0xFFB3B3B3),
            modifier = Modifier.size(24.dp)
        )
    }
}