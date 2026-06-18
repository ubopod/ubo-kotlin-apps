package com.ubopod.uboapp.phone.service

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.ubopod.ubokotlin.UboClient
import com.ubopod.ubokotlin.UboError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Streams 16 kHz mono PCM16 audio from the device's mic to the connected
 * Ubo over gRPC.
 *
 * Mirrors `ubo-swift-app/ubo-swift-app/Services/MicCaptureService.swift`.
 * Each captured buffer is forwarded via `client.reportAudioSample(...)`,
 * which dispatches `AudioReportSampleAction` (the same path the Web UI's
 * native input takes for the assistant pipeline).
 *
 * Requires the runtime `RECORD_AUDIO` permission. The caller is
 * responsible for requesting it before invoking [start]; without it
 * `AudioRecord` will throw on construction.
 */
public class MicCaptureService {

    private var client: UboClient? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var captureJob: Job? = null
    private var record: AudioRecord? = null

    /** Tags every streamed sample so the core binds the listening session to
     *  this app's mic and ignores the device's built-in mic. Set at [start]. */
    private var audioSource: String = ""

    public val isRunning: Boolean
        get() = captureJob?.isActive == true

    /** Configure the service. Idempotent; safe to call repeatedly. */
    public fun bind(client: UboClient) {
        this.client = client
    }

    /** Tear down. After this no [start] / [stop] will work. */
    public fun unbind() {
        stop()
        client = null
        scope.cancel()
    }

    /**
     * Start capturing. No-op if already running. Must be called from a
     * scope where the `RECORD_AUDIO` permission is granted.
     */
    @SuppressLint("MissingPermission")
    public fun start(audioSource: String = "") {
        if (isRunning) return
        val client = this.client ?: return
        this.audioSource = audioSource

        val sampleRate = SAMPLE_RATE_HZ
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val encoding = AudioFormat.ENCODING_PCM_16BIT
        val minBuffer = AudioRecord.getMinBufferSize(sampleRate, channelConfig, encoding)
        if (minBuffer == AudioRecord.ERROR || minBuffer == AudioRecord.ERROR_BAD_VALUE) return

        // Buffer twice the minimum so we don't drop frames under load. Each
        // emission still carries CHUNK_FRAMES samples (≈16 ms at 16 kHz).
        val bufferSize = (minBuffer * 2).coerceAtLeast(CHUNK_FRAMES * 2)
        val ar = try {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                sampleRate,
                channelConfig,
                encoding,
                bufferSize,
            )
        } catch (_: SecurityException) {
            return
        } catch (_: IllegalArgumentException) {
            return
        }
        if (ar.state != AudioRecord.STATE_INITIALIZED) {
            ar.release()
            return
        }
        record = ar
        ar.startRecording()

        captureJob = scope.launch {
            val buffer = ByteArray(CHUNK_FRAMES * 2) // PCM16 → 2 bytes per frame
            var startedAt = System.nanoTime()
            while (isActive) {
                val read = withContext(Dispatchers.IO) { ar.read(buffer, 0, buffer.size) }
                if (read <= 0) continue
                val timestamp = (System.nanoTime() - startedAt).toFloat() / 1_000_000_000f
                val payload = if (read == buffer.size) buffer.copyOf() else buffer.copyOf(read)
                runCatching {
                    client.reportAudioSample(
                        timestamp = timestamp,
                        data = payload,
                        channels = 1,
                        rate = sampleRate,
                        width = 2,
                        audioSource = audioSource,
                    )
                }.onFailure { throwable ->
                    if (throwable !is UboError) throw throwable
                }
            }
        }
    }

    /** Stop capturing. Safe to call multiple times. */
    public fun stop() {
        captureJob?.cancel()
        captureJob = null
        record?.let {
            runCatching { it.stop() }
            it.release()
        }
        record = null
    }

    public companion object {
        private const val SAMPLE_RATE_HZ = 16_000
        private const val CHUNK_FRAMES = 256
    }
}
