package com.hevincj.cashflow.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.hevincj.cashflow.ui.screen.AddCardScreen
import com.hevincj.cashflow.ui.screen.SubscriptionManagerScreen
import com.hevincj.cashflow.ui.screen.AddTransactionScreen
import com.hevincj.cashflow.ui.screen.AllTransactionsScreen
import com.hevincj.cashflow.ui.screen.MainScreen
import com.hevincj.cashflow.ui.screen.BatchScanScreen
import com.hevincj.cashflow.ui.screen.ReceiptScanScreen
import com.hevincj.cashflow.ui.screen.ScanOptionsUi
import com.hevincj.cashflow.ui.screen.auth.LoginScreen
import com.hevincj.cashflow.ui.screen.auth.RegisterScreen
import com.hevincj.cashflow.ui.screen.splash.SplashScreen

@Composable
fun RootNavigation() {
    val rootNavController = rememberNavController()

    NavHost(
        navController = rootNavController,
        startDestination = "splash",
        enterTransition = { fadeIn(animationSpec = tween(300)) },
        exitTransition = { fadeOut(animationSpec = tween(300)) },
        popEnterTransition = { fadeIn(animationSpec = tween(300)) },
        popExitTransition = { fadeOut(animationSpec = tween(300)) }
    ) {
        composable("splash") {
            SplashScreen(navController = rootNavController)
        }
        composable(
            route = "login",
            enterTransition = {
                slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
            },
            popEnterTransition = {
                slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300))
            },
            popExitTransition = {
                slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
            }
        ) {
            LoginScreen(navController = rootNavController)
        }
        composable(
            route = "register",
            enterTransition = {
                slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
            },
            popEnterTransition = {
                slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300))
            },
            popExitTransition = {
                slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
            }
        ) {
            RegisterScreen(navController = rootNavController)
        }
        composable("main") {
            MainScreen(rootNavController = rootNavController)
        }
        composable(
            route = "add_transaction?transactionId={transactionId}&title={title}&amount={amount}&category={category}&date={date}&description={description}&barcode={barcode}",
            arguments = listOf(
                navArgument("transactionId") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("title") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("amount") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("category") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("date") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("description") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("barcode") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) {
            AddTransactionScreen(navController = rootNavController)
        }
        composable(
            route = "scan_hub",
            enterTransition = {
                slideInVertically(initialOffsetY = { it }, animationSpec = tween(400)) + fadeIn(animationSpec = tween(400))
            },
            exitTransition = {
                slideOutVertically(targetOffsetY = { it }, animationSpec = tween(400)) + fadeOut(animationSpec = tween(400))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(400))
            },
            popExitTransition = {
                slideOutVertically(targetOffsetY = { it }, animationSpec = tween(400)) + fadeOut(animationSpec = tween(400))
            }
        ) {
            ScanOptionsUi(
                onDismissRequest = { rootNavController.popBackStack() },
                onBatchBarcodeClick = { rootNavController.navigate("batch_scan") },
                onReceiptScanClick = { rootNavController.navigate("receipt_scan") },
                onUpiQrClick = {}, // Handled internally in ScanOptionsUi
                rootNavController = rootNavController
            )
        }
        composable(
            route = "batch_scan",
            enterTransition = {
                slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
            },
            popEnterTransition = {
                slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300))
            },
            popExitTransition = {
                slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
            }
        ) {
            BatchScanScreen(onNavigateBack = { rootNavController.popBackStack() })
        }
        composable(
            route = "receipt_scan",
            enterTransition = {
                slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
            },
            popEnterTransition = {
                slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300))
            },
            popExitTransition = {
                slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
            }
        ) {
            ReceiptScanScreen(
                onNavigateBack = { rootNavController.popBackStack() },
                onNavigateToAddTransaction = { title, amount, category, date, description ->
                    rootNavController.navigate("add_transaction?title=$title&amount=$amount&category=$category&date=$date&description=$description") {
                        popUpTo("receipt_scan") { inclusive = true }
                    }
                }
            )
        }
        composable("all_transactions") {
            AllTransactionsScreen(navController = rootNavController)
        }
        composable("add_card") {
            AddCardScreen(navController = rootNavController)
        }
        composable("subscription_manager") {
            SubscriptionManagerScreen(navController = rootNavController)
        }
    }
}
