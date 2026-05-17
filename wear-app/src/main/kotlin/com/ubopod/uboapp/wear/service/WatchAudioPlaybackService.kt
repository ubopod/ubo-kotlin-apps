package com.ubopod.uboapp.wear.service

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import com.ubopod.ubokotlin.UboClient
import com.ubopod.ubokotlin.models.AudioSampleData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/**
 * Wear counterpart of the phone-app's `AudioPlaybackService`. Same
 * single-thread-dispatcher pattern: all AudioTrack mutations
 * (open / write / release) serialise so cross-thread writes can't race
 * against a release and crash natively. See the phone-app docstring
 * for the full rationale.
 */
public class WatchAudioPlaybackService {

    @Suppress("unused")
    private var client: UboClient? = null
    private val trackDispatcher = Executors.newSingleThreadExecutor { r ->
        Thread(r, "UboWatchAudioPlayback")
    }.asCoroutineDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + trackDispatcher)
    private var track: AudioTrack? = null

    public fun bind(client: UboClient) {
        this.client = client
    }

    public fun unbind() {
        stop()
        client = null
        scope.cancel()
        trackDispatcher.close()
    }

    public fun start(sampleRate: Int = 16_000, channels: Int = 1) {
        scope.launch { openLocked(sampleRate, channels) }
    }

    public fun play(sample: AudioSampleData, volume: Float = 1f) {
        scope.launch {
            val current = track
            if (current == null || current.sampleRate != sample.rate) {
                openLocked(sample.rate, sample.channels)
            }
            val live = track ?: return@launch
            try {
                live.setVolume(volume.coerceIn(0f, 1f))
                live.write(sample.data, 0, sample.data.size)
            } catch (_: IllegalStateException) {
                // Track was released or reconfigured mid-call — drop
                // this sample, the next one will reopen the track.
            }
        }
    }

    public fun enqueueSequenceChunk(
        sequenceId: String,
        index: Int,
        sample: AudioSampleData?,
        volume: Float,
    ) {
        synchronized(sequenceLock) {
            val state = sequenceStates.getOrPut(sequenceId) { SequenceState() }
            if (sample != null) state.pending[index] = SequenceChunk(sample, volume)
            while (true) {
                val next = state.pending.remove(state.nextIndex) ?: break
                play(next.sample, next.volume)
                state.nextIndex++
            }
        }
    }

    public fun stop() {
        scope.launch { closeLocked() }
        synchronized(sequenceLock) { sequenceStates.clear() }
    }

    // ---- private helpers, all invoked on [trackDispatcher] ----

    private fun openLocked(sampleRate: Int, channels: Int) {
        closeLocked()
        val channelConfig = if (channels == 2) AudioFormat.CHANNEL_OUT_STEREO
                            else AudioFormat.CHANNEL_OUT_MONO
        val bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, AudioFormat.ENCODING_PCM_16BIT)
        if (bufferSize <= 0) return
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        val format = AudioFormat.Builder()
            .setSampleRate(sampleRate)
            .setChannelMask(channelConfig)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .build()
        track = runCatching {
            AudioTrack(
                attrs,
                format,
                bufferSize.coerceAtLeast(MIN_BUFFER_BYTES),
                AudioTrack.MODE_STREAM,
                AudioManager.AUDIO_SESSION_ID_GENERATE,
            ).also { it.play() }
        }.getOrNull()
    }

    private fun closeLocked() {
        track?.let {
            runCatching { it.pause() }
            runCatching { it.flush() }
            runCatching { it.release() }
        }
        track = null
    }

    private data class SequenceChunk(val sample: AudioSampleData, val volume: Float)
    private class SequenceState(
        val pending: MutableMap<Int, SequenceChunk> = mutableMapOf(),
        var nextIndex: Int = 0,
    )

    private val sequenceLock = Any()
    private val sequenceStates: MutableMap<String, SequenceState> = mutableMapOf()

    private companion object {
        private const val MIN_BUFFER_BYTES = 4_096
    }
}
