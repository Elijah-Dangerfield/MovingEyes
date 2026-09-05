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
 * quietly needs the unlock.
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
     * The one scene meant to be graphic rather than alive. A cartoon is a
     * drawing, so it stays exactly level and mirrored — the opposite of the
     * rule every other preset follows.
     */
    @Test
    fun `the cartoon preset stays perfectly symmetric`() {
        val (left, right) = ScenePresets.PumpkinPals.eyes
        val centre = (left.x + right.x) / 2f

        assertTrue(abs((centre - left.x) - (right.x - centre)) < 0.0001f, "asymmetric")
        assertEquals(left.y, right.y, "not level")
        assertEquals(left.sizeFraction, right.sizeFraction, "mismatched eyes")
        assertEquals(0f, left.rotationDegrees)
        assertEquals(0f, right.rotationDegrees)
    }

    /**
     * And the ones that are meant to be alive. A pair sitting at identical
     * heights, identical sizes and zero degrees reads as a logo, so these carry
     * a deliberate tilt — small enough not to look like a mistake.
     */
    @Test
    fun `the living preset is tilted rather than level`() {
        val (left, right) = ScenePresets.Trapped.eyes

        assertTrue(left.rotationDegrees != 0f, "dead level")
        assertTrue(left.y != right.y, "no head tilt")
        assertTrue(left.sizeFraction != right.sizeFraction, "no perspective")

        // Small enough to read as life rather than as a bug.
        assertTrue(abs(left.rotationDegrees) < 8f, "tilted $left")
        assertTrue(abs(left.y - right.y) < 0.03f, "eyes are at different heights")
        assertTrue(
            abs(left.sizeFraction - right.sizeFraction) / left.sizeFraction < 0.1f,
            "eyes are visibly different sizes",
        )
    }

    /** Pure white is the loudest tell that an eye was drawn rather than seen. */
    @Test
    fun `no sclera is pure white`() {
        allEyes().forEach { (preset, eye) ->
            assertTrue(eye.scleraColor != 0xFFFFFFFF, "${preset.id} has a pure white sclera")
        }
    }

    /** Distance takes size, colour and glow together; shrinking alone reads as
     *  small eyes rather than distant ones. */
    @Test
    fun `the wall recedes on every axis at once`() {
        val pairs = ScenePresets.WallOfEyes.eyes.chunked(2).map { it.first() }
        val nearest = pairs.maxBy { it.sizeFraction }
        val farthest = pairs.minBy { it.sizeFraction }

        assertTrue(farthest.sizeFraction < nearest.sizeFraction)
        assertTrue(farthest.glowFraction < nearest.glowFraction)
        assertTrue(farthest.irisColor != nearest.irisColor, "the far pair is the same colour")
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
     * All of them, not most of them. A preset is a demonstration of what the
     * app does, and a locked demonstration is a scene you can look at and not
     * use. If this fails, a paid style or mood has crept into a preset and the
     * drawer has started nagging instead of showing.
     */
    @Test
    fun `every preset is free`() {
        ScenePresets.All.forEach { preset ->
            assertFalse(preset.isPaid, "${preset.id} needs the unlock")
        }
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

    /** The busiest scene the app ships, and the one the renderer's frame budget
     *  is measured against. */
    @Test
    fun `the wall holds the count the renderer was profiled against`() {
        assertEquals(14, ScenePresets.WallOfEyes.eyes.size)
    }

    @Test
    fun `a scene with no custom mood carries no custom behaviour`() {
        assertFalse(allEyes().any { (_, eye) -> eye.mood != Mood.Custom && eye.customBehavior != null })
    }

    private fun allEyes(): List<Pair<ScenePreset, SceneEye>> =
        ScenePresets.All.flatMap { preset -> preset.eyes.map { preset to it } }
}
