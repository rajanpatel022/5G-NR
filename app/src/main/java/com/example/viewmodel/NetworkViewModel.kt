package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.CellularTelemetry
import com.example.telephony.LaunchResult
import com.example.telephony.NetworkSettingsLauncher
import com.example.telephony.TelephonyTracker
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

sealed class UiEvent {
    data class ShowToast(val message: String) : UiEvent()
    data class ShowSnackbar(val message: String, val actionLabel: String? = null, val action: (() -> Unit)? = null) : UiEvent()
}

class NetworkViewModel(application: Application) : AndroidViewModel(application) {

    private val telephonyTracker = TelephonyTracker(application.applicationContext, viewModelScope)

    val telemetry: StateFlow<CellularTelemetry> = telephonyTracker.telemetry

    private val _uiEvents = MutableSharedFlow<UiEvent>()
    val uiEvents: SharedFlow<UiEvent> = _uiEvents.asSharedFlow()

    init {
        telephonyTracker.startListening()
    }

    fun refreshNetworkState() {
        telephonyTracker.refresh()
        viewModelScope.launch {
            _uiEvents.emit(UiEvent.ShowToast("Refreshed cellular telemetry"))
        }
    }

    fun launchMethod1PhoneInfo(context: Context) {
        when (val result = NetworkSettingsLauncher.launchPhoneInfo(context)) {
            is LaunchResult.Success -> {}
            is LaunchResult.Error -> {
                viewModelScope.launch {
                    _uiEvents.emit(UiEvent.ShowSnackbar(result.message))
                }
            }
        }
    }

    fun launchMethod2TestingSettings(context: Context) {
        when (val result = NetworkSettingsLauncher.launchTestingSettings(context)) {
            is LaunchResult.Success -> {}
            is LaunchResult.Error -> {
                viewModelScope.launch {
                    _uiEvents.emit(UiEvent.ShowSnackbar(result.message))
                }
            }
        }
    }

    fun launchSamsungBandSelection(context: Context) {
        when (val result = NetworkSettingsLauncher.launchSamsungBandSelection(context)) {
            is LaunchResult.Success -> {}
            is LaunchResult.Error -> {
                viewModelScope.launch {
                    _uiEvents.emit(UiEvent.ShowSnackbar(result.message))
                }
            }
        }
    }

    fun launchMediaTekEngineerMode(context: Context) {
        when (val result = NetworkSettingsLauncher.launchMediaTekEngineerMode(context)) {
            is LaunchResult.Success -> {}
            is LaunchResult.Error -> {
                viewModelScope.launch {
                    _uiEvents.emit(UiEvent.ShowSnackbar(result.message))
                }
            }
        }
    }

    fun launchDialerCode(context: Context, code: String) {
        NetworkSettingsLauncher.launchDialerCode(context, code)
    }

    fun copyCode(context: Context, code: String) {
        NetworkSettingsLauncher.copyToClipboard(context, code)
        viewModelScope.launch {
            _uiEvents.emit(UiEvent.ShowToast("Copied $code to clipboard"))
        }
    }

    fun launchOnePlusEngineerMode(context: Context) {
        when (val result = NetworkSettingsLauncher.launchOnePlusEngineerMode(context)) {
            is LaunchResult.Success -> {}
            is LaunchResult.Error -> {
                viewModelScope.launch {
                    _uiEvents.emit(UiEvent.ShowSnackbar(result.message))
                }
            }
        }
    }

    fun launchDeveloperOptions(context: Context) {
        when (val result = NetworkSettingsLauncher.launchDeveloperOptions(context)) {
            is LaunchResult.Success -> {}
            is LaunchResult.Error -> {
                viewModelScope.launch {
                    _uiEvents.emit(UiEvent.ShowSnackbar(result.message))
                }
            }
        }
    }

    fun copyAdbCommand(context: Context) {
        NetworkSettingsLauncher.copyToClipboard(context, NetworkSettingsLauncher.ADB_RADIOINFO_COMMAND)
        viewModelScope.launch {
            _uiEvents.emit(UiEvent.ShowToast("Copied ADB command to clipboard"))
        }
    }

    fun openSystemMobileSettings(context: Context) {
        NetworkSettingsLauncher.openMobileNetworkSettings(context)
    }

    override fun onCleared() {
        super.onCleared()
        telephonyTracker.stopListening()
    }
}
