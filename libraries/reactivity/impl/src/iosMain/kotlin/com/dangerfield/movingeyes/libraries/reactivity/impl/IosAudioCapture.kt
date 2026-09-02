package com.dangerfield.movingeyes.libraries.reactivity.impl

import com.dangerfield.movingeyes.libraries.reactivity.AudioCapture
import com.dangerfield.movingeyes.libraries.reactivity.AudioCaptureHost
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

/** Delegates to the Swift `IOSAudioCapture`. See [AudioCaptureHost]. */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class IosAudioCapture(
    private val host: AudioCaptureHost,
) : AudioCapture {

    override fun start(onFrames: (FloatArray, Int) -> Unit): Boolean = host.start(onFrames)

    override fun stop() = host.stop()
}
