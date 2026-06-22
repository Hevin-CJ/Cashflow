package com.hevincj.cashflow.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import android.content.Intent
import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hevincj.cashflow.utils.UpiPaymentData
import com.hevincj.cashflow.ui.screen.viewmodel.ScanViewModel
import com.hevincj.cashflow.ui.theme.CardBackground
import com.hevincj.cashflow.ui.theme.TextPrimary
import com.hevincj.cashflow.ui.theme.TextSecondary
import com.hevincj.cashflow.utils.UpiUriParser
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpiSendMoneyDialog(
    upiUri: String,
    payeeVpa: String = "",
    payeeName: String = "",
    onDismissRequest: () -> Unit,
    viewModel: ScanViewModel,
    onPaymentSuccess: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val paymentData = remember(upiUri, payeeVpa, payeeName) {
        if (upiUri.isNotBlank()) {
            UpiUriParser.parse(upiUri)
        } else {
            UpiPaymentData(payeeVpa, payeeName, null, null, isValid = true)
        }
    }

    var amountInput by remember {
        mutableStateOf(paymentData.amount?.let { String.format("%.2f", it) } ?: "")
    }
    var noteInput by remember {
        mutableStateOf(paymentData.note ?: "")
    }

    var isProcessing by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showSimulateButton by remember { mutableStateOf(false) }

    val isAmountEditable = paymentData.amount == null

    val isFormValid = remember(amountInput, paymentData.isValid) {
        val parsedAmt = amountInput.toDoubleOrNull()
        paymentData.isValid && parsedAmt != null && parsedAmt > 0.0
    }

    val upiPayLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val dataIntent = result.data
        val responseStr = dataIntent?.getStringExtra("response") ?: dataIntent?.data?.toString() ?: ""
        
        // Parse key-value parameters from standard UPI Intent query format
        val params = mutableMapOf<String, String>()
        if (responseStr.isNotBlank()) {
            val pairs = responseStr.split("&")
            for (pair in pairs) {
                val idx = pair.indexOf("=")
                if (idx != -1) {
                    val key = pair.substring(0, idx).trim().lowercase()
                    val value = pair.substring(idx + 1).trim()
                    params[key] = value
                }
            }
        }
        
        val status = params["status"]?.uppercase() ?: ""
        val responseCode = params["responsecode"] ?: ""
        val approvalRefNo = params["approvalrefno"] ?: params["txnref"]
        
        if (status == "SUCCESS" || responseCode == "00" || approvalRefNo != null) {
            val amount = amountInput.toDoubleOrNull() ?: 0.0
            viewModel.sendUpiPayment(
                vpa = paymentData.payeeVpa,
                name = paymentData.payeeName,
                amount = amount,
                note = noteInput.takeIf { it.isNotBlank() },
                rrn = approvalRefNo
            ) { success ->
                if (success) {
                    isSuccess = true
                    coroutineScope.launch {
                        delay(1800)
                        isProcessing = false
                        onPaymentSuccess()
                    }
                } else {
                    isProcessing = false
                    errorMessage = "Failed to save payment history"
                }
            }
        } else if (status == "SUBMITTED") {
            errorMessage = "Payment submitted, pending verification"
            isProcessing = false
        } else {
            errorMessage = "Payment failed or cancelled by user"
            isProcessing = false
        }
    }

    Dialog(
        onDismissRequest = { if (!isProcessing && !isSuccess) onDismissRequest() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isProcessing && !isSuccess,
            dismissOnClickOutside = !isProcessing && !isSuccess
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
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
                        Text(
                            text = "UPI Payment Transfer",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        if (!isProcessing && !isSuccess) {
                            IconButton(onClick = onDismissRequest) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Close",
                                    tint = Color.Gray
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (!paymentData.isValid) {
                        // Error State for Invalid URL
                        Icon(
                            imageVector = Icons.Rounded.Payments,
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = paymentData.errorMessage ?: "Invalid UPI QR Code",
                            color = Color.Red,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        OutlinedButton(
                            onClick = onDismissRequest,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Go Back", color = Color(0xFF9C27B0))
                        }
                    } else {
                        // Recipient Details Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Payee Name",
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = paymentData.payeeName,
                                    fontSize = 16.sp,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "UPI ID (VPA)",
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = paymentData.payeeVpa,
                                    fontSize = 14.sp,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Amount Input
                        OutlinedTextField(
                            value = amountInput,
                            onValueChange = { if (isAmountEditable) amountInput = it },
                            label = { Text("Amount (INR)") },
                            placeholder = { Text("e.g. 500") },
                            leadingIcon = { Text("₹", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.padding(start = 12.dp, end = 4.dp)) },
                            enabled = isAmountEditable && !isProcessing,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF9C27B0),
                                unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                                disabledBorderColor = Color.Gray.copy(alpha = 0.3f),
                                disabledLabelColor = TextSecondary,
                                disabledTextColor = TextPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Note Input
                        OutlinedTextField(
                            value = noteInput,
                            onValueChange = { if (it.length <= 80) noteInput = it },
                            label = { Text("Note / Description (Optional)") },
                            placeholder = { Text("e.g. Coffee or Dinner") },
                            enabled = !isProcessing,
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            supportingText = {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    Text(text = "${noteInput.length}/80", color = TextSecondary, fontSize = 11.sp)
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF9C27B0),
                                unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                            )
                        )

                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = errorMessage!!,
                                color = Color.Red,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }

                        if (showSimulateButton) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    isProcessing = true
                                    errorMessage = null
                                    showSimulateButton = false
                                    val amount = amountInput.toDoubleOrNull() ?: 0.0
                                    viewModel.sendUpiPayment(
                                        vpa = paymentData.payeeVpa,
                                        name = paymentData.payeeName,
                                        amount = amount,
                                        note = noteInput.takeIf { it.isNotBlank() },
                                        rrn = "DEMO" + System.currentTimeMillis().toString().takeLast(8)
                                    ) { success ->
                                        if (success) {
                                            isSuccess = true
                                            coroutineScope.launch {
                                                delay(1800)
                                                isProcessing = false
                                                onPaymentSuccess()
                                            }
                                        } else {
                                            isProcessing = false
                                            errorMessage = "Failed to save payment history"
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                            ) {
                                Text("Simulate Payment (Demo Mode)", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (!isProcessing) {
                                OutlinedButton(
                                    onClick = onDismissRequest,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF9C27B0)),
                                    border = BorderStroke(1.dp, Color(0xFF9C27B0))
                                ) {
                                    Text("Cancel", fontWeight = FontWeight.Bold)
                                }
                            }

                            Button(
                                onClick = {
                                    val amount = amountInput.toDoubleOrNull() ?: return@Button
                                    isProcessing = true
                                    errorMessage = null
                                    showSimulateButton = false

                                    try {
                                        val rawName = paymentData.payeeName
                                        val encodedName = java.net.URLEncoder.encode(rawName, "UTF-8")
                                        val noteParam = if (noteInput.isNotBlank()) "&tn=${java.net.URLEncoder.encode(noteInput, "UTF-8")}" else ""
                                        val upiUriToLaunch = "upi://pay?pa=${paymentData.payeeVpa}&pn=$encodedName&cu=INR&am=$amount$noteParam"

                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            data = Uri.parse(upiUriToLaunch)
                                        }
                                        val chooser = Intent.createChooser(intent, "Pay with UPI app")
                                        upiPayLauncher.launch(chooser)
                                    } catch (e: ActivityNotFoundException) {
                                        errorMessage = "No UPI apps installed. A real device with a UPI payment app is required."
                                        showSimulateButton = true
                                        isProcessing = false
                                    } catch (e: Exception) {
                                        errorMessage = "Failed to launch payment app: ${e.message}"
                                        isProcessing = false
                                    }
                                },
                                enabled = isFormValid && !isProcessing,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0))
                            ) {
                                if (isProcessing && !isSuccess) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                } else {
                                    Text("Pay ₹${amountInput.takeIf { it.isNotBlank() } ?: "0"}", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Success Overlay Screen
                androidx.compose.animation.AnimatedVisibility(
                    visible = isSuccess,
                    enter = fadeIn(animationSpec = tween(400)),
                    exit = fadeOut(animationSpec = tween(400))
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(CardBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(Brush.radialGradient(listOf(Color(0xFFE8F5E9), Color(0xFFC8E6C9)))),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Success",
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = "Payment Successful!",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "₹$amountInput paid to ${paymentData.payeeName}",
                                fontSize = 14.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}
