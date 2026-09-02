@file:Suppress("MagicNumber")

package com.dangerfield.movingeyes.libraries.reactivity.impl

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.dangerfield.movingeyes.libraries.core.logging.KLog
import com.dangerfield.movingeyes.libraries.reactivity.AudioCapture
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import kotlin.concurrent.thread

/**
 * `AudioRecord` on its own thread.
 *
 * Stereo is requested but not required — plenty of hardware only ever gives
 * mono, and the analyser already treats an unknown direction as a first-class
 * case, so falling back is better than refusing to run.
 *
 * `UNPROCESSED` is preferred over `MIC` where the device supports it: the usual
 * voice processing chain applies AGC and noise suppression, which is exactly
 * the processing that flattens the onsets this feature exists to detect.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class AndroidAudioCapture : AudioCapture {

    private val logger = KLog.withTag("Reactivity")

    private var record: AudioRecord? = null
    private var worker: Thread? = null

    @Volatile
    private var running = false

    /**
     * The permission is checked by the caller before we get here — see
     * `MicrophoneReactivitySource` — and `AudioRecord` throws rather than
     * returning null if it's missing, which the runCatching below turns into a
     * false.
     */
    @SuppressLint("MissingPermission")
    override fun start(onFrames: (FloatArray, Int) -> Unit): Boolean {
        if (running) return true

        val opened = openRecorder() ?: return false
        val (recorder, channelCount) = opened
        record = recorder
        running = true

        val bufferFrames = FramesPerBuffer * channelCount
        worker = thread(name = "moving-eyes-audio", isDaemon = true) {
            val buffer = ShortArray(bufferFrames)
            val samples = FloatArray(bufferFrames)
            while (running) {
                val read = recorder.read(buffer, 0, buffer.size)
                if (read <= 0) continue
                for (index in 0 until read) {
                    samples[index] = buffer[index] / ShortScale
                }
                onFrames(samples.copyOf(read), channelCount)
            }
        }
        return true
    }

    override fun stop() {
        running = false
        worker?.join(StopTimeoutMillis)
        worker = null
        runCatching {
            record?.stop()
            record?.release()
        }.onFailure { logger.e(it) { "Failed to release the recorder" } }
        record = null
    }

    /** Tries stereo, then mono. Returns the recorder and how many channels it
     *  actually opened with. */
    @SuppressLint("MissingPermission")
    private fun openRecorder(): Pair<AudioRecord, Int>? {
        listOf(
            AudioFormat.CHANNEL_IN_STEREO to 2,
            AudioFormat.CHANNEL_IN_MONO to 1,
        ).forEach { (mask, channels) ->
            val minimum = AudioRecord.getMinBufferSize(SampleRate, mask, AudioFormat.ENCODING_PCM_16BIT)
            if (minimum <= 0) return@forEach

            val recorder = runCatching {
                AudioRecord(
                    MediaRecorder.AudioSource.UNPROCESSED,
                    SampleRate,
                    mask,
                    AudioFormat.ENCODING_PCM_16BIT,
                    maxOf(minimum, FramesPerBuffer * channels * BytesPerSample * 2),
                )
            }.getOrNull() ?: return@forEach

            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                recorder.release()
                return@forEach
            }

            return runCatching {
                recorder.startRecording()
                recorder to channels
            }.getOrElse {
                logger.e(it) { "Could not start recording at $channels channel(s)" }
                recorder.release()
                null
            }
        }

        logger.e { "No usable microphone configuration" }
        return null
    }

    private companion object {
        const val SampleRate = 44_100

        /** ~23ms at 44.1kHz. Short enough to catch an onset, long enough that
         *  the RMS is stable. */
        const val FramesPerBuffer = 1024

        const val BytesPerSample = 2
        const val ShortScale = 32_768f
        const val StopTimeoutMillis = 500L
    }
}
