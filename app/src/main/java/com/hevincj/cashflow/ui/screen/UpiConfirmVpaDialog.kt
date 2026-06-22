package com.hevincj.cashflow.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hevincj.cashflow.ui.theme.CardBackground
import com.hevincj.cashflow.ui.theme.TextPrimary
import com.hevincj.cashflow.ui.theme.TextSecondary

private val VPA_REGEX_CONFIRM = Regex("^[a-zA-Z0-9._\\-+]+@[a-zA-Z]{3,}$")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpiConfirmVpaDialog(
    contactName: String,
    contactPhone: String,
    onDismissRequest: () -> Unit,
    onConfirm: (vpa: String, displayName: String) -> Unit
) {
    var vpaInput by remember { mutableStateOf("") }
    var hasAttemptedSubmit by remember { mutableStateOf(false) }

    val isVpaValid = remember(vpaInput) { VPA_REGEX_CONFIRM.matches(vpaInput.trim()) }
    val vpaError = if (hasAttemptedSubmit && !isVpaValid) {
        when {
            vpaInput.isBlank() -> "Please enter ${contactName.substringBefore(" ")}'s UPI ID"
            !vpaInput.contains("@") -> "UPI ID must contain @ (e.g. name@oksbi)"
            else -> "Invalid format — use name@bankcode"
        }
    } else null

    val keyboard = LocalSoftwareKeyboardController.current

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Confirm UPI ID",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    IconButton(onClick = onDismissRequest) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Contact avatar + info
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE1BEE7)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = contactName.take(1).uppercase(),
                        color = Color(0xFF4A148C),
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = contactName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = contactPhone,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Info card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFF3CD).copy(alpha = 0.4f),
                    tonalElevation = 0.dp
                ) {
                    Text(
                        text = "Phone numbers don't automatically map to UPI IDs. Please enter ${contactName.substringBefore(" ")}'s actual UPI ID.",
                        fontSize = 12.sp,
                        color = Color(0xFF7B5E00),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // VPA input
                OutlinedTextField(
                    value = vpaInput,
                    onValueChange = {
                        vpaInput = it.trim()
                        hasAttemptedSubmit = false
                    },
                    label = { Text("${contactName.substringBefore(" ")}'s UPI ID") },
                    placeholder = { Text("e.g. ${contactName.substringBefore(" ").lowercase()}@oksbi", color = Color.Gray.copy(alpha = 0.6f)) },
                    singleLine = true,
                    isError = vpaError != null,
                    supportingText = {
                        AnimatedVisibility(
                            visible = vpaError != null || (isVpaValid && vpaInput.isNotBlank()),
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            if (vpaError != null) {
                                Text(vpaError, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                            } else {
                                Text("✓ Valid UPI ID", color = Color(0xFF4CAF50), fontSize = 12.sp)
                            }
                        }
                    },
                    trailingIcon = {
                        if (isVpaValid) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50)
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboard?.hide()
                            hasAttemptedSubmit = true
                            if (isVpaValid) onConfirm(vpaInput.trim(), contactName)
                        }
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF9C27B0),
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                        errorBorderColor = MaterialTheme.colorScheme.error
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        keyboard?.hide()
                        hasAttemptedSubmit = true
                        if (isVpaValid) onConfirm(vpaInput.trim(), contactName)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0))
                ) {
                    Text(
                        text = "Pay ${contactName.substringBefore(" ")}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}
