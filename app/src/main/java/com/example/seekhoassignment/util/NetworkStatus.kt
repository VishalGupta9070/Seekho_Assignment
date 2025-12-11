package com.example.seekhoassignment.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

enum class NetworkStatus { Available, Unavailable }

/**
 * Simple network monitor that emits network availability changes.
 * Uses ConnectivityManager callback (works on modern Android).
 */
interface NetworkMonitor {
    fun networkStatusFlow(): Flow<NetworkStatus>
}

@Singleton
class NetworkMonitorImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : NetworkMonitor {

    override fun networkStatusFlow(): Flow<NetworkStatus> = callbackFlow {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // helper to compute current status
        fun isOnline(): Boolean {
            val active = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(active) ?: return false
            return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(NetworkStatus.Available)
            }

            override fun onLost(network: Network) {
                trySend(NetworkStatus.Unavailable)
            }

            override fun onUnavailable() {
                trySend(NetworkStatus.Unavailable)
            }
        }

        // register callback (safe across API levels)
        try {
            val request = android.net.NetworkRequest.Builder().build()
            cm.registerNetworkCallback(request, callback)
        } catch (t: Throwable) {
            // Fallback: send current status
            trySend(if (isOnline()) NetworkStatus.Available else NetworkStatus.Unavailable)
        }

        // emit initial state
        trySend(if (isOnline()) NetworkStatus.Available else NetworkStatus.Unavailable)

        awaitClose {
            try {
                cm.unregisterNetworkCallback(callback)
            } catch (_: Throwable) { }
        }
    }.distinctUntilChanged()
}
