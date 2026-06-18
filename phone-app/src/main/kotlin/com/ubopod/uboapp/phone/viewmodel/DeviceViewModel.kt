package com.ubopod.uboapp.phone.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import com.ubopod.uboapp.phone.service.AudioPlaybackService
import com.ubopod.uboapp.phone.service.CameraService
import com.ubopod.uboapp.phone.service.CameraSourceRegistrar
import com.ubopod.uboapp.phone.service.MicCaptureService
import com.ubopod.uboapp.phone.storage.UboSettings
import com.ubopod.uboapp.widget.SharedSystemStats
import com.ubopod.uboapp.widget.UboWidgetRefreshWorker
import com.ubopod.uboapp.widget.WidgetDataStore
import com.ubopod.ubokotlin.UboClient
import com.ubopod.ubokotlin.connection.ConnectionState
import com.ubopod.ubokotlin.connection.UboDiscovery
import com.ubopod.ubokotlin.connection.DiscoveredDevice
import com.ubopod.ubokotlin.models.StatusBarData
import com.ubopod.ubokotlin.models.SystemStats
import com.ubopod.ubokotlin.models.ViewData
import com.ubopod.ubokotlin.models.WebUIInputDescription
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

/**
 * Central observable state for the phone app.
 *
 * Mirrors `ubo-swift-app/ubo-swift-app/ViewModels/DeviceViewModel.swift`.
 * Owns one [UboClient] for the lifetime of the ViewModel; subscribes the
 * client's [StateFlow] surfaces and re-publishes them so Composables can
 * collect via [collectAsStateWithLifecycle][androidx.lifecycle.compose.collectAsStateWithLifecycle].
 */
public class DeviceViewModel(application: Application) : AndroidViewModel(application) {

    // Don't pass viewModelScope here — UboClient's default scope is
    // SupervisorJob() + Dispatchers.Default, which keeps the long-lived
    // subscription jobs OFF the Main thread. Otherwise the
    // Dispatchers.Main.immediate dispatch on viewModelScope leaks gRPC's
    // synchronous transport-setup work into the UI thread and trips ANR
    // on a slow first-connect. Lifecycle is still bound: onCleared
    // calls client.close() which cancels the default scope.
    public val client: UboClient = UboClient()
    private val settings = UboSettings(application)

    /**
     * Hardware services owned by the ViewModel. They survive across
     * Composition recompositions and are torn down in [onCleared].
     * `MainActivity.onCreate` calls [bindHardwareServices] with itself
     * as the LifecycleOwner so CameraX can bind to a lifecycle.
     */
    public val cameraService: CameraService = CameraService(application)
    public val micCapture: MicCaptureService = MicCaptureService()
    public val audioPlayback: AudioPlaybackService = AudioPlaybackService()
    private val cameraSourceRegistrar: CameraSourceRegistrar =
        CameraSourceRegistrar(settings, client, viewModelScope)

    private val _isMicCapturing = MutableStateFlow(false)
    public val isMicCapturing: StateFlow<Boolean> = _isMicCapturing.asStateFlow()

    public val connectionState: StateFlow<ConnectionState> = client.connectionState
    public val currentView: StateFlow<ViewData?> = client.currentView
    public val statusBar: StateFlow<StatusBarData?> = client.statusBar
    public val systemStats: StateFlow<SystemStats?> = client.systemStats
    public val activeInputs: StateFlow<List<WebUIInputDescription>> = client.activeInputs
    public val isCameraViewfinderActive: StateFlow<Boolean> = client.isCameraViewfinderActive
    public val isConnected: StateFlow<Boolean> = client.connectionState
        .let { state ->
            MutableStateFlow(state.value.isConnected).also { mirror ->
                viewModelScope.launch {
                    state.collect { mirror.value = it.isConnected }
                }
            }.asStateFlow()
        }

    public val savedHost: StateFlow<String> = settings.savedHost
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")
    public val savedPort: StateFlow<Int> = settings.savedPort
        .stateIn(viewModelScope, SharingStarted.Eagerly, UboSettings.DEFAULT_PORT)
    public val hasCompletedOnboarding: StateFlow<Boolean> = settings.hasCompletedOnboarding
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _discovered = MutableStateFlow<Set<DiscoveredDevice>>(emptySet())
    public val discovered: StateFlow<Set<DiscoveredDevice>> = _discovered.asStateFlow()
    private var discoveryJob: Job? = null

    init {
        // Push throttled stats / connection updates to the widget DataStore
        // so the home-screen widget mirrors the in-app state. Cap at 5 s
        // between pushes (matches the Swift port's `lastWidgetUpdate`
        // throttle).
        viewModelScope.launch {
            UboWidgetRefreshWorker.enqueue(application)
            var lastPushedAt = 0L
            client.systemStats.collect { stats ->
                val now = System.currentTimeMillis()
                if (now - lastPushedAt < 5_000L) return@collect
                lastPushedAt = now
                pushWidgetStats(stats)
            }
        }
        viewModelScope.launch {
            client.connectionState.collect {
                pushWidgetStats(client.systemStats.value)
            }
        }

        // Auto-attempt the saved connection once on cold-start. Runs in
        // viewModelScope so the readiness probe survives composition
        // swaps — the ConnectionScreen is allowed to leave the tree
        // (because connectionState flips to CONNECTING) without
        // cancelling the probe in flight.
        viewModelScope.launch {
            runCatching { connectWithSavedSettings() }
        }
    }

    private suspend fun pushWidgetStats(stats: com.ubopod.ubokotlin.models.SystemStats?) {
        val context = getApplication<Application>().applicationContext
        WidgetDataStore.save(
            context,
            SharedSystemStats(
                cpuPercent = stats?.cpuPercent ?: 0f,
                ramPercent = stats?.ramPercent ?: 0f,
                temperature = stats?.temperature,
                isConnected = client.connectionState.value.isConnected,
                deviceHost = settings.savedHost.first(),
                lastUpdatedEpochMs = System.currentTimeMillis(),
            ),
        )
        UboWidgetRefreshWorker.refreshNow(context)
    }

    /**
     * Connect to a device and persist the host/port for future launches.
     * Starts the view subscription on success; the Composables collect
     * `currentView` to render. Errors propagate to [UboClient.lastError].
     */
    public suspend fun connect(host: String, port: Int) {
        settings.setHost(host)
        settings.setPort(port)
        client.connect(host, port)
        client.startViewSubscription()
        client.startStatsSubscription()
        client.startInputsSubscription()
        client.startCameraSubscription()
        // Register as a remote camera source so the Pi's camera picker
        // lists this phone alongside its local USB / picamera entries.
        // bind() also wires re-registration in response to the Pi's
        // "detect cameras" event so a fresh device sees us without the
        // user touching anything on this side.
        cameraSourceRegistrar.bind()
        runCatching { cameraSourceRegistrar.register() }
        micCapture.bind(client)
        audioPlayback.bind(client)
        audioPlayback.start()
        // CameraService.bind needs a LifecycleOwner; that's done by
        // MainActivity via [bindHardwareServices].
        startCameraAutoTrigger()
        startPlaybackForwarding()
    }

    private var cameraAutoJob: Job? = null

    /**
     * Watch [UboClient.isCameraViewfinderActive] and start / stop the
     * local CameraX pipeline accordingly. Mirrors Swift
     * `DeviceViewModel.startCameraObservation()`.
     */
    private fun startCameraAutoTrigger() {
        cameraAutoJob?.cancel()
        cameraAutoJob = viewModelScope.launch {
            var wasActive = false
            client.isCameraViewfinderActive.collect { isActive ->
                if (isActive && !wasActive) {
                    cameraService.start()
                } else if (!isActive && wasActive) {
                    cameraService.stop()
                }
                wasActive = isActive
            }
        }
    }

    /**
     * Route each device-emitted PlaybackEvent through AudioPlaybackService.
     * Uses [UboClient.startPlaybackSubscription] so a transient gRPC
     * failure (e.g. the device drops off the network mid-session)
     * auto-retries via the connection's [ReconnectPolicy] instead of
     * crashing the ViewModel scope.
     */
    private fun startPlaybackForwarding() {
        client.startPlaybackSubscription { event ->
            when (event) {
                is com.ubopod.ubokotlin.models.PlaybackEvent.Sample ->
                    audioPlayback.play(event.sample, event.volume)
                is com.ubopod.ubokotlin.models.PlaybackEvent.Sequence ->
                    audioPlayback.enqueueSequenceChunk(
                        sequenceId = event.id,
                        index = event.index,
                        sample = event.sample,
                        volume = event.volume,
                    )
                com.ubopod.ubokotlin.models.PlaybackEvent.Stop -> audioPlayback.stop()
            }
        }
    }

    public suspend fun disconnect() {
        cameraAutoJob?.cancel()
        cameraAutoJob = null
        cameraSourceRegistrar.unbind()
        client.stopPlaybackSubscription()
        stopMicCapture()
        cameraService.stop()
        audioPlayback.stop()
        client.disconnect()
    }

    /**
     * Fire-and-forget connect launched on [viewModelScope] so the
     * suspending probe survives a composition swap. Composables (e.g.
     * `ConnectionScreen`'s Connect button) should call this rather than
     * dispatching `connect(...)` from a `rememberCoroutineScope` — when
     * `connectionState` flips to `CONNECTING`, `ContentScreen` routes
     * away from `ConnectionScreen` and any local scope it owned would be
     * cancelled mid-probe with `LeftCompositionCancellationException`.
     */
    public fun triggerConnect(host: String, port: Int) {
        viewModelScope.launch {
            runCatching { connect(host, port) }
        }
    }

    /** Same idea, called by the disconnect button on the connected shell. */
    public fun triggerDisconnect() {
        viewModelScope.launch { disconnect() }
    }

    /**
     * Hand the camera service a [androidx.lifecycle.LifecycleOwner] so
     * CameraX can bind its use-cases to a real lifecycle. Called by
     * `MainActivity.onCreate`.
     */
    public fun bindHardwareServices(lifecycleOwner: androidx.lifecycle.LifecycleOwner) {
        cameraService.bind(client, lifecycleOwner)
    }

    public fun startMicCapture() {
        micCapture.start()
        _isMicCapturing.value = micCapture.isRunning
    }

    public fun stopMicCapture() {
        micCapture.stop()
        _isMicCapturing.value = false
    }

    public suspend fun toggleMicCapture() {
        if (micCapture.isRunning) {
            micCapture.stop()
            _isMicCapturing.value = false
            runCatching { client.stopAssistantListening() }
        } else {
            // Same id on the session and every sample, so the core listens to
            // this app's mic and drops the device's built-in mic.
            val source = settings.getOrCreateAudioSourceId()
            runCatching { client.startAssistantListening(audioSource = source) }
            micCapture.start(audioSource = source)
            _isMicCapturing.value = micCapture.isRunning
        }
    }

    public suspend fun connectWithSavedSettings(): Boolean {
        val host = settings.savedHost.first()
        if (host.isEmpty()) return false
        val port = settings.savedPort.first()
        connect(host, port)
        return true
    }

    public suspend fun markOnboardingComplete() {
        settings.markOnboardingComplete()
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

    override fun onCleared() {
        super.onCleared()
        cameraService.unbind()
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
