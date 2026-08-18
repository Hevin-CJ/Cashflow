package com.hevincj.cashflow.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.toFontFamily
import com.hevincj.cashflow.R
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.hevincj.cashflow.domain.models.TransactionType
import com.hevincj.cashflow.domain.models.TransactionCategory
import com.hevincj.cashflow.domain.models.RecurringFrequency
import com.hevincj.cashflow.ui.screen.viewmodel.AddTransactionViewModel
import com.hevincj.cashflow.ui.theme.*

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    navController: NavController,
    viewModel: AddTransactionViewModel = hiltViewModel()
) {
    val uiState by viewModel.state.collectAsState()

    var amountValue by remember {
        mutableStateOf(TextFieldValue(uiState.amount))
    }
    var descriptionValue by remember {
        mutableStateOf(TextFieldValue(uiState.description))
    }

    LaunchedEffect(uiState.amount) {
        if (uiState.amount != amountValue.text) {
            amountValue = amountValue.copy(
                text = uiState.amount,
                selection = TextRange(uiState.amount.length)
            )
        }
    }

    LaunchedEffect(uiState.description) {
        if (uiState.description != descriptionValue.text) {
            descriptionValue = descriptionValue.copy(
                text = uiState.description,
                selection = TextRange(uiState.description.length)
            )
        }
    }

    // Dynamic accent color depending on income/expense state
    val accentColor by animateColorAsState(
        targetValue = if (uiState.type == TransactionType.INCOME) IncomePurpleColor else ExpenseOrangeColor,
        animationSpec = tween(300)
    )

    // Automatically navigate back on successful transaction creation
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (uiState.isEditMode) "Update Transaction" else "Add Transaction",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CardBackground
                )
            )
        },
        containerColor = BackgroundGray // Sleek off-white background
    ) { innerPadding ->
        if (uiState.isLoading && uiState.isEditMode && uiState.amount.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = accentColor)
            }
        } else {
            Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Segmented toggle control for Income / Expense
            TransactionTypeToggle(
                selectedType = uiState.type,
                onTypeSelected = { viewModel.onTypeChange(it) }
            )

            // Large Formatted Amount Display Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "ENTER AMOUNT",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondaryColor,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Text(
                            text = "$",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        BasicTextField(
                            value = amountValue,
                            onValueChange = { newValue ->
                                val newText = newValue.text
                                if (newText.isEmpty()) {
                                    amountValue = newValue
                                    viewModel.onAmountChange(newText)
                                } else if (newText.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                    val doubleVal = newText.toDoubleOrNull()
                                    if (doubleVal == null || doubleVal <= 999999.0) {
                                        amountValue = newValue
                                        viewModel.onAmountChange(newText)
                                    }
                                }
                            },
                            modifier = Modifier.widthIn(min = 120.dp),
                            textStyle = TextStyle(
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = Font(R.font.plus_jakarta_sans_semibold).toFontFamily(),
                                color = accentColor
                            ),
                            cursorBrush = SolidColor(accentColor),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            decorationBox = { innerTextField ->
                                Box(
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (uiState.amount.isEmpty()) {
                                        Text(
                                            text = "0.00",
                                            fontSize = 42.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = Font(R.font.plus_jakarta_sans_semibold).toFontFamily(),
                                            color = Color.LightGray
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    }
                }
            }

            // Categories Section Title
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "SELECT CATEGORY",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondaryColor,
                    letterSpacing = 1.sp
                )
                
                // Horizontal category selector cards
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TransactionCategory.values().filter { it.supportedTypes.contains(uiState.type) }.forEach { catItem ->
                        val isSelected = uiState.category == catItem
                        val cardBorderColor = if (isSelected) accentColor else Color.Transparent
                        val cardBg = if (isSelected) (if (LocalDarkTheme.current) Color(0xFF2C2C2E) else Color.White) else (if (LocalDarkTheme.current) Color(0xFF1E1E1E) else Color(0xFFF3F4F6))

                        Column(
                            modifier = Modifier
                                .size(width = 92.dp, height = 90.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(cardBg)
                                .border(
                                    width = 2.dp,
                                    color = cardBorderColor,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { viewModel.onCategoryChange(catItem) }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(catItem.iconBgColor, shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = catItem.icon,
                                    contentDescription = catItem.displayName,
                                    tint = Color.DarkGray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = catItem.displayName,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) TextPrimaryColor else TextSecondaryColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Description Input Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "ADD NOTE (OPTIONAL)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondaryColor,
                        letterSpacing = 1.sp
                    )
                    OutlinedTextField(
                        value = descriptionValue,
                        onValueChange = { newValue ->
                            descriptionValue = newValue
                            viewModel.onDescriptionChange(newValue.text)
                        },
                        placeholder = {
                            Text(
                                text = "e.g. Weekly grocery or monthly income",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = Color.LightGray
                        )
                    )
                }
            }

            // Recurring Subscription Option
            val isDark = LocalDarkTheme.current
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF8121FD).copy(alpha = if (isDark) 0.25f else 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Autorenew,
                                    contentDescription = "Recurring Subscription",
                                    tint = if (isDark) Color(0xFFA78BFA) else Color(0xFF8121FD),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Recurring Subscription",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Auto-repeat this payment on schedule",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                        Switch(
                            checked = uiState.isRecurring,
                            onCheckedChange = { viewModel.onRecurringChange(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF8121FD),
                                uncheckedThumbColor = if (isDark) Color(0xFF9CA3AF) else Color.White,
                                uncheckedTrackColor = if (isDark) Color(0xFF3A3A3C) else Color(0xFFE5E5EA)
                            )
                        )
                    }

                    AnimatedVisibility(visible = uiState.isRecurring) {
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            Text(
                                text = "Repeat Frequency",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RecurringFrequency.values().forEach { freq ->
                                    val isSelected = uiState.recurringFrequency == freq
                                    val label = when (freq) {
                                        RecurringFrequency.DAILY -> "Daily"
                                        RecurringFrequency.WEEKLY -> "Weekly"
                                        RecurringFrequency.MONTHLY -> "Monthly"
                                        RecurringFrequency.YEARLY -> "Yearly"
                                    }
                                    val chipBg = if (isSelected) Color(0xFF8121FD) else (if (isDark) Color(0xFF2C2C2E) else Color(0xFFF3F4F6))
                                    val chipBorder = if (isSelected) Color.Transparent else (if (isDark) Color(0xFF3A3A3C) else Color.Transparent)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(chipBg)
                                            .border(1.dp, chipBorder, RoundedCornerShape(10.dp))
                                            .clickable { viewModel.onRecurringFrequencyChange(freq) }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (isSelected) Color.White else TextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Error Message Banner (Animated visibility)
            AnimatedVisibility(visible = uiState.errorMessage != null) {
                uiState.errorMessage?.let { error ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFDE8E8), shape = RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFF05252), shape = RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Error,
                            contentDescription = "Error",
                            tint = Color(0xFFC81E1E)
                        )
                        Text(
                            text = error,
                            color = Color(0xFF9B1C1C),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Save Transaction CTA Button
            Button(
                onClick = { viewModel.saveTransaction() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = PrimaryGradient,
                            shape = RoundedCornerShape(28.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Save",
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (uiState.isEditMode) "Update Transaction" else "Save Transaction",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun TransactionTypeToggle(
    selectedType: TransactionType,
    onTypeSelected: (TransactionType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .background(Color(0xFFEEEEEE), shape = CircleShape)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Income Tab
        val isIncome = selectedType == TransactionType.INCOME
        val incomeBg by animateColorAsState(
            targetValue = if (isIncome) IncomePurpleColor else Color.Transparent,
            animationSpec = tween(250)
        )
        val incomeTextCol by animateColorAsState(
            targetValue = if (isIncome) Color.White else TextSecondaryColor,
            animationSpec = tween(250)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(incomeBg, shape = CircleShape)
                .clickable { onTypeSelected(TransactionType.INCOME) },
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.TrendingUp,
                    contentDescription = "Income",
                    tint = incomeTextCol,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Income",
                    color = incomeTextCol,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }

        // Expense Tab
        val isExpense = selectedType == TransactionType.EXPENSE
        val expenseBg by animateColorAsState(
            targetValue = if (isExpense) ExpenseOrangeColor else Color.Transparent,
            animationSpec = tween(250)
        )
        val expenseTextCol by animateColorAsState(
            targetValue = if (isExpense) Color.White else TextSecondaryColor,
            animationSpec = tween(250)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(expenseBg, shape = CircleShape)
                .clickable { onTypeSelected(TransactionType.EXPENSE) },
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.TrendingDown,
                    contentDescription = "Expense",
                    tint = expenseTextCol,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Expense",
                    color = expenseTextCol,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}


