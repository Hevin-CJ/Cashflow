package com.hevincj.cashflow.ui.screen

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.hevincj.cashflow.ui.screen.viewmodel.ScanViewModel
import com.hevincj.cashflow.ui.theme.PrimaryGradient
import com.hevincj.cashflow.ui.theme.IncomePurpleColor
import com.hevincj.cashflow.ui.theme.CardBackground
import com.hevincj.cashflow.ui.theme.BackgroundGray
import com.hevincj.cashflow.ui.theme.LocalDarkTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanOptionsUi(
    onDismissRequest: () -> Unit,
    onBatchBarcodeClick: () -> Unit,
    onReceiptScanClick: () -> Unit,
    onUpiQrClick: () -> Unit,
    rootNavController: NavController,
    viewModel: ScanViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = Receipts, 1 = QR code
    var isFlashActive by remember { mutableStateOf(false) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    
    var scannedUpiUri by remember { mutableStateOf<String?>(null) }
    var showContactPicker by remember { mutableStateOf(false) }
    var showCheckBalance by remember { mutableStateOf(false) }
    var contactPayeeName by remember { mutableStateOf("") }
    var contactPayeeVpa by remember { mutableStateOf("") }
    var showSendMoneyForContact by remember { mutableStateOf(false) }
    // New send money flow states
    var showSendMoneyChooser by remember { mutableStateOf(false) }
    var showEnterVpaDialog by remember { mutableStateOf(false) }
    var contactPayeePhone by remember { mutableStateOf("") }
    var showConfirmVpaDialog by remember { mutableStateOf(false) }
    var showAppPickerForContact by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    if (scannedUpiUri != null) {
        UpiSendMoneyDialog(
            upiUri = scannedUpiUri!!,
            onDismissRequest = { scannedUpiUri = null },
            viewModel = viewModel,
            onPaymentSuccess = {
                scannedUpiUri = null
                onDismissRequest()
            }
        )
    }

    if (showSendMoneyForContact) {
        UpiSendMoneyDialog(
            upiUri = "",
            payeeVpa = contactPayeeVpa,
            payeeName = contactPayeeName,
            onDismissRequest = { showSendMoneyForContact = false },
            viewModel = viewModel,
            onPaymentSuccess = {
                showSendMoneyForContact = false
                onDismissRequest()
            }
        )
    }

    if (showContactPicker) {
        UpiContactPickerDialog(
            onDismissRequest = { showContactPicker = false },
            onContactSelected = { name, phone ->
                contactPayeeName = name
                contactPayeePhone = phone
                showContactPicker = false
                showAppPickerForContact = true   // → go to app picker, not VPA entry
            }
        )
    }

    // New: show installed UPI apps to pay the selected contact directly
    if (showAppPickerForContact) {
        UpiAppPickerForContactDialog(
            contactName = contactPayeeName,
            contactPhone = contactPayeePhone,
            onDismissRequest = { showAppPickerForContact = false }
        )
    }

    if (showConfirmVpaDialog) {
        UpiConfirmVpaDialog(
            contactName = contactPayeeName,
            contactPhone = contactPayeePhone,
            onDismissRequest = { showConfirmVpaDialog = false },
            onConfirm = { vpa, name ->
                contactPayeeVpa = vpa
                contactPayeeName = name
                showConfirmVpaDialog = false
                showSendMoneyForContact = true
            }
        )
    }

    if (showEnterVpaDialog) {
        UpiEnterVpaDialog(
            onDismissRequest = { showEnterVpaDialog = false },
            onVpaConfirmed = { vpa, name ->
                contactPayeeVpa = vpa
                contactPayeeName = name
                showEnterVpaDialog = false
                showSendMoneyForContact = true
            }
        )
    }

    // 2-option send money chooser bottom sheet
    if (showSendMoneyChooser) {
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showSendMoneyChooser = false },
            containerColor = com.hevincj.cashflow.ui.theme.CardBackground,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            androidx.compose.foundation.layout.Column(
                modifier = androidx.compose.ui.Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
            ) {
                androidx.compose.material3.Text(
                    text = "Send Money via UPI",
                    fontSize = 17.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = com.hevincj.cashflow.ui.theme.TextPrimary
                )
                androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(6.dp))
                androidx.compose.material3.Text(
                    text = "Choose how to send money",
                    fontSize = 13.sp,
                    color = com.hevincj.cashflow.ui.theme.TextSecondary
                )
                androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(24.dp))

                // Option 1: Pay by UPI ID
                androidx.compose.material3.OutlinedButton(
                    onClick = {
                        showSendMoneyChooser = false
                        showEnterVpaDialog = true
                    },
                    modifier = androidx.compose.ui.Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, androidx.compose.ui.graphics.Color(0xFF9C27B0))
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = androidx.compose.material.icons.Icons.Rounded.AccountBalance,
                        contentDescription = null,
                        tint = androidx.compose.ui.graphics.Color(0xFF9C27B0),
                        modifier = androidx.compose.ui.Modifier.size(20.dp)
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.width(10.dp))
                    androidx.compose.material3.Text(
                        text = "Pay by UPI ID",
                        fontSize = 15.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                        color = androidx.compose.ui.graphics.Color(0xFF9C27B0)
                    )
                }

                androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(12.dp))

                // Option 2: Pay via Contacts
                androidx.compose.material3.Button(
                    onClick = {
                        showSendMoneyChooser = false
                        showContactPicker = true
                    },
                    modifier = androidx.compose.ui.Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = androidx.compose.ui.graphics.Color(0xFF9C27B0)
                    )
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = androidx.compose.material.icons.Icons.Rounded.Person,
                        contentDescription = null,
                        tint = androidx.compose.ui.graphics.Color.White,
                        modifier = androidx.compose.ui.Modifier.size(20.dp)
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.width(10.dp))
                    androidx.compose.material3.Text(
                        text = "Pay via Contacts",
                        fontSize = 15.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                        color = androidx.compose.ui.graphics.Color.White
                    )
                }
            }
        }
    }

    if (showCheckBalance) {
        UpiCheckBalanceDialog(
            onDismissRequest = { showCheckBalance = false }
        )
    }

    // Animate Card Height depending on active scanner mode:
    // Tab 0 (Receipts/Barcode): Horizontal rectangle aspect ratio (220.dp height)
    // Tab 1 (QR Code): Square aspect ratio (320.dp height)
    val cardHeight by animateDpAsState(
        targetValue = if (selectedTab == 0) 220.dp else 320.dp,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "CardHeightAnimation"
    )

    // Morphing background gradient colors using official app brand colors
    val startColor by animateColorAsState(
        targetValue = if (selectedTab == 0) Color(0xFF4FC3F7) else Color(0xFF9575CD), // GradientLightBlue vs GradientPurple
        animationSpec = tween(600, easing = LinearOutSlowInEasing),
        label = "StartGradientColor"
    )
    val endColor by animateColorAsState(
        targetValue = if (selectedTab == 0) Color(0xFF9575CD) else Color(0xFF8121FD), // GradientPurple vs IncomePurpleColor
        animationSpec = tween(600, easing = LinearOutSlowInEasing),
        label = "EndGradientColor"
    )

    // Morphing ambient shadow spot color using official brand colors
    val shadowColor by animateColorAsState(
        targetValue = if (selectedTab == 0) Color(0xFFFFD700) else Color(0xFF8121FD), // Gold vs IncomePurpleColor glow
        animationSpec = tween(600),
        label = "AmbientShadowColor"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGray)
    ) {
        // Immersive shifting top gradient area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            startColor,
                            endColor.copy(alpha = 0.5f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Main UI Column layout
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left spacer slot matching the width of close button to center the switcher
                Spacer(modifier = Modifier.width(48.dp))

                // Sliding switcher in center
                SlidingSegmentedControl(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )

                // Right Simple Close Button
                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            // Viewfinder Card & Actions grouped closely in remaining top-aligned space
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                // Card Viewfinder (Refactored to Box to prevent Material3 Card elevation overlay tints)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(cardHeight) // Animates dynamically between 220.dp and 320.dp
                        .padding(horizontal = 12.dp) // Less padding for more length
                        .shadow(
                            elevation = 20.dp,
                            shape = RoundedCornerShape(32.dp),
                            clip = false,
                            spotColor = shadowColor
                        )
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color.Black)
                        .border(2.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(32.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Live Scanner camera view
                        if (hasCameraPermission) {
                            CardScannerView(
                                selectedTab = selectedTab,
                                isFlashActive = isFlashActive,
                                onFlashControlReady = { cameraControl = it },
                                viewModel = viewModel,
                                onDismiss = onDismissRequest,
                                rootNavController = rootNavController,
                                onUpiQrScanned = { scannedUpiUri = it }
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFF1E293B)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    Text(
                                        text = "Camera access required for live scanning.",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )
                                    Button(
                                        onClick = { launcher.launch(Manifest.permission.CAMERA) },
                                        colors = ButtonDefaults.buttonColors(containerColor = IncomePurpleColor),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Grant Permission", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Gold scan line animation (Only show when camera permission is granted)
                        if (hasCameraPermission) {
                            val transition = rememberInfiniteTransition(label = "laser")
                            val animatedPosition by transition.animateFloat(
                                initialValue = 0.05f,
                                targetValue = 0.95f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(durationMillis = 2000, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "laserPosition"
                            )

                            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                                if (selectedTab == 0) {
                                    // Barcode Scanner: Gold line is vertical and moves horizontally (left-to-right)

                                    // Vertical laser line
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .width(2.dp)
                                            .offset {
                                                IntOffset(x = (constraints.maxWidth * animatedPosition).toInt(), y = 0)
                                            }
                                            .background(
                                                brush = Brush.verticalGradient(
                                                    colors = listOf(
                                                        Color.Transparent,
                                                        Color(0xFFFFD700), // Gold
                                                        Color(0xFFFFD700),
                                                        Color.Transparent
                                                    )
                                                )
                                            )
                                            .shadow(elevation = 6.dp, spotColor = Color(0xFFFFD700))
                                    )

                                    // Volumetric glow
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .width(30.dp)
                                            .offset {
                                                val xPos = (constraints.maxWidth * animatedPosition).toInt()
                                                val offsetXPx = xPos - 15.dp.roundToPx()
                                                IntOffset(x = offsetXPx, y = 0)
                                            }
                                            .background(
                                                brush = Brush.horizontalGradient(
                                                    colors = listOf(
                                                        Color.Transparent,
                                                        Color(0xFFFFD700).copy(alpha = 0.15f),
                                                        Color.Transparent
                                                    )
                                                )
                                            )
                                    )
                                } else {
                                    // QR Code Scanner: Gold line is horizontal and moves vertically (up-and-down)

                                    // Horizontal laser line
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(2.dp)
                                            .offset {
                                                IntOffset(x = 0, y = (constraints.maxHeight * animatedPosition).toInt())
                                            }
                                            .background(
                                                brush = Brush.horizontalGradient(
                                                    colors = listOf(
                                                        Color.Transparent,
                                                        Color(0xFFFFD700), // Gold
                                                        Color(0xFFFFD700),
                                                        Color.Transparent
                                                    )
                                                )
                                            )
                                            .shadow(elevation = 6.dp, spotColor = Color(0xFFFFD700))
                                    )

                                    // Volumetric glow
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(30.dp)
                                            .offset {
                                                val yPos = (constraints.maxHeight * animatedPosition).toInt()
                                                val offsetYPx = yPos - 15.dp.roundToPx()
                                                IntOffset(x = 0, y = offsetYPx)
                                            }
                                            .background(
                                                brush = Brush.verticalGradient(
                                                    colors = listOf(
                                                        Color.Transparent,
                                                        Color(0xFFFFD700).copy(alpha = 0.15f),
                                                        Color.Transparent
                                                    )
                                                )
                                            )
                                    )
                                }
                            }
                        }

                        // Tab-specific overlays inside card
                        if (hasCameraPermission) {
                            if (selectedTab == 0) {
                            // RECEIPTS TAB OVERLAYS
                            
                            // Central horizontal barcode aiming area box
                            Box(
                                modifier = Modifier
                                    .size(260.dp, 130.dp)
                                    .align(Alignment.Center)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.Black.copy(alpha = 0.2f))
                                    .border(1.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                            ) {
                                // Subtle 12 vertical barcode lines watermark
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 30.dp, vertical = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    repeat(12) {
                                        Box(
                                            modifier = Modifier
                                                .width(2.5.dp)
                                                .fillMaxHeight()
                                                .background(Color.White.copy(alpha = 0.15f))
                                        )
                                    }
                                }

                                Text(
                                    text = "Align 12-Digit Barcode Here",
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp,
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .background(Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            // Top Right: Gallery + Flashlight Controls
                            Column(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Gallery Image Picker
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.35f))
                                        .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                                        .clickable {
                                            onDismissRequest()
                                            onReceiptScanClick() // navigates to ReceiptScanScreen which handles gallery picking
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Image,
                                        contentDescription = "Gallery",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Flashlight Toggle
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isFlashActive) Color(0xFFFFD700).copy(alpha = 0.35f)
                                            else Color.Black.copy(alpha = 0.35f)
                                        )
                                        .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                                        .clickable {
                                            isFlashActive = !isFlashActive
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Lightbulb,
                                        contentDescription = "Flashlight",
                                        tint = if (isFlashActive) Color(0xFFFFD700) else Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Center Info Text removed to clean scanner UI
                        } else {
                            // QR CODE TAB OVERLAYS
                            val toneGenerator = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 100) }

                            // Pulsing corner frame animation for high-tech look
                            val transition = rememberInfiniteTransition(label = "pulse")
                            val pulseScale by transition.animateFloat(
                                initialValue = 0.98f,
                                targetValue = 1.02f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(durationMillis = 1250, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "pulseScale"
                            )
                            
                            // Center Frame: L-corners bounding box
                            Box(
                                modifier = Modifier
                                    .size(170.dp)
                                    .graphicsLayer {
                                        scaleX = pulseScale
                                        scaleY = pulseScale
                                    }
                                    .align(Alignment.Center),
                                contentAlignment = Alignment.Center
                            ) {
                                QrFrame(modifier = Modifier.fillMaxSize())

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.QrCodeScanner,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.25f),
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Align QR Code",
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Cashback Info Text removed to clean scanner UI
                        }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp)) // Cozy padding right next to the bottom of the scanner

                // Action Buttons Row (Placed OUTSIDE the card right next to the bottom of the card!)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectedTab == 0) {
                        // Receipts Tab: Secondary "Bulk adding" + Primary "Receipt AI scan"
                        ActionButton(
                            text = "Bulk adding",
                            onClick = {
                                onDismissRequest()
                                onBatchBarcodeClick()
                            },
                            useGradient = false,
                            modifier = Modifier.weight(1f)
                        )

                        ActionButton(
                            text = "Receipt AI scan",
                            onClick = {
                                onDismissRequest()
                                onReceiptScanClick()
                            },
                            useGradient = true,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        // QR Code Tab: Secondary "Send money" + Primary "Check balance"
                        ActionButton(
                            text = "Send money",
                            onClick = {
                                showSendMoneyChooser = true
                            },
                            useGradient = false,
                            modifier = Modifier.weight(1f)
                        )

                        ActionButton(
                            text = "Check balance",
                            onClick = {
                                showCheckBalance = true
                            },
                            useGradient = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    useGradient: Boolean = false,
    containerColor: Color = CardBackground,
    textColor: Color = IncomePurpleColor
) {
    val buttonBackgroundModifier = if (useGradient) {
        Modifier.background(brush = PrimaryGradient)
    } else {
        Modifier.background(containerColor)
    }

    val buttonBorderModifier = if (!useGradient) {
        Modifier.border(
            border = BorderStroke(1.5.dp, PrimaryGradient),
            shape = RoundedCornerShape(16.dp)
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .height(54.dp)
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .then(buttonBackgroundModifier)
            .then(buttonBorderModifier)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (useGradient) Color.White else textColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SlidingSegmentedControl(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val transition = updateTransition(targetState = selectedTab, label = "TabTransition")
    
    val indicatorOffset by transition.animateDp(
        transitionSpec = { spring(stiffness = Spring.StiffnessLow) },
        label = "IndicatorOffset"
    ) { tab ->
        if (tab == 0) 4.dp else 104.dp
    }

    Box(
        modifier = modifier
            .width(208.dp)
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = 0.22f))
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(22.dp))
            .padding(4.dp)
    ) {
        // Active sliding capsule background
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(96.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White)
                .shadow(elevation = 2.dp, shape = RoundedCornerShape(18.dp))
        )

        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onTabSelected(0) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Receipts",
                    fontWeight = FontWeight.Bold,
                    color = if (selectedTab == 0) Color(0xFF1E293B) else Color.White,
                    fontSize = 13.sp
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onTabSelected(1) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "QR code",
                    fontWeight = FontWeight.Bold,
                    color = if (selectedTab == 1) Color(0xFF1E293B) else Color.White,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun QrFrame(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val lineLength = 24.dp.toPx()
        val strokeWidth = 3.dp.toPx()
        val greenColor = Color(0xFF7CFF7C) // Neon bright green

        // Top-Left Corner
        drawLine(greenColor, Offset(0f, 0f), Offset(lineLength, 0f), strokeWidth)
        drawLine(greenColor, Offset(0f, 0f), Offset(0f, lineLength), strokeWidth)

        // Top-Right Corner
        drawLine(greenColor, Offset(size.width, 0f), Offset(size.width - lineLength, 0f), strokeWidth)
        drawLine(greenColor, Offset(size.width, 0f), Offset(size.width, lineLength), strokeWidth)

        // Bottom-Left Corner
        drawLine(greenColor, Offset(0f, size.height), Offset(lineLength, size.height), strokeWidth)
        drawLine(greenColor, Offset(0f, size.height), Offset(0f, size.height - lineLength), strokeWidth)

        // Bottom-Right Corner
        drawLine(greenColor, Offset(size.width, size.height), Offset(size.width - lineLength, size.height), strokeWidth)
        drawLine(greenColor, Offset(size.width, size.height), Offset(size.width, size.height - lineLength), strokeWidth)
    }
}

@Composable
private fun CardScannerView(
    selectedTab: Int,
    isFlashActive: Boolean,
    onFlashControlReady: (CameraControl?) -> Unit,
    viewModel: ScanViewModel,
    onDismiss: () -> Unit,
    rootNavController: NavController,
    onUpiQrScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val toneGenerator = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 100) }
    
    // Crucial: Wrap callbacks and selections in rememberUpdatedState to prevent stale state capture by CameraX analyzer closures!
    val currentSelectedTab by rememberUpdatedState(selectedTab)
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    val currentRootNavController by rememberUpdatedState(rootNavController)
    val currentOnUpiQrScanned by rememberUpdatedState(onUpiQrScanned)

    var isScanningEnabled by remember { mutableStateOf(true) }
    val currentScanningEnabled by rememberUpdatedState(isScanningEnabled)
    var cameraControlState by remember { mutableStateOf<CameraControl?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            toneGenerator.release()
        }
    }

    // Connect flash controls
    LaunchedEffect(isFlashActive, cameraControlState) {
        try {
            cameraControlState?.enableTorch(isFlashActive)
        } catch (e: Exception) {
            Log.e("CardScannerView", "Torch activation failed", e)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val resolutionSelector = ResolutionSelector.Builder()
                        .setResolutionStrategy(
                            ResolutionStrategy(
                                Size(1280, 720),
                                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                            )
                        )
                        .build()

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setResolutionSelector(resolutionSelector)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        // Read updated state delegates dynamically inside the background thread analyzer
                        if (currentScanningEnabled && (currentSelectedTab == 0 || currentSelectedTab == 1)) {
                            processImageProxy(
                                imageProxy = imageProxy,
                                onSuccess = { barcode ->
                                    if (isScanningEnabled) {
                                        val barcodeValue = barcode.rawValue ?: return@processImageProxy
                                        if (currentSelectedTab == 0) {
                                            val isUrl = barcodeValue.startsWith("http://", ignoreCase = true) ||
                                                    barcodeValue.startsWith("https://", ignoreCase = true) ||
                                                    barcodeValue.startsWith("www.", ignoreCase = true) ||
                                                    barcodeValue.contains("://", ignoreCase = true) ||
                                                    barcodeValue.startsWith("upi://", ignoreCase = true)
                                            val isFullBarcode = barcodeValue.length >= 12 && barcodeValue.all { it.isDigit() }
                                            if (!isUrl && isFullBarcode) {
                                                val rect = barcode.boundingBox
                                                if (rect != null) {
                                                    // Centering constraint (middle 70% width and 60% height region)
                                                    val marginX = imageProxy.width * 0.15f
                                                    val marginY = imageProxy.height * 0.20f
                                                    val isFullyInside = rect.left >= marginX &&
                                                            rect.right <= (imageProxy.width - marginX) &&
                                                            rect.top >= marginY &&
                                                            rect.bottom <= (imageProxy.height - marginY)

                                                    if (isFullyInside) {
                                                        isScanningEnabled = false
                                                        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
                                                        triggerVibration(context)

                                                        // Async navigation trigger using updated ViewModel & controller
                                                        viewModel.lookupSingleBarcode(barcodeValue) { product ->
                                                            currentOnDismiss()
                                                            val isProductValid = product != null &&
                                                                com.hevincj.cashflow.utils.isProductValid(product.productName, barcodeValue)
                                                            if (isProductValid) {
                                                                currentRootNavController.navigate(
                                                                    "add_transaction?" +
                                                                    "title=${product!!.productName}" +
                                                                    "&amount=${product.price ?: ""}" +
                                                                    "&category=${product.category}" +
                                                                    "&barcode=$barcodeValue"
                                                                )
                                                            } else {
                                                                currentRootNavController.navigate("add_transaction?barcode=$barcodeValue")
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            // Tab 1: UPI QR Codes
                                            if (barcodeValue.startsWith("upi://", ignoreCase = true)) {
                                                isScanningEnabled = false
                                                toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
                                                triggerVibration(context)
                                                currentOnUpiQrScanned(barcodeValue)
                                            }
                                        }
                                    }
                                }
                            )
                        } else {
                            imageProxy.close()
                        }
                    }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    try {
                        cameraProvider.unbindAll()
                        val camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                        cameraControlState = camera.cameraControl
                        onFlashControlReady(camera.cameraControl)
                    } catch (e: Exception) {
                        Log.e("CardScannerView", "Camera binding failed", e)
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Tint overlay inside card depending on tab
        if (selectedTab == 1) {
            // Indigo gradient overlay tint
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF635BFF).copy(alpha = 0.55f),
                                Color(0xFF3F51B5).copy(alpha = 0.65f)
                            )
                        )
                    )
            )
        } else {
            // Clean view for Receipts (with a very soft dark filter)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.05f))
            )
        }
    }
}

@SuppressLint("UnsafeOptInUsageError")
private fun processImageProxy(
    imageProxy: ImageProxy,
    onSuccess: (Barcode) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val options = com.google.mlkit.vision.barcode.BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_QR_CODE
            )
            .build()
        val scanner = BarcodeScanning.getClient(options)
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    onSuccess(barcode)
                }
            }
            .addOnFailureListener {
                Log.e("CardScannerView", "Barcode extraction failed", it)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}

private fun triggerVibration(context: Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    } ?: return

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(100)
    }
}
