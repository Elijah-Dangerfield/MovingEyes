import SwiftUI
import UIKit
import ComposeApp

@main
struct iOSApp: App {

    let permissionManager = IOSPermissionManager()
    let reviewLauncher = IOSReviewLauncher()
    let storeKitCoordinator = IOSStoreKitCoordinator()
    @ObservedObject private var displayHost = IOSDisplayHost()
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate
    private let iOSAppComponent: IosAppComponent

    init() {
        self.iOSAppComponent = create(
            permissionManager: permissionManager,
            reviewLauncher: reviewLauncher,
            storeKitCoordinator: storeKitCoordinator,
            displayHost: displayHost
        )
        iOSAppComponent.telemetry.initialize()
        // Construct every @AutoInit singleton up front — resolving the set is
        // what forces construction (AppEventDispatcher's lifecycle attach etc.).
        _ = iOSAppComponent.autoInits
    }

    var body: some Scene {
        WindowGroup {
            ComposeView(appComponent: iOSAppComponent)
                .ignoresSafeArea()
                // Display mode hides both. Driven from Kotlin through
                // `IOSDisplayHost` — see `DisplayHost` for why these two can't
                // be set from Kotlin/Native directly.
                .statusBarHidden(displayHost.chromeHidden)
                .persistentSystemOverlays(displayHost.chromeHidden ? .hidden : .automatic)
                .onOpenURL { url in
                    // Forward URLs from custom-scheme links and Universal Links
                    // into the Kotlin DeepLinkBridge — App.kt collects from it
                    // and calls navController.handleDeepLink.
                    iOSAppComponent.deepLinkBridge.emit(url: url.absoluteString)
                }
        }
    }
}

struct ComposeView: UIViewControllerRepresentable {
    let appComponent: IosAppComponent

    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController(appComponent: appComponent)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
