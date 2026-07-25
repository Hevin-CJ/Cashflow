package com.hevincj.cashflow.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.hevincj.cashflow.ui.theme.*
import com.hevincj.cashflow.ui.screen.viewmodel.ExchangeRateViewModel
import java.text.DecimalFormat

private val currencyNames = mapOf(
    "INR" to "Indian Rupee",
    "USD" to "US Dollar",
    "EUR" to "Euro",
    "GBP" to "British Pound",
    "JPY" to "Japanese Yen",
    "AUD" to "Australian Dollar",
    "CAD" to "Canadian Dollar"
)

private fun getFallbackRate(from: String, to: String): Double {
    if (from == to) return 1.0
    val ratesFromInr = mapOf(
        "INR" to 1.0,
        "USD" to 0.012,
        "EUR" to 0.011,
        "GBP" to 0.0094,
        "JPY" to 1.86,
        "AUD" to 0.018,
        "CAD" to 0.016
    )
    val fromRate = ratesFromInr[from] ?: 1.0
    val toRate = ratesFromInr[to] ?: 1.0
    return toRate / fromRate
}

@Composable
fun BlinkingCursor() {
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorAlpha"
    )
    Box(
        modifier = Modifier
            .padding(start = 2.dp)
            .width(2.5.dp)
            .height(28.dp)
            .graphicsLayer(alpha = alpha)
            .background(AccentOrange)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExchangeRateScreen(
    innerPadding: PaddingValues,
    navController: NavController = rememberNavController(),
    viewModel: ExchangeRateViewModel = hiltViewModel()
) {
    val uiState by viewModel.state.collectAsState()

    val currentRate = if (uiState.currencyTop == uiState.currencyBottom) {
        1.0
    } else {
        uiState.exchangeRates[uiState.currencyBottom] ?: getFallbackRate(uiState.currencyTop, uiState.currencyBottom)
    }

    val displayTop: String
    val displayBottom: String

    val displayFormatter = DecimalFormat("#,##,##0.0000")

    if (uiState.isTopActive) {
        displayTop = uiState.topInputValue
        val parsed = uiState.topInputValue.toDoubleOrNull() ?: 0.0
        val converted = parsed * currentRate
        displayBottom = if (uiState.topInputValue == "0") "0" else displayFormatter.format(converted)
    } else {
        displayBottom = uiState.bottomInputValue
        val parsed = uiState.bottomInputValue.toDoubleOrNull() ?: 0.0
        val converted = if (currentRate != 0.0) parsed / currentRate else 0.0
        displayTop = if (uiState.bottomInputValue == "0") "0" else displayFormatter.format(converted)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .background(BackgroundGray)
            .padding(vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }

            Text(
                text = "Exchange Currency",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.size(48.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column {
            CurrencyDisplayBlock(
                currencyName = currencyNames[uiState.currencyTop] ?: "Unknown",
                currencyCode = uiState.currencyTop,
                value = displayTop,
                isEditing = uiState.isTopActive,
                onCardClick = { viewModel.setTopActive(true) },
                onCurrencySelect = { selected ->
                    viewModel.setCurrencyTop(selected)
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            CurrencyDisplayBlock(
                currencyName = currencyNames[uiState.currencyBottom] ?: "Unknown",
                currencyCode = uiState.currencyBottom,
                value = displayBottom,
                isEditing = !uiState.isTopActive,
                onCardClick = { viewModel.setTopActive(false) },
                onCurrencySelect = { selected ->
                    viewModel.setCurrencyBottom(selected)
                }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = uiState.lastUpdatedDate,
                color = TextGrayDark,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
            if (uiState.isLoading) {
                Spacer(modifier = Modifier.width(8.dp))
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 1.5.dp,
                    color = IncomePurpleColor
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        CustomNumpad(
            onDigitClick = { digit ->
                if (uiState.isTopActive) {
                    val currentVal = uiState.topInputValue
                    val newVal = if (currentVal == "0") digit else currentVal + digit
                    viewModel.updateTopInput(newVal)
                } else {
                    val currentVal = uiState.bottomInputValue
                    val newVal = if (currentVal == "0") digit else currentVal + digit
                    viewModel.updateBottomInput(newVal)
                }
            },
            onDeleteClick = {
                if (uiState.isTopActive) {
                    val currentVal = uiState.topInputValue
                    if (currentVal.isNotEmpty() && currentVal != "0") {
                        val newVal = currentVal.dropLast(1)
                        viewModel.updateTopInput(if (newVal.isEmpty()) "0" else newVal)
                    }
                } else {
                    val currentVal = uiState.bottomInputValue
                    if (currentVal.isNotEmpty() && currentVal != "0") {
                        val newVal = currentVal.dropLast(1)
                        viewModel.updateBottomInput(if (newVal.isEmpty()) "0" else newVal)
                    }
                }
            },
            onClearClick = {
                if (uiState.isTopActive) {
                    viewModel.updateTopInput("0")
                } else {
                    viewModel.updateBottomInput("0")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun CurrencyDisplayBlock(
    currencyName: String,
    currencyCode: String,
    value: String,
    isEditing: Boolean,
    onCardClick: () -> Unit,
    onCurrencySelect: (String) -> Unit
) {
    var dropdownExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
            .padding(horizontal = 24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { dropdownExpanded = true }
                    .padding(vertical = 4.dp, horizontal = 8.dp)
                    .background(Color.White.copy(alpha = 0.05f)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currencyName,
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = currencyCode,
                    color = TextGraySubtle,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(4.dp))
                SurfaceIcon(icon = Icons.Rounded.ChevronRight)
            }

            DropdownMenu(
                expanded = dropdownExpanded,
                onDismissRequest = { dropdownExpanded = false },
                modifier = Modifier
                    .background(CardBackground, shape = RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), shape = RoundedCornerShape(16.dp))
            ) {
                val targets = listOf("INR", "USD", "EUR", "GBP", "JPY", "AUD", "CAD")
                targets.forEach { code ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "${currencyNames[code]} ($code)",
                                color = TextPrimary,
                                fontSize = 14.sp
                            )
                        },
                        onClick = {
                            onCurrencySelect(code)
                            dropdownExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                color = TextPrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1
            )
            if (isEditing) {
                BlinkingCursor()
            }
        }
    }
}

@Composable
private fun CustomNumpad(
    onDigitClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    onClearClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val keys = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("AC", "0", "delete")
        )

        keys.forEach { row ->
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(GridIconContainerColor)
                            .clickable {
                                when (key) {
                                    "AC" -> onClearClick()
                                    "delete" -> onDeleteClick()
                                    else -> onDigitClick(key)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (key == "delete") {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Backspace,
                                contentDescription = "Delete",
                                tint = AccentOrange,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text(
                                text = key,
                                color = if (key == "AC") AccentOrange else TextPrimary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                style = TextStyle(
                                    platformStyle = PlatformTextStyle(
                                        includeFontPadding = false
                                    ),
                                    lineHeightStyle = LineHeightStyle(
                                        alignment = LineHeightStyle.Alignment.Center,
                                        trim = LineHeightStyle.Trim.Both
                                    )
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SurfaceIcon(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(PrimaryGradient),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PhoneBackgroundColor,
            modifier = Modifier.size(14.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ExchangeRateScreenPreview() {
    MaterialTheme {
        ExchangeRateScreen(PaddingValues())
    }
}