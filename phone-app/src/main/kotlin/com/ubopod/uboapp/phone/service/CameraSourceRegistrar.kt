package com.ubopod.uboapp.phone.service

import android.os.Build
import com.ubopod.uboapp.phone.storage.UboSettings
import com.ubopod.ubokotlin.UboClient
import com.ubopod.ubokotlin.UboError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Tracks this phone's registration with the Pi-side camera picker.
 *
 * On each [register] call:
 *   1. Load (or lazily generate + persist) a stable UUID via [UboSettings].
 *   2. Assign it to the client's [UboClient.cameraSourceId].
 *   3. Dispatch [UboClient.registerAsCameraSource] so the Pi lists this
 *      phone in the picker.
 *
 * The Pi can ask all known clients to re-advertise themselves by
 * dispatching a `CameraDetectAdvertiseEvent`. [bind] subscribes to
 * [UboClient.cameraDetectAdvertise] and re-registers in response so
 * a freshly-attached Pi sees this phone without the user needing to
 * tap anything on their side.
 *
 * Mirrors the iPhone-side wiring in `DeviceViewModel.swift` /
 * `ContentView.swift`'s `cameraSourceId` initialisation.
 */
public class CameraSourceRegistrar(
    private val settings: UboSettings,
    private val client: UboClient,
    private val scope: CoroutineScope,
    private val deviceLabel: String = defaultDeviceLabel(),
) {
    private var advertiseJob: Job? = null

    /**
     * Subscribe to the device's "detect cameras" event so we re-register
     * whenever the Pi asks. Safe to call repeatedly; cancels any prior
     * subscription. Should be paired with [unbind] on disconnect.
     */
    public fun bind() {
        advertiseJob?.cancel()
        advertiseJob = scope.launch {
            client.cameraDetectAdvertise.collect {
                runCatching { register() }
            }
        }
    }

    public fun unbind() {
        advertiseJob?.cancel()
        advertiseJob = null
    }

    /**
     * Push this phone's source-id + label to the Pi. Idempotent on the
     * client side; the Pi treats the message as upsert by sourceId.
     * Swallows [UboError] (connection dropped between connect and
     * register) so the caller doesn't have to wrap.
     */
    public suspend fun register() {
        val sourceId = settings.getOrCreateCameraSourceId()
        client.cameraSourceId = sourceId
        runCatching {
            client.registerAsCameraSource(sourceId, deviceLabel)
        }.onFailure { t ->
            if (t !is UboError) throw t
        }
    }

    public companion object {
        /**
         * Best-effort human label for the picker entry. Manufacturer +
         * model is what the Pi UI shows alongside USB / picamera entries
         * — keeps the picker readable on a 480x480 panel.
         */
        public fun defaultDeviceLabel(): String {
            val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
            val model = Build.MODEL
            return when {
                manufacturer.isEmpty() && model.isEmpty() -> "Android"
                model.startsWith(manufacturer, ignoreCase = true) -> model
                else -> "$manufacturer $model"
            }
        }
    }
}
