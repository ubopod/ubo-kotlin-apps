package com.ubopod.uboapp.phone.service

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.SystemClock
import com.ubopod.ubokotlin.UboClient
import com.ubopod.ubokotlin.models.AudioSampleData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/**
 * `AudioTrack`-backed PCM playback service.
 *
 * Mirrors `ubo-swift-app/ubo-swift-app/Services/AudioPlaybackService.swift`.
 * Routes the Pi's playback event stream — one-shot samples and ordered
 * sequence chunks — through the system audio output.
 *
 * Thread safety: every AudioTrack mutation ([start], the actual write,
 * [stop]) runs on a single-thread coroutine dispatcher so writes cannot
 * race against a release. AudioTrack write/release across threads is
 * undefined and crashes natively with SIGSEGV when it loses, so the
 * single-thread serialisation is load-bearing — see the in-line comment
 * on [trackDispatcher].
 *
 * Why [play] must never block the gRPC event collector: the core's
 * per-subscription event queue
 * (`ubo_app/rpc/store_service.py` `_make_queue_event`) holds only 30
 * pending events and, on overflow, *silently drops the event* rather than
 * blocking — "a dropped `AudioPlayAudioSequenceEvent` is a hole in the
 * middle of spoken audio", per that file's own comment. At the observed
 * burst rate (~19 chunks/s), that queue fills in under two seconds. Any
 * client that pauses its network reads for that long — for any reason —
 * loses chunks server-side, permanently; there is no retransmission.
 *
 * An earlier version of this file made [play] suspend on the actual
 * blocking `AudioTrack.write()`, reasoning (wrongly, for this server) that
 * the gRPC collector should feel backpressure from real-time playback
 * pacing. That starved the core's 30-slot queue during any response
 * longer than a couple of seconds, which explains both the chunk loss
 * this file used to work around with a reorder-buffer skip-ahead timeout
 * *and* a harder failure: long stretches with no network reads at all
 * eventually killed the connection outright (observed both as a bare
 * "UNAVAILABLE: End of stream" after ~150s, and — after a keepAliveTime
 * experiment that made it worse and was reverted — as "Keepalive failed"
 * when the core couldn't even answer a ping in time).
 *
 * iOS/the Web UI never hit this because they drain their event stream as
 * fast as it arrives, unconditionally, and buffer entirely on their own
 * side (`AVAudioPlayerNode.scheduleBuffer` is non-blocking; the Web
 * Audio API queues similarly) — network consumption speed and playback
 * pacing are completely decoupled. [playbackChannel] reproduces that:
 * effectively unbounded, so [play] never suspends the caller (the gRPC
 * collector always immediately drains the core's queue, exactly like the
 * other clients), while [player] is the only place that ever calls the
 * actual blocking `AudioTrack.write()`, entirely decoupled from how fast
 * chunks arrive.
 */
public class AudioPlaybackService {

    @Suppress("unused")
    private var client: UboClient? = null

    // All track-touching coroutines run here so start / write / stop
    // serialise via the executor's FIFO queue. Multi-threaded access to
    // AudioTrack is unsafe in native land — Android writes the buffer
    // pointer, then a release frees the buffer, and a concurrent write
    // dereferences the freed pointer. Single-thread closes the race.
    private val trackDispatcher = Executors.newSingleThreadExecutor { r ->
        Thread(r, "UboAudioPlayback")
    }.asCoroutineDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + trackDispatcher)
    private var track: AudioTrack? = null

    private data class QueuedChunk(val sample: AudioSampleData, val volume: Float)

    // Unbounded on purpose — see the class doc. `send()` must never
    // suspend the gRPC event collector, or the core's 30-slot server-side
    // queue overflows and starts dropping chunks within ~2 seconds. One
    // response's worth of PCM is at most a few MB, trivial to hold
    // in-memory; iOS's `scheduleBuffer` queue is equally unbounded.
    private val playbackChannel = Channel<QueuedChunk>(capacity = Channel.UNLIMITED)

    // The only coroutine that ever touches `track` for writing — drains
    // playbackChannel and performs the actual blocking write(), entirely
    // decoupled from how fast chunks arrive over the network.
    private val player = scope.launch {
        for (chunk in playbackChannel) {
            writeLocked(chunk.sample, chunk.volume)
        }
    }

    public fun bind(client: UboClient) {
        this.client = client
    }

    public fun unbind() {
        stop()
        client = null
        scope.cancel()
        trackDispatcher.close()
    }

    /**
     * Open the AudioTrack at [sampleRate] Hz / [channels] channel(s) /
     * 16-bit PCM. Must be called once before the first [play]. Re-call
     * with different parameters re-opens the track.
     *
     * Fire-and-forget — the actual open runs on [trackDispatcher]. A
     * one-off pre-warm before any real audio has arrived, not part of the
     * steady-state playback path.
     */
    public fun start(sampleRate: Int = 16_000, channels: Int = 1) {
        scope.launch { openLocked(sampleRate, channels) }
    }

    /**
     * Push a single PCM sample at the device's nominal volume. Never
     * suspends in practice — [playbackChannel] is unbounded — so the
     * caller (ultimately the gRPC event collector) always proceeds
     * immediately to the next event. See the class doc for why that's
     * load-bearing, not just an optimization.
     */
    public suspend fun play(sample: AudioSampleData, volume: Float = 1f) {
        playbackChannel.send(QueuedChunk(sample, volume))
    }

    /**
     * Enqueue a chunk of a multi-part audio sequence (TTS, file playback).
     *
     * The Pi may dispatch sequence chunks out-of-order; this buffer keeps
     * them keyed by `(sequenceId, index)` so they replay in the order
     * the device intended, regardless of network arrival order. Once the
     * next-expected index is present the buffer is drained as far as it
     * can be, then idle slots wait for the missing chunk. Calling [stop]
     * (or receiving a `PlaybackEvent.Stop`) flushes every pending slot
     * so a fresh sequence doesn't replay stale data.
     *
     * A chunk can also go missing entirely rather than merely arriving
     * late (observed in the field: index N never arrives at all, while
     * indices well beyond N keep coming). Waiting on it forever would
     * silently drop every later chunk too, since they all sit in
     * `pending` behind a `nextIndex` that can never advance — the
     * response plays normally up to the gap, then goes silent for good.
     * If we've been stuck on the same index longer than reordering could
     * plausibly still resolve, give up on it and resume from the next
     * chunk we do have: one skipped ~85ms glitch instead of losing the
     * rest of the response.
     *
     * The bookkeeping (mutating `pending`/`nextIndex`) stays inside the
     * `synchronized` block since `synchronized` isn't suspend-aware; the
     * actual (non-blocking in practice) enqueue happens in a second pass
     * over the chunks that block determined were ready.
     *
     * Mirrors Swift `AudioPlaybackService.handleSequenceChunk(...)`.
     */
    public suspend fun enqueueSequenceChunk(
        sequenceId: String,
        index: Int,
        sample: AudioSampleData?,
        volume: Float,
    ) {
        val ready = synchronized(sequenceLock) {
            val state = sequenceStates.getOrPut(sequenceId) { SequenceState() }
            if (sample != null) state.pending[index] = SequenceChunk(sample, volume)

            if (!state.pending.containsKey(state.nextIndex) && state.pending.isNotEmpty()) {
                val stuckSince = state.stuckSinceMs
                if (stuckSince == null) {
                    state.stuckSinceMs = SystemClock.elapsedRealtime()
                } else if (SystemClock.elapsedRealtime() - stuckSince > SKIP_AHEAD_TIMEOUT_MS) {
                    val resumeAt = state.pending.keys.filter { it > state.nextIndex }.minOrNull()
                    if (resumeAt != null) {
                        state.pending.keys.filter { it < resumeAt }.forEach(state.pending::remove)
                        state.nextIndex = resumeAt
                    }
                    state.stuckSinceMs = null
                }
            }

            val drained = mutableListOf<SequenceChunk>()
            while (true) {
                val next = state.pending.remove(state.nextIndex) ?: break
                drained.add(next)
                state.nextIndex++
                state.stuckSinceMs = null
            }
            drained
        }
        for (chunk in ready) {
            play(chunk.sample, chunk.volume)
        }
    }

    /** Stop playback and release the track. Safe to call multiple times. */
    public fun stop() {
        // Drop anything already queued so a fresh sequence after an
        // interrupt doesn't play stale backlog.
        while (playbackChannel.tryReceive().isSuccess) { /* discard */ }
        scope.launch { closeLocked() }
        synchronized(sequenceLock) { sequenceStates.clear() }
    }

    // ---- private helpers, all invoked on [trackDispatcher] ----

    private fun writeLocked(sample: AudioSampleData, volume: Float) {
        val current = track
        if (current == null || current.sampleRate != sample.rate) {
            openLocked(sample.rate, sample.channels)
        }
        val live = track ?: return
        try {
            live.setVolume(volume.coerceIn(0f, 1f))
            // Genuinely blocks until the buffer has room. This is the
            // *only* place that happens — draining playbackChannel, never
            // the network-facing play()/enqueueSequenceChunk() callers.
            live.write(sample.data, 0, sample.data.size)
        } catch (_: IllegalStateException) {
            // Track was released or reconfigured between our null check
            // and the write — drop the sample, the next one will reopen
            // via the rate-mismatch path above.
        }
    }

    private fun openLocked(sampleRate: Int, channels: Int) {
        closeLocked()
        val channelConfig = if (channels == 2) AudioFormat.CHANNEL_OUT_STEREO
                            else AudioFormat.CHANNEL_OUT_MONO
        val bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, AudioFormat.ENCODING_PCM_16BIT)
        if (bufferSize <= 0) return
        // getMinBufferSize() returns the minimum survivable buffer (~40-80ms
        // at typical rates) — enough to avoid an immediate underrun, not
        // enough to absorb network jitter. Sized for a couple of seconds of
        // cushion so a chunk stalling on enqueueSequenceChunk's `nextIndex`
        // wait doesn't starve playback before the next one arrives.
        val targetBufferBytes = sampleRate * channels * BYTES_PER_SAMPLE * TARGET_BUFFER_SECONDS
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        val format = AudioFormat.Builder()
            .setSampleRate(sampleRate)
            .setChannelMask(channelConfig)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .build()
        @Suppress("DEPRECATION") // streamType variant for API 31 minSdk
        track = runCatching {
            AudioTrack(
                attrs,
                format,
                maxOf(bufferSize * MIN_BUFFER_SAFETY_MULTIPLIER, targetBufferBytes),
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
        var stuckSinceMs: Long? = null,
    )

    private val sequenceLock = Any()
    private val sequenceStates: MutableMap<String, SequenceState> = mutableMapOf()

    private companion object {
        private const val BYTES_PER_SAMPLE = 2 // 16-bit PCM
        private const val TARGET_BUFFER_SECONDS = 2
        private const val MIN_BUFFER_SAFETY_MULTIPLIER = 8

        // Observed normal reordering resolves within ~3s worst case; a
        // chunk still missing well past that is gone for good, not late.
        private const val SKIP_AHEAD_TIMEOUT_MS = 5_000L
    }
}
