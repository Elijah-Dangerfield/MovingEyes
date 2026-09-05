@file:Suppress("MagicNumber")

package com.dangerfield.movingeyes.libraries.reactivity

import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Turns buffers of audio into occasional [SoundEvent]s.
 *
 * Three things have to be right for this to work in a real hallway rather than
 * a quiet room:
 *
 * **The threshold follows the room, not a constant.** A fixed one either never
 * fires next to a fridge or fires constantly at a party. The floor tracks the
 * quiet level and rises fast / falls slow, so a sustained noise stops
 * triggering while a bang still does.
 *
 * **Onsets, not loudness.** Eyes should react to a door slamming, not to music
 * being loud for three minutes. An event needs the level to *rise* past the
 * floor, and a refractory period stops one bang becoming six.
 *
 * **A guessed direction is labelled as guessed.** Phone mics sit a few
 * centimetres apart, so the level difference between them is small and easily
 * swamped. Below [MinDirectionConfidence] the direction is random rather than
 * confidently wrong, and [SoundEvent.isDirectionKnown] says which happened.
 *
 * Not thread-safe; call [process] from one thread.
 */
class AudioAnalyzer(private val random: Random = Random.Default) {

    private var noiseFloor = 0f
    private var lastLevel = 0f
    private var buffersSinceEvent = RefractoryBuffers
    private var hasFloor = false

    /** The current level, for a meter. */
    var level: Float = 0f
        private set

    /**
     * Returns an event when this buffer contains one.
     *
     * [samples] is interleaved and [channelCount] is 1 or 2; anything wider is
     * treated as its first two channels.
     */
    fun process(samples: FloatArray, channelCount: Int): SoundEvent? {
        if (samples.isEmpty() || channelCount < 1) return null

        // Saturating, because incrementing past Int.MAX_VALUE wraps negative
        // and the refractory check then never passes again.
        if (buffersSinceEvent < RefractoryBuffers) buffersSinceEvent++

        val channels = rootMeanSquarePerChannel(samples, channelCount)
        val loudest = channels.max()
        level = loudest

        if (!hasFloor) {
            noiseFloor = loudest
            hasFloor = true
            lastLevel = loudest
            return null
        }

        // Tested against the floor as it stood *before* this buffer. Adapting
        // first let the sound being tested drag the floor up to meet itself, so
        // the real ratio a noise had to clear was far higher than [OnsetRatio]
        // and only a violent transient ever qualified.
        val threshold = noiseFloor * OnsetRatio + MinimumOnsetLevel
        val isOnset = loudest > threshold && loudest > lastLevel

        // Rises quickly so a sustained noise stops triggering within a second,
        // falls slowly so the room going quiet doesn't re-arm on every gap
        // between words.
        val adaptation = if (loudest > noiseFloor) FloorRiseRate else FloorFallRate
        noiseFloor += (loudest - noiseFloor) * adaptation
        lastLevel = loudest

        if (!isOnset || buffersSinceEvent < RefractoryBuffers) return null
        buffersSinceEvent = 0

        val balance = stereoBalance(channels)
        return SoundEvent(
            direction = balance ?: (random.nextFloat() * 2f - 1f),
            intensity = intensityOf(loudest, threshold),
            isDirectionKnown = balance != null,
        )
    }

    /** Forget the room. Call when capture restarts somewhere else. */
    fun reset() {
        noiseFloor = 0f
        lastLevel = 0f
        level = 0f
        buffersSinceEvent = RefractoryBuffers
        hasFloor = false
    }

    private fun rootMeanSquarePerChannel(samples: FloatArray, channelCount: Int): FloatArray {
        val channels = minOf(channelCount, 2)
        val sums = FloatArray(channels)
        var index = 0
        var frames = 0
        while (index + channelCount <= samples.size) {
            for (channel in 0 until channels) {
                val sample = samples[index + channel]
                sums[channel] += sample * sample
            }
            index += channelCount
            frames++
        }
        if (frames == 0) return FloatArray(channels)
        return FloatArray(channels) { sqrt(sums[it] / frames) }
    }

    /**
     * -1..1 from the level difference between channels, or null when the
     * reading can't be trusted: mono input, or two channels too alike to mean
     * anything given how close phone microphones are.
     */
    private fun stereoBalance(channels: FloatArray): Float? {
        if (channels.size < 2) return null
        val left = channels[0]
        val right = channels[1]
        val total = left + right
        if (total <= MinimumOnsetLevel) return null

        val balance = (right - left) / total
        if (abs(balance) < MinDirectionConfidence) return null

        // Rescaled so the usable band spans the full range; otherwise a real
        // sound off to one side would only ever nudge the eyes slightly.
        val scaled = (balance - MinDirectionConfidence * kotlin.math.sign(balance)) /
            (1f - MinDirectionConfidence)
        return scaled.coerceIn(-1f, 1f)
    }

    /** How far above the threshold, saturating so a scream and a door slam
     *  don't produce wildly different reactions. */
    /**
     * How hard to startle, as multiples of the threshold rather than an
     * absolute level — so a quiet room and a loud one both get the full range
     * rather than one of them living at 0 and the other pinned at 1.
     *
     * This used to divide by a threshold that moved with the sound being
     * measured, which produced a plausible-looking spread only because the
     * denominator grew with the numerator. Once the threshold stopped moving
     * (it must, or an onset raises its own bar) that scale saturated on
     * anything above a murmur, so the ceiling moved out to
     * [IntensitySaturationRatio] multiples of threshold.
     */
    private fun intensityOf(level: Float, threshold: Float): Float {
        if (threshold <= 0f) return 1f
        val multiples = level / threshold
        return ((multiples - 1f) / (IntensitySaturationRatio - 1f)).coerceIn(0f, 1f)
    }

    private companion object {
        const val FloorRiseRate = 0.25f
        const val FloorFallRate = 0.02f

        /** How far above the floor counts as a sound rather than the room. */
        const val OnsetRatio = 2.2f

        /** Keeps near-silence from producing events on floating-point dust. */
        const val MinimumOnsetLevel = 0.008f

        /** At ~43 buffers a second this is roughly half a second. */
        const val RefractoryBuffers = 20

        /**
         * Below this level difference the direction is indistinguishable from
         * mic variation. Phone mics are centimetres apart, so this is generous.
         */
        const val MinDirectionConfidence = 0.06f

        /**
         * Multiples of the onset threshold at which a startle is as hard as it
         * gets. Roughly: a door in a quiet hallway lands mid-range and a shout
         * at arm's length saturates.
         */
        const val IntensitySaturationRatio = 24f
    }
}
