package com.hevincj.cashflow.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.hilt.navigation.compose.hiltViewModel
import com.hevincj.cashflow.ui.screen.viewmodel.CardsViewModel
import java.text.NumberFormat
import java.util.Locale
import com.hevincj.cashflow.ui.theme.*

enum class CardBrand(val displayName: String, val patternDescription: String) {
    VISA("Visa", "Starts with 4"),
    MASTERCARD("Mastercard", "Starts with 51-55 or 2221-2720"),
    AMERICAN_EXPRESS("AmEx", "Starts with 34 or 37"),
    DISCOVER("Discover", "Starts with 6011 or 65"),
    JCB("JCB", "Starts with 3528-3589"),
    RUPAY("RuPay", "Starts with 508, 60, 65, 81 or 82"),
    UNKNOWN("Card", "Unknown Network")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCardScreen(
    navController: NavController,
    viewModel: CardsViewModel = hiltViewModel()
) {
    var cardHolder by remember { mutableStateOf("") }
    var cardNumber by remember { mutableStateOf("") }
    var balanceString by remember { mutableStateOf("") }

    val gradientPresets = listOf(
        listOf(0xFF67E2AEL, 0xFFE8679AL, 0xFFF19E79L), // Emerald Dream
        listOf(0xFF6C73D1L, 0xFFB96BB2L, 0xFFE28A6DL), // Purple Sunset
        listOf(0xFF2193B0L, 0xFF6DD5EDL),             // Ocean Breeze
        listOf(0xFFf12711L, 0xFFf5af19L),             // Fire Sunset
        listOf(0xFF1F1C2CL, 0xFF928DABL)              // Cosmic Dusk
    )
    var selectedGradientIndex by remember { mutableStateOf(0) }

    // Brand and validation computations
    val cleanCardNumber = cardNumber.filter { it.isDigit() }
    val detectedBrand = detectCardBrand(cleanCardNumber)
    val isLuhnValid = validateCardLuhn(cleanCardNumber)
    val showLuhnError = cleanCardNumber.length >= 15 && !isLuhnValid

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = BackgroundGray,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Add Card",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundGray
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Live Card Preview
            LiveCardPreview(
                cardHolder = if (cardHolder.isBlank()) "CARDHOLDER NAME" else cardHolder.uppercase(),
                cardNumber = if (cardNumber.isBlank()) "•••• •••• •••• ••••" else cardNumber,
                balance = balanceString.toDoubleOrNull() ?: 0.0,
                brand = detectedBrand,
                gradientColors = gradientPresets[selectedGradientIndex]
            )

            // 2. Input Fields Container
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // Card Holder Input
                    OutlinedTextField(
                        value = cardHolder,
                        onValueChange = { cardHolder = it },
                        label = { Text("Card Holder Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Card Number Input with Brand & Checksum Indicators
                    OutlinedTextField(
                        value = cardNumber,
                        onValueChange = { value ->
                            val clean = value.filter { it.isDigit() }
                            val formatted = buildString {
                                for (i in clean.indices) {
                                    append(clean[i])
                                    if ((i + 1) % 4 == 0 && i < 15) {
                                        append(" ")
                                    }
                                }
                            }
                            if (clean.length <= 16) {
                                cardNumber = formatted
                            }
                        },
                        label = { Text("Card Number") },
                        placeholder = { Text("4111 2222 3333 4444") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            if (cleanCardNumber.isNotEmpty()) {
                                if (isLuhnValid) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Valid Card Checksum",
                                        tint = Color(0xFF4CAF50)
                                    )
                                } else if (showLuhnError) {
                                    Icon(
                                        imageVector = Icons.Default.Error,
                                        contentDescription = "Invalid Card Checksum",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        },
                        isError = showLuhnError,
                        supportingText = {
                            if (showLuhnError) {
                                Text(
                                    text = "Invalid card number (checksum failed)",
                                    color = MaterialTheme.colorScheme.error
                                )
                            } else if (cleanCardNumber.isNotEmpty()) {
                                Text(
                                    text = "Detected Network: ${detectedBrand.displayName}",
                                    color = Color.Gray
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Initial Balance Input
                    OutlinedTextField(
                        value = balanceString,
                        onValueChange = { balanceString = it },
                        label = { Text("Initial Balance") },
                        placeholder = { Text("0.00") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Theme Presets Selector
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Select Card Theme",
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            gradientPresets.forEachIndexed { index, colors ->
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(
                                            brush = Brush.linearGradient(
                                                colors = colors.map { Color(it) }
                                            )
                                        )
                                        .clickable { selectedGradientIndex = index }
                                        .run {
                                            if (selectedGradientIndex == index) {
                                                background(Color.Black.copy(alpha = 0.15f), shape = CircleShape)
                                                clip(CircleShape)
                                            } else this
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selectedGradientIndex == index) {
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .background(Color.White)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Save Button
            Button(
                onClick = {
                    val bal = balanceString.toDoubleOrNull() ?: 0.0
                    viewModel.addCard(cardHolder, cardNumber, bal, gradientPresets[selectedGradientIndex])
                    navController.popBackStack()
                },
                enabled = cardHolder.isNotBlank() && cleanCardNumber.length >= 15 && isLuhnValid,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6C73D1),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "Add Card",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun LiveCardPreview(
    cardHolder: String,
    cardNumber: String,
    balance: Double,
    brand: CardBrand,
    gradientColors: List<Long>
) {
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)
    val formattedBalance = currencyFormatter.format(balance)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = gradientColors.map { Color(it) },
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                )
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = brand.displayName.uppercase(),
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontStyle = FontStyle.Italic
                    )

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Current Balance",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formattedBalance,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = cardNumber,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = cardHolder,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// Luhn Algorithm validation
fun validateCardLuhn(number: String): Boolean {
    if (number.isEmpty()) return false
    var sum = 0
    var alternate = false
    for (i in number.length - 1 downTo 0) {
        var n = number[i] - '0'
        if (n < 0 || n > 9) return false // Not a digit
        if (alternate) {
            n *= 2
            if (n > 9) {
                n = (n % 10) + 1
            }
        }
        sum += n
        alternate = !alternate
    }
    return sum % 10 == 0
}

// Card brand prefix BIN/IIN identification
fun detectCardBrand(number: String): CardBrand {
    if (number.isEmpty()) return CardBrand.UNKNOWN
    return when {
        number.startsWith("4") -> CardBrand.VISA
        number.startsWith("51") || number.startsWith("52") || number.startsWith("53") || number.startsWith("54") || number.startsWith("55") ||
        (number.length >= 4 && number.substring(0, 4).toIntOrNull() in 2221..2720) -> CardBrand.MASTERCARD
        number.startsWith("34") || number.startsWith("37") -> CardBrand.AMERICAN_EXPRESS
        number.startsWith("6011") || number.startsWith("65") || (number.length >= 3 && number.substring(0, 3).toIntOrNull() in 644..649) -> CardBrand.DISCOVER
        (number.length >= 4 && number.substring(0, 4).toIntOrNull() in 3528..3589) -> CardBrand.JCB
        number.startsWith("508") || number.startsWith("60") || number.startsWith("65") || number.startsWith("81") || number.startsWith("82") -> CardBrand.RUPAY
        else -> CardBrand.UNKNOWN
    }
}
