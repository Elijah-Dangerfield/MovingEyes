@file:Suppress("MagicNumber")

package com.dangerfield.movingeyes.libraries.reactivity

import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Turns buffers of audio into occasional [SoundEvent]s.
 *
 * Three things have to be right for this to work in a party room rather than a
 * quiet one:
 *
 * **It measures how unusual a moment is, not how loud.** The room's own level
 * and its own restlessness are both tracked, in decibels, and a sound registers
 * when it stands out from *that* room by more than that room normally varies.
 * A ratio against a noise floor cannot do this: 2.2x a quiet hallway is a
 * knock, and 2.2x a party is a gunshot.
 *
 * This replaces a fast-attack / slow-release follower, which is an envelope
 * detector — it tracks the *peaks* of a fluctuating room, not its average.
 * Every peak ratcheted the floor up in a tenth of a second and it bled off over
 * a second, so in a loud room the floor sat at the peaks and nothing could
 * exceed them. In practice the only thing that fired was a fingernail on the
 * device body, which reaches the mic mechanically.
 *
 * **Decibels, not amplitude.** A room spans orders of magnitude between its
 * quiet and its loud, so an average taken in linear amplitude is dominated
 * entirely by the loudest moments in it.
 *
 * **Onsets, not loudness.** Eyes should react to a door slamming, not to music
 * being loud for three minutes. An event needs the level to *rise*, and a
 * refractory period stops one bang becoming six.
 *
 * **Direction comes from arrival time first, loudness second.** Two phone mics
 * sit centimetres apart, so the *level* difference between them is tiny and a
 * reverberant room swamps it entirely — which is why the eyes used to pick a
 * direction that had nothing to do with the sound. The *timing* difference
 * survives: 10cm is about 13 samples at 44.1kHz, which a cross-correlation
 * finds readily as long as the peak is sharp enough to trust.
 *
 * When neither measure is confident the last trusted direction is held rather
 * than a new one invented. A held direction is a scene that keeps watching
 * where it last heard something; a random one is a scene with a twitch, and the
 * twitch is what reads as broken. [SoundEvent.isDirectionKnown] still says
 * which happened.
 *
 * Not thread-safe; call [process] from one thread.
 */
class AudioAnalyzer(private val random: Random = Random.Default) {

    /** The room's own level, in dBFS. A slow *symmetric* average on purpose:
     *  the moment it rises faster than it falls it becomes a peak follower. */
    private var ambientDb = SilenceDb

    /** How much this room normally swings, as a mean absolute deviation in dB.
     *  Big in a party, small in a library — which is what lets one threshold
     *  work in both. */
    private var swingDb = 0f

    private var lastDb = SilenceDb

    /** The last direction worth believing, held so an unclear moment keeps the
     *  scene looking where it last heard something. */
    private var heldDirection: Float? = null
    private var buffersSinceEvent = RefractoryBuffers
    private var warmupLeft = WarmupBuffers

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
        val db = decibels(loudest)

        if (warmupLeft > 0) {
            warmupLeft--
            // Seed rather than average in, so a second of silence at the start
            // doesn't leave the room believing it is silent.
            ambientDb = if (warmupLeft == WarmupBuffers - 1) db else blend(ambientDb, db, WarmupRate)
            swingDb = blend(swingDb, abs(db - ambientDb), WarmupRate)
            lastDb = db
            return null
        }

        // Measured against the room as it stood *before* this buffer: adapting
        // first lets a sound drag up the bar it then has to clear.
        val margin = maxOf(MinimumSpikeDb, swingDb * SpikeDeviations)
        val threshold = ambientDb + margin
        val isOnset = db > threshold && db > lastDb && db > SilenceDb

        ambientDb = blend(ambientDb, db, AmbientRate)
        swingDb = blend(swingDb, abs(db - ambientDb), SwingRate)
        lastDb = db

        if (!isOnset || buffersSinceEvent < RefractoryBuffers) return null
        buffersSinceEvent = 0

        val bearing = arrivalDirection(samples, channelCount) ?: stereoBalance(channels)
        if (bearing != null) heldDirection = bearing

        return SoundEvent(
            direction = bearing ?: heldDirection ?: (random.nextFloat() * 2f - 1f),
            intensity = ((db - threshold) / IntensityRangeDb).coerceIn(0f, 1f),
            isDirectionKnown = bearing != null,
        )
    }

    /**
     * -1..1 from which microphone the sound reached first.
     *
     * Cross-correlates the two channels across the handful of samples a phone's
     * mic spacing can account for, and takes the lag of the best match. The
     * result is only returned when that peak is genuinely peaked: a diffuse
     * room correlates weakly at every lag, and a confident-looking answer read
     * off a flat curve is worse than admitting there isn't one.
     *
     * Sign follows [stereoBalance]: positive is to the right. A source on the
     * right reaches the right mic first, so the right channel *leads*, so
     * `left[i] * right[i + lag]` peaks at a negative lag — hence the negation.
     */
    private fun arrivalDirection(samples: FloatArray, channelCount: Int): Float? {
        if (channelCount < 2) return null
        val frames = samples.size / channelCount
        if (frames <= MaxLagSamples * 4) return null

        var bestLag = 0
        var bestScore = -1f
        var total = 0f
        var considered = 0

        for (lag in -MaxLagSamples..MaxLagSamples) {
            var product = 0f
            var leftEnergy = 0f
            var rightEnergy = 0f

            val start = maxOf(0, -lag)
            val end = minOf(frames, frames - lag)
            for (frame in start until end) {
                val left = samples[frame * channelCount]
                val right = samples[(frame + lag) * channelCount + 1]
                product += left * right
                leftEnergy += left * left
                rightEnergy += right * right
            }

            val energy = sqrt(leftEnergy * rightEnergy)
            if (energy <= 0f) continue
            val score = product / energy

            total += score
            considered++
            if (score > bestScore) {
                bestScore = score
                bestLag = lag
            }
        }

        if (considered == 0) return null

        // A zero lag is not "the sound is centred", it is "there is no bearing
        // here": the set of points equidistant from both mics is a whole plane,
        // running behind the device as well as in front of it. Level difference
        // is a better guess than the middle, so hand it on rather than
        // reporting a confident nothing.
        if (bestLag == 0) return null

        val average = total / considered
        // How much the best lag stands out from every other lag. Flat means the
        // room is reverberant or the sound is everywhere, and neither has a
        // direction worth reporting.
        if (bestScore - average < MinCorrelationPeak) return null

        return (-bestLag.toFloat() / MaxLagSamples).coerceIn(-1f, 1f)
    }

    private fun blend(current: Float, target: Float, rate: Float) = current + (target - current) * rate

    /** dBFS, floored rather than allowed to reach negative infinity on silence. */
    private fun decibels(amplitude: Float): Float =
        DecibelScale * log10(max(amplitude, MinimumAmplitude))

    /** Forget the room. Call when capture restarts somewhere else. */
    fun reset() {
        ambientDb = SilenceDb
        swingDb = 0f
        lastDb = SilenceDb
        level = 0f
        buffersSinceEvent = RefractoryBuffers
        warmupLeft = WarmupBuffers
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
        if (total <= MinimumAmplitude * 2f) return null

        val balance = (right - left) / total
        if (abs(balance) < MinDirectionConfidence) return null

        // Rescaled so the usable band spans the full range; otherwise a real
        // sound off to one side would only ever nudge the eyes slightly.
        val scaled = (balance - MinDirectionConfidence * kotlin.math.sign(balance)) /
            (1f - MinDirectionConfidence)
        return scaled.coerceIn(-1f, 1f)
    }

    private companion object {
        /**
         * Adaptation rates, as a fraction per ~23ms buffer. The ambient is a
         * 1.5-second average and the swing a 2-second one — the swing lags on
         * purpose, so a single bang doesn't widen the band enough to hide the
         * bang after it.
         */
        const val AmbientRate = 0.015f
        const val SwingRate = 0.012f

        /** Faster while settling, so reactivity is usable a fraction of a
         *  second after it is switched on rather than ten seconds later. */
        const val WarmupRate = 0.2f
        const val WarmupBuffers = 12

        /** How many times the room's own swing a sound must exceed it by. */
        const val SpikeDeviations = 2.2f

        /** And a floor on that, in dB, for a room so steady its swing is
         *  almost zero — otherwise a fridge hum would fire on its own ripple. */
        const val MinimumSpikeDb = 5f

        /** Below this the room is silent and nothing in it is an event. */
        const val SilenceDb = -55f

        /**
         * dB above threshold at which a startle is as hard as it gets.
         *
         * Wide, because dB is a log scale and the span from "a voice across the
         * room" to "a shout at the tablet" really is thirty of them. A narrow
         * range here pins everything above a murmur at full intensity, which
         * makes every sound produce the same reaction.
         */
        const val IntensityRangeDb = 30f

        /** At ~43 buffers a second this is roughly half a second. */
        const val RefractoryBuffers = 20

        /**
         * Below this level difference the direction is indistinguishable from
         * mic variation. Phone mics are centimetres apart, so this is generous.
         */
        const val MinDirectionConfidence = 0.06f

        /**
         * Sample offsets the two mics can plausibly differ by. Roughly 12cm at
         * 44.1kHz — generous for a phone, and a ceiling rather than a guess:
         * anything beyond it is not a direction, it is a different sound.
         */
        const val MaxLagSamples = 16

        /** How far the best lag must stand out from the average to be believed. */
        const val MinCorrelationPeak = 0.12f

        const val DecibelScale = 20f
        const val MinimumAmplitude = 1e-5f
    }
}
