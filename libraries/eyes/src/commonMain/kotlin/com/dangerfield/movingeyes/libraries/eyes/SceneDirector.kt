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
 * **A pair is the atom.** [blinksTogether] chooses between one timeline for the
 * whole scene and one timeline *per pair* — never one per eye. Two eyes of a
 * face blinking separately is the bug this class exists to prevent, at any
 * scale; what changes with scale is whether the whole crowd should blink as
 * one. Fourteen eyes in unison read as a single enormous creature, so a wall
 * wants [blinksTogether] off and gets seven independent pairs rather than
 * fourteen independent eyes.
 */
class SceneDirector(
    behavior: BehaviorConfig,
    var blinksTogether: Boolean = true,
    private val random: Random = Random.Default,
) {

    /**
     * One blink timeline per group, grown on demand. Runtimes watch their
     * group's tick rather than being pushed to, so an eye added mid-scene joins
     * the rhythm on the next blink instead of firing one immediately.
     */
    private var ticks = IntArray(1)
    private var doubles = BooleanArray(1)
    private var nextBlinkAts = FloatArray(1)

    /** Which timeline an eye in [group] is on. Everything shares group 0 when
     *  the scene blinks as one. */
    private fun timelineFor(group: Int) = if (blinksTogether) 0 else group.coerceAtLeast(0)

    fun blinkTick(group: Int): Int = ticks.getOrElse(timelineFor(group)) { 0 }

    /**
     * Whether the blink this group just announced is a double.
     *
     * Rolled here rather than per eye. Left to the eyes, one of a synced pair
     * would occasionally blink twice while the other blinked once — a face
     * winking at itself, and the exact desync the director exists to prevent.
     */
    fun blinkIsDouble(group: Int): Boolean = doubles.getOrElse(timelineFor(group)) { false }

    /** Called by the scene once it knows how many pairs it has. */
    fun setGroupCount(count: Int) {
        val wanted = count.coerceAtLeast(1)
        if (ticks.size == wanted) return
        ticks = IntArray(wanted)
        doubles = BooleanArray(wanted)
        nextBlinkAts = FloatArray(wanted) { elapsed + behavior.blinkIntervalSeconds.sampleIn(random) }
    }

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

        for (timeline in ticks.indices) {
            if (elapsed < nextBlinkAts[timeline]) continue
            ticks[timeline] += 1
            doubles[timeline] = random.nextFloat() < behavior.doubleBlinkChance
            nextBlinkAts[timeline] = elapsed + behavior.blinkIntervalSeconds.sampleIn(random)
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
    /** Turns the whole scene toward a sound. The deflection is floored the same
     *  way [EyeRuntime.startle] floors its own: a faint noise is still worth
     *  looking at, it is just worth looking at less sharply. */
    fun look(direction: Float, intensity: Float) {
        val strength = intensity.coerceIn(0f, 1f)
        from = gaze
        to = EyeVector(
            x = (direction * (MinimumLookDeflection + (1f - MinimumLookDeflection) * strength))
                .coerceIn(-1f, 1f),
            y = (random.nextFloat() - 0.5f) * 0.3f * strength,
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
        /** The share of a full turn even the quietest event still gets. */
        const val MinimumLookDeflection = 0.55f

        const val BaseSaccadeSeconds = 0.09f
        const val SaccadeOvershoot = 0.06f
        const val VerticalGazeDamping = 0.55f
    }

    private fun scheduleNextBlink() {
        for (timeline in nextBlinkAts.indices) {
            nextBlinkAts[timeline] = elapsed + behavior.blinkIntervalSeconds.sampleIn(random)
        }
    }

}

internal fun ClosedFloatingPointRange<Float>.sampleIn(random: Random): Float =
    if (endInclusive <= start) start else random.nextFloat() * (endInclusive - start) + start
