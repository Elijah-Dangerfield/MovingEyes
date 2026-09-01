package com.dangerfield.movingeyes.libraries.eyes

import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The behaviour engine, driven by a synthetic clock.
 *
 * These run fifteen simulated minutes in a millisecond, which is the whole
 * reason the engine is Compose-free. Everything asserted here is something you
 * could only otherwise catch by staring at a tablet for a long time and
 * getting a bad feeling about it.
 */
class EyeRuntimeTest {

    /** One simulated frame at the app's target rate. */
    private val frameSeconds = 1f / 30f

    @Test
    fun `blink intervals stay inside the configured range`() {
        val behavior = Moods.IdleScan
        val runtime = EyeRuntime(behavior, Random(1), phaseOffset = 0f)

        val intervals = blinkGaps(runtime, seconds = 900f).filter { it > DoubleBlinkCeiling }

        assertTrue(intervals.size > 40, "expected plenty of blinks in 15 minutes, got ${intervals.size}")
        val slack = behavior.blinkDurationSeconds * 2 + frameSeconds * 2
        intervals.forEach { gap ->
            assertTrue(
                gap >= behavior.blinkIntervalSeconds.start - slack &&
                    gap <= behavior.blinkIntervalSeconds.endInclusive + slack,
                "blink gap $gap outside ${behavior.blinkIntervalSeconds}",
            )
        }
    }

    @Test
    fun `double blinks happen, and only as immediate follow-ups`() {
        val runtime = EyeRuntime(Moods.IdleScan, Random(21), phaseOffset = 0f)

        val gaps = blinkGaps(runtime, seconds = 900f)
        val doubles = gaps.filter { it <= DoubleBlinkCeiling }

        // The pair is the point: a double blink is two blinks back to back,
        // not a shortened interval. If these ever start landing a second or
        // two apart they'll read as a stutter instead.
        assertTrue(doubles.isNotEmpty(), "doubleBlinkChance never fired in 15 minutes")
        doubles.forEach { gap ->
            assertTrue(gap < 0.5f, "a 'double' blink landed $gap apart, which reads as a stutter")
        }
    }

    @Test
    fun `blink intervals actually vary rather than settling on one value`() {
        val runtime = EyeRuntime(Moods.IdleScan, Random(2), phaseOffset = 0f)

        val gaps = blinkGaps(runtime, seconds = 600f)
        val distinct = gaps.map { (it * 10).toInt() }.distinct()

        // A fixed timer is the single most recognisable "this is a
        // screensaver" tell, and it would pass every other test here.
        assertTrue(distinct.size > 10, "blink gaps barely vary: $distinct")
    }

    @Test
    fun `two eyes with different phase offsets do not blink together`() {
        val left = EyeRuntime(Moods.IdleScan, Random(3), phaseOffset = 0f)
        val right = EyeRuntime(Moods.IdleScan, Random(4), phaseOffset = 2.3f)

        var simultaneous = 0
        var leftBlinks = 0
        repeat((600f / frameSeconds).toInt()) {
            left.advance(frameSeconds)
            right.advance(frameSeconds)
            val leftShut = left.frame.lidOpenness < 0.4f
            val rightShut = right.frame.lidOpenness < 0.4f
            if (leftShut) leftBlinks += 1
            if (leftShut && rightShut) simultaneous += 1
        }

        assertTrue(leftBlinks > 0, "no blinks recorded")
        val overlap = simultaneous.toFloat() / leftBlinks
        // Some overlap is inevitable and fine — a pair that *never* coincides
        // is its own kind of wrong. Lockstep is the failure.
        assertTrue(overlap < 0.3f, "eyes blink together ${(overlap * 100).toInt()}% of the time")
    }

    @Test
    fun `gaze never leaves the configured range`() {
        val behavior = Moods.Suspicious
        val runtime = EyeRuntime(behavior, Random(5), phaseOffset = 0f)

        var maxX = 0f
        repeat((600f / frameSeconds).toInt()) {
            runtime.advance(frameSeconds)
            maxX = maxOf(maxX, abs(runtime.frame.gaze.x))
        }

        // Range plus overshoot and jitter. A pupil that leaves the iris is the
        // most obvious possible rendering bug.
        val ceiling = behavior.gazeRange + behavior.jitter + 0.12f
        assertTrue(maxX <= ceiling, "gaze reached $maxX, past $ceiling")
        assertTrue(maxX > behavior.gazeRange * 0.5f, "gaze barely moved: $maxX")
    }

    @Test
    fun `a horizontal-axis mood never moves vertically`() {
        val runtime = EyeRuntime(Moods.Sleepy, Random(6), phaseOffset = 0f)

        var maxY = 0f
        repeat((300f / frameSeconds).toInt()) {
            runtime.advance(frameSeconds)
            maxY = maxOf(maxY, abs(runtime.frame.gaze.y))
        }

        // Only jitter should show up on Y.
        assertTrue(maxY <= Moods.Sleepy.jitter + 0.01f, "vertical drift of $maxY on a horizontal mood")
    }

    @Test
    fun `a startle snaps the gaze toward the sound and decays over three seconds`() {
        val runtime = EyeRuntime(Moods.IdleScan, Random(7), phaseOffset = 0f)
        repeat(30) { runtime.advance(frameSeconds) }

        runtime.startle(direction = 1f, intensity = 1f)
        runtime.advance(frameSeconds)

        val snapped = runtime.frame.gaze.x
        assertTrue(snapped > 0.5f, "startle should throw the gaze right, got $snapped")
        assertTrue(runtime.frame.pupilScale > 1.2f, "pupils should blow wide, got ${runtime.frame.pupilScale}")

        // Still settling at two seconds, done by four.
        advance(runtime, seconds = 2f)
        assertTrue(runtime.frame.arousal > 0f, "startle decayed too fast")

        advance(runtime, seconds = 2f)
        assertEquals(0f, runtime.frame.arousal, "startle should be spent after ~3s")
    }

    @Test
    fun `a startle interrupts a saccade instead of finishing it first`() {
        val runtime = EyeRuntime(Moods.Frantic, Random(8), phaseOffset = 0f)
        // Frantic saccades constantly, so this lands mid-flight.
        repeat(20) { runtime.advance(frameSeconds) }

        runtime.startle(direction = -1f, intensity = 1f)
        runtime.advance(frameSeconds)

        // An eye that finished drifting somewhere else before reacting reads
        // as not having heard the sound at all.
        assertTrue(runtime.frame.gaze.x < -0.5f, "startle didn't win: ${runtime.frame.gaze.x}")
    }

    @Test
    fun `a huge delta after a long background does not fire every timer at once`() {
        val runtime = EyeRuntime(Moods.IdleScan, Random(9), phaseOffset = 0f)
        repeat(30) { runtime.advance(frameSeconds) }
        val before = runtime.frame.gaze

        // Coming back from four minutes suspended.
        runtime.advance(240f)

        val jump = abs(runtime.frame.gaze.x - before.x) + abs(runtime.frame.gaze.y - before.y)
        assertTrue(jump < 1.5f, "returning from background produced a $jump spasm")
    }

    @Test
    fun `changing mood re-samples timers instead of running the old one out`() {
        val runtime = EyeRuntime(Moods.Dormant, Random(10), phaseOffset = 0f)
        // Dormant blinks every 11-26s, so a timer is now set a long way out.
        advance(runtime, seconds = 1f)

        runtime.behavior = Moods.Frantic

        // Frantic blinks every 0.8-2.4s. Without a re-sample the eye would sit
        // frozen for up to 25 seconds after the user picked a frantic mood,
        // which reads as the app ignoring the tap.
        var blinked = false
        repeat((4f / frameSeconds).toInt()) {
            runtime.advance(frameSeconds)
            if (runtime.frame.lidOpenness < 0.4f) blinked = true
        }
        assertTrue(blinked, "no blink within 4s of switching to Frantic")
    }

    @Test
    fun `lid rest position reflects the mood`() {
        val sleepy = settle(EyeRuntime(Moods.Sleepy, Random(11), phaseOffset = 0f))
        val idle = settle(EyeRuntime(Moods.IdleScan, Random(11), phaseOffset = 0f))

        assertTrue(
            sleepy < idle,
            "Sleepy lids ($sleepy) should sit lower than Idle ($idle)",
        )
    }

    @Test
    fun `every mood produces motion`() {
        // Dormant included: "barely moving" still has to be moving, or the
        // scare scheduler has nothing to contrast against.
        Mood.entries.filter { it != Mood.Custom }.forEach { mood ->
            val runtime = EyeRuntime(Moods.forMood(mood), Random(12), phaseOffset = 0f)
            val seen = mutableSetOf<Int>()
            repeat((120f / frameSeconds).toInt()) {
                runtime.advance(frameSeconds)
                seen += (runtime.frame.gaze.x * 100).toInt()
            }
            assertTrue(seen.size > 3, "$mood produced almost no gaze movement")
        }
    }

    /** Anything closer together than this is a double blink, not an interval —
     *  the shortest configured interval in any mood is 0.8s. */
    private val DoubleBlinkCeiling = 0.6f

    private fun advance(runtime: EyeRuntime, seconds: Float) {
        repeat((seconds / frameSeconds).toInt()) { runtime.advance(frameSeconds) }
    }

    /** Mean lid openness once past the first blink, so a blink in progress
     *  doesn't decide the answer. */
    private fun settle(runtime: EyeRuntime): Float {
        var total = 0f
        var samples = 0
        repeat((60f / frameSeconds).toInt()) {
            runtime.advance(frameSeconds)
            total += runtime.frame.lidOpenness
            samples += 1
        }
        return total / samples
    }

    /** Gaps between the starts of consecutive blinks. */
    private fun blinkGaps(runtime: EyeRuntime, seconds: Float): List<Float> {
        val gaps = mutableListOf<Float>()
        var wasShut = false
        var sinceLast = 0f
        var seenFirst = false

        repeat((seconds / frameSeconds).toInt()) {
            runtime.advance(frameSeconds)
            sinceLast += frameSeconds
            val shut = runtime.frame.lidOpenness < 0.5f
            if (shut && !wasShut) {
                if (seenFirst) gaps += sinceLast
                seenFirst = true
                sinceLast = 0f
            }
            wasShut = shut
        }
        return gaps
    }
}
