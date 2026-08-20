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
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.camera.core.CameraControl
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.composed
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameMillis
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.hevincj.cashflow.ui.screen.state.ScanUiState
import com.hevincj.cashflow.ui.screen.viewmodel.ScanEvent
import com.hevincj.cashflow.ui.screen.viewmodel.ScanViewModel
import com.hevincj.cashflow.ui.theme.TextPrimary
import com.hevincj.cashflow.ui.theme.TextSecondary
import com.hevincj.cashflow.ui.theme.BackgroundGray
import com.hevincj.cashflow.ui.theme.CardBackground
import com.hevincj.cashflow.ui.theme.LocalDarkTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchScanScreen(
    onNavigateBack: () -> Unit,
    viewModel: ScanViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    val shimmerTransition = rememberInfiniteTransition(label = "batchShimmer")
    val shimmerProgress = shimmerTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "batchShimmerProgress"
    )
    val shimmerProgressProvider = remember(shimmerProgress) { { shimmerProgress.value } }

    // Observe saveSuccess to pop the backstack
    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) {
            Toast.makeText(context, "Saved transaction!", Toast.LENGTH_SHORT).show()
            onNavigateBack()
        }
    }

    // Observe errorMessage to show error toast
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Batch Barcode Scanner") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CardBackground
                )
            )
        }
    ) { innerPadding ->
        if (hasCameraPermission) {
            BatchScanContent(
                modifier = Modifier.padding(innerPadding),
                onNavigateBack = onNavigateBack,
                viewModel = viewModel,
                state = state,
                shimmerProgressProvider = shimmerProgressProvider
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Camera permission is required to scan barcodes.",
                        textAlign = TextAlign.Center,
                        color = TextSecondary,
                        modifier = Modifier.padding(24.dp)
                    )
                    Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) {
                        Text("Grant Permission")
                    }
                }
            }
        }
    }
}

@Composable
private fun BatchScanContent(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit,
    viewModel: ScanViewModel,
    state: ScanUiState,
    shimmerProgressProvider: () -> Float
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var isFlashActive by remember { mutableStateOf(false) }
    var cameraControlState by remember { mutableStateOf<CameraControl?>(null) }

    LaunchedEffect(isFlashActive, cameraControlState) {
        try {
            cameraControlState?.enableTorch(isFlashActive)
        } catch (e: Exception) {
            com.hevincj.cashflow.utils.CrashLogger.w("BatchScanScreen", "Torch activation failed: ${e.message}", e)
        }
    }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val toneGenerator = remember {
        try {
            ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        } catch (e: Exception) {
            com.hevincj.cashflow.utils.CrashLogger.w("BatchScanScreen", "ToneGenerator initialization failed: ${e.message}", e)
            null
        }
    }

    val barcodeScanner = remember {
        val options = com.google.mlkit.vision.barcode.BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_EAN_13
            )
            .build()
        BarcodeScanning.getClient(options)
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            toneGenerator?.release()
            barcodeScanner.close()
        }
    }

    // Collect ScanEvents for Haptic/Beep triggers
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is ScanEvent.BeepAndVibrate -> {
                    toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
                    triggerVibration(context)
                }
                is ScanEvent.Vibrate -> {
                    triggerVibration(context)
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundGray)
    ) {
        // Camera viewfinder box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.Black)
        ) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    }
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                    cameraProviderFuture.addListener({
                        try {
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
                                processImageProxy(
                                    scanner = barcodeScanner,
                                    imageProxy = imageProxy,
                                    onSuccess = { barcode ->
                                        val barcodeValue = barcode.rawValue ?: return@processImageProxy
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
                                                    coroutineScope.launch(Dispatchers.Main) {
                                                        viewModel.onBarcodeScanned(barcodeValue)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                )
                            }

                            val cameraSelector = if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                                CameraSelector.DEFAULT_BACK_CAMERA
                            } else if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                                CameraSelector.DEFAULT_FRONT_CAMERA
                            } else {
                                null
                            }

                            if (cameraSelector != null) {
                                cameraProvider.unbindAll()
                                val camera = cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imageAnalysis
                                )
                                cameraControlState = camera.cameraControl
                            } else {
                                com.hevincj.cashflow.utils.CrashLogger.e("BatchScanScreen", "No available camera found on device")
                            }
                        } catch (e: Throwable) {
                            com.hevincj.cashflow.utils.CrashLogger.e("BatchScanScreen", "Camera setup or binding failed", e)
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Transparent overlay aiming area box
            Box(
                modifier = Modifier
                    .size(280.dp, 160.dp)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.3f))
            ) {
                // Subtle 12 vertical barcode lines watermark
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 40.dp, vertical = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(12) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .fillMaxHeight()
                                .background(Color.White.copy(alpha = 0.15f))
                        )
                    }
                }

                Text(
                    text = "Align 12-Digit Barcode Here",
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            // Flashlight button in top right of camera preview
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
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

        // List of scanned barcodes
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(480.dp)
                .navigationBarsPadding()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Scanned Items (${state.scannedCodes.size})",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    if (state.scannedCodes.isNotEmpty()) {
                        TextButton(onClick = { viewModel.onClearAll() }) {
                            Text("Clear All", color = Color.Red)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Scan once per barcode",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                    Switch(
                        checked = state.addBarcodeOnce,
                        onCheckedChange = { viewModel.onAddBarcodeOnceChanged(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF635BFF)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (state.scannedCodes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No barcodes scanned yet",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(state.scannedCodes, key = { index, code -> "$code-$index" }) { index, code ->
                             val productName = state.scannedProducts[code]?.productName
                             val isProductValid = com.hevincj.cashflow.utils.isProductValid(productName, code)
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (LocalDarkTheme.current) Color(0xFF2C2C2E) else Color(0xFFF3F4F6))
                                    .clickable(enabled = !state.resolvingCodes.contains(code)) {
                                        viewModel.onStartEditingProduct(code, if (isProductValid) productName!! else "")
                                    }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (state.resolvingCodes.contains(code)) {
                                    Box(
                                        modifier = Modifier
                                            .size(180.dp, 16.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .batchScannerShimmerEffect(shimmerProgressProvider)
                                    )
                                } else {
                                    Text(
                                        text = if (isProductValid) productName!! else "Barcode: $code",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextPrimary
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.onRemoveBarcode(code) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Text("✕", color = Color.Gray, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }

                if (state.scannedCodes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = state.commonTitle,
                            onValueChange = { viewModel.onTitleChanged(it) },
                            label = { Text("Title (Optional)") },
                            placeholder = { Text("e.g. Weekly Grocery") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF635BFF),
                                focusedLabelColor = Color(0xFF635BFF)
                            )
                        )
                        OutlinedTextField(
                            value = state.commonAmountString,
                            onValueChange = { viewModel.onAmountChanged(it) },
                            label = { Text("Amount") },
                            placeholder = { Text("e.g. 500") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF635BFF),
                                focusedLabelColor = Color(0xFF635BFF)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        viewModel.saveBatchTransaction()
                    },
                    enabled = state.scannedCodes.isNotEmpty() && !state.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF635BFF))
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Add to Transactions", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (state.editingCode != null) {
        val code = state.editingCode!!
        AlertDialog(
            onDismissRequest = { viewModel.onCancelEditingProduct() },
            title = { Text("Edit Product Name") },
            text = {
                Column {
                    Text("Barcode: $code", color = TextSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = state.editingName,
                        onValueChange = { viewModel.onEditingNameChanged(it) },
                        label = { Text("Product Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.onSaveEditedProduct()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF635BFF))
                ) {
                    Text("Save", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onCancelEditingProduct() }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}

private fun Modifier.batchScannerShimmerEffect(
    progressProvider: () -> Float,
    colors: List<Color> = listOf(
        Color(0xFFEAEAEA),
        Color(0xFFF5F5F5),
        Color(0xFFEAEAEA),
    )
): Modifier = this
    .clearAndSetSemantics { }
    .drawBehind {
        val width = size.width
        val height = size.height
        val progress = progressProvider()
        val startOffsetX = (progress * 4f - 2f) * width
        drawRect(
            brush = Brush.linearGradient(
                colors = colors,
                start = Offset(startOffsetX, 0f),
                end = Offset(startOffsetX + width, height)
            )
        )
    }

@SuppressLint("UnsafeOptInUsageError")
private fun processImageProxy(
    scanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    imageProxy: ImageProxy,
    onSuccess: (Barcode) -> Unit
) {
    try {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        onSuccess(barcode)
                    }
                }
                .addOnFailureListener {
                    com.hevincj.cashflow.utils.CrashLogger.w("BatchScanScreen", "Barcode analysis failed: ${it.message}", it)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    } catch (e: Exception) {
        com.hevincj.cashflow.utils.CrashLogger.e("BatchScanScreen", "Exception in processImageProxy", e)
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
