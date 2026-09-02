@file:OptIn(ExperimentalObjCName::class)

package com.dangerfield.movingeyes.libraries.reactivity

import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * The Swift side of [AudioCapture].
 *
 * A Swift Twin rather than a Kotlin Twin, per
 * `docs/swift-kotlin-communication-patterns.md`: `AVAudioEngine`'s tap block
 * hands back an `AVAudioPCMBuffer` whose samples are reached through raw
 * pointers, and selecting the built-in stereo data source needs
 * `setPreferredPolarPattern` on an `AVAudioSessionDataSourceDescription`. Both
 * are ordinary Swift and both are pointer-wrangling from Kotlin/Native, so
 * Swift owns the implementation and Kotlin holds the abstraction.
 *
 * Android has no binding for this — `AndroidAudioCapture` implements
 * [AudioCapture] directly.
 *
 * Plain callbacks and no suspend functions: a Kotlin suspend function reaches
 * Swift as a completion handler that must be called exactly once, and an audio
 * callback is the worst place to get that wrong.
 */
@ObjCName("AudioCaptureHost", exact = true)
interface AudioCaptureHost {

    /**
     * [onFrames] is called from the audio thread with interleaved samples in
     * -1..1. Returns false when the engine could not be started.
     */
    fun start(onFrames: (FloatArray, Int) -> Unit): Boolean

    fun stop()
}
