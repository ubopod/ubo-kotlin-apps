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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * CameraX-based service that ships preview frames to the Ubo device.
 *
 * Mirrors `ubo-swift-app/ubo-swift-app/Services/CameraManager.swift` and
 * `CameraCaptureService.swift`. Each ImageAnalysis frame is converted to
 * packed RGB888 (the Pi validates the payload against `width * height *
 * 3` — a compressed format doesn't match) and dispatched as a
 * `CameraReportImageAction` over gRPC; the client tags it with
 * `cameraSourceId` so the Pi can route or drop the frame depending on
 * which source is currently selected in its picker.
 *
 * Frame coalescing: only the latest converted frame is kept in
 * [pendingFrame], guarded by [frameLock]; a single dispatch loop sends it
 * and waits for that send to complete before picking up the next one.
 * Firing an unbounded `launch` per analyzed frame let sends queue up
 * faster than the network could drain them, each holding a ~900KB RGB
 * buffer alive — an OOM within seconds. Mirrors Swift's `pendingFrame` /
 * `startDispatchLoop` pattern in `CameraManager.swift`.
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

    private data class PendingFrame(val data: ByteArray, val width: Int, val height: Int, val timestamp: Float)

    private val frameLock = Any()
    private var pendingFrame: PendingFrame? = null
    private var dispatchJob: Job? = null

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
        startDispatchLoop()
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

            // CameraX's unbindAll()/bindToLifecycle() require the main
            // thread; `scope` has no dispatcher (runs on Dispatchers.Default),
            // so this must switch explicitly rather than inherit.
            withContext(Dispatchers.Main) {
                try {
                    provider.unbindAll()
                    provider.bindToLifecycle(owner, selector, analysis)
                } catch (t: Throwable) {
                    running = false
                    _lastError.value = t.message ?: "Camera bindToLifecycle failed"
                }
            }
        }
    }

    /** Stop streaming. Safe to call multiple times. */
    @MainThread
    public fun stop() {
        if (!running) return
        running = false
        dispatchJob?.cancel()
        dispatchJob = null
        synchronized(frameLock) { pendingFrame = null }
        scope.launch {
            val provider = runCatching { ProcessCameraProvider.getInstance(context).await() }
                .getOrNull() ?: return@launch
            withContext(Dispatchers.Main) {
                runCatching { provider.unbindAll() }
            }
        }
    }

    /**
     * Send [pendingFrame] and wait for that send to finish before picking
     * up the next one, so at most one frame is ever in flight — see the
     * frame-coalescing note on the class doc comment.
     */
    private fun startDispatchLoop() {
        dispatchJob?.cancel()
        dispatchJob = scope.launch {
            while (isActive) {
                val frame = synchronized(frameLock) {
                    val f = pendingFrame
                    pendingFrame = null
                    f
                }
                val client = this@CameraService.client
                if (frame == null || client == null) {
                    delay(10)
                    continue
                }
                runCatching {
                    client.sendCameraFrame(
                        data = frame.data,
                        width = frame.width,
                        height = frame.height,
                        timestamp = frame.timestamp,
                    )
                }.onFailure { throwable ->
                    // Connection dropped between frames is expected; swallow
                    // UboError, surface anything genuinely unexpected.
                    if (throwable !is UboError) throw throwable
                }
            }
        }
    }

    private fun onFrame(image: ImageProxy) {
        try {
            val (rgbBytes, outWidth, outHeight) = yuv420ToRotatedRgb(image)
            synchronized(frameLock) {
                pendingFrame = PendingFrame(
                    data = rgbBytes,
                    width = outWidth,
                    height = outHeight,
                    timestamp = image.imageInfo.timestamp.toFloat() / 1_000_000f,
                )
            }
        } finally {
            image.close()
        }
    }

    private data class RotatedRgb(val data: ByteArray, val width: Int, val height: Int)

    /**
     * Convert a YUV_420_888 [ImageProxy] to packed RGB888 (row-major,
     * R-G-B per pixel), rotated by `imageInfo.rotationDegrees` so the
     * output matches the device's display orientation. ImageAnalysis
     * delivers frames in the sensor's native orientation — CameraX only
     * auto-rotates the Preview use case, not ImageAnalysis — so without
     * this the image comes out sideways (e.g. 90° off in portrait, since
     * the back sensor is mounted landscape). The Pi validates the payload
     * against exactly `width * height * 3` for whatever width/height is
     * sent, so the *output* (rotated) dimensions are what's reported.
     */
    private fun yuv420ToRotatedRgb(image: ImageProxy): RotatedRgb {
        val width = image.width
        val height = image.height
        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]
        val yBuffer = yPlane.buffer
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer
        val yRowStride = yPlane.rowStride
        val yPixelStride = yPlane.pixelStride
        val uRowStride = uPlane.rowStride
        val uPixelStride = uPlane.pixelStride
        val vRowStride = vPlane.rowStride
        val vPixelStride = vPlane.pixelStride

        val rotation = image.imageInfo.rotationDegrees
        val outWidth = if (rotation == 90 || rotation == 270) height else width
        val outHeight = if (rotation == 90 || rotation == 270) width else height
        val rgb = ByteArray(outWidth * outHeight * 3)

        for (row in 0 until height) {
            val yRowOffset = row * yRowStride
            val uvRow = row / 2
            val uRowOffset = uvRow * uRowStride
            val vRowOffset = uvRow * vRowStride
            for (col in 0 until width) {
                val y = yBuffer.get(yRowOffset + col * yPixelStride).toInt() and 0xFF
                val uvCol = col / 2
                val u = (uBuffer.get(uRowOffset + uvCol * uPixelStride).toInt() and 0xFF) - 128
                val v = (vBuffer.get(vRowOffset + uvCol * vPixelStride).toInt() and 0xFF) - 128

                val r = (y + 1.402f * v).toInt().coerceIn(0, 255)
                val g = (y - 0.344136f * u - 0.714136f * v).toInt().coerceIn(0, 255)
                val b = (y + 1.772f * u).toInt().coerceIn(0, 255)

                val (outRow, outCol) = when (rotation) {
                    90 -> col to (height - 1 - row)
                    180 -> (height - 1 - row) to (width - 1 - col)
                    270 -> (width - 1 - col) to row
                    else -> row to col
                }
                val outIndex = (outRow * outWidth + outCol) * 3
                rgb[outIndex] = r.toByte()
                rgb[outIndex + 1] = g.toByte()
                rgb[outIndex + 2] = b.toByte()
            }
        }
        return RotatedRgb(rgb, outWidth, outHeight)
    }
}
