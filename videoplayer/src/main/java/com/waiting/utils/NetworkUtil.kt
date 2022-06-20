package com.waiting.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log


/**
 * @PackageName : com.zchd.core.utils
 * @Author : hechao
 * @Date :   2019-11-29 01:24
 */
object NetworkUtil {

    fun registerListener(context: Context, completion: ((Triple<Boolean, Boolean, Boolean>) -> Unit)? = {}) {
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            connectivityManager.requestNetwork(NetworkRequest.Builder().build(), object : ConnectivityManager.NetworkCallback() {
                private var lastNetwork: Network? = null
                private var state = Triple(first = true, second = true, third = false)
                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    super.onCapabilitiesChanged(network, networkCapabilities)
                    if (state.third && isNetworkValidated(context = context)) {
                        Log.i("TAG", "network connection change")
                        state = state.copy(third = false)
                        completion?.invoke(state)
                    }
                }

                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    state = Triple(isNetworkConnected(context = context),
                        isWifiConnected(context = context),
                        this.lastNetwork == null || network != this.lastNetwork)
                    Log.i("TAG", "network connection onAvailable $state $network ${this.lastNetwork}")
                    completion?.invoke(state)
                    this.lastNetwork = network
                }

                override fun onLost(network: Network) {
                    super.onLost(network)
                    this.lastNetwork = network
                    state = Triple(false, isWifiConnected(context = context), false)
                    Log.i("TAG", "network connection onLost $state $network")
                    completion?.invoke(state)
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isNetworkConnected(context: Context?): Boolean {
        return try {
            (context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager)
                .let {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        it.activeNetwork != null
                    } else {
                        it.activeNetworkInfo?.isConnected
                    }
                } ?: false
        } catch (ignored: Exception) {
            false
        }
    }

    private fun isNetworkValidated(context: Context?): Boolean {
        return try {
            (context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    it.getNetworkCapabilities(
                        it.activeNetwork
                    )?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                } else {
                    it.activeNetworkInfo?.isAvailable
                }
            } ?: false
        } catch (ignored: Exception) {
            false
        }
    }

    fun isWifiConnected(context: Context?): Boolean {
        return try {
            (context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    it.getNetworkCapabilities(
                        it.activeNetwork
                    )?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                } else {
                    it.activeNetworkInfo?.type == ConnectivityManager.TYPE_WIFI
                }
            } ?: false
        } catch (ignored: Exception) {
            false
        }
    }

    fun isMobileConnected(context: Context?): Boolean {
        return try {
            (context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    it.getNetworkCapabilities(
                        it.activeNetwork
                    )?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                } else {
                    it.activeNetworkInfo?.type == ConnectivityManager.TYPE_MOBILE
                }
            } ?: false
        } catch (ignored: Exception) {
            false
        }
    }
}
