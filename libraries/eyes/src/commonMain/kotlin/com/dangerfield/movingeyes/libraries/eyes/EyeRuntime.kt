@file:Suppress("MagicNumber")

package com.dangerfield.movingeyes.libraries.eyes

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/** A point in the eye's own -1..1 space. Deliberately not Compose's Offset —
 *  this module has no Compose dependency, which is what makes it testable. */
data class EyeVector(val x: Float, val y: Float) {
    companion object {
        val Zero = EyeVector(0f, 0f)
    }
}

/**
 * Everything the renderer needs to draw one eye this frame. Read-only, and
 * cheap to copy — the renderer reads these fields inside its draw lambda.
 */
data class EyeFrame(
    /** Pupil offset from centre, -1..1 on each axis. */
    val gaze: EyeVector = EyeVector.Zero,

    /** 1 = fully open, 0 = shut. */
    val lidOpenness: Float = 1f,

    /** Multiplier on the resting pupil size. */
    val pupilScale: Float = 1f,

    /** 0..1. Drives glow intensity during a startle. */
    val arousal: Float = 0f,
)

/**
 * One eye's motion, advanced by an injected time delta.
 *
 * Deliberately **not** a coroutine, not a Flow, and not Compose-aware: it is a
 * plain state machine you call [advance] on. That's what lets a test drive
 * fifteen simulated minutes through it in a millisecond and assert that
 * saccades land in range and that two eyes never fall into step.
 *
 * Construct one per eye. [phaseOffset] is sampled per eye at creation and is
 * the single most important field in this class — see [BehaviorConfig].
 */
class EyeRuntime(
    behavior: BehaviorConfig,
    private val random: Random = Random.Default,
    /**
     * Where the whole scene is looking. Null leaves the eye to wander on its
     * own, which is only right for a lone preview.
     */
    private val sceneDirector: SceneDirector? = null,
    /**
     * Seconds of head start. Randomised per eye so nothing syncs by accident,
     * across eyes or across devices.
     */
    val phaseOffset: Float = random.nextFloat() * MaxPhaseOffsetSeconds,
) {

    /** Which of the director's blink timelines this eye is on. Eyes that form a
     *  pair share one; see [SceneDirector]. */
    var blinkGroup: Int = 0

    var behavior: BehaviorConfig = behavior
        set(value) {
            field = value
            // Re-sample against the new ranges rather than letting a timer set
            // under Sleepy run to completion after a switch to Frantic.
            scheduleNextSaccade()
            scheduleNextBlink()
        }

    /** This frame's output. */
    var frame: EyeFrame = EyeFrame()
        private set

    private var elapsed = 0f

    private var gazeFrom = EyeVector.Zero
    private var gazeTo = EyeVector.Zero
    private var saccadeElapsed = Float.MAX_VALUE
    private var saccadeDuration = 0.09f
    private var nextSaccadeAt = 0f

    private var blinkElapsed = Float.MAX_VALUE
    private var nextBlinkAt = 0f

    /** Starts level with the director so joining a scene doesn't fire a blink
     *  on the first frame. */
    private var lastBlinkTick = sceneDirector?.blinkTick(0) ?: 0
    private var queuedBlinks = 0

    private var startleElapsed = Float.MAX_VALUE
    private var startleGaze = EyeVector.Zero

    private var jitterTarget = EyeVector.Zero
    private var jitterCurrent = EyeVector.Zero

    init {
        // The phase offset is spent up front rather than added to every timer,
        // so an eye created later still lands mid-cycle instead of starting
        // fresh alongside its neighbours.
        elapsed = phaseOffset
        scheduleNextSaccade()
        scheduleNextBlink()
        nextSaccadeAt -= phaseOffset * SaccadePhaseBleed
        nextBlinkAt -= phaseOffset * BlinkPhaseBleed
    }

    /** Advance by [deltaSeconds] and recompute [frame]. */
    fun advance(deltaSeconds: Float) {
        val delta = deltaSeconds.coerceIn(0f, MaxDeltaSeconds)
        elapsed += delta

        advanceGaze(delta)
        advanceBlink(delta)
        advanceJitter(delta)

        val startle = advanceStartle(delta)

        frame = EyeFrame(
            gaze = currentGaze(startle),
            lidOpenness = currentLidOpenness(startle),
            pupilScale = currentPupilScale(startle),
            arousal = startle,
        )
    }

    /**
     * A sharp sound. Pupils blow wide, gaze snaps toward [direction] (-1 left,
     * +1 right), then everything relaxes back over three seconds.
     *
     * [intensity] 0..1 scales how far the gaze throws and how wide the pupil
     * opens, so a door slam and a shout don't produce identical reactions.
     */
    fun startle(direction: Float, intensity: Float = 1f) {
        val strength = intensity.coerceIn(0f, 1f)
        startleElapsed = 0f
        startleGaze = EyeVector(
            x = direction.coerceIn(-1f, 1f) * deflectionFor(strength),
            y = (random.nextFloat() - 0.5f) * 0.3f * strength,
        )
        // Cut any saccade in flight: a startle overrides everything, and a
        // gaze that finished drifting somewhere else first would read as the
        // eye not having heard it.
        saccadeElapsed = Float.MAX_VALUE
    }

    private fun advanceGaze(delta: Float) {
        if (sceneDirector != null) return
        if (saccadeElapsed < saccadeDuration) {
            saccadeElapsed += delta
            return
        }
        if (elapsed < nextSaccadeAt) return

        gazeFrom = gazeTo
        gazeTo = sampleGazeTarget()
        saccadeElapsed = 0f
        saccadeDuration = (BaseSaccadeSeconds / behavior.saccadeSpeed.coerceAtLeast(0.05f))
        scheduleNextSaccade()
    }

    /**
     * The scene decides *when* to blink, and whether it is a double, when it is
     * directing. This eye always owns *how long*, which comes from its own
     * mood — so a pair on different moods still blinks at the same moments at
     * its own speed.
     */
    private fun advanceBlink(delta: Float) {
        if (blinkElapsed < behavior.blinkDurationSeconds) {
            blinkElapsed += delta
            return
        }
        if (queuedBlinks > 0) {
            queuedBlinks -= 1
            blinkElapsed = 0f
            return
        }

        val director = sceneDirector
        if (director != null) {
            val tick = director.blinkTick(blinkGroup)
            if (tick == lastBlinkTick) return
            lastBlinkTick = tick
            blinkElapsed = 0f
            if (director.blinkIsDouble(blinkGroup)) queuedBlinks = 1
            return
        }

        if (elapsed < nextBlinkAt) return
        scheduleNextBlink()
        blinkElapsed = 0f
        if (random.nextFloat() < behavior.doubleBlinkChance) queuedBlinks = 1
    }

    private fun advanceJitter(delta: Float) {
        // Re-aim the tremor occasionally and ease toward it, so it reads as a
        // living micro-tremor rather than per-frame noise (which at 30fps
        // looks like a rendering bug).
        if (random.nextFloat() < delta * JitterRetargetsPerSecond) {
            jitterTarget = EyeVector(
                x = (random.nextFloat() * 2f - 1f) * behavior.jitter,
                y = (random.nextFloat() * 2f - 1f) * behavior.jitter,
            )
        }
        val ease = min(1f, delta * JitterEaseRate)
        jitterCurrent = EyeVector(
            x = jitterCurrent.x + (jitterTarget.x - jitterCurrent.x) * ease,
            y = jitterCurrent.y + (jitterTarget.y - jitterCurrent.y) * ease,
        )
    }

    /** Returns the current startle strength, 1 at the moment of the sound
     *  decaying to 0 over [StartleDecaySeconds]. */
    /**
     * How far toward the sound to turn, given how loud it was.
     *
     * Floored well above zero on purpose. Scaling the turn straight off
     * intensity meant a quiet noise aimed the eyes at *centre* — a startle you
     * could only detect as a pupil twitch, because the one part of the reaction
     * that reads across a room had been scaled away. Volume should decide how
     * hard the reaction is, not whether the eyes look at all: something heard
     * faintly still gets looked at.
     */
    private fun deflectionFor(strength: Float): Float =
        MinimumStartleDeflection + (1f - MinimumStartleDeflection) * strength

    private fun advanceStartle(delta: Float): Float {
        if (startleElapsed >= StartleDecaySeconds) return 0f
        startleElapsed += delta
        val progress = (startleElapsed / StartleDecaySeconds).coerceIn(0f, 1f)
        // Ease out: the reaction is instant and the recovery is slow, which is
        // how a real startle feels.
        return (1f - progress) * (1f - progress)
    }

    private fun currentGaze(startle: Float): EyeVector {
        val wandered = sceneDirector?.gaze ?: run {
            val progress = if (saccadeDuration <= 0f) {
                1f
            } else {
                (saccadeElapsed / saccadeDuration).coerceIn(0f, 1f)
            }
            val eased = overshoot(progress)
            EyeVector(
                x = gazeFrom.x + (gazeTo.x - gazeFrom.x) * eased,
                y = gazeFrom.y + (gazeTo.y - gazeFrom.y) * eased,
            )
        }

        // A startle drags the gaze toward the sound and releases it as it decays.
        return EyeVector(
            x = (wandered.x + (startleGaze.x - wandered.x) * startle + jitterCurrent.x).coerceIn(-1f, 1f),
            y = (wandered.y + (startleGaze.y - wandered.y) * startle + jitterCurrent.y).coerceIn(-1f, 1f),
        )
    }

    private fun currentLidOpenness(startle: Float): Float {
        val blink = if (blinkElapsed >= behavior.blinkDurationSeconds) {
            1f
        } else {
            // Down and back up over the blink's duration.
            val progress = blinkElapsed / behavior.blinkDurationSeconds
            abs(cos(progress * PI.toFloat()))
        }
        // A startle forces the lids wide regardless of the mood's resting position.
        val rest = behavior.lidRest + (1f - behavior.lidRest) * startle
        return (blink * rest).coerceIn(0f, 1f)
    }

    private fun currentPupilScale(startle: Float): Float {
        // Slow breathing, plus the hard dilation of a startle.
        val breath = 1f + sin(elapsed * DilationBreathsPerSecond * 2f * PI.toFloat()) * behavior.dilationPulse
        return breath + startle * StartleDilation
    }

    private fun sampleGazeTarget(): EyeVector {
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

    private fun scheduleNextSaccade() {
        nextSaccadeAt = elapsed + behavior.saccadeIntervalSeconds.sampleIn(random)
    }

    private fun scheduleNextBlink() {
        nextBlinkAt = elapsed + behavior.blinkIntervalSeconds.sampleIn(random)
    }

    /**
     * Arrives slightly hot and settles back. A pupil that eases flatly to a
     * stop reads as a screensaver; one that overshoots by a few percent reads
     * as muscle.
     */
    private fun overshoot(progress: Float): Float {
        if (progress >= 1f) return 1f
        val eased = 1f - (1f - progress) * (1f - progress)
        return eased + sin(progress * PI.toFloat()) * SaccadeOvershoot
    }

    private companion object {
        const val MaxPhaseOffsetSeconds = 6f

        /**
         * A frame delta is clamped before use. Coming back from a long
         * background with a 40-second delta would otherwise fire every timer
         * at once and produce a visible spasm on the first frame.
         */
        const val MaxDeltaSeconds = 0.25f

        /** Matches `Motion.Eye.SaccadeMillis`. */
        const val BaseSaccadeSeconds = 0.09f
        const val SaccadeOvershoot = 0.06f

        /** Matches `Motion.Eye.StartleDecayMillis`. */
        const val StartleDecaySeconds = 3f
        const val StartleDilation = 0.4f

        const val DilationBreathsPerSecond = 0.12f
        const val JitterRetargetsPerSecond = 3f
        const val JitterEaseRate = 6f

        /** Eyes travel less vertically than horizontally; the full range on
         *  both axes reads as a fly rather than a person. */
        const val VerticalGazeDamping = 0.55f

        /** How much of the phase offset bleeds into the first timers, so two
         *  eyes don't merely start at different points in the same cycle. */
        const val SaccadePhaseBleed = 0.5f
        const val BlinkPhaseBleed = 0.7f

        /** The share of a full turn even the quietest event still gets. */
        const val MinimumStartleDeflection = 0.55f
    }
}


