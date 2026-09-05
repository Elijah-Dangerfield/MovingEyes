package com.dangerfield.movingeyes.libraries.eyes

import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Eyes in one composition are a face, and a face looks at one thing. Eyes that
 * each wander separately were the single loudest tell that this was a
 * screensaver rather than something watching you.
 */
class SceneDirectorTest {

    @Test
    fun `every eye sharing a director looks the same way`() {
        val director = SceneDirector(Moods.IdleScan, random = Random(1))
        val eyes = List(4) { EyeRuntime(Moods.IdleScan, Random(it), director) }

        repeat(400) {
            director.advance(0.033f)
            eyes.forEach { it.advance(0.033f) }
        }

        val gazes = eyes.map { it.frame.gaze }
        gazes.forEach { gaze ->
            // Jitter keeps them from being pixel-identical, which is the point.
            assertTrue(abs(gaze.x - gazes[0].x) < 0.1f, "eyes disagree: $gazes")
            assertTrue(abs(gaze.y - gazes[0].y) < 0.1f, "eyes disagree: $gazes")
        }
    }

    /**
     * Gaze and blink now travel together — a face does both at once. This used
     * to assert the opposite, on the theory that lockstep blinking looks
     * mechanical. In practice the drift is worse: two independent timers only
     * ever separate further, so a pair reliably ends up winking at itself. The
     * scale where lockstep does look wrong is a crowd, and that is what the
     * per-scene `blinksTogether` switch is for.
     */
    @Test
    fun `a directed scene blinks as one`() {
        val director = SceneDirector(Moods.IdleScan, random = Random(1))
        val eyes = List(4) { EyeRuntime(Moods.IdleScan, Random(it + 1), director) }

        var everDiffered = false
        repeat(600) {
            director.advance(0.033f)
            eyes.forEach { it.advance(0.033f) }
            if (eyes.map { it.frame.lidOpenness }.distinct().size > 1) everDiffered = true
        }

        assertFalse(everDiffered, "the scene's eyes drifted out of step")
    }

    @Test
    fun `the gaze moves over time`() {
        val director = SceneDirector(Moods.IdleScan, random = Random(7))
        val seen = mutableSetOf<Pair<Float, Float>>()

        repeat(2000) {
            director.advance(0.033f)
            seen += director.gaze.x to director.gaze.y
        }

        assertTrue(seen.size > 10, "the gaze barely moved")
    }

    @Test
    fun `the gaze stays in range`() {
        val director = SceneDirector(Moods.Frantic, random = Random(3))

        repeat(3000) {
            director.advance(0.033f)
            assertTrue(director.gaze.x in -1f..1f)
            assertTrue(director.gaze.y in -1f..1f)
        }
    }

    @Test
    fun `a horizontal mood never looks up or down`() {
        val director = SceneDirector(Moods.Sleepy, random = Random(5))

        repeat(2000) {
            director.advance(0.033f)
            assertEquals(0f, director.gaze.y)
        }
    }

    /** A sound pulls the whole scene's attention, not one eye's. */
    @Test
    fun `looking toward a sound moves the shared gaze`() {
        val director = SceneDirector(Moods.IdleScan, random = Random(2))
        repeat(30) { director.advance(0.033f) }

        director.look(direction = 1f, intensity = 1f)
        repeat(6) { director.advance(0.033f) }

        assertTrue(director.gaze.x > 0.3f, "did not look toward the sound: ${director.gaze}")
    }

    @Test
    fun `an eye without a director still wanders on its own`() {
        val lone = EyeRuntime(Moods.IdleScan, Random(9))
        val seen = mutableSetOf<Float>()

        repeat(2000) {
            lone.advance(0.033f)
            seen += lone.frame.gaze.x
        }

        assertTrue(seen.size > 10, "a lone preview eye stopped moving")
    }

    /**
     * The property the whole toggle exists for. Two eyes sharing a director
     * must open and close together; two eyes left to their own timers must not,
     * because that is what a crowd looks like.
     */
    @Test
    fun `a directed pair blinks together and an undirected pair does not`() {
        fun lidsOverRun(director: SceneDirector?): List<Pair<Float, Float>> {
            val left = EyeRuntime(Moods.IdleScan, Random(11), director)
            val right = EyeRuntime(Moods.IdleScan, Random(22), director)
            return (0 until 900).map {
                director?.advance(FrameSeconds)
                left.advance(FrameSeconds)
                right.advance(FrameSeconds)
                left.frame.lidOpenness to right.frame.lidOpenness
            }
        }

        val synced = lidsOverRun(SceneDirector(Moods.IdleScan, random = Random(3)))
        assertTrue(
            synced.none { (left, right) -> abs(left - right) > 0.01f },
            "a directed pair drifted apart",
        )

        val independent = lidsOverRun(null)
        assertTrue(
            independent.any { (left, right) -> abs(left - right) > 0.01f },
            "an undirected pair never drifted, so the director isn't what synced them",
        )
    }

    /**
     * With sync off the scene splits per *pair*, not per eye — a pair is the
     * atom. Two eyes on the same group still blink together however the scene
     * is configured, because a face winking at itself is the bug this whole
     * class exists to prevent.
     */
    @Test
    fun `sync off still keeps a pair together`() {
        val director = SceneDirector(Moods.IdleScan, blinksTogether = false, random = Random(4))
        director.setGroupCount(2)

        val left = EyeRuntime(Moods.IdleScan, Random(11), director)
        val right = EyeRuntime(Moods.IdleScan, Random(22), director)

        repeat(900) {
            director.advance(FrameSeconds)
            left.advance(FrameSeconds)
            right.advance(FrameSeconds)
            assertTrue(
                abs(left.frame.lidOpenness - right.frame.lidOpenness) < 0.01f,
                "a pair on one group drifted apart",
            )
        }
    }

    /** And two *different* groups do come apart, or the toggle would be
     *  cosmetic and a wall would blink as one creature. */
    @Test
    fun `sync off lets separate pairs blink independently`() {
        val director = SceneDirector(Moods.IdleScan, blinksTogether = false, random = Random(4))
        director.setGroupCount(2)

        val near = EyeRuntime(Moods.IdleScan, Random(11), director)
        val far = EyeRuntime(Moods.IdleScan, Random(22), director).apply { blinkGroup = 1 }

        var drifted = false
        repeat(900) {
            director.advance(FrameSeconds)
            near.advance(FrameSeconds)
            far.advance(FrameSeconds)
            if (abs(near.frame.lidOpenness - far.frame.lidOpenness) > 0.01f) drifted = true
        }
        assertTrue(drifted, "two separate pairs blinked in lockstep")
    }

    private companion object {
        const val FrameSeconds = 1f / 30f
    }
}
