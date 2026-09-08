import AVFoundation
import ComposeApp
import OSLog

/// `AVAudioEngine` behind the Kotlin `AudioCaptureHost`.
///
/// Swift owns this because the tap block hands back an `AVAudioPCMBuffer` whose
/// samples are reached through raw pointers, and because selecting the built-in
/// stereo data source needs `setPreferredPolarPattern` on an
/// `AVAudioSessionDataSourceDescription`. Both are markedly cleaner here than
/// from Kotlin/Native. See `docs/swift-kotlin-communication-patterns.md`.
///
/// Nothing is recorded or retained: each buffer is converted, handed to Kotlin,
/// and dropped.
final class IOSAudioCapture: AudioCaptureHost {

    private let engine = AVAudioEngine()
    private let log = Logger(subsystem: "com.dangerfield.movingeyes", category: "Reactivity")
    private var isRunning = false

    // Kotlin's `(FloatArray, Int) -> Unit` exports with the Int *boxed*, so the
    // parameter is KotlinInt and not Int32. Declaring Int32 compiles as a
    // perfectly good method that simply does not satisfy the protocol.
    func start(onFrames: @escaping (KotlinFloatArray, KotlinInt) -> Void) -> Bool {
        guard !isRunning else { return true }

        let session = AVAudioSession.sharedInstance()
        do {
            // `.measurement` skips the voice-processing chain. The usual mode
            // applies AGC and noise suppression, which flattens exactly the
            // onsets this feature detects.
            try session.setCategory(.record, mode: .measurement, options: [])
            try session.setActive(true)
        } catch {
            log.error("Could not activate the audio session: \(error.localizedDescription)")
            releaseSession()
            return false
        }

        configureStereoIfAvailable(session)

        // inputNode is a lazy property, and reading it is what attaches the
        // node to the graph. Nothing may call prepare() before this line:
        // prepare() initialises the graph and hard-asserts that something is
        // attached to it, so an empty graph terminates the process with
        // "required condition is false: inputNode != nullptr || outputNode !=
        // nullptr". prepare() therefore stays down beside start(), where it
        // always was.
        let input = engine.inputNode
        let format = input.inputFormat(forBus: 0)
        guard format.sampleRate > 0, format.channelCount > 0 else {
            log.error("Input format unusable: \(format.sampleRate) Hz, \(format.channelCount) ch")
            releaseSession()
            return false
        }
        log.info("Microphone open: \(format.sampleRate) Hz, \(format.channelCount) ch")

        // Captured by the tap rather than reached through self, so nothing on
        // the audio thread retains the instance. Both are per-start(), which is
        // also what makes the flag safe: the tap is its only writer.
        let tapLog = log
        var hasLoggedFirstBuffer = false

        // nil, not the format read above. installTap raises an ObjC exception
        // when what it is handed disagrees with the node, and Swift cannot
        // catch that, so a route change landing between the read and the
        // install would be a crash rather than a bad reading. nil means "use
        // the bus's own format", which cannot disagree with itself; the read
        // above earns its keep as the guard and the log line.
        input.installTap(onBus: 0, bufferSize: 1024, format: nil) { buffer, _ in
            guard let channels = buffer.floatChannelData else { return }
            let frames = Int(buffer.frameLength)
            // Read the width off the buffer rather than the format captured
            // above. A route change (headset in, phone docked) re-formats the
            // node mid-session, and sizing the array from a stale channel
            // count either truncates the audio or reads past the planes.
            let channelCount = Int(buffer.format.channelCount)
            guard frames > 0, channelCount > 0 else { return }

            // AVAudioEngine hands back non-interleaved planes; Kotlin's
            // analyser expects interleaved, so this is the one place the two
            // conventions meet.
            let interleaved = KotlinFloatArray(size: Int32(frames * channelCount))
            for frame in 0..<frames {
                for channel in 0..<channelCount {
                    interleaved.set(
                        index: Int32(frame * channelCount + channel),
                        value: channels[channel][frame]
                    )
                }
            }
            // One line the first time audio actually arrives, so "the engine
            // started" and "buffers are flowing" stop being the same claim.
            if !hasLoggedFirstBuffer {
                hasLoggedFirstBuffer = true
                tapLog.info("First audio buffer: \(frames) frames, \(channelCount) ch")
            }
            onFrames(interleaved, KotlinInt(int: Int32(channelCount)))
        }

        do {
            engine.prepare()
            try engine.start()
            isRunning = true
            return true
        } catch {
            log.error("Engine would not start: \(error.localizedDescription)")
            input.removeTap(onBus: 0)
            releaseSession()
            return false
        }
    }

    func stop() {
        guard isRunning else { return }
        engine.inputNode.removeTap(onBus: 0)
        engine.stop()
        isRunning = false
        releaseSession()
    }

    /// Handing the session back is what lets music the user was playing resume
    /// instead of staying ducked, and what clears the OS microphone indicator.
    /// Every failure path above needs it too, not just `stop()`.
    private func releaseSession() {
        try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
    }

    /// Direction needs two channels. Only newer iPhones expose a stereo built-in
    /// mic, and asking on hardware that doesn't throws rather than degrading, so
    /// every step is optional and a failure just leaves us in mono, which the
    /// analyser already treats as "direction unknown".
    private func configureStereoIfAvailable(_ session: AVAudioSession) {
        guard let input = session.availableInputs?.first(where: { $0.portType == .builtInMic }),
              let source = input.dataSources?.first(where: {
                  $0.supportedPolarPatterns?.contains(.stereo) == true
              })
        else { return }

        try? source.setPreferredPolarPattern(.stereo)
        try? input.setPreferredDataSource(source)
        try? session.setPreferredInput(input)
        // The polar pattern alone does not widen the input. Without asking for
        // the channel count too the node still reports mono and the whole
        // stereo path is dead code.
        if session.maximumInputNumberOfChannels >= 2 {
            try? session.setPreferredInputNumberOfChannels(2)
        }
    }
}
