@file:Suppress("MagicNumber")

package com.dangerfield.movingeyes.libraries.eyes

import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * The scene's shared nervous system: where it is looking, and when it blinks.
 *
 * Eyes in one composition are a face, and a face looks at one thing and blinks
 * with both lids at once. Left to sample their own timers they drift apart,
 * which is the giveaway that this is a screensaver rather than something
 * watching you — and desync is a one-way door, since two independent timers
 * only ever get further apart.
 *
 * Blink sync is a scene property rather than a law, because it stops being
 * right at scale: two eyes blinking separately looks broken, but fourteen eyes
 * in seven pairs blinking in unison looks like one enormous creature rather
 * than a crowd. Pairs want [blinksTogether] on, which is the default; a wall
 * may want it off.
 */
class SceneDirector(
    behavior: BehaviorConfig,
    var blinksTogether: Boolean = true,
    private val random: Random = Random.Default,
) {

    /**
     * Bumped when the scene should blink. Runtimes watch it rather than being
     * pushed to, so an eye added mid-scene joins the rhythm on the next blink
     * instead of firing one immediately.
     */
    var blinkTick: Int = 0
        private set

    /**
     * Whether the blink [blinkTick] just announced is a double.
     *
     * Rolled here rather than per eye. Left to the eyes, one of a synced pair
     * would occasionally blink twice while the other blinked once — a face
     * winking at itself, and the exact desync the director exists to prevent.
     */
    var blinkIsDouble: Boolean = false
        private set

    private var nextBlinkAt = 0f

    var behavior: BehaviorConfig = behavior
        set(value) {
            field = value
            scheduleNext()
            scheduleNextBlink()
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
        scheduleNextBlink()
    }

    fun advance(deltaSeconds: Float) {
        elapsed += deltaSeconds

        if (elapsed >= nextBlinkAt) {
            blinkTick += 1
            blinkIsDouble = random.nextFloat() < behavior.doubleBlinkChance
            scheduleNextBlink()
        }

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

    private fun scheduleNextBlink() {
        nextBlinkAt = elapsed + behavior.blinkIntervalSeconds.sampleIn(random)
    }
}

internal fun ClosedFloatingPointRange<Float>.sampleIn(random: Random): Float =
    if (endInclusive <= start) start else random.nextFloat() * (endInclusive - start) + start
