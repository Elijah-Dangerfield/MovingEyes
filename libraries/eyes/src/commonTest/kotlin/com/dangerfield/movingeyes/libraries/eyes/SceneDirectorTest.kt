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

    /** Off, the director stops deciding and each eye falls back to its own
     *  timer — otherwise the toggle would only ever be cosmetic. */
    @Test
    fun `turning sync off hands blinking back to the eyes`() {
        val director = SceneDirector(Moods.IdleScan, blinksTogether = false, random = Random(4))
        val left = EyeRuntime(Moods.IdleScan, Random(11), director)
        val right = EyeRuntime(Moods.IdleScan, Random(22), director)

        var drifted = false
        repeat(900) {
            director.advance(FrameSeconds)
            left.advance(FrameSeconds)
            right.advance(FrameSeconds)
            if (abs(left.frame.lidOpenness - right.frame.lidOpenness) > 0.01f) drifted = true
        }
        assertTrue(drifted, "sync was off but the pair still blinked in lockstep")
    }

    /**
     * A faint sound has to visibly move the eyes. The deflection used to scale
     * straight off intensity, so a quiet noise aimed them at centre and the
     * only evidence anything had been heard was a pupil twitch.
     */
    @Test
    fun `even a barely audible sound turns the scene toward it`() {
        val director = SceneDirector(Moods.IdleScan, random = Random(6))

        director.look(direction = 1f, intensity = 0.01f)
        repeat(20) { director.advance(FrameSeconds) }

        assertTrue(director.gaze.x > 0.4f, "a faint sound moved the gaze to ${director.gaze.x}")
    }

    /** And a loud one still has somewhere further to go, or volume would mean
     *  nothing. */
    @Test
    fun `a loud sound turns further than a faint one`() {
        fun gazeAfter(intensity: Float): Float {
            val director = SceneDirector(Moods.IdleScan, random = Random(6))
            director.look(direction = 1f, intensity = intensity)
            repeat(20) { director.advance(FrameSeconds) }
            return director.gaze.x
        }

        assertTrue(gazeAfter(1f) > gazeAfter(0.01f))
    }

    private companion object {
        const val FrameSeconds = 1f / 30f
    }
}
