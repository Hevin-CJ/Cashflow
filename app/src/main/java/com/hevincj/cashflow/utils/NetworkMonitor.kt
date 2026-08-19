package com.hevincj.cashflow.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

class NetworkMonitor(context: Context) {

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val isConnected: Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                val connected = isCurrentlyConnected()
                com.hevincj.cashflow.utils.CrashLogger.setCustomKey("network_status", if (connected) "online" else "offline")
                com.hevincj.cashflow.utils.CrashLogger.i("NetworkMonitor", "Network available (connected: $connected)")
                trySend(connected)
            }

            override fun onLost(network: Network) {
                val connected = isCurrentlyConnected()
                com.hevincj.cashflow.utils.CrashLogger.setCustomKey("network_status", "offline")
                com.hevincj.cashflow.utils.CrashLogger.w("NetworkMonitor", "Network connection lost")
                trySend(connected)
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                // FIX: Evaluate the system's global connection state, not just this isolated network event
                val connected = isCurrentlyConnected()
                trySend(connected)
            }
        }

        // Register callback safely
        try {
            connectivityManager.registerDefaultNetworkCallback(callback)
        } catch (e: Exception) {
            com.hevincj.cashflow.utils.CrashLogger.w("NetworkMonitor", "Failed to register network callback: ${e.message}", e)
            trySend(isCurrentlyConnected())
        }

        // Emit initial baseline connectivity state
        val initial = isCurrentlyConnected()
        com.hevincj.cashflow.utils.CrashLogger.setCustomKey("network_status", if (initial) "online" else "offline")
        trySend(initial)

        awaitClose {
            try {
                connectivityManager.unregisterNetworkCallback(callback)
            } catch (e: Exception) {
                // Ignore cleanup errors on teardown
            }
        }
    }.distinctUntilChanged() // Optimization: Prevent UI rendering pipelines from re-firing on duplicate states

    /**
     * Identifies if the default active data network interface is routing verified internet traffic.
     */
    fun isCurrentlyConnected(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}