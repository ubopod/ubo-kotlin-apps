package com.ubopod.uboapp.phone.service

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import com.ubopod.ubokotlin.UboClient
import com.ubopod.ubokotlin.models.AudioSampleData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * `AudioTrack`-backed PCM playback service.
 *
 * Mirrors `ubo-swift-app/ubo-swift-app/Services/AudioPlaybackService.swift`.
 * The Swift port subscribes to `client.playbackEvents()` and routes each
 * one-shot sample / sequence chunk through the system audio output.
 *
 * On the Kotlin side the device-event subscription decoder hasn't landed
 * yet (`subscribeToPlaybackEvents` on `:lib`'s `UboConnection`), so this
 * v1 exposes a manual [play] entry point. Once the subscription is
 * wired, it'll become a private detail of [start] and the API stays the
 * same.
 */
public class AudioPlaybackService {

    @Suppress("unused")
    private var client: UboClient? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var track: AudioTrack? = null

    public fun bind(client: UboClient) {
        this.client = client
    }

    public fun unbind() {
        stop()
        client = null
        scope.cancel()
    }

    /**
     * Open the AudioTrack at [sampleRate] Hz / [channels] channel(s) /
     * 16-bit PCM. Must be called once before the first [play]. Re-call
     * with different parameters re-opens the track.
     */
    public fun start(sampleRate: Int = 16_000, channels: Int = 1) {
        stop()
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
        @Suppress("DEPRECATION") // streamType for compat with API 31 minSdk
        track = AudioTrack(
            attrs,
            format,
            bufferSize.coerceAtLeast(MIN_BUFFER_BYTES),
            AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE,
        ).also { it.play() }
    }

    /** Push a single PCM sample at the device's nominal volume. */
    public fun play(sample: AudioSampleData, volume: Float = 1f) {
        val current = track ?: run {
            start(sample.rate, sample.channels)
            track ?: return
        }
        if (current.sampleRate != sample.rate) {
            start(sample.rate, sample.channels)
        }
        val live = track ?: return
        live.setVolume(volume.coerceIn(0f, 1f))
        scope.launch {
            withContext(Dispatchers.IO) {
                live.write(sample.data, 0, sample.data.size)
            }
        }
    }

    /** Stop playback and release the track. Safe to call multiple times. */
    public fun stop() {
        track?.let {
            runCatching { it.pause() }
            runCatching { it.flush() }
            runCatching { it.release() }
        }
        track = null
    }

    private companion object {
        private const val MIN_BUFFER_BYTES = 4_096
    }
}
