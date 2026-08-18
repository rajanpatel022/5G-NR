package com.example.telephony

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast

sealed class LaunchResult {
    data object Success : LaunchResult()
    data class Error(val message: String, val suggestedFallback: String? = null) : LaunchResult()
}

object NetworkSettingsLauncher {

    const val SECRET_CODE_TESTING = "*#*#4636#*#*"
    const val SECRET_CODE_ONEPLUS_ENGINEER = "*#899#"
    const val SECRET_CODE_ONEPLUS_5G_LOG = "*#*#5646#*#*"
    const val SECRET_CODE_ONEPLUS_LEGACY = "*#808#"
    const val SECRET_CODE_MEDIATEK = "*#*#3646633#*#*"
    const val SECRET_CODE_SAMSUNG = "*#2263#"
    const val SECRET_CODE_SAMSUNG_SERVICE = "*#0011#"

    const val ADB_RADIOINFO_COMMAND = "adb shell am start -n com.android.settings/.RadioInfo"

    /**
     * Launch OnePlus / OxygenOS / Oppo Engineer Mode
     */
    fun launchOnePlusEngineerMode(context: Context): LaunchResult {
        val intents = listOf(
            Intent().apply {
                component = ComponentName("com.oplus.engineermode", "com.oplus.engineermode.manualtest.modeltest.ModelTest")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            },
            Intent().apply {
                component = ComponentName("com.oplus.engineermode", "com.oplus.engineermode.EngineerMode")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            },
            Intent().apply {
                component = ComponentName("com.oppo.engineermode", "com.oppo.engineermode.EngineerMode")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            },
            Intent().apply {
                component = ComponentName("com.oneplus.factorymode", "com.oneplus.factorymode.FactoryMode")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            },
            Intent().apply {
                component = ComponentName("com.oplus.wirelesssettings", "com.oplus.wirelesssettings.WirelessSettingsActivity")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        )

        for (intent in intents) {
            try {
                context.startActivity(intent)
                return LaunchResult.Success
            } catch (_: Exception) {
                // Try next
            }
        }

        // Dial *#899#
        return try {
            launchDialerCode(context, SECRET_CODE_ONEPLUS_ENGINEER)
            LaunchResult.Error("Opened Dialer with OnePlus code $SECRET_CODE_ONEPLUS_ENGINEER — tap Call to access Engineer Mode / Telephony.", SECRET_CODE_ONEPLUS_ENGINEER)
        } catch (e: Exception) {
            copyToClipboard(context, SECRET_CODE_ONEPLUS_ENGINEER)
            LaunchResult.Error("Copied OnePlus code $SECRET_CODE_ONEPLUS_ENGINEER to clipboard.")
        }
    }

    /**
     * Direct shortcut to Developer Options (Where OxygenOS contains 5G Network Mode: SA Mode)
     */
    fun launchDeveloperOptions(context: Context): LaunchResult {
        return try {
            val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            LaunchResult.Success
        } catch (e: Exception) {
            LaunchResult.Error("Developer Options not enabled yet. Tap 'Build number' 7 times in Settings > About device.")
        }
    }

    /**
     * Primary Launcher (Android 11, 12, 13, 14, 15, 16+)
     * Directly launches Phone Info (RadioInfo)
     */
    fun launchPhoneInfo(context: Context): LaunchResult {
        val intents = listOf(
            // Strategy 1: com.android.settings.RadioInfo via Intent.ACTION_MAIN
            Intent(Intent.ACTION_MAIN).apply {
                setClassName("com.android.settings", "com.android.settings.RadioInfo")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            },
            // Strategy 2: ComponentName explicit
            Intent().apply {
                component = ComponentName("com.android.settings", "com.android.settings.RadioInfo")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            },
            // Strategy 3: TestingSettings
            Intent(Intent.ACTION_MAIN).apply {
                setClassName("com.android.settings", "com.android.settings.TestingSettings")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            },
            // Strategy 4: com.android.phone.settings.RadioInfo
            Intent(Intent.ACTION_MAIN).apply {
                setClassName("com.android.phone", "com.android.phone.settings.RadioInfo")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        )

        for (intent in intents) {
            try {
                context.startActivity(intent)
                return LaunchResult.Success
            } catch (_: Exception) {
                // Continue trying fallbacks
            }
        }

        // If direct intent is blocked by OEM security, open dialer with *#*#4636#*#*
        return try {
            launchDialerCode(context, SECRET_CODE_TESTING)
            LaunchResult.Error(
                "Direct intent blocked by your device ROM. Opened Dialer with code $SECRET_CODE_TESTING — tap Call to access Phone Info.",
                suggestedFallback = SECRET_CODE_TESTING
            )
        } catch (e: Exception) {
            copyToClipboard(context, SECRET_CODE_TESTING)
            LaunchResult.Error(
                "Could not open menu directly. Copied $SECRET_CODE_TESTING to clipboard. Paste into your Phone dialer.",
                suggestedFallback = SECRET_CODE_TESTING
            )
        }
    }

    /**
     * Legacy / Alternative Method (TestingSettings / Android 10 & below)
     */
    fun launchTestingSettings(context: Context): LaunchResult {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            setClassName("com.android.settings", "com.android.settings.TestingSettings")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            context.startActivity(intent)
            LaunchResult.Success
        } catch (e: Exception) {
            LaunchResult.Error("TestingSettings not accessible on this device: ${e.localizedMessage}")
        }
    }

    /**
     * Samsung Specific: Band Selection & Hidden Network Settings
     */
    fun launchSamsungBandSelection(context: Context): LaunchResult {
        val intents = listOf(
            Intent().apply {
                component = ComponentName("com.samsung.android.app.telephonyui", "com.samsung.android.app.telephonyui.hiddennetworksetting.MainActivity")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            },
            Intent().apply {
                component = ComponentName("com.samsung.android.app.telephonyui", "com.samsung.android.app.telephonyui.networkmode.MainActivity")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        )

        for (intent in intents) {
            try {
                context.startActivity(intent)
                return LaunchResult.Success
            } catch (_: Exception) {
                // Try next
            }
        }

        // Fallback to Samsung secret code
        return try {
            launchDialerCode(context, SECRET_CODE_SAMSUNG)
            LaunchResult.Error("Samsung hidden menu requires dialer. Opened with $SECRET_CODE_SAMSUNG", SECRET_CODE_SAMSUNG)
        } catch (e: Exception) {
            copyToClipboard(context, SECRET_CODE_SAMSUNG)
            LaunchResult.Error("Copied Samsung code $SECRET_CODE_SAMSUNG to clipboard.")
        }
    }

    /**
     * MediaTek Engineer Mode
     */
    fun launchMediaTekEngineerMode(context: Context): LaunchResult {
        val intents = listOf(
            Intent().apply {
                component = ComponentName("com.mediatek.engineermode", "com.mediatek.engineermode.EngineerMode")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            },
            Intent().apply {
                component = ComponentName("com.mediatek.engineermode", "com.mediatek.engineermode.mobile.Mobile")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        )

        for (intent in intents) {
            try {
                context.startActivity(intent)
                return LaunchResult.Success
            } catch (_: Exception) {
                // Continue
            }
        }

        return LaunchResult.Error("MediaTek Engineer Mode is not installed on this SoC.")
    }

    /**
     * Launch System Dialer with USSD code
     */
    fun launchDialerCode(context: Context, code: String): LaunchResult {
        return try {
            val encoded = Uri.encode(code)
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$encoded")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            LaunchResult.Success
        } catch (e: Exception) {
            copyToClipboard(context, code)
            LaunchResult.Error("Failed to open dialer. Copied code $code to clipboard.")
        }
    }

    /**
     * Standard Android System Network Settings
     */
    fun openMobileNetworkSettings(context: Context): LaunchResult {
        val intents = listOf(
            Intent(Settings.ACTION_DATA_ROAMING_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK },
            Intent(Settings.ACTION_WIRELESS_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK },
            Intent(Settings.ACTION_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
        )

        for (intent in intents) {
            try {
                context.startActivity(intent)
                return LaunchResult.Success
            } catch (_: Exception) {
                // Continue
            }
        }
        return LaunchResult.Error("Could not open Android Network Settings.")
    }

    fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Secret Code", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied $text to clipboard", Toast.LENGTH_SHORT).show()
    }
}
