package com.dangerfield.movingeyes.libraries.reactivity

import kotlinx.coroutines.flow.Flow

/**
 * Something in the room that the eyes should react to.
 *
 * An interface rather than a microphone class so camera face-tracking can drop
 * in later without the behaviour engine learning what a microphone is.
 */
interface ReactivitySource {

    val events: Flow<SoundEvent>

    /**
     * Returns whether it actually started. Denied permission is a false, not an
     * exception: everything else in the app still works, and scheduled scares
     * take over.
     */
    suspend fun start(): Boolean

    fun stop()
}

/**
 * @param direction -1 fully left, 0 ahead, +1 fully right
 * @param intensity 0..1, how far above the room's noise floor this was
 * @param isDirectionKnown false when the direction was guessed — see
 *   [AudioAnalyzer] for when that happens
 */
data class SoundEvent(
    val direction: Float,
    val intensity: Float,
    val isDirectionKnown: Boolean,
)

/**
 * Raw audio, in whatever the platform gives us.
 *
 * Android implements this in Kotlin; iOS hands it over from Swift. See
 * `docs/swift-kotlin-communication-patterns.md` — this is a Kotlin Twin on one
 * side and a Swift Twin on the other, because `AVAudioEngine`'s tap block and
 * the stereo data-source selection are both markedly cleaner in Swift.
 */
interface AudioCapture {

    /**
     * [onFrames] receives interleaved samples in -1..1 and is called on an
     * audio thread. It must not block.
     */
    fun start(onFrames: (samples: FloatArray, channelCount: Int) -> Unit): Boolean

    fun stop()
}
