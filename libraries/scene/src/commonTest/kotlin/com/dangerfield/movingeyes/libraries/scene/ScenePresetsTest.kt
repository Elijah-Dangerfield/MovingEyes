package com.dangerfield.movingeyes.libraries.scene

import com.dangerfield.movingeyes.libraries.eyes.EyeStyles
import com.dangerfield.movingeyes.libraries.eyes.EyeTier
import com.dangerfield.movingeyes.libraries.eyes.Mood
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Presets are data, so the tests here are the ones that catch a typo in the
 * data: an eye placed off-screen, a pair that isn't symmetric, a preset that
 * quietly needs the unlock without being marked as needing it.
 */
class ScenePresetsTest {

    @Test
    fun `every preset id appears exactly once`() {
        assertEquals(
            ScenePresets.All.map { it.id }.distinct().size,
            ScenePresets.All.size,
        )
    }

    @Test
    fun `every preset id is reachable`() {
        ScenePresetId.entries.forEach { id ->
            assertEquals(id, ScenePresets.byId(id).id)
        }
    }

    @Test
    fun `every eye sits on the canvas`() {
        allEyes().forEach { (preset, eye) ->
            assertTrue(eye.x in 0f..1f, "${preset.id} has an eye at x=${eye.x}")
            assertTrue(eye.y in 0f..1f, "${preset.id} has an eye at y=${eye.y}")
        }
    }

    /**
     * An eye big enough to overflow the canvas or too small to see from the
     * street is a typo, not a design choice.
     */
    @Test
    fun `every eye is a sane size`() {
        allEyes().forEach { (preset, eye) ->
            assertTrue(
                eye.sizeFraction in 0.02f..0.5f,
                "${preset.id} has an eye at ${eye.sizeFraction} of the short edge",
            )
        }
    }

    /**
     * A pair that's off by a few thousandths reads as wrong instantly, and it's
     * exactly the kind of thing that survives a code review.
     */
    @Test
    fun `presets built from pairs are symmetric about their centre`() {
        listOf(
            ScenePresets.PortraitHaunt,
            ScenePresets.PumpkinPals,
            ScenePresets.CatInTheBushes,
            ScenePresets.DemonAwakens,
            ScenePresets.DollsRoom,
        ).forEach { preset ->
            val (left, right) = preset.eyes
            val centre = (left.x + right.x) / 2f
            assertTrue(
                abs((centre - left.x) - (right.x - centre)) < 0.0001f,
                "${preset.id} is asymmetric",
            )
            assertEquals(left.y, right.y, "${preset.id} has a pair at different heights")
            assertEquals(left.sizeFraction, right.sizeFraction, "${preset.id} has mismatched eyes")
        }
    }

    @Test
    fun `a preset is marked paid exactly when it needs the unlock`() {
        ScenePresets.All.forEach { preset ->
            val needsUnlock = preset.eyes.any { eye ->
                EyeStyles.byId(eye.styleId).tier == EyeTier.Paid || eye.mood != Mood.IdleScan
            }
            assertEquals(needsUnlock, preset.isPaid, "${preset.id}")
        }
    }

    /**
     * The free tier has to be able to complete the whole trick, so at least one
     * preset must be usable without paying. If this ever fails, the paywall has
     * crept over the line the product is built on.
     */
    @Test
    fun `at least one preset is free`() {
        assertTrue(ScenePresets.All.any { !it.isPaid })
    }

    @Test
    fun `the blank scene is free and is a plain pair`() {
        val eyes = ScenePresets.blank()

        assertEquals(2, eyes.size)
        eyes.forEach { eye ->
            assertEquals(EyeTier.Free, EyeStyles.byId(eye.styleId).tier)
            assertEquals(Mood.IdleScan, eye.mood)
        }
    }

    @Test
    fun `a preset becomes a scene that round-trips`() {
        ScenePresets.All.forEach { preset ->
            val scene = preset.toScene(id = "id-${preset.id}", name = preset.id.name)
            val decoded = SceneCodec.decode(SceneCodec.encode(scene))

            assertTrue(decoded is SceneDecodeResult.Success, "${preset.id} failed to round-trip")
            assertEquals(scene, decoded.scene)
        }
    }

    @Test
    fun `spider nest holds the count the renderer was profiled against`() {
        assertEquals(8, ScenePresets.SpiderNest.eyes.size)
    }

    @Test
    fun `a scene with no custom mood carries no custom behaviour`() {
        assertFalse(allEyes().any { (_, eye) -> eye.mood != Mood.Custom && eye.customBehavior != null })
    }

    private fun allEyes(): List<Pair<ScenePreset, SceneEye>> =
        ScenePresets.All.flatMap { preset -> preset.eyes.map { preset to it } }
}
