//
//  PermissionManager.swift
//  iosApp
//
//  iOS permission handling. Every case of `Permission` (commonMain) must be
//  handled in both switches below; Swift's exhaustiveness check is the only
//  thing that catches a case added on the Kotlin side, and it only fires when
//  someone actually builds the iOS app.
//
import AVFoundation
import ComposeApp
import Foundation
import UserNotifications
import UIKit

class IOSPermissionManager: PermissionManager {

    func openAppSettings() {
        if let url = URL(string: UIApplication.openSettingsURLString) {
            UIApplication.shared.open(url)
        }
    }

    func __ensurePermission(permission: Permission) async throws -> PermissionResult {
        let currentStatus = checkPermissionStatus(permission: permission)

        if currentStatus == .granted {
            return PermissionResultGranted.shared
        }

        return try await __requestPermission(permission: permission)
    }

    func __requestPermission(permission: Permission) async throws -> PermissionResult {
        switch onEnum(of: permission) {
        case .notifications:
            return await requestNotificationsPermission()
        case .microphone:
            return await requestMicrophonePermission()
        }
    }

    func checkPermissionStatus(permission: Permission) -> PermissionStatus {
        switch onEnum(of: permission) {
        case .notifications:
            return checkNotificationsStatus()
        case .microphone:
            return checkMicrophoneStatus()
        }
    }

    // MARK: - Notifications

    private func checkNotificationsStatus() -> PermissionStatus {
        var status: PermissionStatus = .notDetermined
        let semaphore = DispatchSemaphore(value: 0)

        UNUserNotificationCenter.current().getNotificationSettings { settings in
            switch settings.authorizationStatus {
            case .notDetermined:
                status = .notDetermined
            case .authorized, .provisional, .ephemeral:
                status = .granted
            case .denied:
                status = .denied
            @unknown default:
                status = .notDetermined
            }
            semaphore.signal()
        }

        semaphore.wait()
        return status
    }

    private func requestNotificationsPermission() async -> PermissionResult {
        do {
            let granted = try await UNUserNotificationCenter.current()
                .requestAuthorization(options: [.alert, .badge, .sound])

            if granted {
                return PermissionResultGranted.shared
            } else {
                let settings = await UNUserNotificationCenter.current().notificationSettings()
                let canRequestAgain = settings.authorizationStatus == .notDetermined
                return PermissionResultDenied(canRequestAgain: canRequestAgain)
            }
        } catch {
            return PermissionResultDenied(canRequestAgain: true)
        }
    }

    // MARK: - Microphone

    private func checkMicrophoneStatus() -> PermissionStatus {
        switch AVAudioApplication.shared.recordPermission {
        case .undetermined:
            return .notDetermined
        case .granted:
            return .granted
        case .denied:
            return .denied
        @unknown default:
            return .notDetermined
        }
    }

    private func requestMicrophonePermission() async -> PermissionResult {
        let granted = await AVAudioApplication.requestRecordPermission()
        if granted {
            return PermissionResultGranted.shared
        }
        // iOS asks once and never again. Unlike notifications there is no
        // provisional state to recover from, so the only route back is
        // Settings, which is what canRequestAgain: false tells the caller.
        return PermissionResultDenied(canRequestAgain: false)
    }
}
