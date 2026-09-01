@file:Suppress("MagicNumber")

package com.dangerfield.movingeyes.libraries.eyes

/**
 * Caps the parameters that produce rapid luminance change, for the Reduce
 * flashing setting and for an OS reduce-motion preference.
 *
 * It slows rather than stops: a mood that reduced to stillness would silently
 * turn Frantic into Dormant, and someone who turned this on still wants their
 * decoration to work. Blinks are the main luminance event — the lid is drawn in
 * the canvas colour — so the blink floor does most of the work.
 */
fun BehaviorConfig.reducedFlashing(): BehaviorConfig = copy(
    saccadeSpeed = saccadeSpeed.coerceAtMost(MaxSaccadeSpeed),
    saccadeIntervalSeconds = saccadeIntervalSeconds.atLeast(MinSaccadeIntervalSeconds),
    blinkIntervalSeconds = blinkIntervalSeconds.atLeast(MinBlinkIntervalSeconds),
    blinkDurationSeconds = blinkDurationSeconds.coerceAtLeast(MinBlinkDurationSeconds),
    doubleBlinkChance = doubleBlinkChance.coerceAtMost(MaxDoubleBlinkChance),
    dilationPulse = dilationPulse.coerceAtMost(MaxDilationPulse),
    jitter = jitter.coerceAtMost(MaxJitter),
)

/** Moods with rapid luminance change, which warrant a warning before use. */
val Mood.isStrobing: Boolean get() = this in Moods.Strobing

private fun ClosedFloatingPointRange<Float>.atLeast(
    floor: Float,
): ClosedFloatingPointRange<Float> =
    start.coerceAtLeast(floor)..endInclusive.coerceAtLeast(floor)

private const val MaxSaccadeSpeed = 1.2f
private const val MinSaccadeIntervalSeconds = 1.2f
private const val MinBlinkIntervalSeconds = 2.5f

/** A slower blink is a fade rather than a flash. */
private const val MinBlinkDurationSeconds = 0.25f
private const val MaxDoubleBlinkChance = 0.1f
private const val MaxDilationPulse = 0.08f
private const val MaxJitter = 0.03f
