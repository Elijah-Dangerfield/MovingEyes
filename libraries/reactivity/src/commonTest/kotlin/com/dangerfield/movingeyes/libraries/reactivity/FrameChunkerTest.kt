package com.dangerfield.movingeyes.libraries.reactivity

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The chunker exists because iOS ignores the tap size it is given, so the cases
 * that matter are the awkward sizes: bigger than a chunk, smaller than one, and
 * not a whole multiple of either.
 */
class FrameChunkerTest {

    private fun ramp(size: Int, from: Int = 0) = FloatArray(size) { (from + it).toFloat() }

    private fun collect(
        chunker: FrameChunker,
        samples: FloatArray,
        channelCount: Int,
    ): List<FloatArray> = buildList {
        chunker.accept(samples, channelCount) { chunk, _ -> add(chunk) }
    }

    @Test
    fun `an exact chunk passes straight through`() {
        val chunker = FrameChunker(framesPerChunk = 4)
        val chunks = collect(chunker, ramp(8), channelCount = 2)

        assertEquals(1, chunks.size)
        assertContentEquals(ramp(8), chunks[0])
    }

    @Test
    fun `an oversized buffer is split into whole chunks`() {
        // The iOS case: asked for 1024 frames, handed 4800.
        val chunker = FrameChunker(framesPerChunk = 1024)
        val chunks = collect(chunker, ramp(4800), channelCount = 1)

        assertEquals(4, chunks.size)
        chunks.forEach { assertEquals(1024, it.size) }
        assertContentEquals(ramp(1024), chunks[0])
        assertContentEquals(ramp(1024, from = 3072), chunks[3])
    }

    @Test
    fun `the remainder is carried into the next buffer`() {
        val chunker = FrameChunker(framesPerChunk = 1024)

        // 4800 leaves 704 over; the next buffer completes a chunk from them.
        assertEquals(4, collect(chunker, ramp(4800), 1).size)
        val next = collect(chunker, ramp(4800, from = 4800), 1)

        assertEquals(5, next.size)
        // The carried 704 lead the first chunk, then the new buffer continues
        // from exactly where the previous one stopped.
        assertContentEquals(ramp(1024, from = 4096), next[0])
    }

    @Test
    fun `no sample is lost or repeated across many buffers`() {
        val chunker = FrameChunker(framesPerChunk = 1024)
        val seen = mutableListOf<Float>()
        var produced = 0

        repeat(20) { round ->
            chunker.accept(ramp(4800, from = round * 4800), 1) { chunk, _ ->
                seen += chunk.toList()
            }
            produced += 4800
        }

        // Everything emitted is the original ramp in order, and only the tail
        // shorter than a chunk is still held back.
        assertContentEquals(ramp(seen.size).toList(), seen)
        assertTrue(produced - seen.size < 1024)
    }

    @Test
    fun `a buffer shorter than a chunk emits nothing yet`() {
        val chunker = FrameChunker(framesPerChunk = 1024)

        assertEquals(0, collect(chunker, ramp(512), 1).size)
        // Measuring a half-length window as if it were full is exactly the
        // error the chunker exists to prevent, so it waits.
        assertEquals(1, collect(chunker, ramp(512, from = 512), 1).size)
    }

    @Test
    fun `chunk size follows the channel count`() {
        val chunker = FrameChunker(framesPerChunk = 4)
        val chunks = collect(chunker, ramp(16), channelCount = 2)

        // Four frames of stereo is eight interleaved values, not four.
        assertEquals(2, chunks.size)
        chunks.forEach { assertEquals(8, it.size) }
    }

    @Test
    fun `a channel count change drops the carry rather than interleaving it wrong`() {
        val chunker = FrameChunker(framesPerChunk = 4)

        // Three mono frames: short of a chunk, so they stay as carry.
        assertEquals(0, collect(chunker, ramp(3), channelCount = 1).size)
        // Splicing those onto stereo would misalign every pair after them.
        val stereo = collect(chunker, ramp(8, from = 100), channelCount = 2)

        assertEquals(1, stereo.size)
        assertContentEquals(ramp(8, from = 100), stereo[0])
    }

    @Test
    fun `reset forgets the remainder`() {
        val chunker = FrameChunker(framesPerChunk = 4)

        assertEquals(0, collect(chunker, ramp(3), 1).size)
        chunker.reset()

        // Without the reset the stale 3 would lead this and shift everything.
        val after = collect(chunker, ramp(4, from = 50), 1)
        assertEquals(1, after.size)
        assertContentEquals(ramp(4, from = 50), after[0])
    }

    @Test
    fun `an empty buffer is ignored`() {
        val chunker = FrameChunker(framesPerChunk = 4)
        assertEquals(0, collect(chunker, FloatArray(0), 1).size)
    }

    @Test
    fun `a nonsense channel count is ignored`() {
        val chunker = FrameChunker(framesPerChunk = 4)
        assertEquals(0, collect(chunker, ramp(8), channelCount = 0).size)
    }
}
