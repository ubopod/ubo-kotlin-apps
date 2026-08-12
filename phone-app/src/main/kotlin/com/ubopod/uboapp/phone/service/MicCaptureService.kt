package com.ubopod.uboapp.phone.service

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import com.ubopod.ubokotlin.UboClient
import com.ubopod.ubokotlin.UboError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
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
    private var sendJob: Job? = null
    private var record: AudioRecord? = null

    private data class QueuedSample(val timestamp: Float, val data: ByteArray, val rate: Int, val audioSource: String)

    // Unbounded on purpose: AudioRecord's hardware ring buffer is small, and
    // if read() isn't called promptly it silently overwrites unread audio —
    // a dropout, not a delay. The capture loop below must never block on the
    // network, so sends are handed off here and drained by a separate
    // coroutine, mirroring AudioPlaybackService's outbound decoupling.
    private var sampleChannel: Channel<QueuedSample>? = null

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
        if (isRunning) { Log.i(TAG, "start ignored — already running"); return }
        val client = this.client ?: run { Log.e(TAG, "start aborted — no client bound"); return }
        this.audioSource = audioSource
        Log.i(TAG, "start requested (audioSource=$audioSource)")

        val sampleRate = SAMPLE_RATE_HZ
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val encoding = AudioFormat.ENCODING_PCM_16BIT
        val minBuffer = AudioRecord.getMinBufferSize(sampleRate, channelConfig, encoding)
        if (minBuffer == AudioRecord.ERROR || minBuffer == AudioRecord.ERROR_BAD_VALUE) {
            Log.e(TAG, "start aborted — getMinBufferSize returned $minBuffer")
            return
        }

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
        } catch (e: SecurityException) {
            Log.e(TAG, "start aborted — AudioRecord SecurityException (mic permission?)", e)
            return
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "start aborted — AudioRecord IllegalArgumentException", e)
            return
        }
        if (ar.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "start aborted — AudioRecord not initialized (state=${ar.state})")
            ar.release()
            return
        }
        record = ar
        ar.startRecording()
        Log.i(TAG, "AudioRecord recording (minBuffer=$minBuffer, recordingState=${ar.recordingState})")

        val channel = Channel<QueuedSample>(capacity = Channel.UNLIMITED)
        sampleChannel = channel

        sendJob = scope.launch {
            var sent = 0
            for (item in channel) {
                runCatching {
                    client.reportAudioSample(
                        timestamp = item.timestamp,
                        data = item.data,
                        channels = 1,
                        rate = item.rate,
                        width = 2,
                        audioSource = item.audioSource,
                    )
                    sent++
                    if (sent == 1) Log.i(TAG, "streaming to core (src=${item.audioSource})")
                }.onFailure { throwable ->
                    // Cancellation on stop() surfaces as a dispatch failure
                    // mid-send — that's expected, not an error.
                    if (!isActive) return@onFailure
                    Log.e(TAG, "reportAudioSample failed (sent=$sent): ${throwable.message}", throwable)
                    if (throwable !is UboError) throw throwable
                }
            }
        }

        captureJob = scope.launch {
            val buffer = ByteArray(CHUNK_FRAMES * 2) // PCM16 → 2 bytes per frame
            val startedAt = System.nanoTime()
            var reads = 0
            var silentReads = 0
            while (isActive) {
                val read = withContext(Dispatchers.IO) { ar.read(buffer, 0, buffer.size) }
                if (read <= 0) {
                    silentReads++
                    if (silentReads == 1 || silentReads % 100 == 0) {
                        Log.w(TAG, "ar.read returned $read (silentReads=$silentReads) — no audio from mic")
                    }
                    continue
                }
                reads++
                val timestamp = (System.nanoTime() - startedAt).toFloat() / 1_000_000_000f
                val payload = if (read == buffer.size) buffer.copyOf() else buffer.copyOf(read)
                channel.trySend(QueuedSample(timestamp, payload, sampleRate, audioSource))
            }
            Log.i(TAG, "capture stopped (reads=$reads, silentReads=$silentReads)")
        }
    }

    /** Stop capturing. Safe to call multiple times. */
    public fun stop() {
        captureJob?.cancel()
        captureJob = null
        sendJob?.cancel()
        sendJob = null
        sampleChannel?.close()
        sampleChannel = null
        record?.let {
            runCatching { it.stop() }
            it.release()
        }
        record = null
    }

    public companion object {
        private const val TAG = "MicCapture"
        private const val SAMPLE_RATE_HZ = 16_000
        private const val CHUNK_FRAMES = 256
    }
}
