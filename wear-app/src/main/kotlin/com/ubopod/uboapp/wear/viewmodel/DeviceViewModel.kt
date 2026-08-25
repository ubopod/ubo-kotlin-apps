package com.ubopod.uboapp.wear.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.wear.tiles.TileService
import com.ubopod.uboapp.wear.service.WatchAudioPlaybackService
import com.ubopod.uboapp.wear.service.WatchMicCaptureService
import com.ubopod.uboapp.wear.storage.RecentConnection
import com.ubopod.uboapp.wear.storage.UboWearSettings
import com.ubopod.uboapp.wear.tile.UboTileService
import com.ubopod.uboapp.wear.tile.WearStatsStore
import com.ubopod.ubokotlin.UboClient
import com.ubopod.ubokotlin.connection.ConnectionState
import com.ubopod.ubokotlin.connection.DiscoveredDevice
import com.ubopod.ubokotlin.connection.UboDiscovery
import com.ubopod.ubokotlin.models.AssistantTriggerSource
import com.ubopod.ubokotlin.models.PlaybackEvent
import com.ubopod.ubokotlin.models.StatusBarData
import com.ubopod.ubokotlin.models.SystemStats
import com.ubopod.ubokotlin.models.ViewData
import com.ubopod.ubokotlin.models.WebUIInputDescription
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Wear ViewModel — mirrors the watchOS `DeviceViewModel.swift` after the
 * push-to-talk + playback parity patches (commits `56d17e1`, `af27935`,
 * `4a02f40`). The phone-app's CameraSourceRegistrar / CameraService are
 * intentionally omitted; the watch has no camera.
 */
public class DeviceViewModel(application: Application) : AndroidViewModel(application) {

    public val client: UboClient = UboClient()
    private val settings = UboWearSettings(application)

    public val micCapture: WatchMicCaptureService = WatchMicCaptureService()
    public val audioPlayback: WatchAudioPlaybackService = WatchAudioPlaybackService()

    private val _isMicCapturing = MutableStateFlow(false)
    public val isMicCapturing: StateFlow<Boolean> = _isMicCapturing.asStateFlow()

    public val connectionState: StateFlow<ConnectionState> = client.connectionState
    public val currentView: StateFlow<ViewData?> = client.currentView
    public val statusBar: StateFlow<StatusBarData?> = client.statusBar
    public val systemStats: StateFlow<SystemStats?> = client.systemStats
    public val activeInputs: StateFlow<List<WebUIInputDescription>> = client.activeInputs

    public val savedHost: StateFlow<String> = settings.savedHost
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")
    public val savedPort: StateFlow<Int> = settings.savedPort
        .stateIn(viewModelScope, SharingStarted.Eagerly, UboWearSettings.DEFAULT_PORT)
    public val savedUseTls: StateFlow<Boolean> = settings.savedUseTls
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    public val recentConnections: StateFlow<List<RecentConnection>> = settings.recentConnections
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _discovered = MutableStateFlow<Set<DiscoveredDevice>>(emptySet())
    public val discovered: StateFlow<Set<DiscoveredDevice>> = _discovered.asStateFlow()
    private var discoveryJob: Job? = null

    init {
        // Throttled (5 s) write of SystemStats to the wear-local DataStore
        // that [UboTileService] reads. Tile refresh is pushed after each
        // write so the carousel reflects what the connected app sees.
        viewModelScope.launch {
            var lastPushedAt = 0L
            client.systemStats.collect { stats ->
                val now = System.currentTimeMillis()
                if (now - lastPushedAt < 5_000L) return@collect
                lastPushedAt = now
                val host = settings.savedHost.first()
                WearStatsStore.save(application, stats, host, client.connectionState.value.isConnected)
                runCatching {
                    TileService.getUpdater(application).requestUpdate(UboTileService::class.java)
                }
            }
        }
        viewModelScope.launch {
            client.connectionState.collect {
                val host = settings.savedHost.first()
                WearStatsStore.save(application, client.systemStats.value, host, it.isConnected)
                runCatching {
                    TileService.getUpdater(application).requestUpdate(UboTileService::class.java)
                }
            }
        }

        // Auto-attempt the saved connection once on cold-start, mirroring
        // the phone-app ViewModel. Runs in viewModelScope (not tied to
        // WatchConnectionScreen's composition) so a fast failure doesn't
        // cause repeated auto-connect attempts every time the router
        // remounts that screen — that used to retry in a tight loop,
        // flickering between the connecting and connection screens.
        viewModelScope.launch {
            runCatching { connectWithSavedSettings() }
        }
    }

    public val isConnected: StateFlow<Boolean> = client.connectionState
        .let { source ->
            MutableStateFlow(source.value.isConnected).also { mirror ->
                viewModelScope.launch { source.collect { mirror.value = it.isConnected } }
            }.asStateFlow()
        }

    public suspend fun connect(host: String, port: Int, useTls: Boolean = false) {
        settings.setHost(host)
        settings.setPort(port)
        settings.setUseTls(useTls)
        settings.recordRecentConnection(host, port, useTls)
        client.connect(host, port, useTls)
        client.startViewSubscription()
        client.startStatsSubscription()
        client.startInputsSubscription()
        micCapture.bind(client)
        audioPlayback.bind(client)
        audioPlayback.start()
        startPlaybackForwarding()
        settings.setWasConnected(true)
    }

    public suspend fun disconnect() {
        settings.setWasConnected(false)
        stopMicCapture()
        client.stopPlaybackSubscription()
        audioPlayback.stop()
        client.disconnect()
    }

    public fun triggerConnect(host: String, port: Int, useTls: Boolean = false) {
        viewModelScope.launch { runCatching { connect(host, port, useTls) } }
    }

    public fun triggerDisconnect() {
        viewModelScope.launch { disconnect() }
    }

    public fun startMicCapture() {
        micCapture.start()
        _isMicCapturing.value = micCapture.isRunning
    }

    public fun stopMicCapture() {
        micCapture.stop()
        _isMicCapturing.value = false
    }

    /**
     * Toggle push-to-talk mic capture.
     *
     * [triggerSource] tells the core how the session was triggered so it can
     * pick a turn-completion policy. Left `null` the core applies none and the
     * pipeline falls back to a short silence window; pass a quick-chat wake to
     * have the pod end the turn after its configured silence window instead.
     */
    public suspend fun toggleMicCapture(triggerSource: AssistantTriggerSource? = null) {
        if (micCapture.isRunning) {
            micCapture.stop()
            _isMicCapturing.value = false
            runCatching { client.stopAssistantListening() }
        } else {
            // Same id on the session and every sample, so the core listens to
            // this app's mic and drops the device's built-in mic.
            val source = settings.getOrCreateAudioSourceId()
            runCatching {
                client.startAssistantListening(audioSource = source, source = triggerSource)
            }
            micCapture.start(audioSource = source)
            _isMicCapturing.value = micCapture.isRunning
        }
    }

    private fun startPlaybackForwarding() {
        client.startPlaybackSubscription { event ->
            when (event) {
                is PlaybackEvent.Sample -> audioPlayback.play(event.sample, event.volume)
                is PlaybackEvent.Sequence -> audioPlayback.enqueueSequenceChunk(
                    sequenceId = event.id,
                    index = event.index,
                    sample = event.sample,
                    volume = event.volume,
                )
                PlaybackEvent.Stop -> audioPlayback.stop()
            }
        }
    }

    public fun startDiscovery() {
        discoveryJob?.cancel()
        discoveryJob = viewModelScope.launch {
            UboDiscovery.browse(getApplication()).collect { snapshot ->
                _discovered.value = snapshot
            }
        }
    }

    public fun stopDiscovery() {
        discoveryJob?.cancel()
        discoveryJob = null
        _discovered.value = emptySet()
    }

    public suspend fun connectWithSavedSettings(): Boolean {
        val host = settings.savedHost.first()
        if (host.isEmpty()) return false
        // Only reconnect if the app was actually connected the last time
        // it was closed — a saved host alone isn't consent to silently
        // reconnect underneath the user after they explicitly disconnected.
        if (!settings.wasConnected.first()) return false
        val port = settings.savedPort.first()
        val useTls = settings.savedUseTls.first()
        connect(host, port, useTls)
        return true
    }

    override fun onCleared() {
        super.onCleared()
        micCapture.unbind()
        audioPlayback.unbind()
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
