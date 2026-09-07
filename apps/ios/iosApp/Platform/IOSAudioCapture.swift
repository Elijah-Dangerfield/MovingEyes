import AVFoundation
import ComposeApp

/// `AVAudioEngine` behind the Kotlin `AudioCaptureHost`.
///
/// Swift owns this because the tap block hands back an `AVAudioPCMBuffer` whose
/// samples are reached through raw pointers, and because selecting the built-in
/// stereo data source needs `setPreferredPolarPattern` on an
/// `AVAudioSessionDataSourceDescription` — both markedly cleaner here than from
/// Kotlin/Native. See `docs/swift-kotlin-communication-patterns.md`.
///
/// Nothing is recorded or retained: each buffer is converted, handed to Kotlin,
/// and dropped.
final class IOSAudioCapture: AudioCaptureHost {

    private let engine = AVAudioEngine()
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
            configureStereoIfAvailable(session)
        } catch {
            return false
        }

        let input = engine.inputNode
        let format = input.outputFormat(forBus: 0)
        guard format.sampleRate > 0, format.channelCount > 0 else { return false }

        let channelCount = Int(format.channelCount)
        input.installTap(onBus: 0, bufferSize: 1024, format: format) { buffer, _ in
            guard let channels = buffer.floatChannelData else { return }
            let frames = Int(buffer.frameLength)
            guard frames > 0 else { return }

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
            onFrames(interleaved, KotlinInt(int: Int32(channelCount)))
        }

        do {
            engine.prepare()
            try engine.start()
            isRunning = true
            return true
        } catch {
            input.removeTap(onBus: 0)
            return false
        }
    }

    func stop() {
        guard isRunning else { return }
        engine.inputNode.removeTap(onBus: 0)
        engine.stop()
        isRunning = false
        // Deactivating hands the audio session back, so music the user was
        // playing resumes instead of staying ducked.
        try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
    }

    /// Direction needs two channels. Only newer iPhones expose a stereo built-in
    /// mic, and asking on hardware that doesn't throws rather than degrading, so
    /// every step is optional and a failure just leaves us in mono — which the
    /// analyser already treats as "direction unknown".
    private func configureStereoIfAvailable(_ session: AVAudioSession) {
        guard #available(iOS 14.0, *),
              let input = session.availableInputs?.first(where: { $0.portType == .builtInMic }),
              let source = input.dataSources?.first(where: {
                  $0.supportedPolarPatterns?.contains(.stereo) == true
              })
        else { return }

        try? source.setPreferredPolarPattern(.stereo)
        try? input.setPreferredDataSource(source)
        try? session.setPreferredInput(input)
    }
}
