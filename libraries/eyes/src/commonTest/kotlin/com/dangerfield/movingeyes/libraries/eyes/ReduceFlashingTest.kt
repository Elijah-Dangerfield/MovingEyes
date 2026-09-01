package com.dangerfield.movingeyes.libraries.eyes

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * An accessibility setting that silently did nothing would be worse than not
 * offering it, so every parameter that drives luminance change is asserted
 * against the worst mood in the app.
 */
class ReduceFlashingTest {

    @Test
    fun `the strobing moods are the ones that flash`() {
        assertTrue(Mood.Frantic.isStrobing)
        assertTrue(Mood.Possessed.isStrobing)
        assertTrue(!Mood.IdleScan.isStrobing)
        assertTrue(!Mood.Sleepy.isStrobing)
    }

    @Test
    fun `frantic slows down on every axis that flashes`() {
        val reduced = Moods.Frantic.reducedFlashing()

        assertTrue(reduced.saccadeSpeed < Moods.Frantic.saccadeSpeed)
        assertTrue(reduced.saccadeIntervalSeconds.start > Moods.Frantic.saccadeIntervalSeconds.start)
        assertTrue(reduced.blinkIntervalSeconds.start > Moods.Frantic.blinkIntervalSeconds.start)
        assertTrue(reduced.blinkDurationSeconds > Moods.Frantic.blinkDurationSeconds)
        assertTrue(reduced.doubleBlinkChance < Moods.Frantic.doubleBlinkChance)
        assertTrue(reduced.jitter < Moods.Frantic.jitter)
    }

    @Test
    fun `possessed slows down too`() {
        val reduced = Moods.Possessed.reducedFlashing()

        assertTrue(reduced.saccadeSpeed < Moods.Possessed.saccadeSpeed)
        assertTrue(reduced.dilationPulse < Moods.Possessed.dilationPulse)
    }

    /**
     * It caps, it doesn't flatten. A mood already gentler than the caps must
     * come through untouched, or turning the setting on would quietly make
     * every scene identical.
     */
    @Test
    fun `a calm mood is left alone`() {
        assertEquals(Moods.Sleepy, Moods.Sleepy.reducedFlashing())
        assertEquals(Moods.Dormant, Moods.Dormant.reducedFlashing())
    }

    /** Slower, not stopped: someone who turns this on still wants a decoration. */
    @Test
    fun `reduced moods still move and still blink`() {
        listOf(Moods.Frantic, Moods.Possessed).forEach { mood ->
            val reduced = mood.reducedFlashing()

            assertTrue(reduced.saccadeSpeed > 0f)
            assertTrue(reduced.gazeRange > 0f)
            assertTrue(reduced.blinkIntervalSeconds.endInclusive.isFinite())
        }
    }

    @Test
    fun `reducing twice changes nothing further`() {
        val once = Moods.Frantic.reducedFlashing()

        assertEquals(once, once.reducedFlashing())
    }

    @Test
    fun `every mood survives reduction with a usable range`() {
        Mood.entries.forEach { mood ->
            val reduced = Moods.forMood(mood).reducedFlashing()

            assertTrue(
                reduced.blinkIntervalSeconds.start <= reduced.blinkIntervalSeconds.endInclusive,
                "$mood produced an inverted blink range",
            )
            assertTrue(
                reduced.saccadeIntervalSeconds.start <= reduced.saccadeIntervalSeconds.endInclusive,
                "$mood produced an inverted saccade range",
            )
        }
    }
}
