package com.ubopod.uboapp.phone.service

import android.content.Context
import androidx.annotation.MainThread
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.LifecycleOwner
import com.ubopod.ubokotlin.UboClient
import com.ubopod.ubokotlin.UboError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * CameraX-based service that ships preview frames to the Ubo device.
 *
 * Mirrors `ubo-swift-app/ubo-swift-app/Services/CameraManager.swift` and
 * `CameraCaptureService.swift`. Each ImageAnalysis frame is encoded as
 * JPEG and dispatched as a `CameraReportImageAction` over gRPC; the
 * client tags it with `cameraSourceId` so the Pi can route or drop the
 * frame depending on which source is currently selected in its picker.
 *
 * Lifecycle:
 *   1. Wire `client` and a `LifecycleOwner` via [bind] (called from the
 *      Activity / ComponentActivity that owns the camera UI).
 *   2. Call [start] to begin capture — typically gated on
 *      `client.isCameraViewfinderActive`, which the [com.ubopod.uboapp.phone.viewmodel.DeviceViewModel]'s
 *      auto-trigger watches.
 *   3. Call [stop] to release the camera. [unbind] tears down everything.
 *
 * The CameraX use case here is **ImageAnalysis only** (no preview
 * surface). If a future feature wants a local viewfinder, add a Preview
 * use case alongside.
 */
public class CameraService(private val context: Context) {

    /** Which camera lens to bind. Switching while [running] re-binds. */
    public enum class Lens { BACK, FRONT }

    private var client: UboClient? = null
    private var lifecycleOwner: LifecycleOwner? = null
    private val analyzerExecutor: Executor = Executors.newSingleThreadExecutor()
    private val scope = CoroutineScope(SupervisorJob())

    @Volatile
    private var running: Boolean = false

    private val _lens = MutableStateFlow(Lens.BACK)
    public val lens: StateFlow<Lens> = _lens.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)

    /**
     * Last user-facing error from camera setup (permission denied, no
     * camera available, bindToLifecycle failure). Null when last
     * [start] succeeded. Observed by the dashboard / camera UI to
     * surface a banner without crashing the ViewModel scope.
     */
    public val lastError: StateFlow<String?> = _lastError.asStateFlow()

    /**
     * Configure the service. Call before [start]; safe to call again to
     * swap clients (e.g. on a fresh connect). [lifecycleOwner] must
     * outlive any [start] / [stop] window — typically the hosting
     * Activity.
     */
    @MainThread
    public fun bind(client: UboClient, lifecycleOwner: LifecycleOwner) {
        this.client = client
        this.lifecycleOwner = lifecycleOwner
    }

    /** Tear down the service. After this no [start] / [stop] will work. */
    @MainThread
    public fun unbind() {
        stop()
        this.client = null
        this.lifecycleOwner = null
        scope.cancel()
    }

    /**
     * Switch to a different lens. If a capture session is currently
     * [running], rebinds CameraX so the new lens takes over without
     * the caller having to stop / start manually. Otherwise just
     * records the preference for the next [start].
     */
    @MainThread
    public fun setLens(lens: Lens) {
        if (_lens.value == lens) return
        _lens.value = lens
        if (running) {
            stop()
            start()
        }
    }

    /** Begin streaming frames. No-op if already running or unbound. */
    @MainThread
    public fun start() {
        val owner = lifecycleOwner ?: return
        if (running) return
        running = true
        _lastError.value = null
        scope.launch {
            val provider = runCatching { ProcessCameraProvider.getInstance(context).await() }
                .getOrElse { t ->
                    running = false
                    _lastError.value = t.message ?: "Could not acquire camera provider"
                    return@launch
                }
            val selector = when (_lens.value) {
                Lens.BACK -> CameraSelector.DEFAULT_BACK_CAMERA
                Lens.FRONT -> CameraSelector.DEFAULT_FRONT_CAMERA
            }
            if (!provider.hasCamera(selector)) {
                running = false
                _lastError.value = "Selected camera (${_lens.value}) is unavailable on this device."
                return@launch
            }
            val resolution = ResolutionSelector.Builder()
                .setResolutionStrategy(
                    ResolutionStrategy(
                        android.util.Size(640, 480),
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                    ),
                )
                .build()
            val analysis = ImageAnalysis.Builder()
                .setResolutionSelector(resolution)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .build()
                .also { it.setAnalyzer(analyzerExecutor, ::onFrame) }

            try {
                provider.unbindAll()
                provider.bindToLifecycle(owner, selector, analysis)
            } catch (t: Throwable) {
                running = false
                _lastError.value = t.message ?: "Camera bindToLifecycle failed"
            }
        }
    }

    /** Stop streaming. Safe to call multiple times. */
    @MainThread
    public fun stop() {
        if (!running) return
        running = false
        scope.launch {
            runCatching {
                ProcessCameraProvider.getInstance(context).await().unbindAll()
            }
        }
    }

    private fun onFrame(image: ImageProxy) {
        val client = this.client
        if (client == null) {
            image.close()
            return
        }
        try {
            val jpegBytes = encodeYuv420ToJpeg(image, quality = 70) ?: return
            scope.launch {
                runCatching {
                    client.sendCameraFrame(
                        data = jpegBytes,
                        width = image.width,
                        height = image.height,
                        timestamp = image.imageInfo.timestamp.toFloat() / 1_000_000f,
                    )
                }.onFailure { throwable ->
                    // Connection dropped between frames is expected; swallow
                    // UboError, surface anything genuinely unexpected.
                    if (throwable !is UboError) throw throwable
                }
            }
        } finally {
            image.close()
        }
    }

    private fun encodeYuv420ToJpeg(image: ImageProxy, quality: Int): ByteArray? {
        // Convert YUV_420_888 → NV21 → JPEG via YuvImage. ImageAnalysis with
        // OUTPUT_IMAGE_FORMAT_YUV_420_888 gives us three planes; copy them
        // into a single NV21 byte array for the encoder.
        val yPlane = image.planes[0].buffer
        val uPlane = image.planes[1].buffer
        val vPlane = image.planes[2].buffer
        val ySize = yPlane.remaining()
        val uSize = uPlane.remaining()
        val vSize = vPlane.remaining()
        val nv21 = ByteArray(ySize + uSize + vSize)
        yPlane.get(nv21, 0, ySize)
        // NV21: Y plane, then VU interleaved. Many devices already provide
        // semi-planar VU; copy V then U in linear blocks as a safe fallback.
        vPlane.get(nv21, ySize, vSize)
        uPlane.get(nv21, ySize + vSize, uSize)
        val yuv = android.graphics.YuvImage(
            nv21,
            android.graphics.ImageFormat.NV21,
            image.width,
            image.height,
            null,
        )
        val out = ByteArrayOutputStream()
        return if (yuv.compressToJpeg(android.graphics.Rect(0, 0, image.width, image.height), quality, out)) {
            out.toByteArray()
        } else {
            null
        }
    }
}
