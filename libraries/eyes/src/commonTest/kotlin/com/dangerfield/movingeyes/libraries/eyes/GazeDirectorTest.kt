package com.dangerfield.movingeyes.libraries.eyes

import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Eyes in one composition are a face, and a face looks at one thing. Eyes that
 * each wander separately were the single loudest tell that this was a
 * screensaver rather than something watching you.
 */
class GazeDirectorTest {

    @Test
    fun `every eye sharing a director looks the same way`() {
        val director = GazeDirector(Moods.IdleScan, Random(1))
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

    /** The opposite rule: blinking in lockstep looks mechanical. */
    @Test
    fun `shared gaze does not make eyes blink together`() {
        val director = GazeDirector(Moods.IdleScan, Random(1))
        val eyes = List(4) { EyeRuntime(Moods.IdleScan, Random(it + 1), director) }

        var everDiffered = false
        repeat(600) {
            director.advance(0.033f)
            eyes.forEach { it.advance(0.033f) }
            if (eyes.map { it.frame.lidOpenness }.distinct().size > 1) everDiffered = true
        }

        assertTrue(everDiffered, "every eye blinked in lockstep")
    }

    @Test
    fun `the gaze moves over time`() {
        val director = GazeDirector(Moods.IdleScan, Random(7))
        val seen = mutableSetOf<Pair<Float, Float>>()

        repeat(2000) {
            director.advance(0.033f)
            seen += director.gaze.x to director.gaze.y
        }

        assertTrue(seen.size > 10, "the gaze barely moved")
    }

    @Test
    fun `the gaze stays in range`() {
        val director = GazeDirector(Moods.Frantic, Random(3))

        repeat(3000) {
            director.advance(0.033f)
            assertTrue(director.gaze.x in -1f..1f)
            assertTrue(director.gaze.y in -1f..1f)
        }
    }

    @Test
    fun `a horizontal mood never looks up or down`() {
        val director = GazeDirector(Moods.Sleepy, Random(5))

        repeat(2000) {
            director.advance(0.033f)
            assertEquals(0f, director.gaze.y)
        }
    }

    /** A sound pulls the whole scene's attention, not one eye's. */
    @Test
    fun `looking toward a sound moves the shared gaze`() {
        val director = GazeDirector(Moods.IdleScan, Random(2))
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
}
