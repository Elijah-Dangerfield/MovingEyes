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
 * Source and channel layout are both negotiated rather than assumed — see
 * [openRecorder], which is where the interesting part is.
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

    /**
     * Walks every source and channel layout until one actually records.
     *
     * Two fallbacks, both earned:
     *
     * **Source.** `UNPROCESSED` is preferred because the voice chain's AGC and
     * noise suppression flatten exactly the onsets this feature detects — but a
     * great many devices don't implement it, and the failure is not always an
     * exception. `MIC` is the universally supported source and a processed
     * onset beats no onset at all.
     *
     * **Channels.** Plenty of hardware only ever gives mono, and the analyser
     * already treats an unknown direction as a first-class case, so falling
     * back is better than refusing to run.
     *
     * Written as a loop that *continues* on failure rather than returning. The
     * previous version used `return` inside a `forEach`, which is a non-local
     * return from this whole function, so the first configuration that opened
     * but failed to start took every remaining fallback down with it.
     */
    @SuppressLint("MissingPermission")
    private fun openRecorder(): Pair<AudioRecord, Int>? {
        val sources = listOf(MediaRecorder.AudioSource.UNPROCESSED, MediaRecorder.AudioSource.MIC)
        val layouts = listOf(AudioFormat.CHANNEL_IN_STEREO to 2, AudioFormat.CHANNEL_IN_MONO to 1)

        for (source in sources) {
            for ((mask, channels) in layouts) {
                val opened = tryOpen(source, mask, channels)
                if (opened != null) {
                    logger.d { "Microphone open: source $source, $channels channel(s)" }
                    return opened
                }
            }
        }

        logger.e { "No usable microphone configuration" }
        return null
    }

    @SuppressLint("MissingPermission")
    private fun tryOpen(source: Int, mask: Int, channels: Int): Pair<AudioRecord, Int>? {
        val minimum = AudioRecord.getMinBufferSize(SampleRate, mask, AudioFormat.ENCODING_PCM_16BIT)
        if (minimum <= 0) return null

        val recorder = runCatching {
            AudioRecord(
                source,
                SampleRate,
                mask,
                AudioFormat.ENCODING_PCM_16BIT,
                maxOf(minimum, FramesPerBuffer * channels * BytesPerSample * 2),
            )
        }.getOrNull() ?: return null

        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            return null
        }

        return runCatching {
            recorder.startRecording()
            check(recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                "startRecording() returned without recording"
            }
            recorder to channels
        }.getOrElse {
            recorder.release()
            null
        }
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
