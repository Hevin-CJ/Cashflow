package com.hevincj.cashflow.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Payment
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hevincj.cashflow.domain.models.Transaction
import com.hevincj.cashflow.ui.theme.NegativeRed
import com.hevincj.cashflow.ui.theme.PositiveGreen
import com.hevincj.cashflow.ui.theme.PrimaryGradient
import com.hevincj.cashflow.ui.theme.TextPrimary
import com.hevincj.cashflow.ui.theme.TextSecondary
import com.hevincj.cashflow.ui.theme.CardBackground
import com.hevincj.cashflow.ui.theme.BackgroundGray
import com.hevincj.cashflow.ui.theme.LocalDarkTheme
import kotlin.math.abs
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.hilt.navigation.compose.hiltViewModel
import com.hevincj.cashflow.ui.screen.state.BalanceRange
import com.hevincj.cashflow.ui.screen.state.HomeUiState
import com.hevincj.cashflow.ui.screen.viewmodel.HomeViewModel
import com.hevincj.cashflow.domain.models.TransactionType
import java.text.DecimalFormat
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.hevincj.cashflow.ui.screen.viewmodel.ScanViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.runtime.rememberCoroutineScope
import com.hevincj.cashflow.domain.models.TransactionCategory

@Composable
fun HomeScreen(
    innerPadding: PaddingValues,
    rootNavController: NavController = rememberNavController(),
    viewModel: HomeViewModel = hiltViewModel(),
    scanViewModel: ScanViewModel = hiltViewModel()
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var previousSize by rememberSaveable { mutableStateOf(-1) }

    // FIX: Only auto-scroll to top if the user is explicitly resting at the top index.
    // Prevents gesture deadlocks if database emissions happen during launch touch gestures.
    LaunchedEffect(uiState.transactions.size) {
        val currentSize = uiState.transactions.size
        if (currentSize > 0) {
            val isAtTop = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
            if (previousSize != -1 && currentSize > previousSize && isAtTop) {
                listState.animateScrollToItem(0)
            }
            previousSize = currentSize
        }
    }

    LaunchedEffect(Unit) {
        delay(200)
        viewModel.refreshSync(force = true, limit = 25)
    }

    LaunchedEffect(uiState.isSessionExpired) {
        if (uiState.isSessionExpired) {
            rootNavController.navigate("login") {
                popUpTo("main") { inclusive = true }
            }
        }
    }

    var showUpiQrDialog by remember { mutableStateOf(false) }

    if (showUpiQrDialog) {
        UpiPaymentQrDialog(
            onDismissRequest = { showUpiQrDialog = false },
            viewModel = scanViewModel
        )
    }

    val onRangeSelected = remember(viewModel) { { range: BalanceRange -> viewModel.onBalanceRangeChange(range) } }
    val onDeleteTransaction = remember(viewModel) { { transaction: Transaction -> viewModel.deleteTransaction(transaction) } }
    val onRetrySync = remember(viewModel) { { viewModel.refreshSync() } }
    val onNavigateToAddTransaction = remember(rootNavController) { { transactionId: String? -> rootNavController.navigate("add_transaction?transactionId=$transactionId") } }
    val onNavigateToAllTransactions = remember(rootNavController) { { rootNavController.navigate("all_transactions") } }

    HomeScreenContent(
        innerPadding = innerPadding,
        uiState = uiState,
        onRangeSelected = onRangeSelected,
        onDeleteTransaction = onDeleteTransaction,
        onRetrySync = onRetrySync,
        onNavigateToAddTransaction = onNavigateToAddTransaction,
        onNavigateToAllTransactions = onNavigateToAllTransactions,
        rootNavController = rootNavController,
        listState = listState
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    innerPadding: PaddingValues,
    uiState: HomeUiState,
    onRangeSelected: (BalanceRange) -> Unit,
    onDeleteTransaction: (Transaction) -> Unit,
    onRetrySync: () -> Unit,
    onNavigateToAddTransaction: (String?) -> Unit,
    onNavigateToAllTransactions: () -> Unit,
    rootNavController: NavController,
    listState: androidx.compose.foundation.lazy.LazyListState = rememberLazyListState()
) {
    val shimmerTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerTranslateAnim = shimmerTransition.animateFloat(
        initialValue = -300f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )
    val shimmerTranslateProvider = remember(shimmerTranslateAnim) { { shimmerTranslateAnim.value } }

    var showBatchItemsTransaction by remember { mutableStateOf<Transaction?>(null) }
    var isSyncErrorDismissed by remember { mutableStateOf(false) }
    var isBudgetWarningDismissed by remember { mutableStateOf(false) }

    val itemTextPrimary = TextPrimary
    val itemTextSecondary = TextSecondary
    val itemCardBackground = CardBackground
    val itemBackgroundGray = BackgroundGray
    val itemPositiveGreen = PositiveGreen
    val itemNegativeRed = NegativeRed

    LaunchedEffect(uiState.error) {
        isSyncErrorDismissed = false
    }
    LaunchedEffect(uiState.exceededBudgets) {
        isBudgetWarningDismissed = false
    }

    if (showBatchItemsTransaction != null) {
        val items = parseBatchDescription(showBatchItemsTransaction?.description)
        Dialog(onDismissRequest = { showBatchItemsTransaction = null }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
                    Text(text = "Batch Scanned Items", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(16.dp))
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.heightIn(max = 240.dp)
                    ) {
                        items(items) { (name, code) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (LocalDarkTheme.current) Color(0xFF2C2C2E) else Color(0xFFF3F4F6))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(text = "Barcode: $code", fontSize = 12.sp, color = TextSecondary)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { showBatchItemsTransaction = null },
                        modifier = Modifier.align(Alignment.End),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF635BFF))
                    ) {
                        Text("Close", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        Box(modifier = Modifier.padding(top = 24.dp, start = 24.dp, end = 24.dp, bottom = 12.dp)) {
            TopNavigationBar(onScanClick = { rootNavController.navigate("scan_hub") })
        }

        Box(modifier = Modifier.padding(horizontal = 24.dp)) {
            BalanceCard(
                balance = uiState.totalBalance,
                income = uiState.totalIncome,
                expense = uiState.totalExpense,
                selectedRange = uiState.balanceRange,
                isLoading = uiState.isLoading,
                onRangeSelected = onRangeSelected,
                shimmerTranslateProvider = shimmerTranslateProvider
            )
        }

        if (uiState.error != null && !isSyncErrorDismissed) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFFF0F0))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.Warning, contentDescription = "Sync Error", tint = Color(0xFFE53935), modifier = Modifier.size(20.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Sync Failure", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = uiState.error ?: "", fontSize = 11.sp, color = Color(0xFFD32F2F))
                    }
                    IconButton(onClick = onRetrySync, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Retry Sync", tint = Color(0xFFC62828), modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = { isSyncErrorDismissed = true }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "Dismiss", tint = Color(0xFFC62828), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        if (uiState.exceededBudgets.isNotEmpty() && !isBudgetWarningDismissed) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFFF3E0))
                    .border(1.dp, Color(0xFFFFB300).copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.Warning, contentDescription = "Budget Exceeded Warning", tint = Color(0xFFFFB300), modifier = Modifier.size(20.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        val message = if (uiState.exceededBudgets.size == 1) {
                            val b = uiState.exceededBudgets.first()
                            "You've exceeded your ${b.category.displayName} budget! ($${b.spent.toInt()}/$${b.monthlyLimit.toInt()})"
                        } else {
                            "You've exceeded ${uiState.exceededBudgets.size} budgets this month!"
                        }
                        Text(text = "Budget Exceeded", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = message, fontSize = 11.sp, color = Color(0xFFEF6C00))
                    }
                    IconButton(onClick = { isBudgetWarningDismissed = true }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "Dismiss", tint = Color(0xFFE65100), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.padding(horizontal = 24.dp)) {
            TransactionHeader(isLoading = uiState.isLoading, onSeeAllClick = onNavigateToAllTransactions, shimmerTranslateProvider = shimmerTranslateProvider)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // FIX: Removed Crossfade entirely to prevent main-thread structure invalidation.
        // Shimmer placeholders and actual items alternate cleanly inside a single stable LazyColumn view-tree.
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            userScrollEnabled = !uiState.isLoading
        ) {
            if (uiState.isLoading) {
                items(12, key = { "shimmer_$it" }, contentType = { "shimmer" }) {
                    ShimmerTransactionItem(shimmerTranslateProvider = shimmerTranslateProvider)
                }
            } else if (uiState.transactions.isEmpty()) {
                item(key = "empty_state") {
                    Box(modifier = Modifier.fillParentMaxSize().padding(horizontal = 24.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(24.dp)).background(Color(0xFFF3E8FF)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Payment, contentDescription = "No Transactions", tint = Color(0xFF8121FD), modifier = Modifier.size(36.dp))
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(text = "No transactions found", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = "Your transaction history will appear here once you make your first payment.", fontSize = 12.sp, color = TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp))
                        }
                    }
                }
            } else {
                items(
                    items = uiState.transactions,
                    key = { it.id },
                    contentType = { "transaction" }
                ) { transaction ->
                    val onBatchIconClickLambda = remember(transaction.id) { { showBatchItemsTransaction = transaction } }
                    SwipeableTransactionItem(
                        transaction = transaction,
                        onDeleteTransaction = onDeleteTransaction,
                        onNavigateToAddTransaction = onNavigateToAddTransaction,
                        onBatchIconClick = onBatchIconClickLambda,
                        textPrimary = itemTextPrimary,
                        textSecondary = itemTextSecondary,
                        cardBackground = itemCardBackground,
                        itemBackgroundGray = itemBackgroundGray,
                        positiveGreen = itemPositiveGreen,
                        negativeRed = itemNegativeRed
                    )
                }
            }
        }
    }
}

@Composable
private fun TopNavigationBar(onScanClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Box {
            IconButton(onClick = { }, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(CardBackground)) {
                Icon(Icons.Rounded.NotificationsNone, contentDescription = "Notifications", tint = TextPrimary)
            }
            Box(modifier = Modifier.size(10.dp).align(Alignment.TopEnd).offset(x = (-8).dp, y = 8.dp).clip(CircleShape).background(NegativeRed))
        }
        Text(text = "Home", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        IconButton(onClick = onScanClick, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(CardBackground)) {
            Icon(Icons.Rounded.QrCodeScanner, contentDescription = "Scan Barcode", tint = TextPrimary)
        }
    }
}

@Composable
private fun BalanceCard(
    balance: Double,
    income: Double,
    expense: Double,
    selectedRange: BalanceRange,
    isLoading: Boolean,
    onRangeSelected: (BalanceRange) -> Unit,
    shimmerTranslateProvider: () -> Float
) {
    if (isLoading) {
        Box(modifier = Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(24.dp)).shimmerEffect(shimmerTranslateProvider))
    } else {
        var dropdownExpanded by remember { mutableStateOf(false) }
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(PrimaryGradient).padding(24.dp)) {
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { dropdownExpanded = true }.padding(vertical = 4.dp, horizontal = 8.dp)) {
                            Text(text = selectedRange.displayName, color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Select Range", tint = Color.White, modifier = Modifier.padding(start = 4.dp).size(16.dp))
                        }
                        MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(surface = Color.Transparent), shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(16.dp))) {
                            DropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false },
                                modifier = Modifier.background(PrimaryGradient, shape = RoundedCornerShape(16.dp)).clip(RoundedCornerShape(16.dp)).border(1.dp, Color.White.copy(alpha = 0.2f), shape = RoundedCornerShape(16.dp))
                            ) {
                                BalanceRange.values().forEach { range ->
                                    DropdownMenuItem(
                                        text = { Text(text = range.displayName, color = if (range == selectedRange) Color.White else Color.White.copy(alpha = 0.7f), fontWeight = if (range == selectedRange) FontWeight.Bold else FontWeight.Normal, fontSize = 14.sp) },
                                        onClick = { onRangeSelected(range); dropdownExpanded = false },
                                        colors = MenuDefaults.itemColors(textColor = Color.White, leadingIconColor = Color.White, trailingIconColor = Color.White)
                                    )
                                }
                            }
                        }
                    }
                    Icon(Icons.Rounded.MoreHoriz, contentDescription = "Options", tint = Color.White)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = balance.toCurrencyString(), color = if (balance < 0) Color(0xFFFF8A80) else Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(32.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.ArrowDownward, contentDescription = "Income", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Income", color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = income.toCurrencyString(), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Column(horizontalAlignment = Alignment.Start) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.ArrowUpward, contentDescription = "Expenses", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Expense", color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = expense.toCurrencyString(), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionHeader(isLoading: Boolean, onSeeAllClick: () -> Unit, shimmerTranslateProvider: () -> Float) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        if (isLoading) {
            Box(modifier = Modifier.size(120.dp, 24.dp).clip(RoundedCornerShape(6.dp)).shimmerEffect(shimmerTranslateProvider))
            Box(modifier = Modifier.size(60.dp, 16.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect(shimmerTranslateProvider))
        } else {
            Text(text = "Transactions", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(text = "See All", fontSize = 14.sp, color = TextSecondary, fontWeight = FontWeight.Medium, modifier = Modifier.clip(RoundedCornerShape(4.dp)).clickable { onSeeAllClick() }.padding(horizontal = 8.dp, vertical = 4.dp))
        }
    }
}

@Composable
fun TransactionItem(
    transaction: Transaction,
    onBatchIconClick: (() -> Unit)? = null,
    textPrimary: Color = TextPrimary,
    textSecondary: Color = TextSecondary,
    cardBackground: Color = CardBackground,
    positiveGreen: Color = PositiveGreen,
    negativeRed: Color = NegativeRed
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(modifier = Modifier.weight(1f).padding(end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(cardBackground), contentAlignment = Alignment.Center) {
                Icon(imageVector = transaction.category.icon, contentDescription = transaction.title, tint = textPrimary, modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = transaction.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    if (!transaction.isSynced) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Rounded.CloudOff, contentDescription = "Not Synced", tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                    // FIX: Cache status calculation inside Domain layer map block in a real app,
                    // but for inline rendering stability, this evaluation is kept minimal.
                    if (transaction.description?.contains("Batch scanned barcode") == true) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Rounded.Layers, contentDescription = "Batched Barcode Item", tint = Color(0xFF635BFF), modifier = Modifier.size(16.dp).clickable { onBatchIconClick?.invoke() })
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = transaction.formattedDate, fontSize = 13.sp, color = textSecondary)
            }
        }
        val amountColor = if (transaction.amount > 0) positiveGreen else negativeRed
        val amountPrefix = if (transaction.amount > 0) "+$" else "-$"
        Text(text = "$amountPrefix${abs(transaction.amount).toInt()}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = amountColor)
    }
}

private val shimmerColors = listOf(Color(0xFFEAEAEA), Color(0xFFF5F5F5), Color(0xFFEAEAEA))

fun Modifier.shimmerEffect(translateProvider: () -> Float): Modifier = this
    .clearAndSetSemantics { }
    .drawBehind {
        val translateVal = translateProvider()
        val brush = Brush.linearGradient(colors = shimmerColors, start = Offset(translateVal, 0f), end = Offset(translateVal + 300f, 300f))
        drawRect(brush = brush)
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableTransactionItem(
    transaction: Transaction,
    onDeleteTransaction: (Transaction) -> Unit,
    onNavigateToAddTransaction: (String?) -> Unit,
    onBatchIconClick: () -> Unit,
    modifier: Modifier = Modifier,
    textPrimary: Color = TextPrimary,
    textSecondary: Color = TextSecondary,
    cardBackground: Color = CardBackground,
    itemBackgroundGray: Color = BackgroundGray,
    positiveGreen: Color = PositiveGreen,
    negativeRed: Color = NegativeRed
) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val currentOnDeleteTransaction by rememberUpdatedState(onDeleteTransaction)
    val currentTransaction by rememberUpdatedState(transaction)
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val minThresholdPx = remember(density) { with(density) { 80.dp.toPx() } }

    // FIX: Extracted positional threshold calculation into a static remembered reference.
    // Stops garbage collection allocation churn executing per layout frame evaluation.
    val positionalThresholdCalc = remember(minThresholdPx) {
        { totalDistance: Float -> maxOf(totalDistance * 0.75f, minThresholdPx) }
    }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                showDeleteConfirmation = true
                false
            } else {
                false
            }
        },
        positionalThreshold = positionalThresholdCalc
    )

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirmation = false
                coroutineScope.launch { dismissState.reset() }
            },
            title = { Text("Delete Transaction?") },
            text = { Text("\"${transaction.title}\" will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirmation = false
                    currentOnDeleteTransaction(currentTransaction)
                }) { Text("Delete", color = Color(0xFFE53935)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteConfirmation = false
                    coroutineScope.launch { dismissState.reset() }
                }) { Text("Cancel") }
            }
        )
    }

    val onClick = remember(transaction.id, onNavigateToAddTransaction) { { onNavigateToAddTransaction(transaction.id) } }

    SwipeToDismissBox(
        modifier = modifier,
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)).background(Color(0xFFE53935)).padding(horizontal = 24.dp), contentAlignment = Alignment.CenterEnd) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }
        },
        content = {
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(itemBackgroundGray).clickable(onClick = onClick)) {
                TransactionItem(transaction = transaction, onBatchIconClick = onBatchIconClick, textPrimary = textPrimary, textSecondary = textSecondary, cardBackground = cardBackground, positiveGreen = positiveGreen, negativeRed = negativeRed)
            }
        }
    )
}

@Composable
fun ShimmerTransactionItem(shimmerTranslateProvider: () -> Float) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).shimmerEffect(shimmerTranslateProvider))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Box(modifier = Modifier.size(120.dp, 16.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect(shimmerTranslateProvider))
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.size(80.dp, 12.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect(shimmerTranslateProvider))
            }
        }
        Box(modifier = Modifier.size(60.dp, 18.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect(shimmerTranslateProvider))
    }
}

@Preview(showBackground = true)
@Composable
fun WalletDashboardPreview() {
    MaterialTheme {
        HomeScreenContent(
            innerPadding = PaddingValues(),
            uiState = HomeUiState(
                transactions = persistentListOf(
                    Transaction(
                        id = "1", title = "Groceries", timestamp = System.currentTimeMillis(), amount = -150.0,
                        icon = Icons.Rounded.ShoppingBag, iconBgColor = Color(0xFFF19E79), type = TransactionType.EXPENSE,
                        category = TransactionCategory.GROCERIES, description = "Weekly shop", isSynced = true
                    )
                ),
                totalBalance = 5000.0, totalIncome = 6500.0, totalExpense = 1500.0, isLoading = false
            ),
            onRangeSelected = {}, onDeleteTransaction = {}, onRetrySync = {}, onNavigateToAddTransaction = {}, onNavigateToAllTransactions = {}, rootNavController = rememberNavController()
        )
    }
}

private val currencyFormatter = DecimalFormat("$#,##0.00")
private fun Double.toCurrencyString(): String = currencyFormatter.format(this)

fun parseBatchDescription(description: String?): List<Pair<String, String>> {
    if (description == null || !description.contains("Batch scanned barcode")) return emptyList()
    val prefixIndex = description.indexOf(":")
    if (prefixIndex == -1) return emptyList()
    val detailsPart = description.substring(prefixIndex + 1).trim()
    val items = detailsPart.split(";").map { it.trim() }.filter { it.isNotEmpty() }
    val itemRegex = Regex("""(.+)\s*\((\d+)\)""")
    val parsedItems = mutableListOf<Pair<String, String>>()
    for (item in items) {
        val match = itemRegex.find(item)
        if (match != null) {
            parsedItems.add(Pair(match.groupValues[1].trim(), match.groupValues[2].trim()))
        }
    }
    return parsedItems
}