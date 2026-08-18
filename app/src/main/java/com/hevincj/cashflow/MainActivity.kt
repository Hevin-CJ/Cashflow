package com.hevincj.cashflow

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hevincj.cashflow.data.local.ThemeManager
import com.hevincj.cashflow.data.local.ThemeMode
import com.hevincj.cashflow.data.worker.RecurringExpenseSyncScheduler
import com.hevincj.cashflow.ui.navigation.RootNavigation
import com.hevincj.cashflow.ui.theme.CashFlowTheme
import javax.inject.Inject
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var themeManager: ThemeManager

    @Inject
    lateinit var recurringExpenseSyncScheduler: RecurringExpenseSyncScheduler

    var shouldNavigateToAddTransaction by mutableStateOf(false)
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        requestHighRefreshRate()
        enableEdgeToEdge()

       
        recurringExpenseSyncScheduler.schedulePeriodicRecurringProcessing()

        setContent {
            val themeMode by themeManager.themeMode.collectAsStateWithLifecycle()
            val isDarkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                else -> isSystemInDarkTheme()
            }
            CashFlowTheme(darkTheme = isDarkTheme) {
                RootNavigation()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == "com.hevincj.cashflow.ACTION_ADD_TRANSACTION") {
            shouldNavigateToAddTransaction = true
        }
    }

    fun consumeAddTransactionAction(): Boolean {
        val result = shouldNavigateToAddTransaction
        shouldNavigateToAddTransaction = false
        return result
    }

    private fun requestHighRefreshRate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val currentDisplay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    this.display
                } else {
                    @Suppress("DEPRECATION")
                    windowManager.defaultDisplay
                }
                val modes = currentDisplay?.supportedModes
                if (!modes.isNullOrEmpty()) {
                    val highestMode = modes.maxByOrNull { it.refreshRate }
                    if (highestMode != null) {
                        val layoutParams = window.attributes
                        if (layoutParams.preferredDisplayModeId != highestMode.modeId) {
                            layoutParams.preferredDisplayModeId = highestMode.modeId
                            window.attributes = layoutParams
                        }
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    window.setPreferMinimalPostProcessing(true)
                }
            } catch (e: Exception) {
                // Safe fallback if display APIs are restricted or fail
            }
        }
    }
}