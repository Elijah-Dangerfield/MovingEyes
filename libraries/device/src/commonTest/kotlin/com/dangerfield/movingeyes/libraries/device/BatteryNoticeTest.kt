package com.dangerfield.movingeyes.libraries.device

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The two failure modes here are opposite and both bad: nagging over a scene
 * someone is trying to enjoy, or saying nothing until the tablet dies at 9pm on
 * the 31st. The rule is fussy enough to be worth pinning down.
 */
class BatteryNoticeTest {

    @Test
    fun `a comfortable battery says nothing`() {
        val notice = BatteryNotice()

        assertFalse(notice.shouldAnnounce(BatteryState(percent = 80, isCharging = false)))
    }

    @Test
    fun `a charging device says nothing even when nearly empty`() {
        val notice = BatteryNotice()

        assertFalse(notice.shouldAnnounce(BatteryState(percent = 5, isCharging = true)))
    }

    @Test
    fun `an unknown level says nothing rather than guessing`() {
        val notice = BatteryNotice()

        assertFalse(notice.shouldAnnounce(BatteryState(percent = null)))
    }

    @Test
    fun `crossing into low announces once`() {
        val notice = BatteryNotice()

        assertTrue(notice.shouldAnnounce(BatteryState(percent = 20)))
    }

    /** The anti-nag property. 20 → 19 → 18 is one warning, not three. */
    @Test
    fun `draining within a ten percent step stays quiet`() {
        val notice = BatteryNotice()
        notice.shouldAnnounce(BatteryState(percent = 19))

        // Down to 11, not 10: 10 opens the next band and is meant to speak.
        (18 downTo 11).forEach { percent ->
            assertFalse(
                notice.shouldAnnounce(BatteryState(percent = percent)),
                "announced again at $percent",
            )
        }
    }

    @Test
    fun `each ten percent step gets its own warning, and only one`() {
        val notice = BatteryNotice()

        assertTrue(notice.shouldAnnounce(BatteryState(percent = 19)))
        assertTrue(notice.shouldAnnounce(BatteryState(percent = 9)))
        assertFalse(notice.shouldAnnounce(BatteryState(percent = 4)), "9 and 4 are the same band")
    }

    /** Crossing the threshold must not fire twice on adjacent percentages. */
    @Test
    fun `entering low at exactly the threshold warns once, not twice`() {
        val notice = BatteryNotice()

        assertTrue(notice.shouldAnnounce(BatteryState(percent = 20)))
        assertFalse(notice.shouldAnnounce(BatteryState(percent = 19)))
    }

    /**
     * Plugging in and unplugging again later has to produce a fresh warning.
     * Otherwise someone who topped up at 15% and unplugged at 18% would be
     * told nothing for the rest of the night.
     */
    @Test
    fun `charging resets the memory so a later unplug warns again`() {
        val notice = BatteryNotice()
        assertTrue(notice.shouldAnnounce(BatteryState(percent = 15)))

        notice.shouldAnnounce(BatteryState(percent = 18, isCharging = true))

        assertTrue(notice.shouldAnnounce(BatteryState(percent = 15, isCharging = false)))
    }

    @Test
    fun `low is defined by the documented threshold`() {
        assertTrue(BatteryState(percent = BatteryState.LowBatteryPercent).isLow)
        assertFalse(BatteryState(percent = BatteryState.LowBatteryPercent + 1).isLow)
        assertFalse(BatteryState(percent = 1, isCharging = true).isLow)
    }
}
