@file:Suppress("MagicNumber")

package com.dangerfield.movingeyes.libraries.eyes

import kotlinx.serialization.Serializable

/**
 * How an eye moves. This is what separates the app from a looping GIF.
 *
 * **Every interval here is a range, not a value, and that is not a style
 * choice.** Eyes that blink on a fixed timer look like a screensaver within
 * about ten seconds of watching. Eyes that almost sync and then drift apart
 * read as alive. Each cycle samples a fresh number from its range, and each
 * eye additionally carries a random phase offset ([EyeRuntime.phaseOffset]),
 * so two eyes created in the same instant never march together — and neither
 * do two tablets in the same room.
 *
 * If you ever find yourself collapsing one of these ranges to a single number
 * to make something easier to test, test the range instead.
 */
@Serializable
data class BehaviorConfig(
    val gazeAxis: GazeAxis = GazeAxis.HorizontalAndVertical,

    /** How far from centre the pupil travels, 0..1 of the available room. */
    val gazeRange: Float = 0.55f,

    /** Seconds between look-arounds. Sampled fresh every time. */
    val saccadeIntervalSeconds: ClosedFloatingPointRange<Float> = 1.4f..6.0f,

    /** Multiplier on the base saccade duration. Above 1 is a snap, below is a glide. */
    val saccadeSpeed: Float = 1f,

    /** Seconds between blinks. Sampled fresh every time. */
    val blinkIntervalSeconds: ClosedFloatingPointRange<Float> = 2.5f..7.0f,

    /** How long a blink takes, in seconds. */
    val blinkDurationSeconds: Float = 0.14f,

    /** Probability that a blink is immediately followed by a second one. */
    val doubleBlinkChance: Float = 0.15f,

    /** Amplitude of the slow pupil breathing, 0..1. */
    val dilationPulse: Float = 0.08f,

    /** Micro-tremor amplitude. Small, and the thing that sells "alive". */
    val jitter: Float = 0.02f,

    /**
     * Resting lid position, 1 = wide open. Sleepy sits heavy; Possessed sits
     * wider than natural, which is most of why it's unsettling.
     */
    val lidRest: Float = 1f,

    /**
     * Where the gaze wanders around, in the same -1..1 space as the gaze
     * itself. Lets a scene in a high window look permanently down at the path.
     */
    val gazeCenterX: Float = 0f,
    val gazeCenterY: Float = 0f,
)

@Serializable
enum class GazeAxis {
    /** Left and right only. Reads as scanning. */
    Horizontal,
    HorizontalAndVertical,

    /** Unconstrained, including diagonals at full range. */
    Free,
}

/**
 * The primary control. Nobody adjusts nine sliders at 6pm on the 31st, so
 * moods are the UI and the raw parameters are a "Custom" disclosure behind
 * them.
 */
@Serializable
enum class Mood {
    IdleScan,
    Suspicious,
    Frantic,
    Sleepy,
    Dormant,
    Possessed,
    Custom,
}

object Moods {

    /**
     * What a free scene runs, always. The free tier's motion is fixed — that
     * is the paywall line — so this one has to look good with no tuning at
     * all, on every style, forever.
     */
    val IdleScan = BehaviorConfig(
        gazeAxis = GazeAxis.HorizontalAndVertical,
        gazeRange = 0.5f,
        saccadeIntervalSeconds = 2.0f..6.5f,
        saccadeSpeed = 0.9f,
        blinkIntervalSeconds = 3.0f..8.0f,
        doubleBlinkChance = 0.12f,
        dilationPulse = 0.07f,
        jitter = 0.02f,
    )

    /** Slow tracking, long fixations, rare blinks. Reads as "it noticed you." */
    val Suspicious = BehaviorConfig(
        gazeAxis = GazeAxis.HorizontalAndVertical,
        gazeRange = 0.42f,
        saccadeIntervalSeconds = 4.0f..11.0f,
        saccadeSpeed = 0.55f,
        blinkIntervalSeconds = 7.0f..16.0f,
        doubleBlinkChance = 0.04f,
        dilationPulse = 0.05f,
        jitter = 0.015f,
        lidRest = 0.92f,
    )

    val Frantic = BehaviorConfig(
        gazeAxis = GazeAxis.Free,
        gazeRange = 0.85f,
        saccadeIntervalSeconds = 0.18f..0.7f,
        saccadeSpeed = 2.2f,
        blinkIntervalSeconds = 0.8f..2.4f,
        doubleBlinkChance = 0.45f,
        dilationPulse = 0.18f,
        jitter = 0.07f,
    )

    val Sleepy = BehaviorConfig(
        gazeAxis = GazeAxis.Horizontal,
        gazeRange = 0.3f,
        saccadeIntervalSeconds = 5.0f..13.0f,
        saccadeSpeed = 0.35f,
        blinkIntervalSeconds = 2.5f..6.0f,
        blinkDurationSeconds = 0.5f,
        doubleBlinkChance = 0.05f,
        dilationPulse = 0.04f,
        jitter = 0.01f,
        lidRest = 0.55f,
    )

    /**
     * Closed, or barely moving, waiting. Pairs with the scare scheduler: the
     * whole point is the contrast when it finally opens.
     */
    val Dormant = BehaviorConfig(
        gazeAxis = GazeAxis.Horizontal,
        gazeRange = 0.12f,
        saccadeIntervalSeconds = 9.0f..22.0f,
        saccadeSpeed = 0.3f,
        blinkIntervalSeconds = 11.0f..26.0f,
        blinkDurationSeconds = 0.7f,
        doubleBlinkChance = 0f,
        dilationPulse = 0.02f,
        jitter = 0.006f,
        lidRest = 0.18f,
    )

    /** Wide, dilated, near-motionless, with violent bursts. */
    val Possessed = BehaviorConfig(
        gazeAxis = GazeAxis.Free,
        gazeRange = 0.9f,
        saccadeIntervalSeconds = 0.1f..3.5f,
        saccadeSpeed = 3.0f,
        blinkIntervalSeconds = 6.0f..18.0f,
        blinkDurationSeconds = 0.09f,
        doubleBlinkChance = 0.3f,
        dilationPulse = 0.3f,
        jitter = 0.09f,
        lidRest = 1f,
    )

    fun forMood(mood: Mood): BehaviorConfig = when (mood) {
        Mood.IdleScan -> IdleScan
        Mood.Suspicious -> Suspicious
        Mood.Frantic -> Frantic
        Mood.Sleepy -> Sleepy
        Mood.Dormant -> Dormant
        Mood.Possessed -> Possessed
        // Custom means the scene carries its own config; callers hold it and
        // never ask for one here.
        Mood.Custom -> IdleScan
    }

    /** Free scenes get this and only this. */
    val FreeDefault: BehaviorConfig = IdleScan

    val Free: Set<Mood> = setOf(Mood.IdleScan)

    /**
     * Moods with rapid luminance change. Gate these behind the
     * photosensitivity warning and cap them when the OS reduce-motion
     * preference is on.
     */
    val Strobing: Set<Mood> = setOf(Mood.Frantic, Mood.Possessed)
}
