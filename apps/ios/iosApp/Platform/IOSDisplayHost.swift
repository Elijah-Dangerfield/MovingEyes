import SwiftUI
import UIKit
import ComposeApp

/// The parts of display mode that Kotlin/Native cannot reach.
///
/// Hiding the status bar and the home indicator, and freezing rotation, are all
/// driven on iOS by view-controller overrides and scene geometry requests —
/// there is no imperative UIKit call for any of them, which is why the Kotlin
/// `DisplayHost` interface exists at all. Keep-awake and brightness are plain
/// properties and are set directly from `IosDisplayController`.
///
/// This publishes flags rather than subclassing the Compose view controller.
/// `MainViewController` is constructed by Kotlin, so wrapping it would mean a
/// container controller purely to override two computed properties; SwiftUI's
/// `.statusBarHidden` and `.persistentSystemOverlays` do the same job from the
/// outside and stay correct if Compose changes what it returns.
final class IOSDisplayHost: ObservableObject, DisplayHost {

    @Published private(set) var chromeHidden = false

    func setChromeHidden(hidden: Bool) {
        // Kotlin calls this from whatever thread the effect ran on, and a
        // @Published mutation has to land on the main actor.
        DispatchQueue.main.async { self.chromeHidden = hidden }
    }

    /// Freezes rotation at whatever is on screen right now.
    ///
    /// `requestGeometryUpdate` with the *current* orientation is what pins it;
    /// naming a specific orientation would spin a device that had been mounted
    /// sideways. Releasing asks for `.all` again and then clears the
    /// `AppDelegate` mask so the system takes over.
    func setOrientationLocked(locked: Bool) {
        DispatchQueue.main.async {
            guard let scene = UIApplication.shared.connectedScenes
                .first(where: { $0.activationState == .foregroundActive }) as? UIWindowScene
            else { return }

            let mask: UIInterfaceOrientationMask = locked
                ? Self.mask(for: scene.interfaceOrientation)
                : .all

            AppDelegate.orientationMask = mask
            scene.requestGeometryUpdate(.iOS(interfaceOrientations: mask))
            // Without this the controller keeps the mask it last resolved and
            // the lock only takes effect on the next rotation attempt.
            UIViewController.attemptRotationToDeviceOrientation()
        }
    }

    private static func mask(for orientation: UIInterfaceOrientation) -> UIInterfaceOrientationMask {
        switch orientation {
        case .portrait: return .portrait
        case .portraitUpsideDown: return .portraitUpsideDown
        case .landscapeLeft: return .landscapeLeft
        case .landscapeRight: return .landscapeRight
        default: return .all
        }
    }
}

/// Holds the orientation mask the window scene should honour.
///
/// iOS asks the app delegate, not the view, so the lock has to be readable from
/// here even though it is set from `IOSDisplayHost`.
final class AppDelegate: NSObject, UIApplicationDelegate {

    static var orientationMask: UIInterfaceOrientationMask = .all

    func application(
        _ application: UIApplication,
        supportedInterfaceOrientationsFor window: UIWindow?
    ) -> UIInterfaceOrientationMask {
        AppDelegate.orientationMask
    }
}
