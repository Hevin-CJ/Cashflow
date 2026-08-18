package com.hevincj.cashflow.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.hevincj.cashflow.domain.models.RecurringExpense
import com.hevincj.cashflow.domain.models.Transaction
import com.hevincj.cashflow.domain.models.TransactionCategory
import com.hevincj.cashflow.domain.models.TransactionType
import com.hevincj.cashflow.domain.models.RecurringFrequency
import com.hevincj.cashflow.ui.screen.viewmodel.SubscriptionManagerViewModel
import com.hevincj.cashflow.ui.theme.*
import com.hevincj.cashflow.utils.DateTimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionManagerScreen(
    navController: NavController,
    viewModel: SubscriptionManagerViewModel = hiltViewModel()
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = BackgroundGray,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Subscriptions",
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundGray)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                shape = CircleShape,
                containerColor = Color.Transparent,
                elevation = FloatingActionButtonDefaults.elevation(8.dp, 4.dp),
                modifier = Modifier.size(56.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(PrimaryGradient, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Subscription",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF635BFF))
                }
            } else if (uiState.subscriptions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(RoundedCornerShape(28.dp))
                                .background(if (LocalDarkTheme.current) Color(0xFF33250F) else Color(0xFFFFF7E6)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Autorenew,
                                contentDescription = "No Subscriptions",
                                tint = Color(0xFFFF9F1C),
                                modifier = Modifier.size(44.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Track your Repeating Charges",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Add monthly Netflix, weekly transit cards, or yearly insurance. We'll automatically log them on the due date.",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(uiState.subscriptions, key = { it.localId }) { subscription ->
                        SubscriptionCard(
                            subscription = subscription,
                            onDelete = { viewModel.deleteSubscription(subscription) }
                        )
                    }
                }
            }

            uiState.error?.let { err ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    containerColor = Color(0xFFD32F2F),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(err)
                }
            }
        }
    }

    if (showAddDialog) {
        AddSubscriptionDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { sub ->
                viewModel.addSubscription(sub)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun SubscriptionCard(
    subscription: RecurringExpense,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (subscription.isSynced) Color.Transparent else Color(0xFFFFB300).copy(alpha = 0.4f),
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(subscription.transaction.category.iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = subscription.transaction.category.icon,
                    contentDescription = subscription.transaction.category.displayName,
                    tint = Color(0xFF212121),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subscription.transaction.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                val dateStr = DateTimeUtils.formatDueDate(subscription.nextDueDate)
                Text(
                    text = "Next payment: $dateStr",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                val formattedAmount = String.format("$%.2f", subscription.transaction.amount)
                val freqLabel = when (subscription.frequency) {
                    RecurringFrequency.DAILY -> "day"
                    RecurringFrequency.WEEKLY -> "wk"
                    RecurringFrequency.MONTHLY -> "mo"
                    RecurringFrequency.YEARLY -> "yr"
                }
                Text(
                    text = "$formattedAmount / $freqLabel",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF635BFF)
                )

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Cancel Subscription",
                        tint = Color(0xFFD32F2F).copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AddSubscriptionDialog(
    onDismiss: () -> Unit,
    onConfirm: (RecurringExpense) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountString by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(TransactionCategory.ENTERTAINMENT) }
    var selectedFrequency by remember { mutableStateOf(RecurringFrequency.MONTHLY) }

    val categories = remember {
        TransactionCategory.values().filter { it.supportedTypes.contains(TransactionType.EXPENSE) }
    }
    val frequencies = remember { RecurringFrequency.values() }

    Dialog(
        onDismissRequest = onDismiss, // FIX: Hook dismiss listener to handle back gestures correctly
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(vertical = 24.dp)
                .border(
                    width = 1.dp,
                    color = if (LocalDarkTheme.current) Color(0xFF2C2C2E) else Color.Transparent,
                    shape = RoundedCornerShape(24.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "New Subscription",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    placeholder = { Text("e.g. Netflix, Spotify") },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF635BFF),
                        unfocusedBorderColor = if (LocalDarkTheme.current) Color(0xFF3A3A3C) else Color(0xFFE5E5EA),
                        focusedLabelColor = Color(0xFF635BFF),
                        unfocusedLabelColor = TextSecondary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = amountString,
                    onValueChange = { amountString = it },
                    label = { Text("Amount ($)") },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF635BFF),
                        unfocusedBorderColor = if (LocalDarkTheme.current) Color(0xFF3A3A3C) else Color(0xFFE5E5EA),
                        focusedLabelColor = Color(0xFF635BFF),
                        unfocusedLabelColor = TextSecondary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Category",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { cat ->
                        val isSelected = cat == selectedCategory
                        val isDark = LocalDarkTheme.current
                        val chipBg = if (isSelected) cat.iconBgColor else (if (isDark) Color(0xFF2C2C2E) else Color(0xFFF2F2F7))
                        val chipText = if (isSelected) Color(0xFF212121) else TextPrimary
                        val chipBorder = if (isSelected) Color.Transparent else (if (isDark) Color(0xFF3A3A3C) else Color(0xFFE5E5EA))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(chipBg)
                                .border(1.dp, chipBorder, RoundedCornerShape(20.dp))
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = cat.icon,
                                    contentDescription = cat.displayName,
                                    tint = if (isSelected) Color(0xFF212121) else Color(0xFF635BFF),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = cat.displayName,
                                    color = chipText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Frequency",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    frequencies.forEach { freq ->
                        val isSelected = freq == selectedFrequency
                        val isDark = LocalDarkTheme.current
                        val chipBg = if (isSelected) Color(0xFF635BFF) else (if (isDark) Color(0xFF2C2C2E) else Color(0xFFF2F2F7))
                        val chipText = if (isSelected) Color.White else TextPrimary

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(chipBg)
                                .clickable { selectedFrequency = freq }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = freq.name.substring(0, 1) + freq.name.substring(1).lowercase(),
                                color = chipText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = TextSecondary, fontWeight = FontWeight.SemiBold)
                    }

                    val amount = amountString.toDoubleOrNull() ?: 0.0
                    val isFormValid = title.isNotBlank() && amount > 0.0
                    val isDark = LocalDarkTheme.current

                    Button(
                        onClick = {
                            val now = System.currentTimeMillis()
                            val nextDue = when (selectedFrequency) {
                                RecurringFrequency.DAILY -> now + 86400000L
                                RecurringFrequency.WEEKLY -> now + 7 * 86400000L
                                RecurringFrequency.MONTHLY -> now + 30 * 86400000L
                                RecurringFrequency.YEARLY -> now + 365 * 86400000L
                            }
                            val cleanTitle = title.trim()
                            val newSub = RecurringExpense(
                                id = "",
                                localId = 0,
                                frequency = selectedFrequency,
                                startDate = now,
                                lastProcessedDate = now,
                                nextDueDate = nextDue,
                                isSynced = false,
                                transaction = Transaction(
                                    id = "",
                                    title = cleanTitle,
                                    timestamp = now,
                                    amount = -kotlin.math.abs(amount),
                                    icon = selectedCategory.icon,
                                    iconBgColor = selectedCategory.iconBgColor,
                                    type = TransactionType.EXPENSE,
                                    category = selectedCategory,
                                    description = "$cleanTitle subscription",
                                    isSynced = false
                                )
                            )
                            onConfirm(newSub)
                        },
                        enabled = isFormValid,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            disabledContainerColor = if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA)
                        ),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        val brush = if (isFormValid) PrimaryGradient else Brush.linearGradient(
                            listOf(
                                if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA),
                                if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA)
                            )
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(brush),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Add",
                                color = if (isFormValid) Color.White else (if (isDark) Color.DarkGray else Color.Gray),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}