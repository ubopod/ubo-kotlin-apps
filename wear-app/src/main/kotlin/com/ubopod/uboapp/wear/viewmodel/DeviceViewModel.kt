package com.ubopod.uboapp.wear.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import com.ubopod.uboapp.wear.storage.UboWearSettings
import com.ubopod.ubokotlin.UboClient
import com.ubopod.ubokotlin.connection.ConnectionState
import com.ubopod.ubokotlin.models.StatusBarData
import com.ubopod.ubokotlin.models.SystemStats
import com.ubopod.ubokotlin.models.ViewData
import com.ubopod.ubokotlin.models.WebUIInputDescription
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn

/**
 * Slim ViewModel for the wear app — same gRPC plumbing as the phone-app
 * counterpart, minus the camera / mic / audio-playback hardware
 * services. Mirrors the watchOS `DeviceViewModel.swift` (no
 * `cameraManager`, no `micCapture`, no `audioPlayback`).
 */
public class DeviceViewModel(application: Application) : AndroidViewModel(application) {

    // Default UboClient scope (SupervisorJob + Dispatchers.Default) keeps
    // subscription work off the Main thread. onCleared → client.close()
    // cancels it.
    public val client: UboClient = UboClient()
    private val settings = UboWearSettings(application)

    public val connectionState: StateFlow<ConnectionState> = client.connectionState
    public val currentView: StateFlow<ViewData?> = client.currentView
    public val statusBar: StateFlow<StatusBarData?> = client.statusBar
    public val systemStats: StateFlow<SystemStats?> = client.systemStats
    public val activeInputs: StateFlow<List<WebUIInputDescription>> = client.activeInputs

    public val savedHost: StateFlow<String> = settings.savedHost
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")
    public val savedPort: StateFlow<Int> = settings.savedPort
        .stateIn(viewModelScope, SharingStarted.Eagerly, UboWearSettings.DEFAULT_PORT)

    public suspend fun connect(host: String, port: Int) {
        settings.setHost(host)
        settings.setPort(port)
        client.connect(host, port)
        client.startViewSubscription()
    }

    public suspend fun disconnect() {
        client.disconnect()
    }

    public suspend fun connectWithSavedSettings(): Boolean {
        val host = settings.savedHost.first()
        if (host.isEmpty()) return false
        val port = settings.savedPort.first()
        connect(host, port)
        return true
    }

    override fun onCleared() {
        super.onCleared()
        client.close()
    }

    public companion object {
        public val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as Application
                DeviceViewModel(application)
            }
        }
    }
}
