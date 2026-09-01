package com.dangerfield.movingeyes.libraries.billing.impl

import com.dangerfield.movingeyes.libraries.billing.DemoControl
import com.dangerfield.movingeyes.libraries.billing.FeatureTrial
import com.dangerfield.movingeyes.libraries.flowroutines.testing.CoroutineTest
import kotlinx.coroutines.test.advanceTimeBy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * The demo is a promise about money: it says a paid control will work for
 * thirty seconds and then stop. Both halves matter — a demo that never ends is
 * the product given away, and one that ends early is a cheat.
 */
class FeatureTrialTest : CoroutineTest() {

    @Test
    fun `a demo applies immediately and reverts only when the time is up`() = runUnitTest {
        val trial = RealFeatureTrial(backgroundScope)
        var reverted = false

        assertTrue(trial.start(DemoControl.Mood) { reverted = true })

        assertEquals(DemoControl.Mood, trial.active.value?.control)

        advanceTimeBy(FeatureTrial.Duration - 1.seconds)
        assertFalse(reverted, "reverted before the demo was up")

        advanceTimeBy(2.seconds)
        assertTrue(reverted, "did not revert when the demo expired")
        assertNull(trial.active.value)
    }

    @Test
    fun `the countdown runs down to zero`() = runUnitTest {
        val trial = RealFeatureTrial(backgroundScope)
        trial.start(DemoControl.Mood) {}

        assertEquals(FeatureTrial.Duration, trial.active.value?.remaining)

        // The extra millisecond is `advanceTimeBy` being exclusive of the
        // target instant: without it the tick scheduled at exactly 10s hasn't
        // run yet and the countdown reads one tick behind.
        advanceTimeBy(10.seconds + 1.milliseconds)
        assertEquals(20.seconds, trial.active.value?.remaining)
    }

    @Test
    fun `the last five seconds are urgent`() = runUnitTest {
        val trial = RealFeatureTrial(backgroundScope)
        trial.start(DemoControl.Mood) {}

        advanceTimeBy(FeatureTrial.Duration - FeatureTrial.UrgentBelow - 1.seconds)
        assertFalse(trial.active.value?.isUrgent ?: true)

        advanceTimeBy(2.seconds)
        assertTrue(trial.active.value?.isUrgent ?: false)
    }

    @Test
    fun `a control gets one demo per session`() = runUnitTest {
        val trial = RealFeatureTrial(backgroundScope)

        assertTrue(trial.start(DemoControl.Mood) {})
        advanceTimeBy(FeatureTrial.Duration + 1.seconds)

        assertFalse(trial.isAvailable(DemoControl.Mood))
        assertFalse(trial.start(DemoControl.Mood) {}, "a second demo of the same control ran")
    }

    @Test
    fun `a different control is still available after one has been used`() = runUnitTest {
        val trial = RealFeatureTrial(backgroundScope)

        trial.start(DemoControl.Mood) {}
        advanceTimeBy(FeatureTrial.Duration + 1.seconds)

        assertTrue(trial.isAvailable(DemoControl.EyeStyle))
        assertTrue(trial.start(DemoControl.EyeStyle) {})
    }

    @Test
    fun `only one demo runs at a time`() = runUnitTest {
        val trial = RealFeatureTrial(backgroundScope)
        var secondReverted = false

        trial.start(DemoControl.Mood) {}
        advanceTimeBy(5.seconds)

        assertFalse(trial.start(DemoControl.BlinkRate) { secondReverted = true })
        assertEquals(DemoControl.Mood, trial.active.value?.control)

        advanceTimeBy(FeatureTrial.Duration)
        assertFalse(secondReverted, "the refused demo still ran its revert")
    }

    /**
     * The one that would cost real money if it broke: buying mid-demo must not
     * then take the feature away.
     */
    @Test
    fun `keeping a demo cancels the revert`() = runUnitTest {
        val trial = RealFeatureTrial(backgroundScope)
        var reverted = false

        trial.start(DemoControl.Mood) { reverted = true }
        advanceTimeBy(10.seconds)
        trial.keep()

        advanceTimeBy(FeatureTrial.Duration * 2)

        assertFalse(reverted, "a purchased feature was reverted anyway")
        assertNull(trial.active.value)
        assertNull(trial.justEnded.value)
    }

    @Test
    fun `cancelling reverts immediately`() = runUnitTest {
        val trial = RealFeatureTrial(backgroundScope)
        var reverted = false

        trial.start(DemoControl.Mood) { reverted = true }
        advanceTimeBy(3.seconds)
        trial.cancel()

        assertTrue(reverted)
        assertNull(trial.active.value)
    }

    @Test
    fun `cancelling twice only reverts once`() = runUnitTest {
        val trial = RealFeatureTrial(backgroundScope)
        var reverts = 0

        trial.start(DemoControl.Mood) { reverts++ }
        trial.cancel()
        trial.cancel()
        advanceTimeBy(FeatureTrial.Duration * 2)

        assertEquals(1, reverts)
    }

    @Test
    fun `the ended banner appears on expiry and clears itself`() = runUnitTest {
        val trial = RealFeatureTrial(backgroundScope)
        trial.start(DemoControl.WanderRadius) {}

        advanceTimeBy(FeatureTrial.Duration + 100.milliseconds)
        assertEquals(DemoControl.WanderRadius, trial.justEnded.value)

        advanceTimeBy(FeatureTrial.EndedBannerDuration + 1.seconds)
        assertNull(trial.justEnded.value, "the ended banner never cleared")
    }
}
