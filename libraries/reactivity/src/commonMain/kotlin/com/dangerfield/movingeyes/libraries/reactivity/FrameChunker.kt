package com.dangerfield.movingeyes.libraries.reactivity

/**
 * Re-cuts captured audio into fixed-size chunks before it reaches
 * [AudioAnalyzer].
 *
 * The analyser counts buffers rather than seconds: its refractory period, its
 * warm-up and both adaptation rates are all expressed per buffer, against a
 * documented assumption of roughly 23ms each. That makes the buffer size part
 * of its tuning rather than an implementation detail, and neither platform
 * guarantees one.
 *
 * Android asks `AudioRecord` for 1024 frames and gets them. iOS treats
 * `installTap`'s `bufferSize` as a request it is free to ignore, and does: a
 * tap asking for 1024 was observed delivering 4800. That single difference
 * stretches a half-second refractory to two seconds and, worse, takes the RMS
 * across 100ms of room, which flattens the short transients the whole feature
 * exists to notice.
 *
 * Chunking here rather than in either capture keeps the two platforms honest
 * against the same contract, and keeps this testable off-device.
 *
 * Not thread-safe: like the analyser it feeds, call [accept] from one thread.
 */
class FrameChunker(private val framesPerChunk: Int = DefaultFramesPerChunk) {

    init {
        require(framesPerChunk > 0) { "framesPerChunk must be positive" }
    }

    /** Frames left over from the last call, always shorter than one chunk. */
    private var carry = FloatArray(0)
    private var carryChannels = 0

    /**
     * Splits [samples] into whole chunks of `framesPerChunk` frames and hands
     * each to [onChunk], keeping any remainder for next time.
     *
     * [samples] is interleaved, so a chunk is `framesPerChunk * channelCount`
     * values. A buffer smaller than a chunk produces no callback at all, which
     * is the point: a short read should join the next one rather than be
     * measured as if it were a full window.
     */
    fun accept(samples: FloatArray, channelCount: Int, onChunk: (FloatArray, Int) -> Unit) {
        if (channelCount < 1 || samples.isEmpty()) return

        // A route change (headset in, phone docked) re-formats the stream
        // mid-session. Splicing frames of the old width onto the new ones would
        // interleave them wrong for every chunk that straddles the change.
        if (channelCount != carryChannels) {
            carry = FloatArray(0)
            carryChannels = channelCount
        }

        val chunkSize = framesPerChunk * channelCount
        val pending = if (carry.isEmpty()) samples else carry + samples

        var offset = 0
        while (pending.size - offset >= chunkSize) {
            onChunk(pending.copyOfRange(offset, offset + chunkSize), channelCount)
            offset += chunkSize
        }

        // Always a fresh array, so nothing here aliases a buffer the caller is
        // free to reuse on the next callback.
        carry = pending.copyOfRange(offset, pending.size)
    }

    /** Drop the remainder. Call when capture stops, so a restart elsewhere
     *  doesn't open with a fragment of the last room. */
    fun reset() {
        carry = FloatArray(0)
        carryChannels = 0
    }

    private companion object {
        /** ~23ms at 44.1kHz and ~21ms at 48kHz, matching what Android asks
         *  `AudioRecord` for and what the analyser is tuned against. */
        const val DefaultFramesPerChunk = 1024
    }
}
