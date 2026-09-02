package com.dangerfield.movingeyes.libraries.reactivity.impl

import com.dangerfield.movingeyes.libraries.core.logging.KLog
import com.dangerfield.movingeyes.libraries.movingeyes.Permission
import com.dangerfield.movingeyes.libraries.movingeyes.PermissionManager
import com.dangerfield.movingeyes.libraries.movingeyes.PermissionResult
import com.dangerfield.movingeyes.libraries.reactivity.AudioAnalyzer
import com.dangerfield.movingeyes.libraries.reactivity.AudioCapture
import com.dangerfield.movingeyes.libraries.reactivity.ReactivitySource
import com.dangerfield.movingeyes.libraries.reactivity.SoundEvent
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

/**
 * Microphone in, [SoundEvent]s out.
 *
 * ## The privacy rule, which is not negotiable
 *
 * Audio is analysed in the callback and dropped. Nothing is recorded, nothing
 * is buffered past the window being measured, and nothing leaves the device.
 * The only thing that outlives a buffer is a running average of loudness. Keep
 * it that way: a Halloween decoration that uploads audio is a news story.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class MicrophoneReactivitySource(
    private val capture: AudioCapture,
    private val permissions: PermissionManager,
) : ReactivitySource {

    private val logger = KLog.withTag("Reactivity")
    private val analyzer = AudioAnalyzer()

    private val _events = MutableSharedFlow<SoundEvent>(
        extraBufferCapacity = 1,
        // A missed startle is better than a queue of stale ones firing at once
        // after the renderer catches up.
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val events: Flow<SoundEvent> = _events.asSharedFlow()

    override suspend fun start(): Boolean {
        val result = permissions.ensurePermission(Permission.Microphone)
        if (result !is PermissionResult.Granted) {
            logger.d { "Microphone denied; reactivity stays off" }
            return false
        }

        analyzer.reset()
        val started = capture.start { samples, channelCount ->
            analyzer.process(samples, channelCount)?.let(_events::tryEmit)
        }
        logger.d { "Reactivity ${if (started) "listening" else "could not open the microphone"}" }
        return started
    }

    override fun stop() {
        capture.stop()
        analyzer.reset()
    }
}
