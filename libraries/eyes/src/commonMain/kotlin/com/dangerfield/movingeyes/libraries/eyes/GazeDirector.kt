@file:Suppress("MagicNumber")

package com.dangerfield.movingeyes.libraries.eyes

import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Where the scene is looking.
 *
 * Eyes in one composition are a face, and a face looks at one thing. Left to
 * sample their own targets they drift apart and read as a bag of unrelated
 * eyeballs — the giveaway that this is a screensaver rather than something
 * watching you.
 *
 * Blinking stays per-eye: two eyes blinking in lockstep looks mechanical, but
 * two eyes looking in different directions looks broken. Opposite problems,
 * opposite answers.
 */
class GazeDirector(
    behavior: BehaviorConfig,
    private val random: Random = Random.Default,
) {

    var behavior: BehaviorConfig = behavior
        set(value) {
            field = value
            scheduleNext()
        }

    /** Current gaze in -1..1 on each axis. */
    var gaze: EyeVector = EyeVector.Zero
        private set

    private var elapsed = 0f
    private var from = EyeVector.Zero
    private var to = EyeVector.Zero
    private var saccadeElapsed = Float.MAX_VALUE
    private var saccadeDuration = BaseSaccadeSeconds
    private var nextAt = 0f

    init {
        scheduleNext()
    }

    fun advance(deltaSeconds: Float) {
        elapsed += deltaSeconds

        if (saccadeElapsed < saccadeDuration) {
            saccadeElapsed += deltaSeconds
        } else if (elapsed >= nextAt) {
            from = to
            to = sampleTarget()
            saccadeElapsed = 0f
            saccadeDuration = BaseSaccadeSeconds / behavior.saccadeSpeed.coerceAtLeast(0.05f)
            scheduleNext()
        }

        val progress = if (saccadeDuration <= 0f) 1f else (saccadeElapsed / saccadeDuration).coerceIn(0f, 1f)
        val eased = overshoot(progress)
        gaze = EyeVector(
            x = from.x + (to.x - from.x) * eased,
            y = from.y + (to.y - from.y) * eased,
        )
    }

    /** A sound pulls the whole scene's attention, not one eye's. */
    fun look(direction: Float, intensity: Float) {
        from = gaze
        to = EyeVector(
            x = (direction * intensity).coerceIn(-1f, 1f),
            y = (random.nextFloat() - 0.5f) * 0.3f * intensity,
        )
        saccadeElapsed = 0f
        saccadeDuration = BaseSaccadeSeconds / behavior.saccadeSpeed.coerceAtLeast(0.05f)
        scheduleNext()
    }

    private fun sampleTarget(): EyeVector {
        val range = behavior.gazeRange.coerceIn(0f, 1f)
        val x = (random.nextFloat() * 2f - 1f) * range
        val y = when (behavior.gazeAxis) {
            GazeAxis.Horizontal -> 0f
            GazeAxis.HorizontalAndVertical -> (random.nextFloat() * 2f - 1f) * range * VerticalGazeDamping
            GazeAxis.Free -> (random.nextFloat() * 2f - 1f) * range
        }
        return EyeVector(
            x = (behavior.gazeCenterX + x).coerceIn(-1f, 1f),
            y = (behavior.gazeCenterY + y).coerceIn(-1f, 1f),
        )
    }

    private fun scheduleNext() {
        nextAt = elapsed + behavior.saccadeIntervalSeconds.sampleIn(random)
    }

    private fun overshoot(progress: Float): Float {
        if (progress >= 1f) return 1f
        val eased = 1f - (1f - progress) * (1f - progress)
        return eased + sin(progress * PI.toFloat()) * SaccadeOvershoot
    }

    private companion object {
        const val BaseSaccadeSeconds = 0.09f
        const val SaccadeOvershoot = 0.06f
        const val VerticalGazeDamping = 0.55f
    }
}

internal fun ClosedFloatingPointRange<Float>.sampleIn(random: Random): Float =
    if (endInclusive <= start) start else random.nextFloat() * (endInclusive - start) + start
