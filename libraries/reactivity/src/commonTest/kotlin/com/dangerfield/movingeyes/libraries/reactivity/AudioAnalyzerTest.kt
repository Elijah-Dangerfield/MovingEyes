package com.dangerfield.movingeyes.libraries.reactivity

import kotlin.math.sin
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The analyser is the only part of reactivity that can be tested without a
 * microphone, and it is where every interesting decision lives: what counts as
 * a sound, what counts as the room, and whether a direction can be trusted.
 */
class AudioAnalyzerTest {

    private val frames = 1024

    @Test
    fun `silence produces nothing`() {
        val analyzer = AudioAnalyzer()

        repeat(50) {
            assertNull(analyzer.process(FloatArray(frames), channelCount = 1))
        }
    }

    @Test
    fun `an empty buffer is ignored rather than crashing`() {
        val analyzer = AudioAnalyzer()

        assertNull(analyzer.process(FloatArray(0), channelCount = 2))
        assertNull(analyzer.process(FloatArray(frames), channelCount = 0))
    }

    @Test
    fun `a bang against a quiet room fires`() {
        val analyzer = AudioAnalyzer()
        settle(analyzer, amplitude = 0.01f)

        val event = analyzer.process(mono(0.5f), channelCount = 1)

        assertNotNull(event)
        assertTrue(event.intensity > 0f)
    }

    /**
     * The property that makes this usable at a party: a room that gets loud and
     * stays loud stops being an event.
     */
    @Test
    fun `sustained noise stops triggering once the floor catches up`() {
        val analyzer = AudioAnalyzer()
        settle(analyzer, amplitude = 0.01f)

        analyzer.process(mono(0.4f), channelCount = 1)

        var fired = 0
        repeat(200) {
            if (analyzer.process(mono(0.4f), channelCount = 1) != null) fired++
        }

        assertTrue(fired <= 1, "sustained noise fired $fired times")
    }

    /** And the other half: after adapting to a loud room, a louder bang still lands. */
    @Test
    fun `a bang still fires in a loud room`() {
        val analyzer = AudioAnalyzer()
        settle(analyzer, amplitude = 0.2f, buffers = 300)

        val event = analyzer.process(mono(0.9f), channelCount = 1)

        assertNotNull(event)
    }

    @Test
    fun `one bang does not become six`() {
        val analyzer = AudioAnalyzer()
        settle(analyzer, amplitude = 0.01f)

        var fired = 0
        // A bang and its decay, the shape an actual door slam has.
        listOf(0.6f, 0.5f, 0.4f, 0.3f, 0.2f, 0.1f, 0.05f).forEach { amplitude ->
            if (analyzer.process(mono(amplitude), channelCount = 1) != null) fired++
        }

        assertEquals(1, fired)
    }

    @Test
    fun `a louder sound reports a higher intensity`() {
        val quiet = AudioAnalyzer().let {
            settle(it, amplitude = 0.01f)
            it.process(mono(0.15f), channelCount = 1)
        }
        val loud = AudioAnalyzer().let {
            settle(it, amplitude = 0.01f)
            it.process(mono(0.9f), channelCount = 1)
        }

        assertNotNull(quiet)
        assertNotNull(loud)
        assertTrue(loud.intensity > quiet.intensity)
    }

    @Test
    fun `intensity saturates rather than running away`() {
        val analyzer = AudioAnalyzer()
        settle(analyzer, amplitude = 0.01f)

        val event = analyzer.process(mono(1f), channelCount = 1)

        assertNotNull(event)
        assertTrue(event.intensity <= 1f)
    }

    // ---- Direction ------------------------------------------------------

    @Test
    fun `a sound on the right reports a rightward direction`() {
        val analyzer = AudioAnalyzer()
        settle(analyzer, amplitude = 0.01f, channelCount = 2)

        val event = analyzer.process(stereo(left = 0.1f, right = 0.6f), channelCount = 2)

        assertNotNull(event)
        assertTrue(event.isDirectionKnown)
        assertTrue(event.direction > 0f, "expected rightward, got ${event.direction}")
    }

    @Test
    fun `a sound on the left reports a leftward direction`() {
        val analyzer = AudioAnalyzer()
        settle(analyzer, amplitude = 0.01f, channelCount = 2)

        val event = analyzer.process(stereo(left = 0.6f, right = 0.1f), channelCount = 2)

        assertNotNull(event)
        assertTrue(event.isDirectionKnown)
        assertTrue(event.direction < 0f, "expected leftward, got ${event.direction}")
    }

    /**
     * The honesty property. Phone mics are centimetres apart, so a centred
     * sound produces near-identical channels — that must read as "don't know"
     * rather than as "dead ahead", which would freeze the eyes forward every
     * time something happened in front of the tablet.
     */
    @Test
    fun `a centred sound admits it does not know the direction`() {
        val analyzer = AudioAnalyzer()
        settle(analyzer, amplitude = 0.01f, channelCount = 2)

        val event = analyzer.process(stereo(left = 0.5f, right = 0.5f), channelCount = 2)

        assertNotNull(event)
        assertFalse(event.isDirectionKnown)
    }

    @Test
    fun `mono input never claims to know a direction`() {
        val analyzer = AudioAnalyzer()
        settle(analyzer, amplitude = 0.01f)

        val event = analyzer.process(mono(0.6f), channelCount = 1)

        assertNotNull(event)
        assertFalse(event.isDirectionKnown)
    }

    /** A guessed direction still has to be usable — not always the same one. */
    @Test
    fun `guessed directions vary`() {
        val directions = (1..12).map { seed ->
            val analyzer = AudioAnalyzer(Random(seed))
            settle(analyzer, amplitude = 0.01f)
            analyzer.process(mono(0.6f), channelCount = 1)?.direction
        }

        assertTrue(directions.all { it != null })
        assertTrue(directions.distinct().size > 1, "every guess was identical")
        assertTrue(directions.filterNotNull().all { it in -1f..1f })
    }

    @Test
    fun `direction stays in range even when one channel is silent`() {
        val analyzer = AudioAnalyzer()
        settle(analyzer, amplitude = 0.01f, channelCount = 2)

        val event = analyzer.process(stereo(left = 0f, right = 0.8f), channelCount = 2)

        assertNotNull(event)
        assertTrue(event.direction in -1f..1f)
    }

    @Test
    fun `resetting forgets the room`() {
        val analyzer = AudioAnalyzer()
        settle(analyzer, amplitude = 0.3f, buffers = 300)
        analyzer.reset()
        settle(analyzer, amplitude = 0.01f)

        assertNotNull(analyzer.process(mono(0.2f), channelCount = 1))
    }

    // ---- Helpers --------------------------------------------------------

    /** Runs quiet room tone through until the floor has settled and the
     *  refractory window has cleared. */
    private fun settle(
        analyzer: AudioAnalyzer,
        amplitude: Float,
        buffers: Int = 60,
        channelCount: Int = 1,
    ) {
        repeat(buffers) {
            val samples = if (channelCount == 2) {
                stereo(amplitude, amplitude)
            } else {
                mono(amplitude)
            }
            analyzer.process(samples, channelCount)
        }
    }

    /** A sine rather than a constant, so the RMS is a realistic fraction of
     *  the amplitude rather than equal to it. */
    private fun mono(amplitude: Float) =
        FloatArray(frames) { amplitude * sin(it * 0.1f) }

    private fun stereo(left: Float, right: Float) = FloatArray(frames * 2) { index ->
        val frame = index / 2
        val amplitude = if (index % 2 == 0) left else right
        amplitude * sin(frame * 0.1f)
    }

    /**
     * The regression that made reactivity look broken. The floor used to adapt
     * *before* the comparison, so a sound dragged the bar it had to clear up to
     * meet itself and only a violent transient ever qualified. A knock at a few
     * times the room's level has to register.
     */
    @Test
    fun `a modest knock over a quiet room still fires`() {
        val analyzer = AudioAnalyzer()
        settle(analyzer, amplitude = 0.01f)

        assertNotNull(
            analyzer.process(mono(0.05f), channelCount = 1),
            "a sound five times the room's level was swallowed",
        )
    }

    /** And the thing that stops it firing constantly: a level that stays up
     *  becomes the new normal within about a second. */
    @Test
    fun `a sustained noise stops firing once it becomes the room`() {
        val analyzer = AudioAnalyzer()
        settle(analyzer, amplitude = 0.01f)

        val fired = (0 until 200).count {
            analyzer.process(mono(0.05f), channelCount = 1) != null
        }

        assertTrue(fired in 1..3, "a constant noise fired $fired times")
    }
}
