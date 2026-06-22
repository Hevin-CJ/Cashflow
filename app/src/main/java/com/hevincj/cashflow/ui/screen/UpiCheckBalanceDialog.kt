package com.hevincj.cashflow.ui.screen

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.hevincj.cashflow.domain.models.CreditCard
import com.hevincj.cashflow.ui.screen.viewmodel.CardsViewModel
import com.hevincj.cashflow.ui.theme.CardBackground
import com.hevincj.cashflow.ui.theme.TextPrimary
import com.hevincj.cashflow.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpiCheckBalanceDialog(
    onDismissRequest: () -> Unit,
    cardsViewModel: CardsViewModel = hiltViewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val cardsState by cardsViewModel.state.collectAsState()

    val presetAccounts = remember {
        listOf(
            CreditCard("", 48250.00, "•••• 1234", "SBI Savings", listOf(0xFF1976D2, 0xFF0D47A1)),
            CreditCard("", 124800.50, "•••• 5678", "HDFC Savings", listOf(0xFF2C3E50, 0xFF34495E)),
            CreditCard("", 15200.75, "•••• 9012", "ICICI Salary", listOf(0xFFD84315, 0xFFBF360C))
        )
    }

    val accounts = remember(cardsState.cards) {
        if (cardsState.cards.isNotEmpty()) cardsState.cards else presetAccounts
    }

    var selectedAccount by remember { mutableStateOf(accounts.first()) }
    var currentScreen by remember { mutableStateOf(0) } // 0 = Select Account, 1 = Enter PIN, 2 = Loading, 3 = Show Balance

    var pinDigits by remember { mutableStateOf("") }
    val maxPinLength = 4

    Dialog(
        onDismissRequest = { if (currentScreen != 2) onDismissRequest() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = currentScreen != 2,
            dismissOnClickOutside = currentScreen != 2
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header
                if (currentScreen != 2 && currentScreen != 3) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (currentScreen == 0) "Check Bank Balance" else "Enter UPI PIN",
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
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = if (currentScreen == 2 || currentScreen == 3) 0.dp else 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (currentScreen) {
                        0 -> {
                            // Screen 0: Select Account
                            Text(
                                text = "Choose a bank account or card to check balance lively.",
                                fontSize = 13.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )

                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                items(accounts) { account ->
                                    val isSelected = selectedAccount == account
                                    val brush = Brush.linearGradient(
                                        colors = account.gradientColors.map { Color(it) }
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(width = 160.dp, height = 96.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(brush)
                                            .border(
                                                width = if (isSelected) 3.dp else 0.dp,
                                                color = if (isSelected) Color.White else Color.Transparent,
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                            .clickable { selectedAccount = account }
                                            .padding(12.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxSize(),
                                            verticalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = account.cardHolder,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = account.cardNumber,
                                                color = Color.White.copy(alpha = 0.8f),
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = { currentScreen = 1 },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0))
                            ) {
                                Text("Proceed to UPI PIN", fontWeight = FontWeight.Bold)
                            }
                        }

                        1 -> {
                            // Screen 1: Enter PIN
                            Text(
                                text = "Querying ${selectedAccount.cardHolder} (${selectedAccount.cardNumber.takeLast(4)})",
                                fontSize = 13.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // PIN dots display
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                repeat(maxPinLength) { idx ->
                                    val isFilled = idx < pinDigits.length
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isFilled) Color(0xFF9C27B0)
                                                else Color.Gray.copy(alpha = 0.3f)
                                            )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            // Custom secure PIN number pad
                            UpiPinPad(
                                onDigitClick = { digit ->
                                    if (pinDigits.length < maxPinLength) {
                                        pinDigits += digit
                                    }
                                },
                                onDeleteClick = {
                                    if (pinDigits.isNotEmpty()) {
                                        pinDigits = pinDigits.dropLast(1)
                                    }
                                },
                                onSubmitClick = {
                                    if (pinDigits.length == maxPinLength) {
                                        // Launch USSD balance enquiry — ACTION_DIAL requires no permission
                                        // *99*1*2# is the NPCI USSD code for balance enquiry
                                        val ussdCode = "*99*1*2#"
                                        val dialIntent = Intent(
                                            Intent.ACTION_DIAL,
                                            Uri.parse("tel:${Uri.encode(ussdCode)}")
                                        )
                                        try {
                                            context.startActivity(dialIntent)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                        onDismissRequest()
                                    }
                                },
                                isSubmitEnabled = pinDigits.length == maxPinLength
                            )
                        }

                        2 -> {
                            // Screen 2: Loading State
                            Box(
                                modifier = Modifier
                                    .height(250.dp)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = Color(0xFF9C27B0),
                                        modifier = Modifier.size(54.dp)
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Text(
                                        text = "Connecting securely to bank...",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Validating credentials and retrieving balance",
                                        fontSize = 12.sp,
                                        color = TextSecondary,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }

                        3 -> {
                            // Screen 3: Show balance lively!
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFE8F5E9)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.AccountBalance,
                                            contentDescription = null,
                                            tint = Color(0xFF2E7D32),
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(
                                        text = selectedAccount.cardHolder,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )

                                    Text(
                                        text = "Account: ${selectedAccount.cardNumber}",
                                        fontSize = 13.sp,
                                        color = TextSecondary,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )

                                    Spacer(modifier = Modifier.height(24.dp))

                                    // Glowing lively balance display card
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        Color(0xFFE8F5E9).copy(alpha = 0.8f),
                                                        Color(0xFFC8E6C9).copy(alpha = 0.5f)
                                                    )
                                                )
                                            )
                                            .border(1.dp, Color(0xFFA5D6A7), RoundedCornerShape(16.dp))
                                            .padding(20.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "Available Balance",
                                                fontSize = 11.sp,
                                                color = Color(0xFF2E7D32),
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = String.format("₹%,.2f", selectedAccount.balance),
                                                fontSize = 28.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF1B5E20)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(28.dp))

                                    Button(
                                        onClick = onDismissRequest,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0))
                                    ) {
                                        Text("Done", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UpiPinPad(
    onDigitClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    onSubmitClick: () -> Unit,
    isSubmitEnabled: Boolean
) {
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("delete", "0", "submit")
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                when (key) {
                                    "submit" -> if (isSubmitEnabled) Color(0xFF4CAF50) else Color.Gray.copy(alpha = 0.1f)
                                    "delete" -> Color.Gray.copy(alpha = 0.1f)
                                    else -> Color.White.copy(alpha = 0.05f)
                                }
                            )
                            .clickable(
                                enabled = when (key) {
                                    "submit" -> isSubmitEnabled
                                    else -> true
                                }
                            ) {
                                when (key) {
                                    "submit" -> onSubmitClick()
                                    "delete" -> onDeleteClick()
                                    else -> onDigitClick(key)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        when (key) {
                            "submit" -> Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Submit",
                                tint = if (isSubmitEnabled) Color.White else Color.Gray
                            )
                            "delete" -> Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Backspace,
                                contentDescription = "Delete",
                                tint = TextPrimary
                            )
                            else -> Text(
                                text = key,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}
