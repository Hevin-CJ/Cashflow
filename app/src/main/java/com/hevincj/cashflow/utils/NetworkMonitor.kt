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
                trySend(isCurrentlyConnected())
            }

            override fun onLost(network: Network) {
                trySend(isCurrentlyConnected())
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                // FIX: Evaluate the system's global connection state, not just this isolated network event
                trySend(isCurrentlyConnected())
            }
        }

        // Register callback safely
        try {
            connectivityManager.registerDefaultNetworkCallback(callback)
        } catch (e: Exception) {
            trySend(isCurrentlyConnected())
        }

        // Emit initial baseline connectivity state
        trySend(isCurrentlyConnected())

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