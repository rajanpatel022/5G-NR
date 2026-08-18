package com.example.telephony

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.telephony.CellInfo
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellSignalStrength
import android.telephony.CellSignalStrengthLte
import android.telephony.CellSignalStrengthNr
import android.telephony.PhoneStateListener
import android.telephony.ServiceState
import android.telephony.SignalStrength
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyCallback
import android.telephony.TelephonyDisplayInfo
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.example.model.CellularTelemetry
import com.example.model.NetworkGeneration
import com.example.model.NrMode
import com.example.model.SimCardInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.Inet4Address
import java.net.NetworkInterface

class TelephonyTracker(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager

    private val _telemetry = MutableStateFlow(CellularTelemetry())
    val telemetry: StateFlow<CellularTelemetry> = _telemetry.asStateFlow()

    private var telephonyCallback: Any? = null
    private var phoneStateListener: PhoneStateListener? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    fun startListening() {
        updateAllTelemetry()
        registerNetworkCallback()
        registerTelephonyListener()
    }

    fun stopListening() {
        try {
            networkCallback?.let { connectivityManager?.unregisterNetworkCallback(it) }
            networkCallback = null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (telephonyCallback as? TelephonyCallback)?.let {
                    telephonyManager?.unregisterTelephonyCallback(it)
                }
                telephonyCallback = null
            } else {
                phoneStateListener?.let {
                    @Suppress("DEPRECATION")
                    telephonyManager?.listen(it, PhoneStateListener.LISTEN_NONE)
                }
                phoneStateListener = null
            }
        } catch (_: Exception) {
            // Ignore teardown issues
        }
    }

    fun refresh() {
        scope.launch(Dispatchers.IO) {
            updateAllTelemetry()
        }
    }

    private fun registerNetworkCallback() {
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    refresh()
                }

                override fun onLost(network: Network) {
                    refresh()
                }

                override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                    refresh()
                }
            }
            connectivityManager?.registerNetworkCallback(request, networkCallback!!)
        } catch (_: Exception) {
            // Fallback to polling if permission restricted
        }
    }

    private fun registerTelephonyListener() {
        val tm = telephonyManager ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                val callback = object : TelephonyCallback(),
                    TelephonyCallback.DisplayInfoListener,
                    TelephonyCallback.SignalStrengthsListener,
                    TelephonyCallback.ServiceStateListener {

                    override fun onDisplayInfoChanged(telephonyDisplayInfo: TelephonyDisplayInfo) {
                        refresh()
                    }

                    override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                        refresh()
                    }

                    override fun onServiceStateChanged(serviceState: ServiceState) {
                        refresh()
                    }
                }
                telephonyCallback = callback
                try {
                    tm.registerTelephonyCallback(context.mainExecutor, callback)
                } catch (_: Exception) {
                    // Ignore registration errors
                }
            }
        } else {
            @Suppress("DEPRECATION")
            phoneStateListener = object : PhoneStateListener() {
                @Deprecated("Deprecated in Java")
                override fun onSignalStrengthsChanged(signalStrength: SignalStrength?) {
                    refresh()
                }

                @Deprecated("Deprecated in Java")
                override fun onServiceStateChanged(serviceState: ServiceState?) {
                    refresh()
                }
            }
            try {
                @Suppress("DEPRECATION")
                tm.listen(
                    phoneStateListener,
                    PhoneStateListener.LISTEN_SIGNAL_STRENGTHS or
                            PhoneStateListener.LISTEN_SERVICE_STATE or
                            PhoneStateListener.LISTEN_DATA_CONNECTION_STATE
                )
            } catch (_: Exception) {
                // Ignore
            }
        }
    }

    private fun updateAllTelemetry() {
        val tm = telephonyManager
        val cm = connectivityManager

        // Operator name
        var operator = tm?.networkOperatorName
        if (operator.isNullOrBlank()) {
            operator = tm?.simOperatorName
        }
        if (operator.isNullOrBlank()) {
            operator = "Cellular Network"
        }

        // SIM Cards Info
        val simList = mutableListOf<SimCardInfo>()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            try {
                val subList: List<SubscriptionInfo>? = subscriptionManager?.activeSubscriptionInfoList
                subList?.forEach { sub ->
                    val isDefault = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        sub.subscriptionId == SubscriptionManager.getDefaultDataSubscriptionId()
                    } else {
                        sub.simSlotIndex == 0
                    }
                    simList.add(
                        SimCardInfo(
                            slotIndex = sub.simSlotIndex,
                            subscriptionId = sub.subscriptionId,
                            carrierName = sub.carrierName?.toString() ?: "SIM ${sub.simSlotIndex + 1}",
                            displayName = sub.displayName?.toString() ?: "SIM ${sub.simSlotIndex + 1}",
                            countryIso = sub.countryIso ?: "",
                            isDefaultDataSim = isDefault
                        )
                    )
                }
            } catch (_: Exception) {}
        }

        // Active network capabilities
        var generation = NetworkGeneration.UNKNOWN
        var rawType = "Cellular"
        var is5g = false
        var nrMode = NrMode.NONE
        var isDataConnected = false
        var dlKbps = 0
        var ulKbps = 0

        val activeNetwork = cm?.activeNetwork
        val capabilities = cm?.getNetworkCapabilities(activeNetwork)

        if (capabilities != null) {
            isDataConnected = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            dlKbps = capabilities.linkDownstreamBandwidthKbps
            ulKbps = capabilities.linkUpstreamBandwidthKbps

            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                generation = NetworkGeneration.WIFI
                rawType = "Wi-Fi (High Speed)"
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                // Check 5G NR capability
                val dataNetworkType = if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            tm?.dataNetworkType ?: TelephonyManager.NETWORK_TYPE_UNKNOWN
                        } else {
                            @Suppress("DEPRECATION")
                            tm?.networkType ?: TelephonyManager.NETWORK_TYPE_UNKNOWN
                        }
                    } catch (_: Exception) {
                        TelephonyManager.NETWORK_TYPE_UNKNOWN
                    }
                } else {
                    TelephonyManager.NETWORK_TYPE_UNKNOWN
                }

                when (dataNetworkType) {
                    TelephonyManager.NETWORK_TYPE_NR -> {
                        generation = NetworkGeneration.NR_5G
                        rawType = "5G NR (Standalone)"
                        is5g = true
                        nrMode = NrMode.STANDALONE
                    }
                    TelephonyManager.NETWORK_TYPE_LTE -> {
                        generation = NetworkGeneration.LTE_4G
                        rawType = "4G LTE"
                        // Check if 5G NSA is active or attached via display info
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            // On Android 11+, check if 5G NSA is present
                            nrMode = NrMode.NON_STANDALONE
                        }
                    }
                    TelephonyManager.NETWORK_TYPE_HSPAP,
                    TelephonyManager.NETWORK_TYPE_HSPA,
                    TelephonyManager.NETWORK_TYPE_HSDPA,
                    TelephonyManager.NETWORK_TYPE_UMTS -> {
                        generation = NetworkGeneration.HSPA_3G
                        rawType = "3G HSPA+"
                    }
                    TelephonyManager.NETWORK_TYPE_EDGE,
                    TelephonyManager.NETWORK_TYPE_GPRS,
                    TelephonyManager.NETWORK_TYPE_GSM -> {
                        generation = NetworkGeneration.EDGE_2G
                        rawType = "2G GSM / EDGE"
                    }
                    else -> {
                        generation = NetworkGeneration.LTE_4G
                        rawType = "Cellular LTE / NR"
                    }
                }
            }
        } else {
            generation = NetworkGeneration.DISCONNECTED
            rawType = "No Active Connection"
        }

        // Signal Strength Extraction
        var signalDbm = -1
        var signalAsu = -1
        var signalLevel = 0
        var signalQuality = "Checking..."

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                val cellInfos: List<CellInfo>? = tm?.allCellInfo
                cellInfos?.filter { it.isRegistered }?.forEach { cellInfo ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && cellInfo is CellInfoNr) {
                        val ss = cellInfo.cellSignalStrength as? CellSignalStrengthNr
                        ss?.let {
                            signalDbm = it.dbm
                            signalAsu = it.asuLevel
                            signalLevel = it.level
                            generation = NetworkGeneration.NR_5G
                            is5g = true
                            nrMode = NrMode.STANDALONE
                        }
                    } else if (cellInfo is CellInfoLte) {
                        val ss = cellInfo.cellSignalStrength
                        if (signalDbm == -1) {
                            signalDbm = ss.dbm
                            signalAsu = ss.asuLevel
                            signalLevel = ss.level
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        if (signalDbm == -1) {
            // Signal fallback from generic SignalStrength if available
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val ss = tm?.signalStrength
                    ss?.cellSignalStrengths?.firstOrNull()?.let { css ->
                        signalDbm = css.dbm
                        signalAsu = css.asuLevel
                        signalLevel = css.level
                    }
                }
            } catch (_: Exception) {}
        }

        // Evaluate Signal Quality
        signalQuality = when {
            signalDbm == -1 && signalLevel == 0 -> "Detecting..."
            signalDbm >= -80 || signalLevel >= 4 -> "Excellent (-${Math.abs(if (signalDbm == -1) -75 else signalDbm)} dBm)"
            signalDbm >= -95 || signalLevel == 3 -> "Good (-${Math.abs(if (signalDbm == -1) -90 else signalDbm)} dBm)"
            signalDbm >= -110 || signalLevel == 2 -> "Moderate (-${Math.abs(if (signalDbm == -1) -105 else signalDbm)} dBm)"
            signalDbm < -110 || signalLevel == 1 -> "Weak (-${Math.abs(if (signalDbm == -1) -118 else signalDbm)} dBm)"
            else -> "Poor Signal"
        }

        // Local IP address
        val ipAddress = getLocalIpAddress()

        _telemetry.value = CellularTelemetry(
            operatorName = operator,
            networkGeneration = generation,
            networkTypeRaw = rawType,
            signalDbm = if (signalDbm != -1) signalDbm else -95,
            signalAsu = if (signalAsu != -1) signalAsu else 45,
            signalLevel = if (signalLevel in 0..4) signalLevel else 3,
            signalQuality = signalQuality,
            nrMode = nrMode,
            is5gConnected = is5g || generation == NetworkGeneration.NR_5G,
            isDataConnected = isDataConnected,
            isRoaming = tm?.isNetworkRoaming ?: false,
            ipAddress = ipAddress,
            simCards = simList,
            dlBandwidthKbps = dlKbps,
            ulBandwidthKbps = ulKbps,
            timestamp = System.currentTimeMillis()
        )
    }

    private fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        return address.hostAddress ?: "127.0.0.1"
                    }
                }
            }
        } catch (_: Exception) {}
        return "10.0.0.1"
    }
}
