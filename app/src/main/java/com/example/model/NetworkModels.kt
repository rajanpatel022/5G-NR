package com.example.model

enum class NetworkGeneration(val displayName: String, val badgeColor: Long) {
    NR_5G("5G NR", 0xFF00E5FF),
    LTE_4G("4G LTE", 0xFF00E676),
    HSPA_3G("3G HSPA", 0xFFFFAB00),
    EDGE_2G("2G GSM", 0xFFFF5252),
    WIFI("Wi-Fi", 0xFF7C4DFF),
    DISCONNECTED("Disconnected", 0xFF64748B),
    UNKNOWN("Unknown", 0xFF64748B)
}

enum class NrMode(val title: String, val description: String) {
    STANDALONE("5G SA (Standalone)", "Pure 5G Core & Radio without 4G anchor. Low latency & high throughput."),
    NON_STANDALONE("5G NSA (Non-Standalone)", "5G NR Radio anchored on 4G LTE Core network."),
    NR_ADVANCED("5G mmWave / Ultra-Wideband", "High-frequency mmWave / Mid-band Carrier Aggregation."),
    NONE("No 5G Detected", "Currently operating on legacy LTE/3G or Wi-Fi network."),
    UNKNOWN("Detecting...", "Checking 5G NR carrier allocation...")
}

data class SimCardInfo(
    val slotIndex: Int,
    val subscriptionId: Int,
    val carrierName: String,
    val displayName: String,
    val countryIso: String,
    val isDefaultDataSim: Boolean
)

data class CellularTelemetry(
    val operatorName: String = "Detecting Network...",
    val networkGeneration: NetworkGeneration = NetworkGeneration.UNKNOWN,
    val networkTypeRaw: String = "LTE",
    val signalDbm: Int = -1,
    val signalAsu: Int = -1,
    val signalLevel: Int = 0, // 0 to 4
    val signalQuality: String = "Checking...",
    val nrMode: NrMode = NrMode.UNKNOWN,
    val is5gConnected: Boolean = false,
    val isDataConnected: Boolean = false,
    val isRoaming: Boolean = false,
    val ipAddress: String = "Detecting...",
    val simCards: List<SimCardInfo> = emptyList(),
    val activeSimSlot: Int = 0,
    val dlBandwidthKbps: Int = 0,
    val ulBandwidthKbps: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)
