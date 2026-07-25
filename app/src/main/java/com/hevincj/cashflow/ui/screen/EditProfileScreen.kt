package com.hevincj.cashflow.ui.screen

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.hevincj.cashflow.ui.screen.viewmodel.ProfileViewModel
import com.hevincj.cashflow.ui.theme.*
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.state.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var profileImageBase64 by remember { mutableStateOf<String?>(null) }
    var isImageCompressing by remember { mutableStateOf(false) }

    // Initialize fields with current profile values once loaded
    var isInitialized by remember { mutableStateOf(false) }
    if (!isInitialized && uiState.username.isNotEmpty() && !uiState.isLoading) {
        firstName = uiState.firstName
        lastName = uiState.lastName
        phoneNumber = uiState.phoneNumber
        profileImageBase64 = uiState.profileImage
        isInitialized = true
    }

    // Image Picker Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            isImageCompressing = true
            // Run scaling and compression on a background thread to keep UI interactive
            val currentContext = context
            coroutineScope.launch(Dispatchers.Default) {
                val base64 = compressUriToBase64(currentContext, it)
                withContext(Dispatchers.Main) {
                    profileImageBase64 = base64
                    isImageCompressing = false
                }
            }
        }
    }

    // Handle profile update success routing
    LaunchedEffect(uiState.isUpdateSuccess) {
        if (uiState.isUpdateSuccess) {
            Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
            viewModel.clearUpdateSuccess()
            navController.popBackStack()
        }
    }

    // Handle error toasts
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Account Info", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundGray)
            )
        },
        containerColor = BackgroundGray
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Profile Picture Selector
            Box(
                contentAlignment = Alignment.BottomEnd,
                modifier = Modifier
                    .size(116.dp)
                    .clickable {
                        if (!isImageCompressing && !uiState.isLoading) {
                            imagePickerLauncher.launch("image/*")
                        }
                    }
            ) {
                // Image Box
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(if (LocalDarkTheme.current) Color(0xFF2C2C2E) else Color(0xFFEFEFFF))
                        .border(2.dp, Color(0xFF635BFF).copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isImageCompressing) {
                        CircularProgressIndicator(
                            color = Color(0xFF635BFF),
                            modifier = Modifier.size(36.dp)
                        )
                    } else {
                        val bitmap = remember(profileImageBase64) { base64ToBitmap(profileImageBase64) }
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
                                contentDescription = "Default Avatar",
                                modifier = Modifier.size(60.dp),
                                tint = Color(0xFF635BFF)
                            )
                        }
                    }
                }

                // Edit Camera Icon Badge
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(CardBackground)
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color(0xFF635BFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Choose Photo",
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Form container
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Email / Username (Read-Only)
                OutlinedTextField(
                    value = uiState.username,
                    onValueChange = {},
                    label = { Text("Email (Cannot be modified)") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email", tint = Color.Gray) },
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledBorderColor = if (LocalDarkTheme.current) Color(0xFF3C3C3E) else Color(0xFFE5E7EB),
                        disabledTextColor = TextPrimary.copy(alpha = 0.6f),
                        disabledLabelColor = Color.Gray
                    )
                )

                // First Name Field
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("First Name") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = "First Name", tint = Color(0xFF635BFF)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF635BFF),
                        unfocusedBorderColor = if (LocalDarkTheme.current) Color(0xFF3C3C3E) else Color(0xFFE5E7EB),
                        focusedLabelColor = Color(0xFF635BFF)
                    )
                )

                // Last Name Field
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("Last Name") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Last Name", tint = Color(0xFF635BFF)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF635BFF),
                        unfocusedBorderColor = if (LocalDarkTheme.current) Color(0xFF3C3C3E) else Color(0xFFE5E7EB),
                        focusedLabelColor = Color(0xFF635BFF)
                    )
                )

                // Phone Number Field
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = "Phone Number", tint = Color(0xFF635BFF)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF635BFF),
                        unfocusedBorderColor = if (LocalDarkTheme.current) Color(0xFF3C3C3E) else Color(0xFFE5E7EB),
                        focusedLabelColor = Color(0xFF635BFF)
                    )
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Save Profile Button
            Button(
                onClick = {
                    if (firstName.isBlank()) {
                        Toast.makeText(context, "First Name cannot be empty", Toast.LENGTH_SHORT).show()
                    } else if (lastName.isBlank()) {
                        Toast.makeText(context, "Last Name cannot be empty", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.updateProfile(firstName.trim(), lastName.trim(), phoneNumber.trim(), profileImageBase64)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFF635BFF), Color(0xFF8121FD))
                        )
                    ),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                enabled = !uiState.isLoading && !isImageCompressing
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        text = "Save Changes",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}

/**
 * Resizes the selected Uri image to a max dimension of 300px and compresses it to JPEG 80%
 * to generate a lightweight Base64 string that doesn't bloat MongoDB document sizes.
 */
private fun compressUriToBase64(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()
        if (originalBitmap == null) return null

        val maxSize = 300
        val ratio = originalBitmap.width.toFloat() / originalBitmap.height.toFloat()
        val width = if (ratio > 1) maxSize else (maxSize * ratio).toInt()
        val height = if (ratio > 1) (maxSize / ratio).toInt() else maxSize
        val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, width, height, true)

        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val compressedBytes = outputStream.toByteArray()
        outputStream.close()

        android.util.Base64.encodeToString(compressedBytes, android.util.Base64.NO_WRAP)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
