package com.ubopod.uboapp.wear.service

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
 * Push-to-talk capture for the wear-app — copy of the phone-app's
 * `MicCaptureService` since the watch needs the same AudioRecord
 * pipeline (Swift commit `56d17e1 feat(watch): port MicCaptureService
 * for push-to-talk parity with iOS`).
 *
 * Requires the runtime `RECORD_AUDIO` permission; caller must request it
 * before [start].
 */
public class WatchMicCaptureService {

    private var client: UboClient? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var captureJob: Job? = null
    private var record: AudioRecord? = null

    /** Tags every streamed sample so the core binds the listening session to
     *  this app's mic and ignores the device's built-in mic. Set at [start]. */
    private var audioSource: String = ""

    public val isRunning: Boolean
        get() = captureJob?.isActive == true

    public fun bind(client: UboClient) {
        this.client = client
    }

    public fun unbind() {
        stop()
        client = null
        scope.cancel()
    }

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
            val buffer = ByteArray(CHUNK_FRAMES * 2)
            val startedAt = System.nanoTime()
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
                }.onFailure { t -> if (t !is UboError) throw t }
            }
        }
    }

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
